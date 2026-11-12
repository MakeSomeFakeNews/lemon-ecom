package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓退件单状态（字典 ecom_owh_return_status）：草稿 →（推送）→ 已推送（等待买家退件到仓）→（服务商收货、质检）→ 已收货
 * （ERP 退货入库，不良品转不良品仓或报废）；草稿、已推送可取消
 */
public enum OwhReturnStatus {
    /** 草稿 */
    DRAFT,
    /** 已推送：服务商已生成退件单 */
    PUSHED,
    /** 已收货：服务商收货质检完成 */
    RECEIVED,
    /** 已取消 */
    CANCELLED
}
