package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓调拨单状态（字典 ecom_owh_transfer_status）：草稿 →（提交）→ 调出仓处理中（调出仓是海外仓时，服务商出库单已推送）
 * →（调出仓出库）→ 在途（ERP 在途调拨单已调出；调入仓是海外仓时生成并推送入库单）→（调入仓收货）→ 已收货；草稿、调出仓处理中可取消
 */
public enum OwhTransferStatus {
    /** 草稿 */
    DRAFT,
    /** 调出仓处理中 */
    OUT_PUSHED,
    /** 在途 */
    IN_TRANSIT,
    /** 已收货 */
    RECEIVED,
    /** 已取消 */
    CANCELLED
}
