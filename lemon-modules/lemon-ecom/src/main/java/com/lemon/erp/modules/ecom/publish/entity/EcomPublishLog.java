package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.publish.model.PublishAction;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 刊登日志：提交、调用平台发布、查询异步结果、下架，失败时记录平台返回的错误
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_publish_log")
public class EcomPublishLog extends BaseEntity {

    private Long itemId;

    private Long storeId;

    private PublishAction action;

    /** 1 成功 0 失败 */
    private Integer success;

    private String message;

    private Long createBy;

    @TableField(exist = false)
    private String operatorName;
}
