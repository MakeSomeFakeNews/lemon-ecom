package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

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

    /** 组成方式：单品（materialId × packQty）或组合（bundleItems），为空按单品 */
    private ListingComposeType composeType;

    /** FNSKU（亚马逊等平台仓的商品标签条码） */
    @Size(max = 32, message = "{ecom.valid.catalog.fnskuCannotExceedCharacters}")
    private String fnsku;

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
    /** 组合商品的组成（组成方式为组合时保存、详情与列表返回） */
    @Valid
    @Size(max = 50, message = "{ecom.valid.catalog.bundleTooManyItems}")
    @TableField(exist = false)
    private List<EcomBundleItem> bundleItems;
    /** 近 30 天销量（平台 SKU 件数，不含待付款、已取消的订单；只在列表返回） */
    @TableField(exist = false)
    private BigDecimal sales30;

    /** 最近一次回传给平台的可售数量（渠道同步中心只回传和上次不同的） */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private BigDecimal lastPushedQty;
    /** 最近一次回传库存的时间 */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private java.time.LocalDateTime lastPushedTime;
    /** 手工库存（锁定）1 是：批量改库存时设置，自动库存回传跳过它，直到在「改价记录 / 在线商品」中解除锁定 */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Integer stockLocked;
    /** 平台附加 ID（JSON 对象，如 Shopify 变体 ID / 库存项 ID、平台仓库 ID），由拉取在线商品写入，回传库存时交给连接器 */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String platformExtra;
}
