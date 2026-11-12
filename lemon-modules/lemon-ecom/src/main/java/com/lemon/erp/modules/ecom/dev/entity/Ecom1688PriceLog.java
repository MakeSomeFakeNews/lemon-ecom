package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 1688 价格变动记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_1688_price_log")
public class Ecom1688PriceLog extends BaseEntity {

    /** 1688 关联（ecom_1688_offer.id） */
    private Long offerId;
    private Long materialId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    /** 变动幅度（%） */
    private BigDecimal changeRate;
}
