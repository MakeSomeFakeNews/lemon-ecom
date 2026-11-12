package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.model.CompetitorSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞品价格快照：在线商品在某一时刻的竞品最低价（及运费）、购物车价、本店是否持有购物车、卖家数。
 * 亚马逊经 Product Pricing API 拉取，其他平台手工录入或 Excel 导入；自动调价取每个商品最近的一条
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_competitor_price")
public class EcomCompetitorPrice extends BaseEntity {

    @NotNull(message = "{ecom.valid.pub.listingRequired}")
    private Long listingId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long storeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String sellerSku;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String platformItemId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private CompetitorSource source;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal lowestPrice;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal lowestShipping;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal buyboxPrice;

    /** 本店持有购物车 1 是 */
    private Integer buyboxWinner;

    @Min(value = 0, message = "{ecom.valid.pub.qtyRange}")
    private Integer offerCount;

    @Size(max = 128, message = "{ecom.valid.pub.competitorTooLong}")
    private String competitor;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String currency;

    private LocalDateTime snapshotTime;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String title;
    /** 本店当前售价 */
    @TableField(exist = false)
    private BigDecimal myPrice;
}
