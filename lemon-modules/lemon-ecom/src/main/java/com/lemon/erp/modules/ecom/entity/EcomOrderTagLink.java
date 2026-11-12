package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单打标：一张订单的一个标签
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_tag_link")
public class EcomOrderTagLink extends BaseEntity {

    private Long orderId;
    private Long tagId;
    /** 由审单规则自动打上 1是 0人工 */
    private Integer autoTagged;
}
