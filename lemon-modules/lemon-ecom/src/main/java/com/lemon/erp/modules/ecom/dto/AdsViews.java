package com.lemon.erp.modules.ecom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 广告管理的视图：指标、层级报表行、规则对象与建议、搜索词、TACOS、趋势、授权
 */
public final class AdsViews {

    private AdsViews() {
    }

    /**
     * 广告指标：曝光、点击、花费、销售额、订单为合计；比率按合计计算（ACOS、CTR、CVR 为百分数，ROAS、CPC 为倍数 / 金额），
     * 分母为 0 时为空
     */
    @Data
    public static class Metrics {
        private long impressions;
        private long clicks;
        private BigDecimal spend = BigDecimal.ZERO;
        private BigDecimal sales = BigDecimal.ZERO;
        private long orders;
        private BigDecimal acos;
        private BigDecimal roas;
        private BigDecimal ctr;
        private BigDecimal cpc;
        private BigDecimal cvr;

        public Metrics add(Metrics o) {
            if (o != null) {
                impressions += o.impressions;
                clicks += o.clicks;
                spend = Decimals.nz(spend).add(Decimals.nz(o.spend));
                sales = Decimals.nz(sales).add(Decimals.nz(o.sales));
                orders += o.orders;
            }
            return this;
        }

        /** 按合计计算比率 */
        public Metrics fill() {
            spend = Decimals.money(Decimals.nz(spend));
            sales = Decimals.money(Decimals.nz(sales));
            acos = ratio(spend, sales, true);
            roas = ratio(sales, spend, false);
            ctr = ratio(BigDecimal.valueOf(clicks), BigDecimal.valueOf(impressions), true);
            cpc = ratio(spend, BigDecimal.valueOf(clicks), false);
            cvr = ratio(BigDecimal.valueOf(orders), BigDecimal.valueOf(clicks), true);
            return this;
        }

        /** 按倍数折算金额（折本位币） */
        public Metrics scale(BigDecimal rate) {
            spend = Decimals.nz(spend).multiply(rate);
            sales = Decimals.nz(sales).multiply(rate);
            return this;
        }

