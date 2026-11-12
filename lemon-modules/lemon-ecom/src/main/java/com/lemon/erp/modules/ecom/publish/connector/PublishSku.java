package com.lemon.erp.modules.ecom.publish.connector;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 发布的一个平台 SKU
 *
 * @param variantAttrs  变体属性：属性名 → 值（单品为空）
 * @param variantText   变体属性显示文字（如 Red / XL），没有为 null
 * @param platformSkuId 已有的平台 SKU / 变体 ID（重新发布时），没有为 null
 */
public record PublishSku(String sellerSku, Map<String, String> variantAttrs, String variantText, String barcode, BigDecimal price, int qty,
                         String platformSkuId) {

    public PublishSku {
        variantAttrs = variantAttrs == null ? Map.of() : variantAttrs;
    }
}
