package com.lemon.erp.modules.ecom.owh.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 海外仓出库单状态（字典 ecom_owh_outbound_status）：推送失败（可重推）/ 已推送 →（服务商出库、返回运单号）→ 已出库
 * →（ERP 生成销售出库并回传平台）→ 已发货；已推送的可取消，服务商需要人工截单时为截单中
 */
public enum OwhOutboundStatus {
    /** 推送失败：记录原因，可重新推送 */
    FAILED,
    /** 已推送：服务商处理中（待拣货 / 打包） */
    PUSHED,
    /** 截单中：已申请取消，等待服务商确认 */
    INTERCEPTING,
    /** 已出库：服务商已出库并返回运单号，ERP 还没有发货成功 */
    OUTBOUND,
    /** 已发货：ERP 已出库并回传平台 */
    SHIPPED,
    /** 已取消 */
    CANCELLED;

    /** 占用包裹的状态（包裹不能再推送、不能在本地取消） */
    public static final Set<OwhOutboundStatus> ACTIVE = EnumSet.of(PUSHED, INTERCEPTING, OUTBOUND, SHIPPED);
    /** 需要定时同步的状态 */
    public static final Set<OwhOutboundStatus> SYNCING = EnumSet.of(PUSHED, INTERCEPTING, OUTBOUND);
}