        public static BigDecimal ratio(BigDecimal part, BigDecimal whole, boolean percent) {
            if (part == null || whole == null || whole.signum() == 0) {
                return null;
            }
            BigDecimal v = part.divide(whole, 6, RoundingMode.HALF_UP);
            return (percent ? v.multiply(Decimals.HUNDRED) : v).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** 层级报表的一行（活动 / 广告组 / 投放），金额为店铺币种；prev 为环比（上一个同样长度的期间），yoy 为同比（去年同期） */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HierarchyRow extends Metrics {
        private EcomAdLevel level;
        private Long id;
        private Long storeId;
        private String storeName;
        private String currency;
        private Long campaignId;
        private String campaignName;
        private Long adGroupId;
        private String name;
        private EcomAdType adType;
        private EcomAdStatus status;
        private EcomAdTargetType targetType;
        private EcomAdMatchType matchType;
        /** 活动为日预算，广告组为默认竞价，投放为当前竞价 */
        private BigDecimal bidOrBudget;
        private BigDecimal baseBid;
        private String platformId;
        /** 花费、广告销售额折本位币（按统计截止日所在月的汇率；没有维护汇率时为空），用于跨币种排序 */
        private BigDecimal baseSpend;
        private BigDecimal baseSales;
        private Metrics prev;
        private Metrics yoy;
    }

    /** 层级报表 */
    @Data
    public static class Hierarchy {
        private LocalDate prevBegin;
        private LocalDate prevEnd;
        private LocalDate yoyBegin;
        private LocalDate yoyEnd;
        private List<HierarchyRow> rows = new ArrayList<>();
    }

    /** 概览：金额折本位币（按统计截止日所在月的汇率），prev 为环比、yoy 为同比；tacos = 广告花费 / 订单销售额（利润分析口径） */
    @Data
    public static class Overview {
        private Metrics current;
        private Metrics prev;
        private Metrics yoy;
        private BigDecimal totalSales;
        private BigDecimal tacos;
        private long campaigns;
        private long enabledCampaigns;
        private long pendingSuggestions;
        private long wasteTerms;
        private BigDecimal wasteSpend;
    }

    /** 趋势（本位币） */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TrendRow extends Metrics {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
    }

    /** 搜索词汇总：waste 表示高花费 / 多点击零转化，suggestNegative 表示建议否定（还没有对应的否定关键词） */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SearchTermRow extends Metrics {
        private Long storeId;
        private String storeName;
        private String currency;
        private Long campaignId;
        private String campaignName;
        private Long adGroupId;
        private String adGroupName;
        private String keywordText;
        private String matchType;
        private String searchTerm;
        private boolean waste;
        private boolean negated;
    }

    /** SKU 维度的 TACOS：销售额、广告花费为本位币（取利润分析的结果），ACOS 按广告销售额 */
    @Data
    public static class TacosRow {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String title;
        private BigDecimal sales;
        private BigDecimal profit;
        private BigDecimal adSpend;
        private BigDecimal adSales;
        private BigDecimal acos;
        private BigDecimal tacos;
        private long adClicks;
        private long adOrders;
    }

    /** 规则的一个对象（活动 / 广告组 / 投放）及其当前值 */
    @Data
    public static class RuleEntity {
        private EcomAdLevel level;
        private Long id;
        private Long storeId;
        private String storeName;
        private String currency;
        private Long campaignId;
        private String campaignName;
        private String campaignTags;
        private EcomAdType adType;
        private Long adGroupId;
        private String name;
        private EcomAdStatus status;
        private String platformId;
        private EcomAdTargetType targetType;
        private BigDecimal bid;
        private BigDecimal baseBid;
        private BigDecimal scheduleFactor;
        private BigDecimal budget;
    }

    /** 规则命中的一条建议（预览时不保存，执行时写入执行日志） */
    @Data
    public static class Suggestion {
        private EcomAdLevel level;
        private Long entityId;
        private Long storeId;
        private String storeName;
        private String currency;
        private Long campaignId;
        private String campaignName;
        private String entityName;
        private com.lemon.erp.modules.ecom.model.EcomAdAction action;
        private BigDecimal oldValue;
        private BigDecimal newValue;
        /** 竞价类：平台上实际设置的竞价（有分时系数时 = 新基准竞价 × 系数） */
        private BigDecimal effectiveBid;
        private EcomAdStatus oldStatus;
        private EcomAdStatus newStatus;
        private Metrics metrics;
        /** 平台 ID、投放类型（执行时调用广告接口） */
        private String platformId;
        private EcomAdTargetType targetType;
    }

    /** 规则执行结果 */
    public record RunResult(int matched, int applied, int pending, int failed, int notified, int skipped, String message) {
    }

    /** 报告导入结果 */
    public record ImportResult(com.lemon.erp.modules.ecom.model.EcomAdReportType reportType, int rows, int written, int campaignsCreated,
                               int adGroupsCreated, int targetsCreated, List<String> errors) {
    }

    /** 同步结果 */
    public record SyncResult(int campaigns, int adGroups, int targets, Map<String, Integer> reportRows, String message) {
    }

    /** 广告接口说明（前端按它生成授权表单） */
    public record ConnectorView(EcomPlatform platform, String authType, List<FieldView> fields, boolean hasSandbox, String productionBaseUrl,
                                String sandboxBaseUrl, List<String> capabilities, String docUrl, BigDecimal minBid, boolean needsProfile) {
    }

    public record FieldView(String key, String label, boolean secret, boolean required) {
    }

    /** 店铺的广告授权状态（不返回授权内容，只返回已填写的键） */
    @Data
    public static class StoreAuth {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private EcomPlatform platform;
        private String currency;
        private boolean connectorSupported;
        private boolean hasCredentials;
        private List<String> credentialKeys;
        private Integer adsSandbox;
        private String adsBaseUrl;
        private String adsProfileId;
        private Integer adsSync;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime adsLastSyncTime;
        private String adsLastSyncMessage;
    }
}
