package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 补货建议的一行（店铺 × 在线商品）：
 * 日均销量 = 最近 30 天销量 / 30；可售天数 = (平台仓库存 + 在途) / 日均；建议补货 = 日均 × (补货周期 + 安全天数) − 平台仓库存 − 在途，
 * 预计断货日期 = 今天 + 可售天数；断货日期在补货周期内为紧急，在补货周期 + 安全天数内为预警
 */
@Data
public class ReplenishRow {
    private Long storeId;
    private String storeName;
    private EcomPlatform platform;
    private Long listingId;
    private String sellerSku;
    private String platformItemId;
    private String title;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal packQty;
    /** 最近 30 天销量（平台 SKU 件数） */
    private BigDecimal sales30;
    private BigDecimal dailySales;
    /** 平台仓库存（件数，系统库存 ÷ 多件装） */
    private BigDecimal platformQty;
    /** 在途（已发运未签收的货件） */
    private BigDecimal inTransitQty;
    /** 本地仓库存（件数） */
    private BigDecimal localQty;
    private BigDecimal coverDays;
    private Integer leadDays;
    private Integer safetyDays;
    private BigDecimal suggestQty;
    private LocalDate stockoutDate;
    private ReplenishLevel level;
}
