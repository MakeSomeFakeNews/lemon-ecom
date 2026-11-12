package com.lemon.erp.modules.ecom.publish.connector;

import java.util.Map;

/**
 * 平台上已有的一个在线商品（改价、拉取竞品价格、下架时使用），ID 取自在线商品
 *
 * @param platformItemId 平台 SKU / 变体 ID（没有规格时为商品 ID；亚马逊为 ASIN）
 * @param parentItemId   所属商品 ID，没有为 null
 * @param extra          在线商品的平台附加 ID（platform_extra），不为 null
 */
public record ListingRef(String sellerSku, String platformItemId, String parentItemId, Map<String, String> extra) {

    public ListingRef {
        extra = extra == null ? Map.of() : extra;
    }

    public String extra(String key) {
        String v = extra.get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 商品 ID：有父 ID 时为父 ID，否则为平台商品 ID */
    public String productId() {
        return parentItemId != null && !parentItemId.isBlank() ? parentItemId : platformItemId;
    }

    /** 有规格（父 ID 不为空）时的规格 ID，否则为 null */
    public String variantId() {
        return parentItemId != null && !parentItemId.isBlank() ? platformItemId : null;
    }
}
