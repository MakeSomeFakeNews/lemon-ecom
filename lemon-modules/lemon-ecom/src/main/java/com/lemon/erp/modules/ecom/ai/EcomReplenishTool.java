package com.lemon.erp.modules.ecom.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 平台仓补货建议：每个在售 SKU 的日均销量、平台仓库存、在途、可售天数、预计断货日期和建议补货量，可只看紧急 / 预警的
 */
@Component
@RequiredArgsConstructor
public class EcomReplenishTool implements AiTool {

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
        return "ecom.ai.tool.replenish.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        return AiSchema.object()
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
        List<ReplenishRow> rows = replenishService.rows(args.getLong("storeId"), args.getString("keyword"), level);
        int top = args.getInt("top", 50);
        return AiToolResult.rows(rows.subList(0, Math.min(top, rows.size())), (long) rows.size());
    }
}
