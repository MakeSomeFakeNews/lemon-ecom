package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料关联的 1688 商品（货源）：offerId + specId，同步得到的标题、规格、价格、起批量、库存；价格变动时记录并提醒
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_1688_offer")
public class Ecom1688Offer extends BaseEntity {

    private Long materialId;
    private String offerId;
    private String specId;
    private String skuId;
    private Long supplierId;
    private String sellerLoginId;
    private String sellerName;
    private String title;
    private String specText;
    private String imageUrl;
    private String unit;
    private BigDecimal price;
    private BigDecimal lastPrice;
    private String priceRanges;
    private Integer minOrderQty;
    private Integer stock;
    /** 1 个 1688 单位 = 多少物料单位 */
    private BigDecimal qtyRatio;
    private Integer priceAlert;
    private BigDecimal alertThreshold;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime priceChangeTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String syncMessage;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String materialUnit;
    @TableField(exist = false)
    private String materialImage;
    @TableField(exist = false)
    private String supplierName;
    /** 1688 商品链接 */
    @TableField(exist = false)
    private String offerUrl;
}
