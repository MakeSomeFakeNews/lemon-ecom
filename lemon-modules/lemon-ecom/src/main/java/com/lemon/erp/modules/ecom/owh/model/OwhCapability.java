package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓接口的能力（账号页面按它显示可用的操作）
 */
public enum OwhCapability {
    /** 仓库列表 */
    WAREHOUSES,
    /** 物流产品列表 */
    PRODUCTS,
    /** 库存查询 */
    INVENTORY,
    /** 入库单创建 / 查询 */
    INBOUND,
    /** 出库单创建 / 查询 */
    OUTBOUND,
    /** 出库单取消 / 截单 */
    CANCEL,
    /** 费用查询 */
    FEES,
    /** 退件单创建 / 查询 */
    RETURN,
    /** 调拨出库（海外仓之间 / 海外仓发往本地仓） */
    TRANSFER,
    /** 已推送的入库单可接口取消 */
    CANCEL_INBOUND,
    /** 已推送的退件单可接口取消 */
    CANCEL_RETURN
}
