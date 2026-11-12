package com.lemon.erp.modules.ecom.assistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.assistant.model.OpsAbnormalType;
import com.lemon.erp.modules.ecom.assistant.model.OpsAdAlertType;
import com.lemon.erp.modules.ecom.assistant.model.OpsTodoKind;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 运营日报的结构化内容（以 JSON 存在 ecom_ops_report.content）：全部由程序从业务数据汇总，不依赖 AI。
 * 金额为本位币；比率、变化为百分数（字段名带 Percent / Change）。要点（highlights）只存代码与参数，查看时按当前语言生成文字
 */
@Data
public class OpsReportContent {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    /** 范围内的店铺 */
    private List<StoreRef> stores = new ArrayList<>();
    /** 当天核心指标 */
    private Kpi kpi = new Kpi();
    /** 前一天（环比） */
    private Kpi prev = new Kpi();
    /** 此前 7 天的日均 */
    private Kpi avg7 = new Kpi();
    private List<StoreKpi> storeRows = new ArrayList<>();
    private List<AbnormalSku> abnormalSkus = new ArrayList<>();
    /** 各列表最多保留 30 行，以下为截断前的总数 */
    private int abnormalTotal;
    private int stockRiskTotal;
    private int stockUrgentTotal;
    private int adAlertTotal;
    private List<BadReview> badReviews = new ArrayList<>();
    private List<StockRisk> stockRisks = new ArrayList<>();
    private List<AdAlert> adAlerts = new ArrayList<>();
    private List<Todo> todos = new ArrayList<>();
    private List<Highlight> highlights = new ArrayList<>();

    public record StoreRef(Long id, String name, EcomPlatform platform, String marketplace) {
    }

    /** 核心指标 */
    @Data
    public static class Kpi {
        private long orders;
        private BigDecimal qty = BigDecimal.ZERO;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        /** 利润率（%） */
        private BigDecimal marginPercent;
        private BigDecimal adSpend = BigDecimal.ZERO;
        private BigDecimal adSales = BigDecimal.ZERO;
        /** 广告花费 ÷ 广告销售额（%） */
        private BigDecimal acosPercent;
        /** 广告花费 ÷ 总销售额（%） */
        private BigDecimal tacosPercent;
        private BigDecimal refund = BigDecimal.ZERO;
        /** 新登记的退货退款单数 */
        private long returns;
    }

    /** 店铺当天的指标与环比 */
    @Data
    public static class StoreKpi {
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String marketplace;
        private long orders;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private BigDecimal marginPercent;
        private BigDecimal adSpend = BigDecimal.ZERO;
        private BigDecimal acosPercent;
        private BigDecimal prevSales = BigDecimal.ZERO;
        private BigDecimal prevProfit = BigDecimal.ZERO;
        private BigDecimal salesChange;
        private BigDecimal profitChange;
    }

    /** 异常 SKU：current 为当天（退货率为近 7 天），baseline 为对比基准（前 7 天日均 / 此前 30 天退货率） */
    @Data
    public static class AbnormalSku {
        private OpsAbnormalType type;
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String title;
        private BigDecimal current;
        private BigDecimal baseline;
        private BigDecimal changePercent;
        /** 辅助信息：当天件数 / 前 7 天日均件数、广告花费、退款等，便于判断原因 */
        private BigDecimal qty;
        private BigDecimal avgQty;
        private BigDecimal adSpend;
        private BigDecimal avgAdSpend;
        private BigDecimal refund;
    }

    /** 新差评（1–2 星） */
    @Data
    public static class BadReview {
        private Long id;
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private Integer rating;
        private String title;
        private String content;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDate;
    }

    /** 断货风险（取补货建议中紧急 / 预警的） */
    @Data
    public static class StockRisk {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String title;
        private BigDecimal dailySales;
        private BigDecimal platformQty;
        private BigDecimal inTransitQty;
        /** 可售天数 */
        private BigDecimal coverDays;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate stockoutDate;
        private BigDecimal suggestQty;
        private ReplenishLevel level;
    }

    /** 广告异常（金额为店铺币种） */
    @Data
    public static class AdAlert {
        private OpsAdAlertType type;
        private Long storeId;
        private String storeName;
        private Long campaignId;
        private String campaignName;
        private String currency;
        private BigDecimal spend;
        private BigDecimal sales;
        private long orders;
        private BigDecimal budget;
        private BigDecimal acosPercent;
        private BigDecimal avgAcosPercent;
    }

    /** 待处理事项 */
    public record Todo(OpsTodoKind kind, long count, String link) {
    }

    /**
     * 要点：level 为 GOOD / WARN / BAD / INFO，code 对应消息键 ecom.text.asst.hl.&lt;code&gt;，args 为消息参数；
     * text 在返回详情时按当前语言生成（不存库）
     */
    @Data
    public static class Highlight {
        private String level;
        private String code;
        private List<String> args = new ArrayList<>();
        private String text;

        public static Highlight of(String level, String code, Object... args) {
            Highlight h = new Highlight();
            h.level = level;
            h.code = code;
            for (Object a : args) {
                h.args.add(a == null ? "" : String.valueOf(a));
            }
            return h;
        }
    }
}
