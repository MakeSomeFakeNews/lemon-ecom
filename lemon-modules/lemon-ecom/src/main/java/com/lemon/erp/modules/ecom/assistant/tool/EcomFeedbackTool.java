package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomAssistantQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 退货与差评原因：view = RETURN_REASONS（退货原因分布，本位币退款）、RETURN_SKUS（按 SKU + 原因的明细）、BAD_REVIEWS（1–2 星评价的内容，
 * 可按 SKU 筛选）、REVIEW_SUMMARY（按 SKU 的评价数、平均分、差评数）。查看评价另需「评价管理」权限
 */
@Component
@RequiredArgsConstructor
public class EcomFeedbackTool implements AiTool {

    enum View {
        RETURN_REASONS, RETURN_SKUS, BAD_REVIEWS, REVIEW_SUMMARY
    }

    static final String RETURN_REASON_DICT = "ecom_return_reason";
    static final String REVIEW_PERM = "ecom:csReview:list";

    private final EcomAssistantQueryMapper queryMapper;
    private final AssistantToolSupport support;
    private final DictLookup dictLookup;
    private final PermissionService permissionService;

    @Override
    public String name() {
        return "ecom_returns_reviews";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.asst.tool.feedback";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.asst.tool.feedback.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("view", AiSchema.enumOf("ecom.ai.asst.param.feedbackView", View.RETURN_REASONS.name(), View.RETURN_SKUS.name(),
                        View.BAD_REVIEWS.name(), View.REVIEW_SUMMARY.name()), true);
        AssistantToolSupport.dateProps(s);
        AssistantToolSupport.scopeProps(s).prop("keyword", AiSchema.string("ecom.ai.asst.param.sku").maxLength(64), false);
        return AssistantToolSupport.topProp(s).build();
    }

    @Override
    public String permission() {
        return "ecom:return:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        View view = View.valueOf(args.getString("view"));
        LocalDate end = AssistantToolSupport.end(args);
        LocalDate begin = AssistantToolSupport.begin(args, end, 30);
        AssistantToolSupport.Scope scope = support.scope(args);
        int top = AssistantToolSupport.top(args, 30);
        String keyword = args.getString("keyword");
        if ((view == View.BAD_REVIEWS || view == View.REVIEW_SUMMARY) && !permissionService.hasPerm(REVIEW_PERM)) {
            throw new BizException("ecom.error.asst.noReviewPerm");
        }
        LocalDateTime from = begin.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        switch (view) {
            case RETURN_REASONS -> {
                Map<String, String> labels = dictLookup.options(RETURN_REASON_DICT);
                List<AssistantRows.ReasonRow> reasons = queryMapper.selectReturnReasons(scope.ids(), from, to);
                reasons.forEach(r -> r.setReasonLabel(labels.getOrDefault(r.getReason(), r.getReason())));
                return AiToolResult.rows(reasons);
            }
            case RETURN_SKUS -> {
                Map<String, String> labels = dictLookup.options(RETURN_REASON_DICT);
                List<AssistantRows.SkuReasonRow> skus = queryMapper.selectSkuReturnReasons(scope.ids(), from, to, keyword);
                skus.forEach(r -> r.setReasonLabel(labels.getOrDefault(r.getReason(), r.getReason())));
                return AiToolResult.rows(AssistantToolSupport.head(skus, top), (long) skus.size());
            }
            case BAD_REVIEWS -> {
                List<AssistantRows.ReviewRow> rows = queryMapper.selectReviews(scope.ids(), begin, end, 2, keyword, top);
                rows.forEach(r -> r.setContent(r.getContent() == null || r.getContent().length() <= 400 ? r.getContent() : r.getContent().substring(0, 400) + "…"));
                return AiToolResult.rows(rows);
            }
            default -> {
                List<AssistantRows.ReviewSkuRow> rows = queryMapper.selectReviewSkus(scope.ids(), begin, end, keyword);
                return AiToolResult.rows(AssistantToolSupport.head(rows, top), (long) rows.size());
            }
        }
    }
}
