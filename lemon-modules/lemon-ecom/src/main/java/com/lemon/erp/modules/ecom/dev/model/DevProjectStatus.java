package com.lemon.erp.modules.ecom.dev.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 开发项目状态（字典 ecom_dev_status）：待调研 → 调研中 → 打样中 → 待审批 → 已通过（生成物料）/ 已驳回；进行中的项目可以放弃。
 * 看板上拖拽只能在「可手工流转」的状态之间移动，进入待审批必须提交审批，通过 / 驳回由审批决定
 */
public enum DevProjectStatus {
    /** 待调研 */
    PENDING_RESEARCH,
    /** 调研中 */
    RESEARCHING,
    /** 打样中 */
    SAMPLING,
    /** 待审批（流程已发布时为审批中） */
    PENDING_APPROVAL,
    /** 已通过 */
    APPROVED,
    /** 已驳回 */
    REJECTED,
    /** 已放弃 */
    ABANDONED;

    /** 手工流转（看板拖拽 / 按钮）允许的目标状态 */
    private static final Map<DevProjectStatus, Set<DevProjectStatus>> MOVES = Map.of(
            PENDING_RESEARCH, EnumSet.of(RESEARCHING, ABANDONED),
            RESEARCHING, EnumSet.of(PENDING_RESEARCH, SAMPLING, ABANDONED),
            SAMPLING, EnumSet.of(RESEARCHING, ABANDONED),
            REJECTED, EnumSet.of(RESEARCHING, SAMPLING, ABANDONED),
            ABANDONED, EnumSet.of(PENDING_RESEARCH, RESEARCHING));

    /** 可以提交审批的状态 */
    public static final Set<DevProjectStatus> SUBMITTABLE = EnumSet.of(RESEARCHING, SAMPLING, REJECTED);

    /** 可以修改资料的状态（审批中、已通过后不能改） */
    public static final Set<DevProjectStatus> EDITABLE = EnumSet.of(PENDING_RESEARCH, RESEARCHING, SAMPLING, REJECTED);

    public boolean canMoveTo(DevProjectStatus target) {
        return MOVES.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean editable() {
        return EDITABLE.contains(this);
    }
}
