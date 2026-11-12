package com.lemon.erp.modules.ecom.dto;

import java.math.BigDecimal;

/**
 * 头程货件按平台 SKU 汇总的一行（装箱、装箱单、FNSKU 标签用）：没有平台 SKU 的明细按物料编码
 *
 * @param qty      平台 SKU 件数
 * @param boxedQty 已装箱件数
 */
public record InboundSkuLine(String sellerSku, Long listingId, String fnsku, String title, String materialCode, String materialName,
                             BigDecimal qty, BigDecimal boxedQty) {
}
