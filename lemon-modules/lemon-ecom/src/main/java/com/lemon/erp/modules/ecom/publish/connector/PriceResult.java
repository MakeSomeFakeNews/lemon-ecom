package com.lemon.erp.modules.ecom.publish.connector;

/**
 * 一个 SKU 的改价结果，失败时 message 为平台返回的错误
 */
public record PriceResult(String sellerSku, boolean success, String message) {

    public static PriceResult ok(String sellerSku) {
        return new PriceResult(sellerSku, true, null);
    }

    public static PriceResult fail(String sellerSku, String message) {
        return new PriceResult(sellerSku, false, message);
    }
}
