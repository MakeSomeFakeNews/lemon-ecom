package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单内部备注（历史）：每次添加一条，订单上的备注（remark）为最新一条
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_note")
public class EcomOrderNote extends BaseEntity {

    private Long orderId;
    private String content;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
