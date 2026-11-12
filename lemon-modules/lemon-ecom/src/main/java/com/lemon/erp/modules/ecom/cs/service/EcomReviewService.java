package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.cs.connector.CsCapability;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnector;
import com.lemon.erp.modules.ecom.cs.connector.MessageConnectorRegistry;
import com.lemon.erp.modules.ecom.cs.connector.ReviewDraft;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.dto.FeedbackImportRow;
import com.lemon.erp.modules.ecom.cs.dto.ReviewImportRow;
import com.lemon.erp.modules.ecom.cs.dto.ReviewQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomFeedback;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.cs.model.CsRecordSource;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomFeedbackMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReviewMapper;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 评价监控：商品评价（Excel 导入或连接器拉取，按 店铺 + 评价 ID 新增或更新）与店铺反馈（导入）。
 * 低分（评分不超过 {@link CsConstants#LOW_RATING}）的新记录自动为待处理、处理人为店铺负责人，并发站内消息提醒负责人（店铺没有负责人时发给有「处理评价」权限的用户）。
 * 统计：概况（评价数、平均分、差评数与差评率、待处理、已回复）、按 SKU（平均分、差评率）、按天的评分趋势
 */
@Service
@RequiredArgsConstructor
public class EcomReviewService {

    /** 评价处理权限（店铺没有负责人时提醒有该权限的用户） */
    static final String HANDLE_PERM = "ecom:csReview:edit";

    private final EcomReviewMapper reviewMapper;
    private final EcomFeedbackMapper feedbackMapper;
    private final EcomStoreService storeService;
    private final EcomCsSupport support;
    private final MessageConnectorRegistry registry;
    private final MessageSender messageSender;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /** 新增了一条评价（自动化规则「新差评」在提交后处理） */
    public record ReviewCreatedEvent(Long reviewId) {
    }

    // ---------------------------------------------------------------- 查询与统计

    public PageResult<EcomReview> page(long current, long size, ReviewQuery q) {
        Page<EcomReview> page = Page.of(current, size);
        reviewMapper.selectPageList(page, q, CsConstants.LOW_RATING);
        return PageResult.of(page);
    }

    public EcomReview detail(Long id) {
        EcomReview r = reviewMapper.selectDetail(id);
        if (r == null) {
            throw new BizException("ecom.error.cs.reviewNotFound");
        }
        return r;
    }

    public CsViews.ReviewSummary summary(ReviewQuery q) {
        Map<String, Object> m = reviewMapper.selectSummary(q, CsConstants.LOW_RATING);
        long total = longOf(m, "total");
        long low = longOf(m, "lowCount");
        return new CsViews.ReviewSummary(total, avg(longOf(m, "ratingSum"), total), low, percent(low, total), longOf(m, "pendingCount"),
                longOf(m, "repliedCount"));
    }

    public List<CsViews.SkuStat> skuStats(ReviewQuery q) {
        List<CsViews.SkuStat> out = new ArrayList<>();
        for (Map<String, Object> m : reviewMapper.selectSkuStats(q, CsConstants.LOW_RATING)) {
            long cnt = longOf(m, "cnt");
            long low = longOf(m, "lowCount");
            out.add(new CsViews.SkuStat(val(m, "storeId") == null ? null : ((Number) val(m, "storeId")).longValue(), (String) val(m, "storeName"),
                    (String) val(m, "sellerSku"), (String) val(m, "title"), cnt, avg(longOf(m, "ratingSum"), cnt), low, percent(low, cnt),
                    dateOf(val(m, "lastDate"))));
        }
        return out;
    }

    /** 评分趋势：默认最近 30 天，每天一个点（没有评价的日期数量为 0、平均分为空） */
    public List<CsViews.TrendPoint> trend(ReviewQuery q) {
        LocalDate end = q.getEndDate() == null ? LocalDate.now() : q.getEndDate();
        LocalDate begin = q.getBeginDate() == null ? end.minusDays(29) : q.getBeginDate();
        if (begin.isAfter(end) || begin.plusDays(366).isBefore(end)) {
            throw new BizException("ecom.error.cs.dateRangeInvalid");
        }
        q.setBeginDate(begin);
        q.setEndDate(end);
        Map<LocalDate, Map<String, Object>> byDate = new java.util.HashMap<>();
        for (Map<String, Object> m : reviewMapper.selectTrend(q, CsConstants.LOW_RATING)) {
            byDate.put(dateOf(val(m, "reviewDate")), m);
        }
        List<CsViews.TrendPoint> out = new ArrayList<>();
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> m = byDate.get(d);
            long cnt = m == null ? 0 : longOf(m, "cnt");
            out.add(new CsViews.TrendPoint(d, cnt, m == null ? null : avg(longOf(m, "ratingSum"), cnt), m == null ? 0 : longOf(m, "lowCount")));
        }
        return out;
    }

    public PageResult<EcomFeedback> feedbackPage(long current, long size, ReviewQuery q) {
        Page<EcomFeedback> page = Page.of(current, size);
        feedbackMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 写入

    /** 导入一行商品评价（ImportExecutor 整批一个事务）：同一 店铺 + 评价 ID 已有时更新内容与回复，不改处理状态 */
    public void importRow(ReviewImportRow row) {
        EcomStore store = storeByCode(row.getStoreCode());
        if (!StringUtils.hasText(row.getSellerSku()) && !StringUtils.hasText(row.getPlatformItemId())) {
            throw new BizException("ecom.error.cs.reviewSkuRequired");
        }
        String reviewId = StringUtils.hasText(row.getReviewId()) ? row.getReviewId().trim()
                : generatedId("R", row.getSellerSku(), row.getPlatformItemId(), row.getReviewer(), String.valueOf(row.getReviewDate()),
                row.getTitle(), row.getContent());
        save(store, reviewId, row.getPlatformItemId(), null, row.getSellerSku(), row.getRating(), row.getTitle(), row.getContent(), row.getReviewer(),
                row.getReviewDate(), row.getPlatformOrderNo(), row.getReplyContent(), null, CsRecordSource.IMPORT);
    }

    /** 写入连接器拉取的评价，返回是否新增 */
    @Transactional
    public boolean saveSynced(EcomStore store, ReviewDraft d) {
        LocalDate date = d.reviewTime() == null ? LocalDate.now() : d.reviewTime().toLocalDate();
        return save(store, d.reviewId(), d.platformItemId(), d.parentItemId(), d.sellerSku(), d.rating(), d.title(), d.content(), d.reviewer(), date,
                d.platformOrderNo(), d.replyContent(), d.replyTime(), CsRecordSource.SYNC);
    }

    private boolean save(EcomStore store, String reviewId, String platformItemId, String parentItemId, String sellerSku, int rating, String title,
                         String content, String reviewer, LocalDate date, String orderNo, String reply, LocalDateTime replyTime, CsRecordSource source) {
        if (rating < 1 || rating > 5) {
            throw new BizException("ecom.error.cs.ratingInvalid", rating);
        }
        EcomReview r = reviewMapper.selectOne(Wrappers.<EcomReview>lambdaQuery().eq(EcomReview::getStoreId, store.getId())
                .eq(EcomReview::getReviewId, reviewId));
        boolean created = r == null;
        if (created) {
            r = new EcomReview();
            r.setStoreId(store.getId());
            r.setPlatform(store.getPlatform());
            r.setReviewId(EcomCsThreadService.cut(reviewId, 128));
            r.setSource(source);
            r.setReplied(0);
            if (rating <= CsConstants.LOW_RATING) {
                r.setHandleStatus(CsReviewStatus.PENDING);
                r.setHandlerId(store.getOwnerId());
            } else {
                r.setHandleStatus(CsReviewStatus.NONE);
            }
        }
        EcomListing listing = support.findListing(store.getId(), sellerSku, platformItemId, parentItemId);
        r.setListingId(listing == null ? r.getListingId() : listing.getId());
        r.setSellerSku(listing != null ? listing.getSellerSku() : blank(sellerSku, r.getSellerSku()));
        r.setPlatformItemId(blank(platformItemId, listing != null ? listing.getPlatformItemId() : r.getPlatformItemId()));
        r.setRating(rating);
        r.setTitle(EcomCsThreadService.cut(blank(title, r.getTitle()), 255));
        r.setContent(EcomCsThreadService.cut(blank(content, r.getContent()), 4000));
        r.setReviewer(EcomCsThreadService.cut(blank(reviewer, r.getReviewer()), 128));
        r.setReviewDate(date);
        if (StringUtils.hasText(orderNo)) {
            EcomOrder o = support.findOrder(store.getId(), orderNo);
            r.setOrderId(o == null ? null : o.getId());
            r.setPlatformOrderNo(EcomCsThreadService.cut(orderNo.trim(), 64));
        }
        if (StringUtils.hasText(reply)) {
            r.setReplied(1);
            r.setReplyContent(EcomCsThreadService.cut(reply, 2000));
            r.setReplyTime(replyTime != null ? replyTime : r.getReplyTime() != null ? r.getReplyTime() : LocalDateTime.now());
        }
        if (created) {
            reviewMapper.insert(r);
            eventPublisher.publishEvent(new ReviewCreatedEvent(r.getId()));
            if (rating <= CsConstants.LOW_RATING) {
                alertLow(store, r.getId(), "/ecom/cs-review?id=" + r.getId(), rating, r.getSellerSku(), r.getContent());
            }
        } else {
            reviewMapper.updateById(r);
        }
        return created;
    }

    /** 导入一行店铺反馈：同一 店铺 + 反馈 ID 已有时更新 */
    public void importFeedbackRow(FeedbackImportRow row) {
        EcomStore store = storeByCode(row.getStoreCode());
        String id = StringUtils.hasText(row.getFeedbackId()) ? row.getFeedbackId().trim()
                : generatedId("F", row.getPlatformOrderNo(), String.valueOf(row.getFeedbackDate()), row.getRater(), row.getComments());
        EcomFeedback f = feedbackMapper.selectOne(Wrappers.<EcomFeedback>lambdaQuery().eq(EcomFeedback::getStoreId, store.getId())
                .eq(EcomFeedback::getFeedbackId, id));
        boolean created = f == null;
        if (created) {
            f = new EcomFeedback();
            f.setStoreId(store.getId());
            f.setPlatform(store.getPlatform());
            f.setFeedbackId(EcomCsThreadService.cut(id, 128));
            if (row.getRating() <= CsConstants.LOW_RATING) {
                f.setHandleStatus(CsReviewStatus.PENDING);
                f.setHandlerId(store.getOwnerId());
            } else {
                f.setHandleStatus(CsReviewStatus.NONE);
            }
        }
        f.setRating(row.getRating());
        f.setComments(blank(row.getComments(), f.getComments()));
        f.setRater(blank(row.getRater(), f.getRater()));
        f.setFeedbackDate(row.getFeedbackDate());
        f.setResponse(blank(row.getResponse(), f.getResponse()));
        if (StringUtils.hasText(row.getPlatformOrderNo())) {
            EcomOrder o = support.findOrder(store.getId(), row.getPlatformOrderNo());
            f.setOrderId(o == null ? null : o.getId());
            f.setPlatformOrderNo(row.getPlatformOrderNo().trim());
        }
        if (created) {
            feedbackMapper.insert(f);
            if (row.getRating() <= CsConstants.LOW_RATING) {
                alertLow(store, f.getId(), "/ecom/cs-review?tab=feedback&id=" + f.getId(), row.getRating(), null, f.getComments());
            }
        } else {
            feedbackMapper.updateById(f);
        }
    }

    /** 处理评价：状态、处理人、说明 */
    public void handle(Long id, CsRequests.Handle req) {
        detail(id);
        reviewMapper.update(Wrappers.<EcomReview>lambdaUpdate().set(EcomReview::getHandleStatus, req.handleStatus())
                .set(EcomReview::getHandlerId, req.handlerId()).set(EcomReview::getHandleNote, req.handleNote()).eq(EcomReview::getId, id));
    }

    public void handleFeedback(Long id, CsRequests.Handle req) {
        requireFeedback(id);
        feedbackMapper.update(Wrappers.<EcomFeedback>lambdaUpdate().set(EcomFeedback::getHandleStatus, req.handleStatus())
                .set(EcomFeedback::getHandlerId, req.handlerId()).set(EcomFeedback::getHandleNote, req.handleNote()).eq(EcomFeedback::getId, id));
    }

    /**
     * 回复评价：平台同步来的评价且平台支持回复时调用平台接口（不在事务中），其他（导入的评价、recordOnly）只登记回复内容
     */
    public void reply(Long id, CsRequests.ReviewReply req) {
        EcomReview r = detail(id);
        String content = req.content().trim();
        if (!Boolean.TRUE.equals(req.recordOnly()) && r.getSource() == CsRecordSource.SYNC) {
            MessageConnector c = registry.find(r.getPlatform()).orElse(null);
            if (c == null || !c.supports(CsCapability.REVIEW_REPLY)) {
                throw new BizException("ecom.error.cs.capabilityUnsupported", r.getPlatform().name(), CsCapability.REVIEW_REPLY.name());
            }
            c.replyReview(support.prepare(storeService.get(r.getStoreId())), r.getReviewId(), content);
        }
        reviewMapper.update(Wrappers.<EcomReview>lambdaUpdate().set(EcomReview::getReplied, 1).set(EcomReview::getReplyContent, content)
                .set(EcomReview::getReplyTime, LocalDateTime.now()).eq(EcomReview::getId, id));
    }

    /** 店铺反馈登记卖家回复（亚马逊不开放反馈回复接口，在卖家后台回复后登记） */
    public void replyFeedback(Long id, CsRequests.ReviewReply req) {
        requireFeedback(id);
        feedbackMapper.update(Wrappers.<EcomFeedback>lambdaUpdate().set(EcomFeedback::getResponse, req.content().trim()).eq(EcomFeedback::getId, id));
    }

    public void delete(List<Long> ids) {
        reviewMapper.deleteByIds(ids);
    }

    public void deleteFeedback(List<Long> ids) {
        feedbackMapper.deleteByIds(ids);
    }

    public EcomFeedback requireFeedback(Long id) {
        EcomFeedback f = feedbackMapper.selectById(id);
        if (f == null) {
            throw new BizException("ecom.error.cs.reviewNotFound");
        }
        return f;
    }

    // ---------------------------------------------------------------- 低分提醒

    /** 站内消息提醒店铺负责人（没有负责人时发给有处理评价权限的用户），同一条评价只提醒一次 */
    private void alertLow(EcomStore store, Long id, String link, int rating, String sku, String content) {
        String title = "差评提醒：" + store.getName() + " " + rating + " 星" + (StringUtils.hasText(sku) ? " " + sku : ""); // i18n-ignore 站内消息存库
        String body = StringUtils.hasText(content) ? EcomCsThreadService.cut(content, 200) : "（无内容）"; // i18n-ignore 站内消息存库
        MessageSender.Message message = MessageSender.Message.of(MessageCategory.ALERT, title, body, link).dedup("ecom-cs-low:" + link);
        if (store.getOwnerId() != null) {
            messageSender.send(List.of(store.getOwnerId()), message);
        } else {
            messageSender.sendToPermission(HANDLE_PERM, message);
        }
    }

    // ---------------------------------------------------------------- 工具

    private EcomStore storeByCode(String code) {
        EcomStore store = storeService.findByCode(code.trim().toUpperCase());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", code);
        }
        return store;
    }

    /** 没有平台 ID 时按内容生成稳定的 ID（同一行重复导入得到同一 ID） */
    static String generatedId(String prefix, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p == null ? "" : p.trim()).append('|');
        }
        return prefix + "-" + ConnectorSigns.sha256Hex(sb.toString()).substring(0, 32);
    }

    private static String blank(String v, String fallback) {
        return StringUtils.hasText(v) ? v.trim() : fallback;
    }

    /** 按别名取值（不同数据库返回的别名大小写可能不同） */
    private static Object val(Map<String, Object> m, String key) {
        if (m == null) {
            return null;
        }
        Object v = m.get(key);
        if (v == null) {
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (e.getKey().equalsIgnoreCase(key)) {
                    return e.getValue();
                }
            }
        }
        return v;
    }

    private static long longOf(Map<String, Object> m, String key) {
        Object v = val(m, key);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static BigDecimal avg(long sum, long count) {
        return count == 0 ? null : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(long part, long total) {
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static LocalDate dateOf(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }
}
