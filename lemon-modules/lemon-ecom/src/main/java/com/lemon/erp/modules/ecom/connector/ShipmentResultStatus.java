package com.lemon.erp.modules.ecom.connector;

/**
 * 回传发货的结果
 */
public enum ShipmentResultStatus {
    /** 已把承运商、运单号回传平台（或平台已确认发货） */
    CONFIRMED,
    /** 平台物流托管：运单号由平台分配，ERP 不能 / 不需要回传（Lazada、虾皮 SLS、Ozon、美客多 Mercado Envíos 等），可能带回平台运单号 */
    PLATFORM_MANAGED,
    /** 跳过：本次没有需要回传的内容（如平台仓配送的订单、平台上已发货） */
    SKIPPED
}
