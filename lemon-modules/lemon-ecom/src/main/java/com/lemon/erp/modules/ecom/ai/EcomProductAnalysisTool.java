package com.lemon.erp.modules.ecom.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.analytics.dto.ProductViews;
import com.lemon.erp.modules.ecom.analytics.model.AbcClass;
import com.lemon.erp.modules.ecom.analytics.model.AbcMetric;
import com.lemon.erp.modules.ecom.analytics.service.EcomProductAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分析：view = ABC（按销售额 / 毛利的 ABC 分类与各类汇总）、SLOW_MOVING（滞销物料、滞销库存金额、库龄与动销率）、
 * RETURN_RATE（SKU 退货率排行与原因分布），权限与「商品分析」页面一致。结果为 { summary, rows, total }
 */
@Component
@RequiredArgsConstructor
public class EcomProductAnalysisTool implements AiTool {

    enum View {
        ABC, SLOW_MOVING, RETURN_RATE
    }

    private final EcomProductAnalysisService analysisService;

    @Override
    public String name() {
        return "ecom_product_analysis";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.analytics.product";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.analytics.product.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("view", AiSchema.enumOf("ecom.ai.analytics.param.view", View.ABC.name(), View.SLOW_MOVING.name(), View.RETURN_RATE.name()), true)
                .prop("startDate", AiSchema.date("ecom.ai.param.startDate"), false)
                .prop("endDate", AiSchema.date("ecom.ai.param.endDate"), false)
                .prop("storeId", AiSchema.integer("ecom.ai.param.storeId"), false)
                .prop("keyword", AiSchema.string("ecom.ai.param.skuKeyword").maxLength(64), false)
                .prop("metric", AiSchema.enumOf("ecom.ai.analytics.param.metric", AbcMetric.SALES.name(), AbcMetric.PROFIT.name()), false)
                .prop("abcClass", AiSchema.enumOf("ecom.ai.analytics.param.abcClass", AbcClass.A.name(), AbcClass.B.name(), AbcClass.C.name()), false)
                .prop("days", AiSchema.integer("ecom.ai.analytics.param.slowDays").min(1).max(365), false)
                .prop("top", AiSchema.integer("ecom.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "ecom:productAnalysis:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        View view = View.valueOf(args.getString("view"));
        LocalDate begin = args.getDate("startDate");
        LocalDate end = args.getDate("endDate");
        Long storeId = args.getLong("storeId");
        String keyword = args.getString("keyword");
        int top = args.getInt("top", 50);
        Map<String, Object> out = new LinkedHashMap<>();
        switch (view) {
            case ABC -> {
                AbcMetric metric = args.has("metric") ? AbcMetric.valueOf(args.getString("metric")) : AbcMetric.SALES;
                ProductViews.AbcResult r = analysisService.abc(begin, end, storeId, keyword, metric, null, null);
                AbcClass only = args.has("abcClass") ? AbcClass.valueOf(args.getString("abcClass")) : null;
                List<ProductViews.AbcRow> rows = r.rows().stream().filter(x -> only == null || x.getAbcClass() == only).toList();
                out.put("metric", r.metric());
                out.put("aPercent", r.aPercent());
                out.put("bPercent", r.bPercent());
                out.put("total", r.total());
                out.put("summary", r.summary());
                out.put("rowCount", rows.size());
                out.put("rows", rows.subList(0, Math.min(top, rows.size())));
            }
            case SLOW_MOVING -> {
                ProductViews.SlowResult r = analysisService.slowMoving(args.getInt("days", 30), storeId, keyword);
                out.put("days", r.days());
                out.put("activeSkus", r.activeSkus());
                out.put("movingSkus", r.movingSkus());
                out.put("movingRatePercent", r.movingRatePercent());
                out.put("slowCount", r.slowCount());
                out.put("slowAmount", r.slowAmount());
                out.put("rows", r.rows().subList(0, Math.min(top, r.rows().size())));
            }
            default -> {
                ProductViews.ReturnResult r = analysisService.returnRate(begin, end, storeId, keyword);
                List<ProductViews.ReturnRow> rows = r.rows().stream().filter(x -> x.getReturnQty().signum() > 0).toList();
                out.put("soldQty", r.soldQty());
                out.put("returnQty", r.returnQty());
                out.put("returnRatePercent", r.returnRatePercent());
                out.put("reasons", r.reasons());
                out.put("rowCount", rows.size());
                out.put("rows", rows.subList(0, Math.min(top, rows.size())));
            }
        }
        return AiToolResult.of(out);
    }
}
