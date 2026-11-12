package com.lemon.erp.modules.ecom.carrier;

/**
 * 物流商下单结果
 *
 * @param carrierOrderNo 物流商系统中的订单号（取面单、取消时使用）
 * @param trackingNo     运单号（跟踪号），回传平台
 * @param labelUrl       面单地址（下单即返回面单的物流商），可空
 */
public record CarrierWaybill(String carrierOrderNo, String trackingNo, String labelUrl) {
}
