package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FBA 库存流水（库存分类账明细报告的一行），按行内容摘要去重
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_ledger")
public class EcomFbaLedger extends BaseEntity {

    private Long storeId;
    private String rowKey;
    private LocalDate eventDate;
    private String fnsku;
    private String asin;
    private String sellerSku;
    private String eventType;
    private String referenceId;
    private BigDecimal quantity;
    private String fulfillmentCenter;
    private String disposition;
    private String reason;
    private BigDecimal unreconciledQty;

    @TableField(exist = false)
    private String storeName;
}
