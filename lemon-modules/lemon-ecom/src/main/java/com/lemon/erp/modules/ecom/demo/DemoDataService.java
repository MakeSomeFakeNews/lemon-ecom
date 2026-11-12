package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomStoreMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示数据：收集全部 {@link DemoContributor} 按顺序生成一个跨境卖家的完整业务数据（当前租户）。
 * <ul>
 *   <li>每个组成部分在自己的事务中执行，失败只记录日志和进度、不影响后续部分</li>
 *   <li>已经生成过（存在 DEMO- 开头的店铺）或正在生成时拒绝再次生成</li>
 *   <li>进度按租户保存在内存中（单实例），页面轮询 {@link #status()}</li>
 * </ul>
 * 调用方负责租户上下文和登录用户（制单人、部门），见 {@link DemoDataJob}。
 */
@Slf4j
@Service
public class DemoDataService {

    /** 历史天数 */
    public static final int DAYS = 120;
    /** 固定随机种子：每次生成的数据一致 */
    public static final long SEED = 20261111L;

    private final List<DemoContributor> contributors;
    private final TransactionTemplate transactionTemplate;
    private final EcomStoreMapper storeMapper;
    private final Map<Long, Progress> progress = new ConcurrentHashMap<>();

    public DemoDataService(List<DemoContributor> contributors, TransactionTemplate transactionTemplate, EcomStoreMapper storeMapper) {
        this.contributors = contributors.stream().sorted(Comparator.comparingInt(DemoContributor::order)).toList();
        this.transactionTemplate = transactionTemplate;
        this.storeMapper = storeMapper;
    }

    /** 当前租户是否已经生成过演示数据 */
    public boolean generated() {
        return storeMapper.exists(Wrappers.<EcomStore>lambdaQuery().likeRight(EcomStore::getCode, DemoKeys.STORE_CODE_PREFIX));
    }

    /** 当前租户的生成进度 */
    public DemoStatus status() {
        Progress p = progress.get(TenantContext.currentOrPlatform());
        boolean generated = generated();
        if (p == null) {
            return new DemoStatus(generated, false, null, 0, contributors.size(), List.of(), null, null);
        }
        synchronized (p) {
            return new DemoStatus(generated, p.running, p.current, p.done, p.total, List.copyOf(p.failures), p.startTime, p.endTime);
        }
    }

    /**
     * 占用当前租户的生成资格：已生成或正在生成时报错。成功后必须调用 {@link #run()}
     */
    public void begin() {
        Long tenantId = TenantContext.currentOrPlatform();
        synchronized (progress) {
            Progress old = progress.get(tenantId);
            if (old != null && old.running) {
                throw new BizException("ecom.error.demo.running");
            }
            if (generated()) {
                throw new BizException("ecom.error.demo.generated");
            }
            Progress p = new Progress();
            p.total = contributors.size();
            p.running = true;
            p.startTime = LocalDateTime.now();
            progress.put(tenantId, p);
        }
    }

    /** 生成（同步执行）：先 {@link #begin()} 再调用；用于异步任务、启动时自动生成和测试 */
    public DemoStatus run() {
        Long tenantId = TenantContext.currentOrPlatform();
        Progress p = progress.get(tenantId);
        if (p == null || !p.running) {
            throw new IllegalStateException("begin() first"); // i18n-ignore 内部错误
        }
        DemoContext ctx = new DemoContext(SEED, LocalDate.now(), DAYS);
        long all = System.currentTimeMillis();
        try {
            for (DemoContributor c : contributors) {
                synchronized (p) {
                    p.current = c.key();
                }
                long start = System.currentTimeMillis();
                try {
                    transactionTemplate.executeWithoutResult(s -> c.generate(ctx));
                    log.info("Demo data [{}] done in {} ms", c.key(), System.currentTimeMillis() - start);
                } catch (RuntimeException e) {
                    log.warn("Demo data [{}] failed", c.key(), e);
                    synchronized (p) {
                        p.failures.add(new DemoStatus.Failure(c.key(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                    }
                }
                synchronized (p) {
                    p.done++;
                }
            }
        } finally {
            synchronized (p) {
                p.running = false;
                p.current = null;
                p.endTime = LocalDateTime.now();
            }
            log.info("Demo data generated for tenant {} in {} ms, {} failure(s)", tenantId, System.currentTimeMillis() - all, p.failures.size());
        }
        return status();
    }

    /** 结束占用但没有执行（如提交异步任务失败） */
    public void abort(String message) {
        Progress p = progress.get(TenantContext.currentOrPlatform());
        if (p != null) {
            synchronized (p) {
                p.running = false;
                p.endTime = LocalDateTime.now();
                p.failures.add(new DemoStatus.Failure("start", message));
            }
        }
    }

    private static final class Progress {
        private boolean running;
        private String current;
        private int done;
        private int total;
        private final List<DemoStatus.Failure> failures = new ArrayList<>();
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
