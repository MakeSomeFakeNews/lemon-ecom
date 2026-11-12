package com.lemon.erp.modules.ecom.publish.connector;

import java.util.List;
import java.util.Map;

/**
 * 发布到平台的商品内容（一个刊登记录）
 *
 * @param images         图片的公网地址（第一张为主图）
 * @param barcodeType    条码类型（UPC / EAN / GTIN / JAN / ISBN），没有为 null
 * @param upcExempt      UPC 豁免（亚马逊 supplier_declared_has_product_identifier_exemption）
 * @param categoryId     平台类目 ID（亚马逊为 Product Type），没有为 null
 * @param attributes     平台属性：属性键 → 值（多个值用英文逗号分隔；以 [ 或 { 开头的按 JSON 原样提交）
 * @param fulfillment    PLATFORM 平台仓配送 / SELLER 自发货
 * @param extra          其他平台参数（如 eBay paymentPolicyId、merchantLocationKey）
 * @param parentSku      父 SKU（多变体时）
 * @param platformItemId 已有的平台商品 ID（重新发布时），没有为 null
 * @param marketplace    店铺站点（如 US）
 */
public record PublishPayload(String title, List<String> bullets, String description, List<String> images, String brand, String barcodeType,
                             boolean upcExempt, String exemptionNote, String categoryId, Map<String, String> attributes, String currency,
                             String fulfillment, String shippingTemplate, String returnPolicy, Integer handlingDays, Map<String, String> extra,
                             String parentSku, String platformItemId, String marketplace, List<PublishSku> skus) {

    public PublishPayload {
        bullets = bullets == null ? List.of() : bullets;
        images = images == null ? List.of() : images;
        attributes = attributes == null ? Map.of() : attributes;
        extra = extra == null ? Map.of() : extra;
        skus = skus == null ? List.of() : skus;
    }

    /** 多个变体 */
    public boolean multiVariant() {
        return skus.size() > 1;
    }

    /** extra 中的值，空白为 null */
    public String extra(String key) {
        String v = extra.get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 属性值，空白为 null */
    public String attribute(String key) {
        String v = attributes.get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
