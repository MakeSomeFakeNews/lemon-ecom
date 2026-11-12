package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地仓采购建议的一行（物料，数量为物料的基本单位）：
 * 本地仓安全库存 = 日均 × 采购周期 + 物料安全库存；
 * 建议采购 = 平台仓建议补货（各店铺合计）+ 本地仓安全库存 − 本地仓库存 − 采购在途（已审核未入库的采购订单），向上取整、不为负，
 * 不足默认供应商最小起订量时按起订量。
 * 采购周期取补货设置 → 默认供应商的提前期 → 店铺补货周期（多个店铺取最长）
 */
@Data
public class PurchaseSuggestRow {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    /** 物料日均销量（各店铺平台 SKU 日均 × 多件装 × 组件数量之和；物料级手工日均优先） */
    private BigDecimal dailySales;
    private boolean dailyOverridden;
    /** 各店铺的平台仓建议补货合计（物料数量） */
    private BigDecimal platformSuggestQty;
    /** 各店铺平台仓库存合计（物料数量） */
    private BigDecimal platformQty;
    /** 头程在途（物料数量） */
    private BigDecimal inboundTransitQty;
    private LocalDate inboundEta;
    /** 物料资料中的安全库存 */
    private BigDecimal safetyStock;
    /** 本地仓安全库存 = 日均 × 采购周期 + 物料安全库存 */
    private BigDecimal localSafetyQty;
    /** 本地仓库存（启用店铺的默认发货仓，不含平台仓） */
    private BigDecimal localQty;
    /** 采购在途：已审核、部分入库的采购订单的未入库数量 */
    private BigDecimal purchaseTransitQty;
    /** 最早的采购在途交货日期 */
    private LocalDate purchaseEta;
    /** 采购周期（天） */
    private Integer purchaseDays;
    /** 采购周期的来源：SETTING 补货设置、SUPPLIER 供应商提前期、STORE 店铺补货周期 */
    private String purchaseDaysFrom;
    /** 本地可用天数 = (本地仓库存 + 采购在途 − 平台仓建议补货) ÷ 日均，没有销量为空 */
    private BigDecimal localCoverDays;
    /** 建议采购量 */
    private BigDecimal suggestQty;
    /** 今天下单的预计到货日期 = 今天 + 采购周期 */
    private LocalDate arrivalDate;
    private ReplenishLevel level;
    private boolean disabled;
    private boolean hasSetting;
    /** 默认供应商 */
    private Long supplierId;
    private String supplierName;
    private Integer supplierLeadDays;
    private BigDecimal minQty;
    /** 默认供应商按建议量的含税单价 */
    private BigDecimal price;
    private BigDecimal taxRate;
    /** 多店铺需求构成 */
    private List<Demand> demands = new ArrayList<>();

    /**
     * 一个在线商品的需求：qty 为平台仓建议补货折算的物料数量，dailySales 为折算的物料日均，warehouseId 为店铺默认发货仓
     */
    public record Demand(Long storeId, String storeName, Long listingId, String sellerSku, BigDecimal qty, BigDecimal dailySales,
                         Long warehouseId) {
    }
}
