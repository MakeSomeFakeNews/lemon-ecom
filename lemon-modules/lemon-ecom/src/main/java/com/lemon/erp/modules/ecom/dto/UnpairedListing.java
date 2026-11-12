package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.math.BigDecimal;

/**
 * SKU 配对中心的未配对平台 SKU 与推荐（没有推荐时 suggestion 为空）
 */
public record UnpairedListing(Long listingId, Long storeId, String storeName, EcomPlatform platform, String sellerSku, String platformItemId,
                              String title, String variant, BigDecimal price, String currency, PairingSuggestion suggestion) {
}
