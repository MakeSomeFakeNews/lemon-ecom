package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 平台仓补货建议的一行（店铺 × 在线商品），件数均为平台 SKU 件数（物料数量 ÷ 多件装）：
 * 日均销量 = 最近 7 / 14 / 30 / 60 天日均的加权平均 × 系数（设置了手工日均时取手工日均）；
 * 可售天数 = (平台仓库存 + 头程在途) ÷ 日均；平台仓目标 = 日均 × (补货周期 + 安全天数)；
 * 建议补货 = 平台仓目标 − 平台仓库存 − 头程在途（向上取整，不为负）；预计断货日期 = 今天 + 可售天数；
 * 断货日期在补货周期内为紧急，在补货周期 + 安全天数内为预警
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
    /** 店铺默认发货仓（本地仓），生成发货计划时作为调出仓 */
    private Long warehouseId;
    /** 店铺平台仓 */
    private Long platformWarehouseId;
    /** 最近 7 / 14 / 30 / 60 天销量（平台 SKU 件数） */
    private BigDecimal sales7;
    private BigDecimal sales14;
    private BigDecimal sales30;
    private BigDecimal sales60;
    /** 按销量加权计算的日均（乘系数前） */
    private BigDecimal weightedDaily;
    /** 季节 / 活动系数（设置），没有设置为 1 */
    private BigDecimal factor;
    /** 是否使用了手工日均 */
    private boolean dailyOverridden;
    /** 计算用的日均销量 */
    private BigDecimal dailySales;
    /** 平台仓库存（件数，系统库存 ÷ 多件装） */
    private BigDecimal platformQty;
    /** 头程在途（已发运未签收的货件） */
    private BigDecimal inTransitQty;
    /** 最早到达的在途货件的预计到达日期 */
    private LocalDate inTransitEta;
    /** 本地仓库存（件数，店铺默认发货仓） */
    private BigDecimal localQty;
    private BigDecimal coverDays;
    /** 平台仓补货周期（天） */
    private Integer leadDays;
    private Integer safetyDays;
    /** 平台仓目标库存 = 日均 × (补货周期 + 安全天数) */
    private BigDecimal targetQty;
    /** 平台仓建议补货（件数） */
    private BigDecimal suggestQty;
    private LocalDate stockoutDate;
    private ReplenishLevel level;
    /** 设置了不补货 */
    private boolean disabled;
    /** 有在线商品级的补货设置 */
    private boolean hasSetting;
    /** 组合商品：库存、在途按组件折算（各组件 库存 ÷ 用量 的最小值） */
    private boolean bundle;
    /** 一件平台 SKU 包含的物料（单品为 物料 × 多件装），本地仓采购按它折算 */
    @JsonIgnore
    private List<BundleComponent> components;

    /** 店铺默认补货周期 / 安全天数（查询结果，计算时按设置覆盖） */
    private Integer storeLeadDays;
    private Integer storeSafetyDays;
}
