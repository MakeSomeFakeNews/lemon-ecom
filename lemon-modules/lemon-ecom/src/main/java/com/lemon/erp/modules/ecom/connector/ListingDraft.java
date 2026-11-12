package com.lemon.erp.modules.ecom.connector;

import java.math.BigDecimal;

/**
 * 从平台拉取的一个在线商品（平台 SKU）
 *
 * @param fulfillment PLATFORM 平台仓配送 / SELLER 自发货，未知为 null
 * @param status      ACTIVE / INACTIVE / INCOMPLETE / DELETED（ListingStatus 取值），未知为 null
 */
public record ListingDraft(String sellerSku, String platformItemId, String parentItemId, String title, String variant,
                           BigDecimal price, String currency, BigDecimal platformQty, String fulfillment, String status,
                           String imageUrl, String url) {
}
