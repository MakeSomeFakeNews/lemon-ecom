package com.lemon.erp.modules.ecom.demo.fix;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.ads.AdsReportRow;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomDashboardMapper;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.service.EcomAdIngestService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 品牌推广（SB）与展示型推广（SD）（order 170）：亚马逊美国 / 德国 / 日本站各两个 SB 活动（品牌关键词、品类关键词 + 竞品 ASIN，
 * 其中一个已暂停）与两个 SD 活动（浏览再营销受众、竞品详情页投放），最近 60 天的活动报告、投放报告、SB 搜索词报告、SD 推广商品报告。
 * 规模按店铺同期的订单销售额校准：SB + SD 的广告销售额约为店铺销售额的 {@value #SALES_SHARE}（基础演示数据中 SP 约占 31%，合计约 40%），
 * ACOS 由各投放的 CPC 与转化率决定（约 25%–35%）；
 * 经报告写入服务（{@link EcomAdIngestService}，与接口同步相同的写入逻辑）生成活动 / 广告组 / 投放与日报，利润分析按活动级日报扣除 SB 花费
 */
@Component
@RequiredArgsConstructor
public class SbSdAdsDemoContributor implements DemoContributor {

    static final int DAYS = 60;
    /** SB + SD 广告销售额占店铺订单销售额的比例 */
    static final double SALES_SHARE = 0.09;
    private static final String[] STORES = {"DEMO-AMZ-US", "DEMO-AMZ-DE", "DEMO-AMZ-JP"};

    private final EcomStoreService storeService;
    private final EcomListingMapper listingMapper;
    private final EcomAdIngestService ingestService;
    private final EcomDashboardMapper dashboardMapper;

    /** 一个投放：类型、文字、匹配方式、平均 CPC、转化率 */
    record Target(String group, EcomAdTargetType type, String text, EcomAdMatchType match, double cpc, double cvr) {
    }

    /** 一个活动 */
    record Campaign(String name, EcomAdType adType, EcomAdStatus status, BigDecimal budget, double scale, List<Target> targets) {
    }

    @Override
    public int order() {
        return 170;
    }

    @Override
    public String key() {
        return "adsSbSd";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        for (String code : STORES) {
            Long storeId = stores == null ? null : stores.get(code);
            EcomStore store = storeId == null ? storeService.findByCode(code) : storeService.get(storeId);
            if (store == null) {
                continue;
            }
            List<EcomListing> listings = Queries.limit(listingMapper, Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                    .eq(EcomListing::getStatus, ListingStatus.ACTIVE).isNotNull(EcomListing::getMaterialId).orderByAsc(EcomListing::getId), 6);
            generateStore(ctx, store, listings);
        }
    }

    private void generateStore(DemoContext ctx, EcomStore store, List<EcomListing> listings) {
        double fx = currencyScale(store.getCurrency());
        String market = store.getMarketplace() == null ? "US" : store.getMarketplace();
        String asin = listings.isEmpty() || listings.get(0).getPlatformItemId() == null ? "B0DEMO0001" : listings.get(0).getPlatformItemId();
        List<Campaign> campaigns = List.of(
                new Campaign("SB | Brand Hero | " + market, EcomAdType.SB, EcomAdStatus.ENABLED, money(30 * fx), 1.0, List.of(
                        new Target("Brand KW", EcomAdTargetType.KEYWORD, "lemon home", EcomAdMatchType.EXACT, 0.65, 0.16),
                        new Target("Brand KW", EcomAdTargetType.KEYWORD, "lemon lamp", EcomAdMatchType.PHRASE, 0.72, 0.13),
                        new Target("Category KW", EcomAdTargetType.KEYWORD, "led desk lamp", EcomAdMatchType.BROAD, 1.35, 0.07),
                        new Target("Category KW", EcomAdTargetType.KEYWORD, "reading light", EcomAdMatchType.PHRASE, 1.10, 0.06))),
                new Campaign("SB | Video | Competitor " + market, EcomAdType.SB, EcomAdStatus.PAUSED, money(20 * fx), 0.6, List.of(
                        new Target("Competitor ASIN", EcomAdTargetType.PRODUCT, "asinSameAs=\"B08COMP001\"", null, 1.25, 0.05),
                        new Target("Competitor ASIN", EcomAdTargetType.PRODUCT, "asinSameAs=\"B09COMP002\"", null, 1.05, 0.04))),
                new Campaign("SD | Views Remarketing | " + market, EcomAdType.SD, EcomAdStatus.ENABLED, money(25 * fx), 0.9, List.of(
                        new Target("Remarketing 30d", EcomAdTargetType.AUTO, "views(exactProduct,lookback=\"30\")", null, 0.55, 0.09),
                        new Target("Remarketing 30d", EcomAdTargetType.AUTO, "purchases(relatedProduct,lookback=\"60\")", null, 0.48, 0.07))),
                new Campaign("SD | Competitor PDP | " + market, EcomAdType.SD, EcomAdStatus.ENABLED, money(15 * fx), 0.7, List.of(
                        new Target("Competitor PDP", EcomAdTargetType.PRODUCT, "asinSameAs=\"" + asin + "X\"", null, 0.85, 0.05),
                        new Target("Competitor PDP", EcomAdTargetType.PRODUCT, "asinCategorySameAs=\"Desk Lamps\"", null, 0.70, 0.04))));
        double price = 32 * fx;
        LocalDate today = ctx.today();
        // 按店铺同期订单销售额校准规模：先按基准量生成一次估算广告销售额，再按比例生成（ACOS 由 CPC 与转化率决定，不受规模影响）
        BigDecimal storeSales = dashboardMapper.selectOrderAmount(today.minusDays(DAYS).atStartOfDay(), today.atStartOfDay(), store.getId());
        double generated = build(ctx, campaigns, listings, fx, price, 1).campaign().stream().mapToDouble(r -> r.getSales().doubleValue()).sum();
        double volume = storeSales == null || storeSales.signum() <= 0 || generated <= 0 ? 0.3
                : Math.max(0.05, Math.min(5, storeSales.doubleValue() * SALES_SHARE / generated));
        Rows rows = build(ctx, campaigns, listings, fx, price, volume);
        List<AdsReportRow> campaignRows = rows.campaign();
        List<AdsReportRow> targetRows = rows.target();
        List<AdsReportRow> termRows = rows.term();
        List<AdsReportRow> productRows = rows.product();
        // 活动报告先写（建立活动、预算与状态），再写投放、搜索词与推广商品
        ingestService.ingest(store, EcomAdReportType.CAMPAIGN, null, campaignRows);
        ingestService.ingest(store, EcomAdReportType.TARGETING, null, targetRows);
        if (!termRows.isEmpty()) {
            ingestService.ingest(store, EcomAdReportType.SEARCH_TERM, null, termRows);
        }
        if (!productRows.isEmpty()) {
            ingestService.ingest(store, EcomAdReportType.PRODUCT, null, productRows);
        }
    }

    /** 一次生成的四种报告行 */
    record Rows(List<AdsReportRow> campaign, List<AdsReportRow> target, List<AdsReportRow> term, List<AdsReportRow> product) {
    }

    /** 生成最近 DAYS 天的报告行，volume 为曝光量的倍数 */
    private Rows build(DemoContext ctx, List<Campaign> campaigns, List<EcomListing> listings, double fx, double price, double volume) {
        LocalDate today = ctx.today();
            List<AdsReportRow> campaignRows = new ArrayList<>();
            List<AdsReportRow> targetRows = new ArrayList<>();
            List<AdsReportRow> termRows = new ArrayList<>();
            List<AdsReportRow> productRows = new ArrayList<>();
            for (Campaign c : campaigns) {
                for (int i = DAYS; i >= 1; i--) {
                    LocalDate date = today.minusDays(i);
                    // 暂停的活动最近 15 天没有数据；周末流量高一些，后半段整体增长
                    if (c.status() == EcomAdStatus.PAUSED && i <= 15) {
                        continue;
                    }
                    double season = (date.getDayOfWeek().getValue() >= 6 ? 1.25 : 1.0) * (1 + (DAYS - i) / (double) DAYS * 0.35);
                    AdsReportRow total = row(c, date, null);
                    for (Target t : c.targets()) {
                        long impressions = Math.round(ctx.between(300, 900) * c.scale() * season * volume);
                        long clicks = Math.max(0, Math.round(impressions * (0.003 + ctx.random().nextDouble() * 0.004)));
                        int orders = randomRound(ctx, clicks * t.cvr() * (0.6 + ctx.random().nextDouble() * 0.8));
                        BigDecimal spend = money(clicks * t.cpc() * fx * (0.85 + ctx.random().nextDouble() * 0.3));
                        BigDecimal sales = money(orders * price * (0.9 + ctx.random().nextDouble() * 0.3));
                        AdsReportRow tr = row(c, date, t);
                        fill(tr, impressions, clicks, spend, sales, orders);
                        targetRows.add(tr);
                        add(total, tr);
                        if (c.adType() == EcomAdType.SB && t.type() == EcomAdTargetType.KEYWORD && clicks > 0) {
                            // 搜索词：投放词本身、长尾词，品类词另有一个零转化的词（规则 / 否定建议）
                            String[] terms = {t.text(), t.text() + " for office", t.match() == EcomAdMatchType.BROAD ? "cheap lamp free" : t.text() + " white"};
                            long left = clicks;
                            int leftOrders = orders;
                            for (int k = 0; k < terms.length; k++) {
                                long cl = k == terms.length - 1 ? left : Math.max(0, Math.round(clicks * (k == 0 ? 0.55 : 0.3)));
                                cl = Math.min(cl, left);
                                left -= cl;
                                int od = k == 2 && t.match() == EcomAdMatchType.BROAD ? 0 : (k == terms.length - 1 ? leftOrders : Math.min(leftOrders, (int) Math.round(orders * (k == 0 ? 0.6 : 0.3))));
                                leftOrders -= od;
                                AdsReportRow st = row(c, date, t);
                                st.setSearchTerm(terms[k]);
                                fill(st, Math.round(impressions * cl / (double) Math.max(1, clicks)), cl,
                                        money(spend.doubleValue() * cl / Math.max(1, clicks)), money(price * od), od);
                                termRows.add(st);
                            }
                        }
                    }
                    campaignRows.add(total);
                    if (c.adType() == EcomAdType.SD && !listings.isEmpty()) {
                        // 推广商品：活动花费按 2 个 SKU 拆分
                        int n = Math.min(2, listings.size());
                        for (int k = 0; k < n; k++) {
                            EcomListing l = listings.get((k + (c.name().contains("PDP") ? 2 : 0)) % listings.size());
                            double share = n == 1 ? 1 : (k == 0 ? 0.65 : 0.35);
                            AdsReportRow pr = row(c, date, null);
                            pr.setAdGroupName(c.targets().get(0).group());
                            pr.setSellerSku(l.getSellerSku());
                            pr.setAsin(l.getPlatformItemId());
                            int od = k == 0 ? (int) Math.round(total.getOrders() * share) : total.getOrders() - (int) Math.round(total.getOrders() * 0.65);
                            fill(pr, Math.round(total.getImpressions() * share), Math.round(total.getClicks() * share), money(total.getSpend().doubleValue() * share),
                                    money(total.getSales().doubleValue() * share), od);
                            productRows.add(pr);
                        }
                    }
                }
            }
        return new Rows(campaignRows, targetRows, termRows, productRows);
    }

    /** 按小数部分的概率进位（点击少时订单不会总是被舍成 0） */
    private static int randomRound(DemoContext ctx, double v) {
        int base = (int) Math.floor(v);
        return base + (ctx.random().nextDouble() < v - base ? 1 : 0);
    }

    private static AdsReportRow row(Campaign c, LocalDate date, Target t) {
        AdsReportRow r = new AdsReportRow();
        r.setAdType(c.adType());
        r.setDate(date);
        r.setCampaignName(c.name());
        r.setCampaignStatus(c.status());
        r.setCampaignBudget(c.budget());
        if (t != null) {
            r.setAdGroupName(t.group());
            r.setTargetType(t.type());
            r.setTargetText(t.text());
            r.setMatchType(t.match());
        }
        return r;
    }

    private static void fill(AdsReportRow r, long impressions, long clicks, BigDecimal spend, BigDecimal sales, int orders) {
        r.setImpressions(impressions);
        r.setClicks(clicks);
        r.setSpend(spend);
        r.setSales(sales);
        r.setOrders(orders);
        r.setUnits(orders);
    }

    private static void add(AdsReportRow total, AdsReportRow r) {
        total.setImpressions(total.getImpressions() + r.getImpressions());
        total.setClicks(total.getClicks() + r.getClicks());
        total.setSpend(total.getSpend().add(r.getSpend()));
        total.setSales(total.getSales().add(r.getSales()));
        total.setOrders(total.getOrders() + r.getOrders());
        total.setUnits(total.getUnits() + r.getUnits());
    }

    /** 店铺币种相对美元的大致倍数（让金额看起来合理） */
    static double currencyScale(String currency) {
        if (currency == null) {
            return 1;
        }
        return switch (currency) {
            case "JPY" -> 150;
            case "EUR", "GBP" -> 0.95;
            default -> 1;
        };
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
