package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.cs.connector.CsCapability;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnector;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnectorRegistry;
import com.lemon.erp.modules.ecom.cs.connector.SolicitationResult;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.entity.EcomReviewRequest;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomReviewRequestMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求评价（亚马逊 Solicitations）：列出发货后 5–30 天的订单与请求记录，勾选后逐张请求（平台接口不在事务中，每张订单一条记录），
 * 已成功请求过的订单不再请求；不符合条件、失败的可以再次请求。定时任务（默认停用）每天对窗口内还没请求过的订单自动请求。
 * 每个店铺每批写一条渠道同步日志（类型 REVIEW_REQUEST）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomReviewRequestService {

    /** 定时任务每个店铺每次最多请求的订单数（亚马逊限速每秒 1 次） */
    static final int JOB_BATCH = 100;

    private final EcomReviewRequestMapper requestMapper;
    private final EcomStoreService storeService;
    private final MessageConnectorRegistry registry;
    private final EcomCsSupport support;

    /**
     * 可请求评价的订单：platform 为空取亚马逊
     *
     * @param pendingOnly 只列还没有成功请求过的
     */
    public PageResult<CsViews.RequestCandidate> candidates(long current, long size, EcomPlatform platform, Long storeId, String keyword,
                                                           boolean pendingOnly, CsRequestStatus status) {
        EcomPlatform p = platform == null ? EcomPlatform.AMAZON : platform;
        LocalDateTime now = LocalDateTime.now();
        Page<CsViews.RequestCandidate> page = Page.of(current, size);
        requestMapper.selectCandidates(page, p, storeId, now.minusDays(CsConstants.REQUEST_MAX_DAYS), now.minusDays(CsConstants.REQUEST_MIN_DAYS),
                keyword, pendingOnly, status);
        return PageResult.of(page);
    }

    /** 手工请求：逐张订单调用平台接口，返回每张的结果 */
    public List<CsViews.RequestOutcome> request(List<Long> orderIds) {
        return request(orderIds, CsConstants.MANUAL);
    }

    List<CsViews.RequestOutcome> request(List<Long> orderIds, String trigger) {
        Map<Long, List<EcomOrder>> byStore = new LinkedHashMap<>();
        List<CsViews.RequestOutcome> out = new ArrayList<>();
        for (Long id : orderIds) {
            EcomOrder o = support.order(id);
            if (o == null) {
                throw new BizException("ecom.error.orderNotFound");
            }
            byStore.computeIfAbsent(o.getStoreId(), k -> new ArrayList<>()).add(o);
        }
        for (Map.Entry<Long, List<EcomOrder>> e : byStore.entrySet()) {
            EcomStore store = storeService.get(e.getKey());
            MessageConnector c = registry.find(store.getPlatform()).orElse(null);
            if (c == null || !c.supports(CsCapability.REVIEW_REQUEST)) {
                throw new BizException("ecom.error.cs.capabilityUnsupported", store.getPlatform().name(), CsCapability.REVIEW_REQUEST.name());
            }
            out.addAll(requestStore(store, c, e.getValue(), trigger));
        }
        return out;
    }

    private List<CsViews.RequestOutcome> requestStore(EcomStore store, MessageConnector c, List<EcomOrder> orders, String trigger) {
        LocalDateTime start = LocalDateTime.now();
        List<CsViews.RequestOutcome> out = new ArrayList<>();
        ConnectorContext ctx;
        try {
            ctx = support.prepare(store);
        } catch (RuntimeException e) {
            support.log(store.getId(), SyncType.REVIEW_REQUEST, trigger, start, false, orders.size(), 0, 0, e.getMessage());
            throw e;
        }
        int sent = 0, failed = 0;
        for (EcomOrder o : orders) {
            EcomReviewRequest existing = requestMapper.selectOne(Wrappers.<EcomReviewRequest>lambdaQuery().eq(EcomReviewRequest::getOrderId, o.getId()));
            if (existing != null && existing.getStatus() == CsRequestStatus.SENT) {
                out.add(new CsViews.RequestOutcome(o.getId(), o.getPlatformOrderNo(), CsRequestStatus.SENT, existing.getMessage()));
                continue;
            }
            SolicitationResult r;
            if (o.getStatus() != EcomOrderStatus.SHIPPED) {
                r = SolicitationResult.notEligible(o.getStatus() == null ? null : o.getStatus().name());
            } else {
                try {
                    r = c.requestReview(ctx, o.getPlatformOrderNo());
                } catch (RuntimeException ex) {
                    log.warn("Review request failed for order {} of store {}: {}", o.getPlatformOrderNo(), store.getCode(), ex.getMessage());
                    r = new SolicitationResult(CsRequestStatus.FAILED, HttpJson.abbreviate(ex.getMessage()));
                }
            }
            if (r.status() == CsRequestStatus.SENT) {
                sent++;
            } else if (r.status() == CsRequestStatus.FAILED) {
                failed++;
            }
            save(existing, store, o, r);
            out.add(new CsViews.RequestOutcome(o.getId(), o.getPlatformOrderNo(), r.status(), r.message()));
        }
        support.log(store.getId(), SyncType.REVIEW_REQUEST, trigger, start, failed == 0, orders.size(), sent, 0,
                failed == 0 ? null : out.stream().filter(x -> x.status() == CsRequestStatus.FAILED).map(x -> x.platformOrderNo() + ": " + x.message())
                        .reduce((a, b) -> a + "; " + b).orElse(null));
        return out;
    }

    private void save(EcomReviewRequest existing, EcomStore store, EcomOrder o, SolicitationResult r) {
        EcomReviewRequest q = existing == null ? new EcomReviewRequest() : existing;
        q.setStoreId(store.getId());
        q.setOrderId(o.getId());
        q.setPlatformOrderNo(o.getPlatformOrderNo());
        q.setStatus(r.status());
        q.setMessage(EcomCsThreadService.cut(r.message(), 500));
        q.setRequestTime(LocalDateTime.now());
        if (existing == null) {
            q.setCreateBy(SecurityUtils.getUserIdOrNull());
            requestMapper.insert(q);
        } else {
            requestMapper.updateById(q);
        }
    }

    /** 定时任务：支持请求评价的店铺，发货 5–30 天内还没有请求过（或之前失败）的订单 */
    public List<CsViews.SyncOutcome> requestDueAll() {
        LocalDateTime now = LocalDateTime.now();
        List<CsViews.SyncOutcome> out = new ArrayList<>();
        for (EcomStore s : storeService.syncable()) {
            MessageConnector c = registry.find(s.getPlatform()).orElse(null);
            if (c == null || !c.supports(CsCapability.REVIEW_REQUEST)) {
                continue;
            }
            List<Long> ids = requestMapper.selectDueOrderIds(s.getId(), now.minusDays(CsConstants.REQUEST_MAX_DAYS),
                    now.minusDays(CsConstants.REQUEST_MIN_DAYS), JOB_BATCH);
            if (ids.isEmpty()) {
                continue;
            }
            try {
                List<CsViews.RequestOutcome> r = requestStore(s, c, ids.stream().map(support::order).toList(), CsConstants.JOB);
                int sent = (int) r.stream().filter(x -> x.status() == CsRequestStatus.SENT).count();
                long failed = r.stream().filter(x -> x.status() == CsRequestStatus.FAILED).count();
                out.add(new CsViews.SyncOutcome(s.getId(), s.getName(), ids.size(), sent, 0, failed == 0 ? null : failed + " failed"));
            } catch (RuntimeException e) {
                out.add(new CsViews.SyncOutcome(s.getId(), s.getName(), ids.size(), 0, 0, e.getMessage()));
            }
        }
        return out;
    }
}
