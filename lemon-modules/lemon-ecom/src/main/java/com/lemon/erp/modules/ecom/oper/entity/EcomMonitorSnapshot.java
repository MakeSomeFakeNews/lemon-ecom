package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商品监控的每日快照：同一监控每天一条，同一天再次取数 / 录入时覆盖。没有取到的指标为空（不覆盖为空，沿用前一次的值比较）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_monitor_snapshot")
public class EcomMonitorSnapshot extends BaseEntity {

    private Long monitorId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate snapshotDate;
    private String category;
    private Integer rankMain;
    private String subCategory;
    private Integer rankSub;
    private BigDecimal rating;
    private Integer reviewCount;
    private Integer sellerCount;
    private BigDecimal price;
    private String title;
    private String mainImage;
    private String bullets;
    private ListingStatus listingStatus;
    private EcomMonitorSource source;
}
