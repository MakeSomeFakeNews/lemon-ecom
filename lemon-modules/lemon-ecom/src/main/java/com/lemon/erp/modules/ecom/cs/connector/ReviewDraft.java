package com.lemon.erp.modules.ecom.cs.connector;

import java.time.LocalDateTime;

/**
 * 平台返回的一条商品评价
 *
 * @param reviewId        平台评价 ID（店铺内唯一）
 * @param platformItemId  平台商品 ID（与在线商品的 platformItemId 对应：有规格时为规格 ID）
 * @param parentItemId    所属商品 ID（有规格时），没有为 null
 * @param sellerSku       平台 SKU，平台没有返回时为 null（按在线商品补全）
 * @param replyContent    卖家已有的回复，没有为 null
 */
public record ReviewDraft(String reviewId, String platformItemId, String parentItemId, String sellerSku, int rating, String title,
                          String content, String reviewer, LocalDateTime reviewTime, String platformOrderNo, String replyContent,
                          LocalDateTime replyTime) {
}
