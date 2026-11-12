package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓入库单状态（字典 ecom_owh_inbound_status）：草稿 →（推送）→ 已推送 →（服务商开始上架）→ 收货中 →（收货完成，ERP 在途转可用）→ 已收货；草稿可取消
 */
public enum OwhInboundStatus {
    /** 草稿：还没有推送到服务商（推送失败也停留在草稿，记录原因） */
    DRAFT,
    /** 已推送：服务商已生成入库单，等待到仓 */
    PUSHED,
    /** 收货中：已部分上架 */
    RECEIVING,
    /** 已收货：服务商收货完成 */
    RECEIVED,
    /** 已取消 */
    CANCELLED
}
