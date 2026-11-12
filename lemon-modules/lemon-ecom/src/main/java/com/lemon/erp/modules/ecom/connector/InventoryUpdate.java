package com.lemon.erp.modules.ecom.connector;

import java.math.BigDecimal;

/**
 * 回传给平台的一个 SKU 的可售数量（平台 SKU 件数，已按多件装折算并扣除安全库存）
 *
 * @param platformItemId 平台商品 ID（部分平台按商品 + SKU 更新库存时需要）
 */
public record InventoryUpdate(String sellerSku, String platformItemId, BigDecimal qty) {
}
