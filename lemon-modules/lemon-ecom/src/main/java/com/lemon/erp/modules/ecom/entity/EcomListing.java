package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 在线商品（Listing）：店铺中的一个平台 SKU，映射到本地物料（一个平台 SKU = packQty 个物料，支持多件装）。
 * 订单导入时按 店铺 + 平台 SKU 找到物料，未映射的订单为异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_listing")
public class EcomListing extends BaseEntity {

    @NotNull(message = "{ecom.valid.selectStore}")
    private Long storeId;

    /** 平台 SKU（卖家 SKU） */
    @NotBlank(message = "{ecom.valid.sellerSkuRequired}")
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    /** 平台商品 ID（ASIN、Item ID 等） */
    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;

    /** 父商品 ID（变体的父 ASIN） */
    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String parentItemId;

    @Size(max = 255, message = "{ecom.valid.titleCannotExceedCharacters}")
    private String title;

    /** 变体属性，如 Red / XL */
    @Size(max = 128, message = "{ecom.valid.variantCannotExceedCharacters}")
    private String variant;

    /** 售价（店铺币种） */
    @DecimalMin(value = "0", message = "{ecom.valid.priceCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.priceDecimals}")
    private BigDecimal price;

    @Size(max = 8)
    private String currency;

    private FulfillmentType fulfillment;

    /** 平台可售数量（导入 / 同步带入） */
    private BigDecimal platformQty;

    private ListingStatus status;

    /** 映射的本地物料，为空表示未映射 */
    private Long materialId;

    /** 一个平台 SKU 对应的物料数量（多件装），默认 1 */
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.packQtyMustGreater}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal packQty;

    @Size(max = 255, message = "{ecom.valid.urlCannotExceedCharacters}")
    private String imageUrl;

    @Size(max = 255, message = "{ecom.valid.urlCannotExceedCharacters}")
    private String url;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
    /** 导入时用物料编码定位物料 */
    @TableField(exist = false)
    private String materialCodeInput;
    /** 导入时用店铺编码定位店铺 */
    @TableField(exist = false)
    private String storeCode;
}
