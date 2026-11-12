package com.lemon.erp.modules.ecom.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.analytics.service.EcomForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 销量预测：某个 SKU / 店铺在售 SKU 未来 N 天（1 ~ 90，默认 30）的预测销量合计与日均、使用的预测方法与回测误差（MAPE）、
 * 最近 28 天实际日均，权限与「销量预测」页面一致
 */
@Component
@RequiredArgsConstructor
public class EcomForecastTool implements AiTool {

    private final EcomForecastService forecastService;

    @Override
    public String name() {
        return "ecom_sales_forecast";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.analytics.forecast";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.analytics.forecast.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("storeId", AiSchema.integer("ecom.ai.param.storeId"), false)
                .prop("keyword", AiSchema.string("ecom.ai.param.skuKeyword").maxLength(64), false)
                .prop("days", AiSchema.integer("ecom.ai.analytics.param.forecastDays").min(1).max(EcomForecastService.HORIZON), false)
                .prop("top", AiSchema.integer("ecom.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "ecom:forecast:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        int top = args.getInt("top", 50);
        List<EcomForecastService.ForecastSummary> rows = forecastService.summaries(args.getLong("storeId"), args.getString("keyword"),
                args.getInt("days", 30), top);
        return AiToolResult.rows(rows);
    }
}
