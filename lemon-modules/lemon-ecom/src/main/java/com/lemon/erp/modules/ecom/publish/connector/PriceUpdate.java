package com.lemon.erp.modules.ecom.publish.connector;

import java.math.BigDecimal;

/**
 * 推送给平台的一个 SKU 的新售价（店铺币种）
 */
public record PriceUpdate(ListingRef ref, BigDecimal price, String currency) {

    public String sellerSku() {
        return ref.sellerSku();
    }
}
