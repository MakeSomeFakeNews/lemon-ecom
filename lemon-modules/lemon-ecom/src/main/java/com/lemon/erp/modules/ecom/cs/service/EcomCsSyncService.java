package com.lemon.erp.modules.ecom.cs.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.cs.connector.CsCapability;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnector;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnectorRegistry;
import com.lemon.erp.modules.ecom.cs.connector.ReviewDraft;
import com.lemon.erp.modules.ecom.cs.connector.ThreadDraft;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客服同步：按店铺的平台找到客服连接器，拉取买家消息 / 商品评价后逐个写入（每个会话 / 评价一个事务，一个失败不影响其他），
 * 每次同步写渠道同步日志（类型 MESSAGES / REVIEWS）。平台接口在事务之外调用；增量起点为上次成功同步的开始时间往前重叠 30 分钟，
 * 第一次同步往前 7 天
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomCsSyncService {

    private final EcomStoreService storeService;
    private final MessageConnectorRegistry registry;
    private final EcomCsSupport support;
    private final EcomCsThreadService threadService;
    private final EcomReviewService reviewService;

    // ---------------------------------------------------------------- 买家消息

    /** 定时任务：所有有授权、平台支持拉取消息的启用店铺 */
    public List<CsViews.SyncOutcome> syncMessagesAll() {
        List<CsViews.SyncOutcome> out = new ArrayList<>();
        for (EcomStore s : storeService.syncable()) {
            if (registry.supports(s.getPlatform(), CsCapability.MESSAGES)) {
                out.add(syncMessages(s, CsConstants.JOB));
            }
        }
        return out;
    }

    /** 手工同步一个店铺的买家消息 */
    public CsViews.SyncOutcome syncMessages(Long storeId) {
        EcomStore store = storeService.requireEnabled(storeId);
        if (!registry.supports(store.getPlatform(), CsCapability.MESSAGES)) {
            throw new BizException("ecom.error.cs.capabilityUnsupported", store.getPlatform().name(), CsCapability.MESSAGES.name());
        }
        return syncMessages(store, CsConstants.MANUAL);
    }

    private CsViews.SyncOutcome syncMessages(EcomStore store, String trigger) {
        LocalDateTime start = LocalDateTime.now();
        MessageConnector connector = registry.find(store.getPlatform()).orElseThrow();
        List<ThreadDraft> drafts;
        try {
            ConnectorContext ctx = support.prepare(store);
            LocalDateTime since = support.since(store.getId(), SyncType.MESSAGES, start, CsConstants.OVERLAP_MINUTES, CsConstants.FIRST_SYNC_DAYS);
            drafts = connector.fetchThreads(ctx, since);
        } catch (RuntimeException e) {
            log.warn("Message sync failed for store {}: {}", store.getCode(), e.getMessage());
            support.log(store.getId(), SyncType.MESSAGES, trigger, start, false, 0, 0, 0, e.getMessage());
            return new CsViews.SyncOutcome(store.getId(), store.getName(), 0, 0, 0, e.getMessage());
        }
        int created = 0, updated = 0;
        List<String> errors = new ArrayList<>();
        for (ThreadDraft d : drafts) {
            try {
                EcomCsThreadService.SaveResult r = threadService.saveThread(store, d, connector.replyHours());
                created += r.newMessages();
                if (r.newMessages() > 0 || r.newThread()) {
                    updated++;
                }
            } catch (RuntimeException e) {
                log.warn("Saving conversation {} of store {} failed", d.conversationId(), store.getCode(), e);
                errors.add(d.conversationId() + ": " + e.getMessage());
            }
        }
        String message = errors.isEmpty() ? null : String.join("; ", errors);
        support.log(store.getId(), SyncType.MESSAGES, trigger, start, errors.isEmpty(), drafts.size(), created, updated, message);
        return new CsViews.SyncOutcome(store.getId(), store.getName(), drafts.size(), created, updated, message);
    }

    // ---------------------------------------------------------------- 评价

    /** 定时任务：所有有授权、平台支持拉取评价的启用店铺 */
    public List<CsViews.SyncOutcome> syncReviewsAll() {
        List<CsViews.SyncOutcome> out = new ArrayList<>();
        for (EcomStore s : storeService.syncable()) {
            if (registry.supports(s.getPlatform(), CsCapability.REVIEWS)) {
                out.add(syncReviews(s, CsConstants.JOB));
            }
        }
        return out;
    }

    /** 手工同步一个店铺的商品评价 */
    public CsViews.SyncOutcome syncReviews(Long storeId) {
        EcomStore store = storeService.requireEnabled(storeId);
        if (!registry.supports(store.getPlatform(), CsCapability.REVIEWS)) {
            throw new BizException("ecom.error.cs.capabilityUnsupported", store.getPlatform().name(), CsCapability.REVIEWS.name());
        }
        return syncReviews(store, CsConstants.MANUAL);
    }

    private CsViews.SyncOutcome syncReviews(EcomStore store, String trigger) {
        LocalDateTime start = LocalDateTime.now();
        MessageConnector connector = registry.find(store.getPlatform()).orElseThrow();
        List<ReviewDraft> drafts;
        try {
            ConnectorContext ctx = support.prepare(store);
            LocalDateTime since = support.since(store.getId(), SyncType.REVIEWS, start, CsConstants.OVERLAP_MINUTES, CsConstants.REQUEST_MAX_DAYS);
            drafts = connector.fetchReviews(ctx, since);
        } catch (RuntimeException e) {
            log.warn("Review sync failed for store {}: {}", store.getCode(), e.getMessage());
            support.log(store.getId(), SyncType.REVIEWS, trigger, start, false, 0, 0, 0, e.getMessage());
            return new CsViews.SyncOutcome(store.getId(), store.getName(), 0, 0, 0, e.getMessage());
        }
        int created = 0, updated = 0;
        List<String> errors = new ArrayList<>();
        for (ReviewDraft d : drafts) {
            try {
                if (reviewService.saveSynced(store, d)) {
                    created++;
                } else {
                    updated++;
                }
            } catch (RuntimeException e) {
                log.warn("Saving review {} of store {} failed", d.reviewId(), store.getCode(), e);
                errors.add(d.reviewId() + ": " + e.getMessage());
            }
        }
        String message = errors.isEmpty() ? null : String.join("; ", errors);
        support.log(store.getId(), SyncType.REVIEWS, trigger, start, errors.isEmpty(), drafts.size(), created, updated, message);
        return new CsViews.SyncOutcome(store.getId(), store.getName(), drafts.size(), created, updated, message);
    }
}
