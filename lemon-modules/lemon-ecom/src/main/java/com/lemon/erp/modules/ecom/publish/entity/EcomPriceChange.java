package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 改价 / 改库存历史：每个在线商品每次修改一条（批量修改同一批次号），记录原值、新值、来源、是否推送成功
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_price_change")
public class EcomPriceChange extends BaseEntity {

    private String batchNo;

    private Long listingId;

    private Long storeId;

    private String sellerSku;

    private PriceChangeType changeType;

    private BigDecimal oldValue;

    private BigDecimal newValue;

    private String currency;

    private PriceChangeSource source;

    private PriceChangeStatus status;

    private String message;

    private Long ruleId;

    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private String operatorName;
    @TableField(exist = false)
    private String ruleName;
}
