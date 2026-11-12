package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoDispatcher;
import com.lemon.erp.modules.ecom.cs.service.EcomReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 客服触发器：新差评（新增 3 星及以下的评价，同步 / 导入 / 录入后实时触发，评分阈值在条件中设置）、
 * 买家消息超时未回复（每 15 分钟扫描过了平台回复时限还没有回复的会话）
 */
@Component
@RequiredArgsConstructor
public class CsTriggerSource implements AutoTriggerSource {

    /** 差评：3 星及以下 */
    static final int NEGATIVE_MAX = 3;
    private static final int SCAN_LIMIT = 2000;

    private final EcomAutoScanMapper scanMapper;
    private final AutoDispatcher dispatcher;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        List<AutoField> review = new ArrayList<>(List.of(AutoField.number("rating", 1), AutoField.text("sku", "DM-EARBUD-BK"),
                AutoField.text("itemId", "B0DEMO1234")));
        review.addAll(List.of(AutoField.store()));
        review.addAll(List.of(AutoField.text("title", "Stopped working"), AutoField.text("content", "Left earbud stopped charging after a week."),
                AutoField.text("reviewer", "Amazon Customer"), AutoField.text("orderNo", "114-1234567-1234567"), AutoField.bool("hasOrder", true)));
        map.put(AutoTrigger.REVIEW_NEGATIVE, review);
        List<AutoField> thread = new ArrayList<>(List.of(AutoField.text("buyerName", "John Smith"), AutoField.text("subject", "Where is my order?")));
        thread.addAll(List.of(AutoField.store()));
        thread.addAll(List.of(AutoField.text("lastMessage", "I have not received my package yet."), AutoField.number("overdueHours", new BigDecimal("6")),
                AutoField.text("orderNo", "114-1234567-1234567"), AutoField.number("unread", 2), AutoField.text("tags", "logistics")));
        map.put(AutoTrigger.MESSAGE_TIMEOUT, thread);
        return map;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReview(EcomReviewService.ReviewCreatedEvent e) {
        if (dispatcher.active(AutoTrigger.REVIEW_NEGATIVE)) {
            dispatcher.dispatch(() -> reviews(List.of(e.reviewId()), 1));
        }
    }

    public List<AutoEvent> reviews(List<Long> ids, int limit) {
        return scanMapper.selectReviews(ids, NEGATIVE_MAX, limit).stream().map(CsTriggerSource::review).toList();
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        if (trigger == AutoTrigger.REVIEW_NEGATIVE) {
            return reviews(null, limit);
        }
        return threads(limit);
    }

    @Override
    public List<AutoEvent> scan(AutoTrigger trigger) {
        return trigger == AutoTrigger.MESSAGE_TIMEOUT ? threads(SCAN_LIMIT) : List.of();
    }

    private List<AutoEvent> threads(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return scanMapper.selectOverdueThreads(now, limit).stream().map(t -> thread(t, now)).toList();
    }

    private static AutoEvent review(Map<String, Object> r) {
        Long id = AutoRows.id(r, "id");
        String sku = AutoRows.str(r, "sku");
        Long orderId = AutoRows.id(r, "orderId");
        return AutoEvent.builder(AutoTrigger.REVIEW_NEGATIVE, "REVIEW:" + id)
                .label(AutoRows.integer(r, "rating") + "★ " + (sku == null ? AutoRows.str(r, "itemId") : sku))
                .store(AutoRows.id(r, "storeId"), AutoRows.str(r, "storeName"), AutoRows.platform(r, "platform"), AutoRows.str(r, "site"))
                .skus(sku == null ? List.of() : List.of(sku))
                .field("rating", AutoRows.integer(r, "rating"))
                .field("sku", sku)
                .field("itemId", AutoRows.str(r, "itemId"))
                .field("title", AutoRows.str(r, "title"))
                .field("content", AutoRows.str(r, "content"))
                .field("reviewer", AutoRows.str(r, "reviewer"))
                .field("orderNo", AutoRows.str(r, "orderNo"))
                .field("hasOrder", orderId != null)
                .ref(AutoRef.REVIEW, id)
                .ref(AutoRef.ORDER, orderId)
                .ref(AutoRef.LISTING, AutoRows.id(r, "listingId"))
                .ref(AutoRef.MATERIAL, AutoRows.id(r, "materialId"))
                .link("/ecom/cs-review?id=" + id)
                .build();
    }

    private static AutoEvent thread(Map<String, Object> t, LocalDateTime now) {
        Long id = AutoRows.id(t, "id");
        LocalDateTime deadline = AutoRows.time(t, "replyDeadline");
        BigDecimal hours = deadline == null ? null
                : BigDecimal.valueOf(Duration.between(deadline, now).toMinutes()).divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP);
        String buyer = AutoRows.str(t, "buyerName");
        String subject = AutoRows.str(t, "subject");
        return AutoEvent.builder(AutoTrigger.MESSAGE_TIMEOUT, "THREAD:" + id)
                .label((buyer == null ? "" : buyer + " ") + (subject == null ? "" : subject))
                .store(AutoRows.id(t, "storeId"), AutoRows.str(t, "storeName"), AutoRows.platform(t, "platform"), AutoRows.str(t, "site"))
                .tags(AutoRows.tags(AutoRows.str(t, "tags")))
                .field("buyerName", buyer)
                .field("subject", subject)
                .field("lastMessage", AutoRows.str(t, "lastMessage"))
                .field("overdueHours", hours)
                .field("orderNo", AutoRows.str(t, "orderNo"))
                .field("unread", AutoRows.integer(t, "unread"))
                .field("tags", AutoRows.tags(AutoRows.str(t, "tags")))
                .ref(AutoRef.THREAD, id)
                .ref(AutoRef.ORDER, AutoRows.id(t, "orderId"))
                .link("/ecom/cs-message?id=" + id)
                .build();
    }
}
