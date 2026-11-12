package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.dev.model.DevLogAction;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开发项目流转记录：创建、手工流转、提交 / 撤回审批、通过 / 驳回、放弃、生成物料
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_dev_log")
public class EcomDevLog extends BaseEntity {

    private Long projectId;
    private DevLogAction action;
    private DevProjectStatus fromStatus;
    private DevProjectStatus toStatus;
    private String remark;
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
