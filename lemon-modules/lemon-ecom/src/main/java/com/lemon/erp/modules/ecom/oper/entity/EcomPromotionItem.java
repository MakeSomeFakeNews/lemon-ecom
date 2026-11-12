package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 促销活动的参与 SKU：促销价与折扣二选一（只填折扣时按原价计算促销价）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_promotion_item")
public class EcomPromotionItem extends BaseEntity {

    private Long promotionId;
    private Long listingId;
    @NotBlank(message = "{ecom.valid.oper.promoSkuRequired}")
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;
    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;
    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String parentItemId;
    @Size(max = 255, message = "{ecom.valid.oper.titleTooLong}")
    private String title;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal originalPrice;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal promoPrice;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.discountInvalid}")
    @DecimalMax(value = "100", message = "{ecom.valid.oper.discountInvalid}")
    private BigDecimal discountPercent;
    @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}")
    private Integer promoQty;
    @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}")
    private Integer limitPerBuyer;
    private Integer sort;
}
