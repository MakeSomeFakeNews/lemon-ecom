package com.lemon.erp.modules.ecom.publish.model;

/**
 * 刊登记录的发布状态（字典 ecom_publish_status）：草稿 →（提交）待发布 → 发布中 → 成功 / 失败；失败可重试，成功后可下架
 */
public enum PublishStatus {
    /** 草稿（可编辑） */
    DRAFT,
    /** 待发布（已提交，等待后台执行） */
    PENDING,
    /** 发布中（正在调用平台接口，或平台异步处理中） */
    PUBLISHING,
    /** 成功 */
    SUCCESS,
    /** 失败（显示平台返回的错误，可修改后重试） */
    FAILED,
    /** 已下架 */
    OFFLINE
}
