package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 刊登记录的一个变体（平台 SKU）：物料、平台 SKU、变体属性、条码、售价、库存；成本、建议价、预估利润由价格模板计算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_publish_variant")
public class EcomPublishVariant extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long itemId;

    @NotNull(message = "{ecom.valid.pub.materialRequired}")
    private Long materialId;

    @NotBlank(message = "{ecom.valid.pub.skuRequired}")
    @Size(max = 64, message = "{ecom.valid.pub.skuTooLong}")
    private String sellerSku;

    @Size(max = 128, message = "{ecom.valid.pub.variantTooLong}")
    private String variantText;

    /** 变体属性：属性名 → 值（如 Color → Red） */
    @TableField(exist = false)
    @Size(max = 5, message = "{ecom.valid.pub.tooManyVariantAttrs}")
    private Map<String, String> variantAttrs = new LinkedHashMap<>();

    @JsonIgnore
    private String variantAttrsJson;

    @Size(max = 32, message = "{ecom.valid.pub.barcodeTooLong}")
    private String barcode;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal price;

    @Min(value = 0, message = "{ecom.valid.pub.qtyRange}")
    @Max(value = 999999, message = "{ecom.valid.pub.qtyRange}")
    private Integer qty;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal cost;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal suggestedPrice;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal estProfit;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal estMargin;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String platformSkuId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long listingId;

    private Integer sort;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
