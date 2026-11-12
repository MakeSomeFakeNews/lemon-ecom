package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 运营月度目标：月份 + 对象（店铺 / 运营人员）唯一，金额为本位币
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ops_target")
public class EcomOpsTarget extends BaseEntity {

    private String targetMonth;
    private EcomOpsTargetType targetType;
    private Long refId;
    private Long storeId;
    private BigDecimal salesTarget;
    private BigDecimal profitTarget;
    private Integer orderTarget;
    private String remark;
    private Long createBy;

    @TableField(exist = false)
    private String refName;
    @TableField(exist = false)
    private EcomPlatform platform;
}
