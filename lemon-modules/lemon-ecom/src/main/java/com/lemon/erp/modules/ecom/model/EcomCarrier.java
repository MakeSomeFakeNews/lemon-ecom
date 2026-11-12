package com.lemon.erp.modules.ecom.model;

/**
 * 对接的物流商（字典 ecom_carrier）：物流渠道选择后，包裹可在线申请运单号、获取面单（carrier/CarrierConnector 的实现）
 */
public enum EcomCarrier {
    /** 递四方 4PX（开放平台 open.4px.com） */
    FOURPX,
    /** 云途物流 YunExpress */
    YUNEXPRESS
}
