package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ASIN / 商品监控：平台 + 站点 + 商品 ID 唯一。最新的排名、评分、评论数、跟卖数、标题主图等取自最近一次快照
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_monitor")
public class EcomMonitor extends BaseEntity {

    @NotNull(message = "{ecom.valid.oper.monitorTypeRequired}")
    private EcomMonitorType monitorType;
    @NotNull(message = "{ecom.valid.selectPlatform}")
    private EcomPlatform platform;
    @NotBlank(message = "{ecom.valid.selectMarketplace}")
    @Size(max = 16)
    private String marketplace;
    @NotBlank(message = "{ecom.valid.oper.itemIdRequired}")
    @Size(max = 64, message = "{ecom.valid.oper.itemIdTooLong}")
    private String itemId;
    private Long storeId;
    private Long listingId;
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;
    private Long benchmarkId;
    @Size(max = 128, message = "{ecom.valid.oper.brandTooLong}")
    private String brand;
    @Size(max = 500, message = "{ecom.valid.oper.titleTooLong}")
    private String title;
    @Size(max = 500, message = "{ecom.valid.oper.imageTooLong}")
    private String mainImage;
    private String category;
    private Integer rankMain;
    private String subCategory;
    private Integer rankSub;
    private BigDecimal rating;
    private Integer reviewCount;
    private Integer sellerCount;
    private BigDecimal price;
    private String currency;
    private ListingStatus listingStatus;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastSnapshotDate;
    private Integer autoFetch;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFetchTime;
    private String lastFetchMessage;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.thresholdInvalid}")
    @DecimalMax(value = "1000", message = "{ecom.valid.oper.thresholdInvalid}")
    private BigDecimal rankDropPercent;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.thresholdInvalid}")
    @DecimalMax(value = "5", message = "{ecom.valid.oper.thresholdInvalid}")
    private BigDecimal ratingDrop;
    private Long notifyUserId;
    private EnableStatus status;
    @Size(max = 255, message = "{ecom.valid.oper.remarkTooLong}")
    private String remark;
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String benchmarkItemId;
    @TableField(exist = false)
    private String benchmarkTitle;
    @TableField(exist = false)
    private String notifyUserName;
    /** 未处理的预警数 */
    @TableField(exist = false)
    private Integer newAlerts;
    /** 大类排名较上一次快照的变化（正数为上升） */
    @TableField(exist = false)
    private Integer rankChange;
    /** 评分较上一次快照的变化 */
    @TableField(exist = false)
    private BigDecimal ratingChange;
    /** 评论数较上一次快照的变化 */
    @TableField(exist = false)
    private Integer reviewChange;
    /** 详情：五点描述（取最近一次快照） */
    @TableField(exist = false)
    private String bullets;
}
