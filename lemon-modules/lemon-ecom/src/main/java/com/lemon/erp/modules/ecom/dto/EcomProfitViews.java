package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.model.TaxType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 多维利润报表、VAT 汇总的结构。金额均为本位币；费用、成本为正数（减项），利润 = 不含税销售额 − 各项费用 − 成本
 */
public final class EcomProfitViews {

    private EcomProfitViews() {
    }

    /**
     * 统计单元：店铺 × 平台 SKU × 收件国家 × 月份（yyyyMM）。由 SQL 汇总订单 / 结算数据，再在服务中补上分摊费用与广告花费
     */
    @Data
    public static class Cell {
        private Long storeId;
        private String sellerSku;
        private String country;
        private Integer period;
        private Long materialId;
        /** 本单元内的订单数（同一订单的多个 SKU 各计一次） */
        private Long orders;
        /** 订单第一行在本单元的订单数（按订单级维度汇总时不重复计数） */
        private Long orderHeads;
        private BigDecimal qty;
        /** 含税销售额 */
        private BigDecimal salesGross;
        /** 平台代扣代缴的税 */
        private BigDecimal withheld;
        /** 需卖家申报的 VAT */
        private BigDecimal vat;
        private BigDecimal commission;
        private BigDecimal fbaFee;
        private BigDecimal storageFee;
        private BigDecimal returnFee;
        private BigDecimal otherFee;
        private BigDecimal refund;
        private BigDecimal adSpend;
        private BigDecimal cost;
        private BigDecimal headCost;
        private BigDecimal lastMile;
    }

    /** 按类型汇总的金额（结算明细、费用分摊、广告花费） */
    @Data
    public static class TypedCell {
        private Long storeId;
        private String sellerSku;
        private String country;
        private Integer period;
        private SettlementItemType itemType;
        /** 广告花费的店铺币种 */
        private String currency;
        private BigDecimal amount;
    }

    @Data
    public static class StoreAttr {
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String marketplace;
        private String currency;
        private Long ownerId;
        private String ownerName;
    }

    @Data
    public static class SkuAttr {
        private Long storeId;
        private String sellerSku;
        private String parentItemId;
        private String title;
        private Long materialId;
        private String materialCode;
        private String materialName;
    }

    /** 利润报表的一行（按所选维度汇总） */
    @Data
    public static class ProfitRow {
        /** 维度取值（店铺 ID、平台、SKU 等），前端行键 */
        private String dimKey;
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String marketplace;
        private String sellerSku;
        /** 父商品（SPU），没有父商品时为 SKU */
        private String spu;
        private String title;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Long ownerId;
        private String ownerName;
        private String country;
        /** 月份 yyyy-MM */
        private String month;

        private Long orders;
        private BigDecimal qty;
        /** 销售额（含税） */
        private BigDecimal salesGross;
        /** 销售额（不含税）= 含税 − 平台代扣税 − 需申报 VAT */
        private BigDecimal salesNet;
        private BigDecimal commission;
        private BigDecimal fbaFee;
        /** 仓储费（分摊） */
        private BigDecimal storageFee;
        private BigDecimal adSpend;
        private BigDecimal refund;
        private BigDecimal returnFee;
        private BigDecimal otherFee;
        private BigDecimal cost;
        private BigDecimal headCost;
        private BigDecimal lastMile;
        private BigDecimal vat;
        private BigDecimal withheldTax;
        /** 毛利 = 不含税销售额 − 佣金 − 配送费 − 仓储费 − 广告 − 退款 − 退货处理费 − 其他费用 − 商品成本 − 头程 − 尾程 */
        private BigDecimal profit;
        /** 毛利率（%）= 毛利 ÷ 不含税销售额 */
        private BigDecimal margin;
        /** ROI（%）= 毛利 ÷ (商品成本 + 头程) */
        private BigDecimal roi;
    }

    /** VAT 汇总：收件国家 × 月份 */
    @Data
    public static class VatRow {
        private String country;
        private Integer period;
        private String month;
        private TaxType taxType;
        /** 平台代扣代缴 1 是 */
        private Integer facilitator;
        /** 订单币种；同一国家月份有多个币种时为空（只看本位币） */
        private String currency;
        private Long orders;
        /** 应申报销售额（不含税，扣除退款） */
        private BigDecimal taxableFc;
        private BigDecimal taxableBase;
        /** 销项税 */
        private BigDecimal vatFc;
        private BigDecimal vatBase;
        /** 平台代扣代缴 */
        private BigDecimal withheldFc;
        private BigDecimal withheldBase;
        /** 头程关税（不可抵扣，供参考） */
        private BigDecimal duty;
        /** 进口增值税（可抵扣的进项） */
        private BigDecimal importVat;
        /** 应缴 = 销项税 − 进口增值税（本位币） */
        private BigDecimal payable;
    }
}
