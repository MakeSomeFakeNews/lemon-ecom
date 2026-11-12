package com.lemon.erp.modules.ecom.publish.model;

/**
 * 刊登日志的动作（字典 ecom_publish_action）
 */
public enum PublishAction {
    /** 提交发布 / 重试 */
    SUBMIT,
    /** 调用平台发布 */
    PUBLISH,
    /** 查询平台异步处理结果 */
    CHECK,
    /** 下架 */
    OFFLINE
}
