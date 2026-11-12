package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自动调价日志：原价、新价、竞品最低价 / 购物车价、底价、原因、是否推送成功
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_reprice_log")
public class EcomRepriceLog extends BaseEntity {

    private Long ruleId;

    private Long listingId;

    private Long storeId;

    private String sellerSku;

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private BigDecimal lowestPrice;

    private BigDecimal buyboxPrice;

    private BigDecimal floorPrice;

    private String currency;

    private String reason;

    private RepriceLogStatus status;

    private String message;

    private LocalDateTime runTime;

    private Long createBy;

    @TableField(exist = false)
    private String ruleName;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String title;
}
