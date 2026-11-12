package com.lemon.erp.modules.ecom.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 在独立的新事务中执行一步（演示数据中会逐条处理、一条失败不影响其他的作业，如审核、打单、发货、退货）。
 * 调用方所在的组成部分事务本身不做写操作，避免与新事务互相等待锁
 */
@Slf4j
@Component
public class DemoTx {

    private final TransactionTemplate requiresNew;

    public DemoTx(PlatformTransactionManager transactionManager) {
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 在新事务中查询（不受调用方事务中 MyBatis 一级缓存的影响，能读到其他新事务刚提交的数据） */
    public <T> T read(java.util.function.Supplier<T> query) {
        return requiresNew.execute(s -> query.get());
    }

    /** @return 是否成功；失败只记日志 */
    public boolean run(String step, Runnable action) {
        try {
            requiresNew.executeWithoutResult(s -> action.run());
            return true;
        } catch (RuntimeException e) {
            log.warn("Demo data step [{}] failed: {}", step, e.getMessage());
            return false;
        }
    }
}
