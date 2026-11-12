package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.service.FbaClaimService;
import com.lemon.erp.modules.ecom.fba.service.FbaHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FBA 库存健康：view = SUMMARY（库龄分布、冗余、超龄、预估仓储费、可索赔金额）、AGED（超过 180 天的库存）、EXCESS（冗余库存，
 * 可售超过日均 × 冗余天数）、LOW_STOCK（可售天数不足的 SKU）、CLAIMS（待提交 / 已提交的索赔，按截止日期，另需「FBA 索赔」权限）。
 * 复用库存健康与索赔的查询，权限与「FBA 库存健康」页面一致
 */
@Component
@RequiredArgsConstructor
public class EcomFbaInsightTool implements AiTool {

    enum View {
        SUMMARY, AGED, EXCESS, LOW_STOCK, CLAIMS
    }

    static final String CLAIM_PERM = "ecom:fbaClaim:list";

    private final FbaHealthService healthService;
    private final FbaClaimService claimService;
    private final AssistantToolSupport support;
    private final PermissionService permissionService;

    @Override
    public String name() {
        return "ecom_fba_insight";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.asst.tool.fba";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.asst.tool.fba.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("view", AiSchema.enumOf("ecom.ai.asst.param.fbaView", View.SUMMARY.name(), View.AGED.name(), View.EXCESS.name(),
                        View.LOW_STOCK.name(), View.CLAIMS.name()), true)
                .prop("excessDays", AiSchema.integer("ecom.ai.asst.param.excessDays").min(30).max(365), false)
                .prop("coverDays", AiSchema.integer("ecom.ai.asst.param.coverDays").min(1).max(120), false);
        AssistantToolSupport.scopeProps(s).prop("keyword", AiSchema.string("ecom.ai.asst.param.sku").maxLength(64), false);
        return AssistantToolSupport.topProp(s).build();
    }

    @Override
    public String permission() {
        return "ecom:fbaHealth:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        View view = View.valueOf(args.getString("view"));
        AssistantToolSupport.Scope scope = support.scope(args);
        Long storeId = scope.single();
        Integer excessDays = args.has("excessDays") ? args.getInt("excessDays", FbaHealthService.DEFAULT_EXCESS_DAYS) : null;
        int top = AssistantToolSupport.top(args, 30);
        String keyword = args.getString("keyword");
        switch (view) {
            case SUMMARY -> {
                List<Map<String, Object>> rows = new ArrayList<>();
                if (scope.filtered()) {
                    scope.stores().forEach(st -> rows.add(summary(st.getName(), healthService.summary(st.getId(), excessDays))));
                } else {
                    rows.add(summary(Messages.get("ecom.text.asst.allStores"), healthService.summary(null, excessDays)));
                }
                return AiToolResult.rows(rows);
            }
            case CLAIMS -> {
                if (!permissionService.hasPerm(CLAIM_PERM)) {
                    throw new BizException("ecom.error.asst.noClaimPerm");
                }
                List<EcomFbaClaim> rows = claimService.page(1, top, storeId, null, FbaClaimStatus.PENDING, keyword, null).records().stream()
                        .filter(c -> scope.contains(c.getStoreId())).toList();
                return AiToolResult.rows(rows.stream().map(EcomFbaInsightTool::claim).toList());
            }
            default -> {
                FbaHealthService.Query q = new FbaHealthService.Query(storeId, keyword, null, view == View.EXCESS, view == View.AGED, excessDays);
                List<FbaViews.HealthRow> rows = healthService.rows(q).stream().filter(r -> scope.contains(r.getStoreId())).toList();
                if (view == View.LOW_STOCK) {
                    BigDecimal cover = BigDecimal.valueOf(args.getInt("coverDays", 30));
                    rows = rows.stream().filter(r -> r.getDaysOfSupply() != null && r.getDaysOfSupply().compareTo(cover) < 0)
                            .sorted(Comparator.comparing(FbaViews.HealthRow::getDaysOfSupply)).toList();
                } else if (view == View.AGED) {
                    rows = rows.stream().sorted(Comparator.comparing((FbaViews.HealthRow r) -> nz(r.getAgedQty())).reversed()).toList();
                } else {
                    rows = rows.stream().sorted(Comparator.comparing((FbaViews.HealthRow r) -> nz(r.getExcessQty())).reversed()).toList();
                }
                return AiToolResult.rows(AssistantToolSupport.head(rows, top).stream().map(EcomFbaInsightTool::health).toList(), (long) rows.size());
            }
        }
    }

    /** 汇总的一行：件数、库龄分布、预估费用与可索赔金额（本位币） */
    private static Map<String, Object> summary(String name, FbaViews.HealthSummary s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("storeName", name);
        m.put("skuCount", s.getSkuCount());
        m.put("availableQty", s.getAvailableQty());
        m.put("inboundQty", s.getInboundQty());
        m.put("unfulfillableQty", s.getUnfulfillableQty());
        m.put("excessQty", s.getExcessQty());
        m.put("excessSkus", s.getExcessSkus());
        m.put("agedQty", s.getAgedQty());
        BigDecimal[] b = s.getBuckets();
        m.put("age0To90", b[0]);
        m.put("age91To180", b[1]);
        m.put("age181To270", b[2]);
        m.put("age271To365", b[3]);
        m.put("age365Plus", b[4]);
        m.put("monthlyStorageFeeBase", s.getMonthlyFeeBase());
        m.put("agedSurchargeBase", s.getAgedFeeBase());
        m.put("claimableBase", s.getClaimableBase());
        return m;
    }

    private static Map<String, Object> health(FbaViews.HealthRow r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("storeName", r.getStoreName());
        m.put("sellerSku", r.getSellerSku());
        m.put("asin", r.getAsin());
        m.put("title", r.getTitle() == null || r.getTitle().length() <= 80 ? r.getTitle() : r.getTitle().substring(0, 80) + "…");
        m.put("availableQty", r.getAvailableQty());
        m.put("inboundQty", r.getInboundQty());
        m.put("dailySales", r.getDailySales());
        m.put("daysOfSupply", r.getDaysOfSupply());
        m.put("excessQty", r.getExcessQty());
        m.put("agedQty", r.getAgedQty());
        m.put("age0To90", r.getAge0To90());
        m.put("age91To180", r.getAge91To180());
        m.put("age181To270", r.getAge181To270());
        m.put("age271To365", r.getAge271To365());
        m.put("age365Plus", r.getAge365Plus());
        m.put("monthlyStorageFee", r.getMonthlyFee());
        m.put("agedSurcharge", r.getAgedFee());
        m.put("currency", r.getCurrency());
        m.put("advice", r.getAdvice());
        return m;
    }

    private static Map<String, Object> claim(EcomFbaClaim c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("claimNo", c.getClaimNo());
        m.put("storeName", c.getStoreName());
        m.put("type", c.getClaimType());
        m.put("sellerSku", c.getSellerSku());
        m.put("qty", c.getQty());
        m.put("estAmount", c.getEstAmount());
        m.put("currency", c.getCurrency());
        m.put("deadline", c.getDeadline() == null ? null : c.getDeadline().toString());
        m.put("daysLeft", c.getDaysLeft());
        m.put("detail", c.getDetail());
        return m;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
