package com.lemon.erp.modules.ecom.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.service.EcomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 跨境电商销售与利润：按店铺 / 平台 SKU / 国家汇总一段时间的订单数、销售额、平台费用、退款、成本、物流、广告与利润（本位币），
 * 复用利润分析的查询，权限与「利润分析」页面一致
 */
@Component
@RequiredArgsConstructor
public class EcomSalesTool implements AiTool {

    enum View {
        STORE, SKU, COUNTRY
    }

    private final EcomReportService reportService;

    @Override
    public String name() {
        return "ecom_sales_profit";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.tool.sales";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.tool.sales.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("startDate", AiSchema.date("ecom.ai.param.startDate"), true)
                .prop("endDate", AiSchema.date("ecom.ai.param.endDate"), true)
                .prop("view", AiSchema.enumOf("ecom.ai.param.salesView", View.STORE.name(), View.SKU.name(), View.COUNTRY.name()), false)
                .prop("storeId", AiSchema.integer("ecom.ai.param.storeId"), false)
                .prop("keyword", AiSchema.string("ecom.ai.param.skuKeyword").maxLength(64), false)
                .prop("top", AiSchema.integer("ecom.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "ecom:report:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        LocalDate begin = args.getDate("startDate");
        LocalDate end = args.getDate("endDate");
        Long storeId = args.getLong("storeId");
        int top = args.getInt("top", 50);
        View view = args.has("view") ? View.valueOf(args.getString("view")) : View.STORE;
        return switch (view) {
            case SKU -> {
                List<EcomReports.SkuRow> rows = reportService.bySku(begin, end, storeId, args.getString("keyword"));
                yield AiToolResult.rows(rows.subList(0, Math.min(top, rows.size())), (long) rows.size());
            }
            case COUNTRY -> {
                List<EcomReports.CountryRow> rows = reportService.byCountry(begin, end, storeId);
                yield AiToolResult.rows(rows.subList(0, Math.min(top, rows.size())), (long) rows.size());
            }
            default -> AiToolResult.rows(reportService.byStore(begin, end, storeId));
        };
    }
}
