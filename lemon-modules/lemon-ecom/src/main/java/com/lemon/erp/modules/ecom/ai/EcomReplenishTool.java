package com.lemon.erp.modules.ecom.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 补货建议：stage = PLATFORM（默认）为平台仓补货（每个在售 SKU 的 7 / 14 / 30 / 60 天销量、加权日均、平台仓库存、头程在途、可售天数、
 * 预计断货日期和建议补货量）；PURCHASE 为本地仓采购（每个物料的日均、各店铺平台仓建议合计、本地仓库存、采购在途、采购周期、
 * 建议采购量、默认供应商与单价）。可只看紧急 / 预警的
 */
@Component
@RequiredArgsConstructor
public class EcomReplenishTool implements AiTool {

    /** 平台仓补货 */
    static final String STAGE_PLATFORM = "PLATFORM";
    /** 本地仓采购 */
    static final String STAGE_PURCHASE = "PURCHASE";

    private final EcomReplenishService replenishService;

    @Override
    public String name() {
        return "ecom_replenish_suggestions";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.tool.replenish";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.purchase.replenish.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
                .prop("stage", AiSchema.enumOf("ecom.ai.purchase.param.stage", STAGE_PLATFORM, STAGE_PURCHASE), false)
                .prop("storeId", AiSchema.integer("ecom.ai.param.storeId"), false)
                .prop("level", AiSchema.enumOf("ecom.ai.param.replenishLevel", ReplenishLevel.URGENT.name(), ReplenishLevel.WARN.name(), ReplenishLevel.OK.name()), false)
                .prop("keyword", AiSchema.string("ecom.ai.param.skuKeyword").maxLength(64), false)
                .prop("top", AiSchema.integer("ecom.ai.param.top").min(1).max(AiTool.MAX_ROWS), false)
                .build();
    }

    @Override
    public String permission() {
        return "ecom:replenish:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        ReplenishLevel level = args.has("level") ? ReplenishLevel.valueOf(args.getString("level")) : null;
        int top = args.getInt("top", 50);
        if (STAGE_PURCHASE.equals(args.getString("stage"))) {
            List<PurchaseSuggestRow> rows = replenishService.purchaseRows(args.getString("keyword"), level, ReplenishWeights.DEFAULT);
            return AiToolResult.rows(rows.subList(0, Math.min(top, rows.size())), (long) rows.size());
        }
        List<ReplenishRow> rows = replenishService.rows(args.getLong("storeId"), args.getString("keyword"), level);
        return AiToolResult.rows(rows.subList(0, Math.min(top, rows.size())), (long) rows.size());
    }
}
