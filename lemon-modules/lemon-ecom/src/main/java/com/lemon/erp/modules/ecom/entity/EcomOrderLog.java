package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单操作日志：订单的关键动作（写入、规则、审核、挂起、打单、发货、回传、取消、退货、补发、备注等），由 EcomOrderLogService 统一记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_log")
public class EcomOrderLog extends BaseEntity {

    private Long orderId;
    private EcomOrderLogAction action;
    /** 内容（存库为默认语言） */
    private String content;
    /** 操作人，系统自动执行（同步、定时任务）时为空 */
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
