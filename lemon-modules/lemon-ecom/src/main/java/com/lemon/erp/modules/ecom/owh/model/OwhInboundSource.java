package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓入库单的来源（字典 ecom_owh_inbound_source）：收货完成后按来源把 ERP 的在途库存转为海外仓可用
 */
public enum OwhInboundSource {
    /** 头程货件（ecom_inbound，收货完成时签收货件） */
    INBOUND,
    /** 在途调拨单（wms_stock_doc，收货完成时调拨收货） */
    TRANSFER
}
