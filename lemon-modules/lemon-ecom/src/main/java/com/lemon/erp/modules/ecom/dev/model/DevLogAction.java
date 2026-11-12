package com.lemon.erp.modules.ecom.dev.model;

/**
 * 开发项目流转记录的操作类型（前端按语言包显示）
 */
public enum DevLogAction {
    /** 创建 */
    CREATE,
    /** 手工流转（看板拖拽 / 按钮） */
    MOVE,
    /** 提交审批 */
    SUBMIT,
    /** 撤回审批 */
    WITHDRAW,
    /** 审批通过 */
    APPROVE,
    /** 审批驳回 */
    REJECT,
    /** 放弃 */
    ABANDON,
    /** 生成物料 */
    MATERIAL
}
