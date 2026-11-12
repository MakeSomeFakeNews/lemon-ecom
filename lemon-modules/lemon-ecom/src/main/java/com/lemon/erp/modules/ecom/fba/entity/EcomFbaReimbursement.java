package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FBA 赔偿记录（赔偿报告的一行），自动匹配索赔后记录 claimId
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_reimbursement")
public class EcomFbaReimbursement extends BaseEntity {

    private Long storeId;
    private String reimbursementId;
    private LocalDate approvalDate;
    private String caseId;
    private String amazonOrderId;
    private String reason;
    private String sellerSku;
    private String fnsku;
    private String asin;
    private String currency;
    private BigDecimal amountPerUnit;
    private BigDecimal amountTotal;
    private BigDecimal qtyCash;
    private BigDecimal qtyInventory;
    private BigDecimal amountBase;
    private Long claimId;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String claimNo;
}
