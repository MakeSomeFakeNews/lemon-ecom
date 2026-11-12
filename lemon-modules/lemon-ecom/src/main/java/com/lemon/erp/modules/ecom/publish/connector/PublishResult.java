package com.lemon.erp.modules.ecom.publish.connector;

import java.util.Map;

/**
 * 发布 / 查询发布结果
 *
 * @param status         SUCCESS 成功、PENDING 平台异步处理中（带 ref，之后由定时任务查询）、FAILED 失败（message 为平台返回的错误）
 * @param platformItemId 平台商品 ID（亚马逊发布后要等平台生成 ASIN，可能为 null，之后由拉取在线商品补上）
 * @param url            商品链接，没有为 null
 * @param skuIds         平台 SKU → 平台 SKU / 变体 ID
 * @param skuExtra       平台 SKU → 附加 ID（如 eBay offerId、Shopify 变体 ID / 库存项 ID），写入在线商品的 platform_extra
 * @param ref            平台异步处理编号（沃尔玛 feedId 等）
 */
public record PublishResult(Status status, String platformItemId, String url, Map<String, String> skuIds, Map<String, Map<String, String>> skuExtra,
                            String ref, String message) {

    public enum Status {
        SUCCESS, PENDING, FAILED
    }

    public PublishResult {
        skuIds = skuIds == null ? Map.of() : skuIds;
        skuExtra = skuExtra == null ? Map.of() : skuExtra;
    }

    public static PublishResult success(String platformItemId, String url, Map<String, String> skuIds, Map<String, Map<String, String>> skuExtra,
                                        String message) {
        return new PublishResult(Status.SUCCESS, platformItemId, url, skuIds, skuExtra, null, message);
    }

    public static PublishResult pending(String ref, String message) {
        return new PublishResult(Status.PENDING, null, null, Map.of(), Map.of(), ref, message);
    }

    public static PublishResult failed(String message) {
        return new PublishResult(Status.FAILED, null, null, Map.of(), Map.of(), null, message);
    }
}
