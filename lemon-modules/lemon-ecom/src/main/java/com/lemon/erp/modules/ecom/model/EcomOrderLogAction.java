package com.lemon.erp.modules.ecom.model;

/**
 * 订单操作日志的动作（ecom_order_log.action），订单详情「处理记录」按动作显示名称和颜色
 */
public enum EcomOrderLogAction {
    /** 订单写入系统（渠道同步、Excel 导入、手工建单、补发） */
    CREATE,
    /** 平台状态变化（同步） */
    PLATFORM_STATUS,
    /** 命中审单规则（指定仓库 / 物流、截单时间） */
    RULE,
    /** 打标签 */
    TAG,
    /** 去标签 */
    UNTAG,
    /** 审核通过（生成包裹） */
    AUDIT,
    /** 异常（SKU 未匹配、地址校验不通过） */
    EXCEPTION,
    /** 缺货 */
    OUT_OF_STOCK,
    /** 重新检查库存 */
    RECHECK,
    /** 重新匹配 SKU */
    REMATCH,
    /** 挂起 */
    HOLD,
    /** 取消挂起 */
    UNHOLD,
    /** 修改订单信息 / 收件地址 */
    EDIT,
    /** 指定发货仓 / 物流渠道 */
    ASSIGN,
    /** 包裹变化（合并、拆分、取消包裹） */
    PACKAGE,
    /** 申请运单号 / 填写运单号 */
    LABEL,
    /** 打印面单 */
    PRINT,
    /** 出库 */
    OUTBOUND,
    /** 发货 */
    SHIP,
    /** 回传平台发货结果 */
    SHIP_CONFIRM,
    /** 取消订单 */
    CANCEL,
    /** 退货退款 */
    RETURN,
    /** 退款 */
    REFUND,
    /** 补发 */
    RESEND,
    /** 内部备注 */
    NOTE
}
