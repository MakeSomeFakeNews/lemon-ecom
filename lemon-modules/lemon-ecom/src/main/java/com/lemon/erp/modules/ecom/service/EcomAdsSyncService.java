package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.ads.AdsCapability;
import com.lemon.erp.modules.ecom.ads.AdsConnector;
import com.lemon.erp.modules.ecom.ads.AdsContext;
import com.lemon.erp.modules.ecom.ads.AdsReportRow;
import com.lemon.erp.modules.ecom.ads.AdsStructure;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 广告接口同步：拉取结构（活动 / 广告组 / 投放）与报告（活动、推广商品、投放、搜索词），平台调用不在事务中，拉取后由
 * {@link EcomAdIngestService} 在事务中写入。报告按连接器声明的最长天数分段请求。手工同步在后台线程执行（报告是异步生成的，
 * 可能要等几分钟），结果记在店铺的广告同步时间 / 失败原因上
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomAdsSyncService {

    /** 默认同步最近 3 天（亚马逊广告归因数据在 3 天内仍会变化） */
    public static final int DEFAULT_DAYS = 3;
    /** 手工同步最多回溯的天数 */
    static final int MAX_DAYS = 60;

    private final EcomAdsAuthService authService;
    private final EcomAdIngestService ingestService;
    private final EcomStoreService storeService;

    /** 只同步结构 */
    public AdsViews.SyncResult syncStructure(Long storeId) {
        EcomStore store = storeService.requireEnabled(storeId);
        AdsConnector c = authService.requireConnector(store);
        if (!c.supports(AdsCapability.STRUCTURE)) {
            throw c.unsupported();
        }
        AdsStructure s = c.fetchStructure(authService.requireContext(store));
        return ingestService.applyStructure(store, s);
    }

    /** 同步结构与最近 days 天（截至昨天）的报告，结果写在店铺上 */
    public AdsViews.SyncResult sync(Long storeId, int days) {
        EcomStore store = storeService.requireEnabled(storeId);
        LocalDateTime start = LocalDateTime.now();
        try {
            AdsViews.SyncResult r = syncStore(store, Math.max(1, Math.min(days, MAX_DAYS)));
            authService.markSynced(storeId, start, null);
            return r;
        } catch (RuntimeException e) {
            if (!(e instanceof BizException)) {
                log.warn("Ads sync failed for store {}", store.getCode(), e);
            }
            authService.markSynced(storeId, start, e.getMessage());
            throw e;
        }
    }

    /** 手工同步（后台执行） */
    @Async
    public void syncAsync(Long storeId, int days) {
        try {
            sync(storeId, days);
        } catch (RuntimeException e) {
            // 失败原因已记在店铺上
            log.debug("Ads sync failed: {}", e.getMessage());
        }
    }

    /** 定时任务：全部有广告授权、开启自动同步的店铺，单个失败不影响其他 */
    public List<String> syncAll(int days) {
        List<String> out = new ArrayList<>();
        for (EcomStore s : authService.syncable()) {
            try {
                AdsViews.SyncResult r = sync(s.getId(), days);
                out.add(s.getName() + ": " + Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.syncSummary", r.campaigns(), r.adGroups(), r.targets(),
                        r.reportRows().values().stream().mapToInt(Integer::intValue).sum()));
            } catch (RuntimeException e) {
                out.add(s.getName() + ": " + e.getMessage());
            }
        }
        return out;
    }

    private AdsViews.SyncResult syncStore(EcomStore store, int days) {
        AdsConnector c = authService.requireConnector(store);
        AdsContext ctx = authService.requireContext(store);
        AdsViews.SyncResult structure = new AdsViews.SyncResult(0, 0, 0, Map.of(), null);
        if (c.supports(AdsCapability.STRUCTURE)) {
            structure = ingestService.applyStructure(store, c.fetchStructure(ctx));
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (c.supports(AdsCapability.REPORTS) && !c.info().reportTypes().isEmpty()) {
            LocalDate end = LocalDate.now().minusDays(1);
            LocalDate begin = end.minusDays(days - 1L);
            int step = Math.max(1, c.info().maxReportDays());
            Set<EcomAdReportType> types = EnumSet.copyOf(c.info().reportTypes());
            for (LocalDate from = begin; !from.isAfter(end); from = from.plusDays(step)) {
                LocalDate to = from.plusDays(step - 1L).isAfter(end) ? end : from.plusDays(step - 1L);
                Map<EcomAdReportType, List<AdsReportRow>> reports = c.fetchReports(ctx, types, from, to);
                // 活动、推广商品先写（建立活动 / 广告组），再写投放和搜索词
                for (EcomAdReportType t : EcomAdReportType.values()) {
                    List<AdsReportRow> rows = reports.get(t);
                    if (rows != null && !rows.isEmpty()) {
                        ingestService.ingest(store, t, null, rows);
                        counts.merge(t.name(), rows.size(), Integer::sum);
                    }
                }
            }
        }
        return new AdsViews.SyncResult(structure.campaigns(), structure.adGroups(), structure.targets(), counts, null);
    }
}
