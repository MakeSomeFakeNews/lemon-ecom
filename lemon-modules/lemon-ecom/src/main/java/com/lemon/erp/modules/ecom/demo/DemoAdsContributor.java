package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdRule;
import com.lemon.erp.modules.ecom.entity.EcomAdRuleLog;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdSearchTerm;
import com.lemon.erp.modules.ecom.entity.EcomAdStat;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdDailyMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdRuleLogMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdRuleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdScheduleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdSearchTermMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdStatMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomStoreMapper;
import com.lemon.erp.modules.ecom.model.EcomAdAction;
import com.lemon.erp.modules.ecom.model.EcomAdDailyLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdMetric;
import com.lemon.erp.modules.ecom.model.EcomAdOperator;
import com.lemon.erp.modules.ecom.model.EcomAdRunMode;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * 演示数据 · 广告（order 80）：亚马逊美 / 德 / 日三个店铺的广告结构、近 120 天（截至昨天）报告、搜索词、规则与执行日志、分时策略。
 * <ul>
 *   <li>结构：每店 4–5 个商品推广（SP）活动（自动、手动关键词（精准 / 广泛）、商品投放（竞品 ASIN / 类目，ACOS 偏高）、品牌词，
 *       德国站另有一个已暂停的新品测试活动），每个活动 1–2 个广告组，投放每店二十个左右（关键词含匹配方式、ASIN / 类目表达式、
 *       自动投放的四种匹配组、一个否定精准词），活动带日预算、竞价策略、标签，投放的竞价 = 基准竞价。
 *       品牌推广（SB）、展示型推广（SD）由 {@code SbSdAdsDemoContributor} 生成，这里不重复</li>
 *   <li>报告：以店铺订单销售额（ecom_order，按天，7 天平滑）的 26%→36% 为广告销售额，按 ACOS（随时间缓慢改善）推出花费，
 *       因此跟随订单的增长、周末与大促峰值。先逐个投放生成每日数据（点击按 CPC、曝光按 CTR，订单按花费 ÷ 活动 / 投放的 ACOS 系数分配），
 *       再向上汇总：投放报告 ecom_ad_stat（TARGET）合计 = 活动报告 ecom_ad_stat（CAMPAIGN）= 广告日报 ecom_ad_daily 该活动各 SKU 合计，
 *       层级报表、概览 / 趋势、广告数据页与利润分析的广告花费口径一致</li>
 *   <li>搜索词：最近 {@value #SEARCH_TERM_DAYS} 天关键词 / 自动投放拆分到客户搜索词（不超过投放的数据），每店有一个高点击零转化的无关词（否定词演示），
 *       另一个无关词已在自动组否定（之后不再出现）</li>
 *   <li>规则：高 ACOS 投放降竞价（美国站，每天）、零转化投放暂停（每天）、低 ACOS 活动加预算（手动），按生成的数据计算命中对象，
 *       写入历史的已手工执行 / 已忽略记录和今天的待处理建议（没有广告接口，只记录建议），以及手工修改预算、暂停投放的记录；本地竞价 / 预算 / 状态与日志一致</li>
 *   <li>分时策略：美国站手动关键词活动一个（凌晨降价、白天与晚高峰加价）</li>
 * </ul>
 * 只写本模块的广告表，不设置店铺的广告授权，并关闭演示店铺的广告自动同步，不会访问广告接口。TikTok 等平台没有广告连接器，不生成
 */
@Component
@RequiredArgsConstructor
public class DemoAdsContributor implements DemoContributor {

    private static final int CHUNK = 500;
    /** 搜索词报告的天数 */
    static final int SEARCH_TERM_DAYS = 45;
    /** 自动组否定无关词的日期（几天前） */
    private static final int NEGATE_DAY = 20;
    /** 手工暂停一个 ASIN 投放的日期（几天前） */
    private static final int TARGET_PAUSE_DAY = 25;
    /** 德国站新品测试活动暂停的日期（几天前） */
    private static final int TEST_PAUSE_DAY = 35;
    private static final String STORE_US = "DEMO-AMZ-US";
    private static final String STORE_DE = "DEMO-AMZ-DE";
    private static final String STORE_JP = "DEMO-AMZ-JP";
    private static final String[] AMAZON_STORES = {STORE_US, STORE_DE, STORE_JP};
    private static final String[] AUTO_TARGETS = {"QUERY_HIGH_REL_MATCHES", "QUERY_BROAD_REL_MATCHES", "ASIN_SUBSTITUTE_RELATED", "ASIN_ACCESSORY_RELATED"};

    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;
    private final EcomAdStatMapper statMapper;
    private final EcomAdDailyMapper dailyMapper;
    private final EcomAdSearchTermMapper searchTermMapper;
    private final EcomAdRuleMapper ruleMapper;
    private final EcomAdRuleLogMapper ruleLogMapper;
    private final EcomAdScheduleMapper scheduleMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomStoreMapper storeMapper;

    @Override
    public int order() {
        return 80;
    }

    @Override
    public String key() {
        return "ads";
    }

    // ---------------------------------------------------------------- 内存模型

    private enum Kind { AUTO, KEYWORD, ASIN, BRAND, TEST }

    private static final class StoreAds {
        DemoCatalog.Store store;
        Long storeId;
        int scale;
        double acos;
        Long ownerId;
        final List<Camp> camps = new ArrayList<>();
    }

    private static final class Camp {
        final EcomAdCampaign e = new EcomAdCampaign();
        Kind kind;
        double share;
        double acos;
        /** 有数据的最近一天（几天前），暂停的活动之后没有数据 */
        int minD = 1;
        final List<Grp> groups = new ArrayList<>();
    }

    private static final class Grp {
        final EcomAdGroup e = new EcomAdGroup();
        Camp camp;
        final List<DemoListings.Item> skus = new ArrayList<>();
        final List<Tgt> targets = new ArrayList<>();
    }

    private static final class Tgt {
        final EcomAdTarget e = new EcomAdTarget();
        Grp grp;
        double tw = 1;
        double ta = 1;
        double cpc;
        double ctr;
        int minD = 1;
        final List<Term> terms = new ArrayList<>();
        long[] imp;
        long[] clk;
        BigDecimal[] spend;
        BigDecimal[] sales;
        int[] orders;
        int[] units;

        boolean live() {
            return e.getTargetType() != EcomAdTargetType.NEGATIVE;
        }
    }

    /** 搜索词：点击占比、是否会转化、出现的最近一天（几天前，否定后不再出现） */
    private record Term(String text, double share, boolean converts, int minD) {
    }

    /** 待写入的执行日志（对象 ID 在结构写入后才有） */
    private static final class LogPlan {
        Integer rule;
        LocalDateTime runTime;
        StoreAds sa;
        EcomAdLevel level;
        Camp camp;
        Tgt target;
        EcomAdAction action;
        BigDecimal oldValue;
        BigDecimal newValue;
        EcomAdStatus oldStatus;
        EcomAdStatus newStatus;
        AdsViews.Metrics metrics;
        EcomAdLogStatus status;
        String message;
        LocalDateTime appliedTime;
        Long appliedBy;
    }

    // ---------------------------------------------------------------- 生成

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<Long, Map<String, DemoListings.Item>> listings = ctx.get(DemoContributors.LISTINGS);
        List<Long> orderIds = ctx.get(DemoKeys.ORDER_IDS);
        if (stores == null || listings == null || orderIds == null || orderIds.isEmpty()) {
            return;
        }
        Map<String, Long> owners = ctx.get(DemoContributors.STORE_OWNER);
        List<Long> ids = new ArrayList<>();
        for (String code : AMAZON_STORES) {
            if (stores.get(code) != null) {
                ids.add(stores.get(code));
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, double[][]> sales = storeSales(ids, ctx.today(), ctx.days());
        List<StoreAds> all = new ArrayList<>();
        for (String code : AMAZON_STORES) {
            Long storeId = stores.get(code);
            Map<String, DemoListings.Item> items = storeId == null ? null : listings.get(storeId);
            double[][] s = storeId == null ? null : sales.get(storeId);
            if (items == null || s == null) {
                continue;
            }
            StoreAds sa = build(ctx, DemoCatalog.store(code), storeId, items, owners == null ? null : owners.get(code));
            if (sa != null && simulate(ctx, sa, s)) {
                budgets(sa, ctx.days());
                all.add(sa);
            }
        }
        if (all.isEmpty()) {
            return;
        }
        Long me = SecurityUtils.getUserIdOrNull();
        List<LogPlan> logs = new ArrayList<>();
        manualLogs(ctx, all, logs, me);
        List<EcomAdRule> rules = rules(ctx, all, logs, me);
        persistStructure(all);
        persistReports(ctx, all);
        persistRules(rules, logs);
        schedule(all.get(0));
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getAdsSync, 0).in(EcomStore::getId, stores.values()));
    }

    /** 店铺每天的订单销售额与订单数（店铺币种，不含已取消），下标为几天前 */
    private Map<Long, double[][]> storeSales(List<Long> storeIds, LocalDate today, int days) {
        Map<Long, double[][]> map = new HashMap<>();
        List<EcomOrder> orders = orderMapper.selectList(Wrappers.<EcomOrder>lambdaQuery()
                .select(EcomOrder::getStoreId, EcomOrder::getOrderTime, EcomOrder::getTotalAmount)
                .in(EcomOrder::getStoreId, storeIds)
                .ne(EcomOrder::getStatus, EcomOrderStatus.CANCELLED)
                .ge(EcomOrder::getOrderTime, today.minusDays(days + 7L).atStartOfDay()));
        for (EcomOrder o : orders) {
            if (o.getOrderTime() == null || o.getTotalAmount() == null) {
                continue;
            }
            int d = (int) ChronoUnit.DAYS.between(o.getOrderTime().toLocalDate(), today);
            if (d < 0 || d > days + 7) {
                continue;
            }
            double[][] a = map.computeIfAbsent(o.getStoreId(), k -> new double[2][days + 8]);
            a[0][d] += o.getTotalAmount().doubleValue();
            a[1][d] += 1;
        }
        return map;
    }

    // ---------------------------------------------------------------- 结构

    private StoreAds build(DemoContext ctx, DemoCatalog.Store st, Long storeId, Map<String, DemoListings.Item> items, Long owner) {
        List<DemoListings.Item> pool = items.values().stream()
                .filter(i -> i.materialId() != null && brandOf(i) != null && i.fulfillment() == FulfillmentType.PLATFORM)
                .sorted(Comparator.comparingInt(DemoListings.Item::popularity).reversed().thenComparing(DemoListings.Item::sku)).toList();
        if (pool.isEmpty()) {
            pool = items.values().stream().filter(i -> i.materialId() != null && brandOf(i) != null)
                    .sorted(Comparator.comparingInt(DemoListings.Item::popularity).reversed().thenComparing(DemoListings.Item::sku)).toList();
        }
        if (pool.isEmpty()) {
            return null;
        }
        Map<String, List<DemoListings.Item>> byBrand = new LinkedHashMap<>();
        Map<String, Integer> weight = new HashMap<>();
        for (DemoListings.Item i : pool) {
            String b = brandOf(i);
            byBrand.computeIfAbsent(b, k -> new ArrayList<>()).add(i);
            weight.merge(b, i.popularity(), Integer::sum);
        }
        List<String> brands = new ArrayList<>(byBrand.keySet());
        brands.sort(Comparator.comparing((String b) -> weight.get(b)).reversed().thenComparing(b -> b));
        String brandA = brands.get(0);
        String brandB = brands.size() > 1 ? brands.get(1) : brandA;
        List<DemoListings.Item> a = byBrand.get(brandA);
        List<DemoListings.Item> b = byBrand.get(brandB);
        String[][] kwA = keywords(brandA, st.marketplace());
        String[][] kwB = keywords(brandB, st.marketplace());
        String nameA = DemoCatalog.BRANDS.getOrDefault(brandA, brandA);
        String nameB = DemoCatalog.BRANDS.getOrDefault(brandB, brandB);

        StoreAds sa = new StoreAds();
        sa.store = st;
        sa.storeId = storeId;
        sa.scale = Currency.getInstance(st.currency()).getDefaultFractionDigits();
        sa.ownerId = owner;
        sa.acos = st.code().equals(STORE_DE) ? 0.30 : st.code().equals(STORE_JP) ? 0.25 : 0.27;
        List<String> competitors = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            competitors.add(asin(ctx));
        }

        // SP 自动
        Camp auto = camp(ctx, sa, Kind.AUTO, "SP-自动-" + nameA, 0.25, 1.1, EcomAdType.SP, "AUTO", "LEGACY_FOR_SALES", "主推"); // i18n-ignore 演示数据存库
        Grp ag = grp(ctx, auto, "Auto - " + nameA, skus(a, 0, 3));
        Tgt close = tgt(ctx, sa, ag, EcomAdTargetType.AUTO, AUTO_TARGETS[0], null, 0.75, 1.3, 1.0, 0.0045);
        close.terms.add(new Term(kwA[0][0], 0.5, true, 1));
        close.terms.add(new Term(kwA[1][0], 0.5, true, 1));
        Tgt loose = tgt(ctx, sa, ag, EcomAdTargetType.AUTO, AUTO_TARGETS[1], null, 0.65, 1.2, 1.2, 0.0035);
        loose.terms.add(new Term(kwA[0][1], 0.4, true, 1));
        loose.terms.add(new Term(kwA[1][2], 0.3, true, 1));
        loose.terms.add(new Term(kwA[2][1], 0.3, false, NEGATE_DAY + 1));
        Tgt subs = tgt(ctx, sa, ag, EcomAdTargetType.AUTO, AUTO_TARGETS[2], null, 0.6, 0.8, 1.1, 0.003);
        subs.terms.add(new Term(competitors.get(0).toLowerCase(Locale.ROOT), 0.6, true, 1));
        subs.terms.add(new Term(competitors.get(1).toLowerCase(Locale.ROOT), 0.4, true, 1));
        Tgt comp = tgt(ctx, sa, ag, EcomAdTargetType.AUTO, AUTO_TARGETS[3], null, 0.55, 0.6, 1.3, 0.003);
        comp.terms.add(new Term(competitors.get(2).toLowerCase(Locale.ROOT), 1, true, 1));
        tgt(ctx, sa, ag, EcomAdTargetType.NEGATIVE, kwA[2][1], EcomAdMatchType.NEGATIVE_EXACT, 0, 0, 0, 0);

        // SP 手动关键词：精准 / 广泛（第一个广泛词带来大量无关搜索词，ACOS 很高）
        Camp kw = camp(ctx, sa, Kind.KEYWORD, "SP-手动-核心词-" + nameA, 0.30, 0.9, EcomAdType.SP, "MANUAL", "AUTO_FOR_SALES", "主推,核心词"); // i18n-ignore 演示数据存库
        Grp exact = grp(ctx, kw, "Exact", skus(a, 0, 2));
        double[][] exactParams = {{1.1, 1.4, 0.8}, {1.0, 1.0, 0.9}, {0.95, 0.8, 1.0}};
        for (int i = 0; i < 3; i++) {
            Tgt t = tgt(ctx, sa, exact, EcomAdTargetType.KEYWORD, kwA[0][i], EcomAdMatchType.EXACT, exactParams[i][0], exactParams[i][1], exactParams[i][2], 0.009);
            t.terms.add(new Term(kwA[0][i], 1, true, 1));
        }
        Grp broad = grp(ctx, kw, "Broad", skus(a, 2, 1));
        Tgt bad = tgt(ctx, sa, broad, EcomAdTargetType.KEYWORD, kwA[0][3], EcomAdMatchType.BROAD, 0.85, 2.2, 2.8, 0.004);
        bad.terms.add(new Term(kwA[0][3], 0.4, true, 1));
        bad.terms.add(new Term(kwA[1][1], 0.25, true, 1));
        bad.terms.add(new Term(kwA[2][0], 0.35, false, 1));
        Tgt phrase = tgt(ctx, sa, broad, EcomAdTargetType.KEYWORD, kwA[0][4], EcomAdMatchType.PHRASE, 0.8, 1.0, 1.0, 0.005);
        phrase.terms.add(new Term(kwA[0][4], 0.7, true, 1));
        phrase.terms.add(new Term(kwA[1][2], 0.3, true, 1));
        Tgt broad2 = tgt(ctx, sa, broad, EcomAdTargetType.KEYWORD, kwA[0][5], EcomAdMatchType.BROAD, 0.75, 0.9, 1.1, 0.004);
        broad2.terms.add(new Term(kwA[0][5], 0.6, true, 1));
        broad2.terms.add(new Term(kwA[1][0], 0.4, true, 1));

        // SP 商品投放：竞品 ASIN 与类目（整体 ACOS 偏高，最后一个 ASIN 投放已手工暂停）
        Camp asin = camp(ctx, sa, Kind.ASIN, "SP-商品投放-竞品-" + nameB, 0.17, 1.8, EcomAdType.SP, "MANUAL", "LEGACY_FOR_SALES", "竞品"); // i18n-ignore 演示数据存库
        Grp cg = grp(ctx, asin, "Competitor ASIN", skus(b, 0, 2));
        for (int i = 0; i < 4; i++) {
            Tgt t = tgt(ctx, sa, cg, EcomAdTargetType.PRODUCT, "asin=\"" + competitors.get(3 + i) + "\"", null, 0.7 + 0.05 * i,
                    0.7 + ctx.random().nextDouble() * 0.8, 0.8 + ctx.random().nextDouble() * 0.8, 0.0035);
            if (i == 3) {
                t.minD = TARGET_PAUSE_DAY + 1;
            }
        }
        Grp cat = grp(ctx, asin, "Category", skus(b, 2, 1));
        String[] categories = categories(brandB);
        tgt(ctx, sa, cat, EcomAdTargetType.PRODUCT, "category=\"" + categories[0] + "\"", null, 0.55, 1.0, 1.0, 0.003);
        tgt(ctx, sa, cat, EcomAdTargetType.PRODUCT, "category=\"" + categories[1] + "\"", null, 0.5, 0.8, 4.0, 0.0025);

        // SP 品牌词（防守品牌流量）：ACOS 低
        Camp brandCamp = camp(ctx, sa, Kind.BRAND, "SP-品牌词-" + nameA, 0.14, 0.5, EcomAdType.SP, "MANUAL", "LEGACY_FOR_SALES", "品牌"); // i18n-ignore 演示数据存库
        Grp bg = grp(ctx, brandCamp, "Brand Terms", skus(a, 0, 3));
        String brand = nameA.toLowerCase(Locale.ROOT);
        for (String text : new String[]{brand, brand + " " + kwA[0][0], brand + " " + kwA[0][1]}) {
            Tgt t = tgt(ctx, sa, bg, EcomAdTargetType.KEYWORD, text, EcomAdMatchType.EXACT, 0.6, 1.0, 1.0, 0.012);
            t.terms.add(new Term(text, 1, true, 1));
        }
        if (st.code().equals(STORE_DE)) {
            // 新品测试：表现不好，已暂停
            Camp test = camp(ctx, sa, Kind.TEST, "SP-新品测试-" + nameB, 0.12, 1.6, EcomAdType.SP, "MANUAL", "LEGACY_FOR_SALES", "测试"); // i18n-ignore 演示数据存库
            test.minD = TEST_PAUSE_DAY + 1;
            Grp g = grp(ctx, test, "Test", skus(b, b.size() - 1, 1));
            for (int i = 0; i < 3; i++) {
                Tgt t = tgt(ctx, sa, g, EcomAdTargetType.KEYWORD, kwB[0][i], i == 1 ? EcomAdMatchType.PHRASE : EcomAdMatchType.BROAD, 0.7, 1.0, 1.0, 0.004);
                t.terms.add(new Term(kwB[0][i], 1, true, 1));
            }
        }
        for (Camp c : sa.camps) {
            for (Grp g : c.groups) {
                g.e.setDefaultBid(g.targets.stream().filter(t -> t.e.getBid() != null).map(t -> t.e.getBid())
                        .reduce(BigDecimal::add).map(x -> x.divide(BigDecimal.valueOf(g.targets.stream().filter(t -> t.e.getBid() != null).count()),
                                sa.scale, RoundingMode.HALF_UP)).orElse(null));
            }
        }
        return sa;
    }

    private Camp camp(DemoContext ctx, StoreAds sa, Kind kind, String name, double share, double acos, EcomAdType type, String targeting,
                      String strategy, String tags) {
        Camp c = new Camp();
        c.kind = kind;
        c.share = share;
        c.acos = acos;
        c.e.setStoreId(sa.storeId);
        c.e.setAdType(type);
        c.e.setName(name);
        c.e.setPlatformCampaignId(digits(ctx, 15));
        c.e.setStatus(kind == Kind.TEST ? EcomAdStatus.PAUSED : EcomAdStatus.ENABLED);
        c.e.setBiddingStrategy(strategy);
        c.e.setTargetingType(targeting);
        c.e.setStartDate(ctx.today().minusDays(ctx.days() + (long) ctx.between(5, 150)));
        c.e.setTags(tags);
        sa.camps.add(c);
        return c;
    }

    private Grp grp(DemoContext ctx, Camp c, String name, List<DemoListings.Item> skus) {
        Grp g = new Grp();
        g.camp = c;
        g.e.setName(name);
        g.e.setPlatformAdGroupId(digits(ctx, 15));
        g.e.setStatus(EcomAdStatus.ENABLED);
        g.skus.addAll(skus);
        c.groups.add(g);
        return g;
    }

    private Tgt tgt(DemoContext ctx, StoreAds sa, Grp g, EcomAdTargetType type, String text, EcomAdMatchType match, double usdBid, double tw,
                    double ta, double ctr) {
        Tgt t = new Tgt();
        t.grp = g;
        t.tw = tw;
        t.ta = ta;
        t.ctr = ctr;
        t.e.setTargetType(type);
        t.e.setTargetText(text);
        t.e.setMatchType(match);
        t.e.setPlatformTargetId(digits(ctx, 15));
        t.e.setStatus(EcomAdStatus.ENABLED);
        if (type != EcomAdTargetType.NEGATIVE) {
            BigDecimal bid = money(usdBid * sa.store.fx() * (0.9 + ctx.random().nextDouble() * 0.2), sa.scale);
            t.e.setBid(bid);
            t.e.setBaseBid(bid);
            t.cpc = bid.doubleValue() * (0.6 + ctx.random().nextDouble() * 0.3);
        }
        g.targets.add(t);
        return t;
    }

    private static List<DemoListings.Item> skus(List<DemoListings.Item> list, int from, int count) {
        List<DemoListings.Item> r = new ArrayList<>();
        for (int i = 0; i < count && i < list.size(); i++) {
            DemoListings.Item item = list.get((from + i) % list.size());
            if (!r.contains(item)) {
                r.add(item);
            }
        }
        return r;
    }

    private static String brandOf(DemoListings.Item i) {
        if (i.productCode() == null) {
            return null;
        }
        return DemoCatalog.PRODUCTS.stream().filter(p -> p.code().equals(i.productCode())).findFirst().map(DemoCatalog.Product::brand).orElse(null);
    }

    private static String digits(DemoContext ctx, int n) {
        StringBuilder sb = new StringBuilder().append(ctx.between(1, 9));
        for (int i = 1; i < n; i++) {
            sb.append(ctx.random().nextInt(10));
        }
        return sb.toString();
    }

    private static String asin(DemoContext ctx) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("B0");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(ctx.random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** 关键词：[核心词 6 个, 长尾搜索词 3 个, 无关搜索词 2 个]，按品牌（品类）与站点 */
    private static String[][] keywords(String brand, String market) {
        return switch (brand) {
            case "HOMELUX" -> switch (market) {
                case "DE" -> new String[][]{{"küchenhelfer silikon", "vakuumbeutel kleidung", "nachtlicht bewegungsmelder", "thermobecher", "schubladen organizer", "kaffeemühle elektrisch"},
                        {"vakuumbeutel mit pumpe", "nachtlicht steckdose", "thermobecher to go"}, {"vakuumiergerät", "kaffeevollautomat"}};
                case "JP" -> new String[][]{{"シリコン キッチンツール", "圧縮袋 衣類", "センサーライト 室内", "タンブラー 保温", "引き出し 収納", "コーヒーミル 電動"}, // i18n-ignore 演示数据存库（日文搜索词）
                        {"布団圧縮袋 ポンプ付き", "足元灯 人感センサー", "真空断熱タンブラー"}, {"布団乾燥機", "コーヒーメーカー"}}; // i18n-ignore 演示数据存库（日文搜索词）
                default -> new String[][]{{"silicone utensils", "vacuum storage bags", "night light", "insulated tumbler", "drawer organizer", "coffee grinder"},
                        {"silicone cooking utensils set", "motion sensor night light", "space saver bags"}, {"vacuum sealer machine", "tumbler with straw"}};
            };
            case "TRAILPRO" -> switch (market) {
                case "DE" -> new String[][]{{"campinglampe", "campingstuhl", "trinkflasche 1l", "yogamatte", "wanderstöcke", "stirnlampe"},
                        {"campinglampe akku", "campingstuhl faltbar", "yogamatte rutschfest"}, {"zelt 4 personen", "wanderschuhe"}};
                case "JP" -> new String[][]{{"ランタン led", "アウトドアチェア", "水筒 1リットル", "ヨガマット", "トレッキングポール", "ヘッドライト"}, // i18n-ignore 演示数据存库（日文搜索词）
                        {"ランタン 充電式", "折りたたみ チェア", "ヨガマット 6mm"}, {"テント", "登山靴"}}; // i18n-ignore 演示数据存库（日文搜索词）
                default -> new String[][]{{"camping lantern", "camping chair", "water bottle", "yoga mat", "trekking poles", "headlamp"},
                        {"rechargeable camping lantern", "folding camping chair", "yoga mat thick"}, {"camping tent", "hiking boots"}};
            };
            case "PAWJOY" -> switch (market) {
                case "DE" -> new String[][]{{"trinkbrunnen katze", "kratzbrett katze", "hundeleine ausziehbar", "katzenbürste", "katzenspielzeug", "futterautomat katze"},
                        {"katzenbrunnen leise", "flexi leine 5m", "fellbürste hund"}, {"katzenklo", "hundefutter"}};
                case "JP" -> new String[][]{{"猫 給水器", "爪とぎ 猫", "リード 犬 伸縮", "ペット ブラシ", "猫 おもちゃ", "自動給餌器"}, // i18n-ignore 演示数据存库（日文搜索词）
                        {"給水器 静音", "伸縮リード 5m", "抜け毛 ブラシ 猫"}, {"猫砂", "ドッグフード"}}; // i18n-ignore 演示数据存库（日文搜索词）
                default -> new String[][]{{"cat water fountain", "cat scratcher", "dog leash", "pet brush", "cat toys", "automatic pet feeder"},
                        {"cat fountain stainless", "retractable dog leash", "deshedding brush for cats"}, {"cat litter box", "dog food"}};
            };
            default -> switch (market) {
                case "DE" -> new String[][]{{"powerbank", "ladekabel usb c", "schnellladegerät", "powerbank 10000mah", "bluetooth kopfhörer", "usb c ladegerät"},
                        {"powerbank klein leicht", "ladekabel usb c 2m", "netzteil 65w"}, {"solar powerbank", "powerbank 50000mah"}};
                case "JP" -> new String[][]{{"モバイルバッテリー", "usb-c ケーブル", "急速充電器", "ワイヤレスイヤホン", "充電器 タイプc", "モバイルバッテリー 大容量"}, // i18n-ignore 演示数据存库（日文搜索词）
                        {"モバイルバッテリー 軽量 薄型", "充電ケーブル 2m", "gan 充電器 65w"}, {"ソーラー モバイルバッテリー", "ポータブル電源"}}; // i18n-ignore 演示数据存库（日文搜索词）
                default -> new String[][]{{"power bank", "portable charger", "usb c cable", "fast charger", "wireless earbuds", "phone charger"},
                        {"power bank for iphone", "portable charger 10000mah", "usb c fast charging cable"}, {"solar power bank", "car battery jump starter"}};
            };
        };
    }

    private static String[] categories(String brand) {
        return switch (brand) {
            case "HOMELUX" -> new String[]{"Kitchen Utensils & Gadgets", "Home Storage & Organization"};
            case "TRAILPRO" -> new String[]{"Camping Lanterns", "Yoga Mats"};
            case "PAWJOY" -> new String[]{"Cat Water Fountains", "Dog Leashes"};
            default -> new String[]{"Portable Power Banks", "USB Cables"};
        };
    }

    // ---------------------------------------------------------------- 每日数据

    /** 逐日生成各投放的数据；店铺没有订单时返回 false */
    private boolean simulate(DemoContext ctx, StoreAds sa, double[][] s) {
        int days = ctx.days();
        double[] amt = s[0];
        double totalAmt = 0;
        double totalCnt = 0;
        for (int d = 1; d <= days; d++) {
            totalAmt += amt[d];
            totalCnt += s[1][d];
        }
        if (totalCnt <= 0 || totalAmt <= 0) {
            return false;
        }
        double aov = totalAmt / totalCnt;
        Random rnd = ctx.random();
        List<Tgt> all = targets(sa);
        for (Tgt t : all) {
            t.imp = new long[days + 1];
            t.clk = new long[days + 1];
            t.spend = new BigDecimal[days + 1];
            t.sales = new BigDecimal[days + 1];
            t.orders = new int[days + 1];
            t.units = new int[days + 1];
        }
        for (int d = days; d >= 1; d--) {
            double progress = (days - d) / (double) days;
            double avg7 = 0;
            for (int k = d; k < d + 7; k++) {
                avg7 += amt[k];
            }
            avg7 /= 7;
            double base = 0.6 * amt[d] + 0.4 * avg7;
            if (base <= 0) {
                continue;
            }
            double adSales = base * (0.26 + 0.10 * progress) * (0.92 + 0.16 * rnd.nextDouble());
            double spendTotal = adSales * sa.acos * (1.12 - 0.16 * progress) * (0.9 + 0.2 * rnd.nextDouble());
            int day = d;
            List<Camp> active = sa.camps.stream().filter(c -> c.minD <= day).toList();
            double shareSum = active.stream().mapToDouble(c -> c.share).sum();
            List<Tgt> live = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            for (Camp c : active) {
                List<Tgt> ts = c.groups.stream().flatMap(g -> g.targets.stream()).filter(t -> t.live() && t.minD <= day).toList();
                double twSum = ts.stream().mapToDouble(t -> t.tw).sum();
                for (Tgt t : ts) {
                    double expSpend = spendTotal * c.share / shareSum * t.tw / twSum;
                    double expClicks = expSpend / t.cpc * (0.75 + 0.5 * rnd.nextDouble());
                    long clicks = stochRound(expClicks, rnd);
                    BigDecimal spend = clicks == 0 ? BigDecimal.ZERO : money(clicks * t.cpc * (0.85 + 0.3 * rnd.nextDouble()), sa.scale);
                    t.clk[d] = clicks;
                    t.spend[d] = spend;
                    t.imp[d] = Math.round(expClicks / t.ctr * (0.8 + 0.4 * rnd.nextDouble())) + ctx.between(5, 40);
                    live.add(t);
                    weights.add(spend.doubleValue() / (c.acos * t.ta));
                }
            }
            double[] sales = new double[live.size()];
            int orders = (int) stochRound(adSales / aov, rnd);
            for (int i = 0; i < orders; i++) {
                int k = pick(weights, rnd);
                if (k < 0) {
                    break;
                }
                Tgt t = live.get(k);
                t.orders[d]++;
                t.units[d] += rnd.nextDouble() < 0.15 ? 2 : 1;
                sales[k] += aov * (0.8 + 0.4 * rnd.nextDouble());
                if (t.orders[d] >= t.clk[d]) {
                    weights.set(k, 0.0);
                }
            }
            for (int k = 0; k < live.size(); k++) {
                live.get(k).sales[d] = money(sales[k], sa.scale);
            }
        }
        for (Tgt t : all) {
            for (int d = 0; d <= days; d++) {
                t.spend[d] = t.spend[d] == null ? BigDecimal.ZERO : t.spend[d];
                t.sales[d] = t.sales[d] == null ? BigDecimal.ZERO : t.sales[d];
            }
        }
        return true;
    }

    /** 日预算：最近 14 个有数据日的平均花费 × 1.8 与最近 30 天最高花费 × 1.05 取大，按整数档向上取整 */
    private void budgets(StoreAds sa, int days) {
        double step = sa.scale == 0 ? 500 : 5;
        for (Camp c : sa.camps) {
            double sum = 0;
            double max = 0;
            int n = 0;
            for (int d = c.minD; d <= days && d < c.minD + 30; d++) {
                double v = 0;
                for (Grp g : c.groups) {
                    for (Tgt t : g.targets) {
                        if (t.live()) {
                            v += t.spend[d].doubleValue();
                        }
                    }
                }
                if (d < c.minD + 14) {
                    sum += v;
                    n++;
                }
                max = Math.max(max, v);
            }
            double budget = Math.max(n == 0 ? 0 : sum / n * 1.8, max * 1.05);
            c.e.setDailyBudget(BigDecimal.valueOf(Math.max(2, Math.ceil(budget / step)) * step).setScale(sa.scale, RoundingMode.HALF_UP));
        }
    }

    // ---------------------------------------------------------------- 规则与日志

    /** 手工修改记录：暂停一个 ASIN 投放、暂停德国站测试活动、给手动关键词活动加预算 */
    private void manualLogs(DemoContext ctx, List<StoreAds> all, List<LogPlan> logs, Long me) {
        for (StoreAds sa : all) {
            for (Camp c : sa.camps) {
                if (c.kind == Kind.ASIN) {
                    Tgt t = c.groups.get(0).targets.get(c.groups.get(0).targets.size() - 1);
                    t.e.setStatus(EcomAdStatus.PAUSED);
                    LogPlan l = manual(ctx, sa, EcomAdLevel.TARGET, c, t, EcomAdAction.PAUSE, TARGET_PAUSE_DAY, me);
                    l.oldStatus = EcomAdStatus.ENABLED;
                    l.newStatus = EcomAdStatus.PAUSED;
                    logs.add(l);
                } else if (c.kind == Kind.TEST) {
                    LogPlan l = manual(ctx, sa, EcomAdLevel.CAMPAIGN, c, null, EcomAdAction.PAUSE, TEST_PAUSE_DAY, me);
                    l.oldStatus = EcomAdStatus.ENABLED;
                    l.newStatus = EcomAdStatus.PAUSED;
                    logs.add(l);
                } else if (c.kind == Kind.KEYWORD) {
                    LogPlan l = manual(ctx, sa, EcomAdLevel.CAMPAIGN, c, null, EcomAdAction.BUDGET_SET, 10, me);
                    BigDecimal budget = c.e.getDailyBudget();
                    BigDecimal step = BigDecimal.valueOf(sa.scale == 0 ? 500 : 5);
                    BigDecimal old = budget.multiply(BigDecimal.valueOf(0.8)).divide(step, 0, RoundingMode.HALF_UP).multiply(step).max(step);
                    l.oldValue = old.setScale(sa.scale, RoundingMode.HALF_UP);
                    l.newValue = budget;
                    logs.add(l);
                }
            }
        }
    }

    private LogPlan manual(DemoContext ctx, StoreAds sa, EcomAdLevel level, Camp c, Tgt t, EcomAdAction action, int d, Long me) {
        LogPlan l = new LogPlan();
        l.runTime = ctx.today().minusDays(d).atTime(ctx.between(9, 17), ctx.between(0, 59));
        l.sa = sa;
        l.level = level;
        l.camp = c;
        l.target = t;
        l.action = action;
        l.status = EcomAdLogStatus.MANUAL;
        l.appliedTime = l.runTime;
        l.appliedBy = me;
        return l;
    }

    /**
     * 三条规则，按生成的数据计算命中对象（与 EcomAdRuleService 的指标口径相同：窗口截至执行日前一天）：
     * 0 高 ACOS 投放降竞价（美国站 SP，近 14 天 ACOS &gt; 45% 且点击 ≥ 10，竞价 -15%，下限 0.2，每天 6 点）；
     * 1 零转化投放暂停（三个站，近 14 天点击 ≥ 15 且订单 = 0，每天 7 点）；
     * 2 低 ACOS 活动加预算（近 7 天 ACOS &lt; 20% 且订单 ≥ 3，预算 +20%，手动执行）
     */
    private List<EcomAdRule> rules(DemoContext ctx, List<StoreAds> all, List<LogPlan> logs, Long me) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        StoreAds us = all.stream().filter(sa -> sa.store.code().equals(STORE_US)).findFirst().orElse(all.get(0));
        String noApi = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.noApi");

        EcomAdRule r0 = rule("高 ACOS 投放降竞价", EcomAdLevel.TARGET, String.valueOf(us.storeId), EcomAdType.SP, 14, // i18n-ignore 演示数据存库
                List.of(cond(EcomAdMetric.ACOS, EcomAdOperator.GT, 45), cond(EcomAdMetric.CLICKS, EcomAdOperator.GTE, 10)),
                EcomAdAction.BID_PERCENT, BigDecimal.valueOf(-15), us.scale == 0 ? BigDecimal.valueOf(20) : new BigDecimal("0.20"), null,
                EcomAdRunMode.DAILY, 6, us.ownerId != null ? us.ownerId : me);
        r0.setRemark("ACOS 长期偏高的关键词 / ASIN 逐步降价，运营确认后执行"); // i18n-ignore 演示数据存库
        String storeIds = String.join(",", all.stream().map(sa -> String.valueOf(sa.storeId)).toList());
        EcomAdRule r1 = rule("零转化投放暂停", EcomAdLevel.TARGET, storeIds, null, 14, // i18n-ignore 演示数据存库
                List.of(cond(EcomAdMetric.CLICKS, EcomAdOperator.GTE, 15), cond(EcomAdMetric.ORDERS, EcomAdOperator.EQ, 0)),
                EcomAdAction.PAUSE, null, null, null, EcomAdRunMode.DAILY, 7, me);
        r1.setRemark("两周点击不少于 15 次仍没有订单的投放建议暂停"); // i18n-ignore 演示数据存库
        EcomAdRule r2 = rule("低 ACOS 活动加预算", EcomAdLevel.CAMPAIGN, null, null, 7, // i18n-ignore 演示数据存库
                List.of(cond(EcomAdMetric.ACOS, EcomAdOperator.LT, 20), cond(EcomAdMetric.ORDERS, EcomAdOperator.GTE, 3)),
                EcomAdAction.BUDGET_PERCENT, BigDecimal.valueOf(20), null, null, EcomAdRunMode.MANUAL, 6, me);
        r2.setRemark("转化好的活动增加日预算，避免预算提前用完"); // i18n-ignore 演示数据存库

        // 7 天前：规则 0 的建议已手工执行（第一条忽略），规则 1 的建议已忽略
        LocalDateTime run7 = ctx.today().minusDays(7).atTime(6, 10);
        List<LogPlan> hits = bidRule(us, 7, run7, 0);
        for (int i = 0; i < hits.size(); i++) {
            LogPlan l = hits.get(i);
            if (i == 0) {
                l.status = EcomAdLogStatus.IGNORED;
            } else {
                l.status = EcomAdLogStatus.MANUAL;
                l.appliedTime = run7.plusHours(3).plusMinutes(i);
                l.appliedBy = me;
                l.target.e.setBid(l.newValue);
                l.target.e.setBaseBid(l.newValue);
            }
        }
        logs.addAll(hits);
        LocalDateTime run7b = ctx.today().minusDays(7).atTime(7, 10);
        for (LogPlan l : pauseRule(all, 7, run7b)) {
            l.status = EcomAdLogStatus.IGNORED;
            logs.add(l);
        }
        // 3 天前：规则 2 手动执行，第一条已手工执行，其他待处理
        LocalDateTime run3 = ctx.today().minusDays(3).atTime(10, 30);
        List<LogPlan> budgetHits = budgetRule(all, 3, run3);
        for (int i = 0; i < budgetHits.size(); i++) {
            LogPlan l = budgetHits.get(i);
            if (i == 0) {
                l.status = EcomAdLogStatus.MANUAL;
                l.appliedTime = run3.plusMinutes(20);
                l.appliedBy = me;
                l.camp.e.setDailyBudget(l.newValue);
            } else {
                l.status = EcomAdLogStatus.PENDING;
                l.message = noApi;
            }
        }
        logs.addAll(budgetHits);
        r2.setLastRunTime(run3);
        r2.setLastRunMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.runSummary", budgetHits.size(), 0, budgetHits.size(), 0, 0, 0));
        // 今天：规则 0、1 每天执行，没有广告接口，生成待处理建议
        LocalDateTime run0 = runTime(ctx, now, 6);
        List<LogPlan> today0 = bidRule(us, 0, run0, 0);
        LocalDateTime run1 = runTime(ctx, now, 7);
        List<LogPlan> today1 = pauseRule(all, 0, run1);
        for (LogPlan l : today0) {
            l.status = EcomAdLogStatus.PENDING;
            l.message = noApi;
        }
        for (LogPlan l : today1) {
            l.status = EcomAdLogStatus.PENDING;
            l.message = noApi;
        }
        logs.addAll(today0);
        logs.addAll(today1);
        r0.setLastRunTime(run0);
        r0.setLastRunMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.runSummary", today0.size(), 0, today0.size(), 0, 0, 0));
        r1.setLastRunTime(run1);
        r1.setLastRunMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.runSummary", today1.size(), 0, today1.size(), 0, 0, 0));
        return List.of(r0, r1, r2);
    }

    /** 每天执行的规则今天的执行时间：到点了取今天该整点 10 分，否则取几分钟前 */
    private static LocalDateTime runTime(DemoContext ctx, LocalDateTime now, int hour) {
        LocalDateTime t = ctx.today().atTime(hour, 10);
        return t.isAfter(now) ? now.minusMinutes(5) : t;
    }

    private List<LogPlan> bidRule(StoreAds sa, int runD, LocalDateTime runTime, int rule) {
        List<LogPlan> r = new ArrayList<>();
        BigDecimal min = sa.scale == 0 ? BigDecimal.valueOf(20) : new BigDecimal("0.20");
        for (Camp c : sa.camps) {
            if (c.e.getAdType() != EcomAdType.SP || !campaignOn(c, runD)) {
                continue;
            }
            for (Grp g : c.groups) {
                for (Tgt t : g.targets) {
                    if (!targetOn(t, runD)) {
                        continue;
                    }
                    AdsViews.Metrics m = metrics(List.of(t), runD + 1, runD + 14);
                    boolean acosHit = m.getSales().signum() == 0 ? m.getSpend().signum() > 0 : m.getAcos().compareTo(BigDecimal.valueOf(45)) > 0;
                    if (acosHit && m.getClicks() >= 10) {
                        LogPlan l = plan(sa, rule, runTime, EcomAdLevel.TARGET, c, t, EcomAdAction.BID_PERCENT, m);
                        l.oldValue = t.e.getBid();
                        l.newValue = t.e.getBid().multiply(BigDecimal.valueOf(0.85)).setScale(sa.scale, RoundingMode.HALF_UP).max(min);
                        r.add(l);
                    }
                }
            }
        }
        return r;
    }

    private List<LogPlan> pauseRule(List<StoreAds> all, int runD, LocalDateTime runTime) {
        List<LogPlan> r = new ArrayList<>();
        for (StoreAds sa : all) {
            for (Camp c : sa.camps) {
                if (!campaignOn(c, runD)) {
                    continue;
                }
                for (Grp g : c.groups) {
                    for (Tgt t : g.targets) {
                        if (!targetOn(t, runD)) {
                            continue;
                        }
                        AdsViews.Metrics m = metrics(List.of(t), runD + 1, runD + 14);
                        if (m.getClicks() >= 15 && m.getOrders() == 0) {
                            LogPlan l = plan(sa, 1, runTime, EcomAdLevel.TARGET, c, t, EcomAdAction.PAUSE, m);
                            l.oldStatus = EcomAdStatus.ENABLED;
                            l.newStatus = EcomAdStatus.PAUSED;
                            r.add(l);
                        }
                    }
                }
            }
        }
        return r;
    }

    private List<LogPlan> budgetRule(List<StoreAds> all, int runD, LocalDateTime runTime) {
        List<LogPlan> r = new ArrayList<>();
        for (StoreAds sa : all) {
            for (Camp c : sa.camps) {
                if (!campaignOn(c, runD)) {
                    continue;
                }
                List<Tgt> ts = c.groups.stream().flatMap(g -> g.targets.stream()).filter(Tgt::live).toList();
                AdsViews.Metrics m = metrics(ts, runD + 1, runD + 7);
                if (m.getAcos() != null && m.getAcos().compareTo(BigDecimal.valueOf(20)) < 0 && m.getOrders() >= 3) {
                    LogPlan l = plan(sa, 2, runTime, EcomAdLevel.CAMPAIGN, c, null, EcomAdAction.BUDGET_PERCENT, m);
                    l.oldValue = c.e.getDailyBudget();
                    l.newValue = c.e.getDailyBudget().multiply(BigDecimal.valueOf(1.2)).setScale(sa.scale, RoundingMode.HALF_UP);
                    r.add(l);
                }
            }
        }
        return r;
    }

    private static boolean campaignOn(Camp c, int runD) {
        return c.e.getStatus() == EcomAdStatus.ENABLED && c.minD <= runD + 1;
    }

    /** 执行时投放是启用的（之后手工暂停的投放在暂停之前也算） */
    private static boolean targetOn(Tgt t, int runD) {
        return t.live() && t.minD <= runD + 1;
    }

    private static LogPlan plan(StoreAds sa, int rule, LocalDateTime runTime, EcomAdLevel level, Camp c, Tgt t, EcomAdAction action,
                                AdsViews.Metrics m) {
        LogPlan l = new LogPlan();
        l.rule = rule;
        l.runTime = runTime;
        l.sa = sa;
        l.level = level;
        l.camp = c;
        l.target = t;
        l.action = action;
        l.metrics = m;
        return l;
    }

    /** 投放在 [from, to]（几天前）内的合计指标 */
    private static AdsViews.Metrics metrics(List<Tgt> ts, int from, int to) {
        AdsViews.Metrics m = new AdsViews.Metrics();
        for (Tgt t : ts) {
            for (int d = from; d <= to && d < t.imp.length; d++) {
                AdsViews.Metrics x = new AdsViews.Metrics();
                x.setImpressions(t.imp[d]);
                x.setClicks(t.clk[d]);
                x.setSpend(t.spend[d]);
                x.setSales(t.sales[d]);
                x.setOrders(t.orders[d]);
                m.add(x);
            }
        }
        return m.fill();
    }

    private static EcomAdRule rule(String name, EcomAdLevel level, String storeIds, EcomAdType adType, int window, List<EcomAdRule.Condition> conditions,
                                   EcomAdAction action, BigDecimal actionValue, BigDecimal min, BigDecimal max, EcomAdRunMode mode, int hour, Long owner) {
        EcomAdRule r = new EcomAdRule();
        r.setName(name);
        r.setStatus(EnableStatus.ENABLED);
        r.setLevel(level);
        r.setStoreIds(storeIds);
        r.setAdType(adType);
        r.setWindowDays(window);
        r.setConditions(conditions);
        r.setConditionsJson(HttpJson.MAPPER.writeValueAsString(conditions));
        r.setAction(action);
        r.setActionValue(actionValue);
        r.setMinValue(min);
        r.setMaxValue(max);
        r.setRunMode(mode);
        r.setRunHour(hour);
        r.setAutoApply(0);
        r.setOwnerId(owner);
        return r;
    }

    private static EcomAdRule.Condition cond(EcomAdMetric metric, EcomAdOperator op, int value) {
        EcomAdRule.Condition c = new EcomAdRule.Condition();
        c.setMetric(metric);
        c.setOperator(op);
        c.setValue(BigDecimal.valueOf(value));
        return c;
    }

    // ---------------------------------------------------------------- 写入

    private void persistStructure(List<StoreAds> all) {
        for (StoreAds sa : all) {
            for (Camp c : sa.camps) {
                campaignMapper.insert(c.e);
                for (Grp g : c.groups) {
                    g.e.setStoreId(sa.storeId);
                    g.e.setCampaignId(c.e.getId());
                    groupMapper.insert(g.e);
                    for (Tgt t : g.targets) {
                        t.e.setStoreId(sa.storeId);
                        t.e.setCampaignId(c.e.getId());
                        t.e.setAdGroupId(g.e.getId());
                        targetMapper.insert(t.e);
                    }
                }
            }
        }
    }

    /** 投放报告、活动报告、广告日报（活动 × SKU）、搜索词 */
    private void persistReports(DemoContext ctx, List<StoreAds> all) {
        int days = ctx.days();
        LocalDate today = ctx.today();
        List<EcomAdStat> stats = new ArrayList<>();
        List<EcomAdSearchTerm> terms = new ArrayList<>();
        for (StoreAds sa : all) {
            String currency = sa.store.currency();
            for (Camp c : sa.camps) {
                for (int d = days; d >= c.minD; d--) {
                    LocalDate date = today.minusDays(d);
                    EcomAdStat cs = stat(sa, date, EcomAdLevel.CAMPAIGN, c.e.getId(), c.e.getId(), null, currency);
                    Map<DemoListings.Item, long[]> skuCounts = new LinkedHashMap<>();
                    Map<DemoListings.Item, BigDecimal[]> skuMoney = new LinkedHashMap<>();
                    Map<DemoListings.Item, Set<Long>> skuGroups = new HashMap<>();
                    for (Grp g : c.groups) {
                        long[] gc = new long[3];
                        BigDecimal gSpend = BigDecimal.ZERO;
                        BigDecimal gSales = BigDecimal.ZERO;
                        for (Tgt t : g.targets) {
                            if (!t.live() || t.imp[d] == 0) {
                                continue;
                            }
                            EcomAdStat ts = stat(sa, date, EcomAdLevel.TARGET, t.e.getId(), c.e.getId(), g.e.getId(), currency);
                            ts.setImpressions(t.imp[d]);
                            ts.setClicks(t.clk[d]);
                            ts.setSpend(t.spend[d]);
                            ts.setSales(t.sales[d]);
                            ts.setOrders(t.orders[d]);
                            ts.setUnits(t.units[d]);
                            stats.add(ts);
                            add(cs, ts);
                            gc[0] += t.imp[d];
                            gc[1] += t.clk[d];
                            gc[2] += t.orders[d];
                            gSpend = gSpend.add(t.spend[d]);
                            gSales = gSales.add(t.sales[d]);
                            if (d <= SEARCH_TERM_DAYS) {
                                searchTerms(ctx, sa, t, d, date, terms);
                            }
                        }
                        if (gc[0] == 0 || g.skus.isEmpty()) {
                            continue;
                        }
                        // 广告组的数据按 SKU 销量权重拆到推广商品
                        double[] w = g.skus.stream().mapToDouble(i -> Math.max(1, i.popularity())).toArray();
                        long[] imp = splitLong(gc[0], w);
                        long[] clk = splitLong(gc[1], w);
                        long[] ord = splitLong(gc[2], w);
                        BigDecimal[] spend = splitMoney(gSpend, toDouble(clk), sa.scale);
                        BigDecimal[] sales = splitMoney(gSales, toDouble(ord), sa.scale);
                        for (int i = 0; i < g.skus.size(); i++) {
                            DemoListings.Item item = g.skus.get(i);
                            long[] acc = skuCounts.computeIfAbsent(item, k -> new long[3]);
                            acc[0] += imp[i];
                            acc[1] += clk[i];
                            acc[2] += ord[i];
                            BigDecimal[] money = skuMoney.computeIfAbsent(item, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                            money[0] = money[0].add(spend[i]);
                            money[1] = money[1].add(sales[i]);
                            skuGroups.computeIfAbsent(item, k -> new LinkedHashSet<>()).add(g.e.getId());
                        }
                    }
                    if (cs.getImpressions() == 0) {
                        continue;
                    }
                    stats.add(cs);
                    for (Map.Entry<DemoListings.Item, long[]> en : skuCounts.entrySet()) {
                        DemoListings.Item item = en.getKey();
                        Set<Long> groups = skuGroups.get(item);
                        EcomAdDaily a = new EcomAdDaily();
                        a.setStoreId(sa.storeId);
                        a.setAdDate(date);
                        a.setCampaign(c.e.getName());
                        a.setSellerSku(item.sku());
                        a.setPlatformItemId(item.platformItemId());
                        a.setImpressions(en.getValue()[0]);
                        a.setClicks(en.getValue()[1]);
                        a.setAdOrders((int) en.getValue()[2]);
                        a.setSpend(skuMoney.get(item)[0]);
                        a.setAdSales(skuMoney.get(item)[1]);
                        a.setCurrency(currency);
                        a.setCampaignId(c.e.getId());
                        a.setAdGroupId(groups.size() == 1 ? groups.iterator().next() : null);
                        a.setAdLevel(EcomAdDailyLevel.PRODUCT);
                        dailyMapper.insert(a);
                    }
                }
            }
        }
        for (int i = 0; i < stats.size(); i += CHUNK) {
            statMapper.insertBatch(stats.subList(i, Math.min(stats.size(), i + CHUNK)));
        }
        for (int i = 0; i < terms.size(); i += CHUNK) {
            searchTermMapper.insertBatch(terms.subList(i, Math.min(terms.size(), i + CHUNK)));
        }
    }

    private static EcomAdStat stat(StoreAds sa, LocalDate date, EcomAdLevel level, Long entityId, Long campaignId, Long adGroupId, String currency) {
        EcomAdStat s = new EcomAdStat();
        s.setStoreId(sa.storeId);
        s.setStatDate(date);
        s.setLevel(level);
        s.setEntityId(entityId);
        s.setCampaignId(campaignId);
        s.setAdGroupId(adGroupId);
        s.setImpressions(0L);
        s.setClicks(0L);
        s.setSpend(BigDecimal.ZERO);
        s.setSales(BigDecimal.ZERO);
        s.setOrders(0);
        s.setUnits(0);
        s.setCurrency(currency);
        return s;
    }

    private static void add(EcomAdStat to, EcomAdStat x) {
        to.setImpressions(to.getImpressions() + x.getImpressions());
        to.setClicks(to.getClicks() + x.getClicks());
        to.setSpend(to.getSpend().add(x.getSpend()));
        to.setSales(to.getSales().add(x.getSales()));
        to.setOrders(to.getOrders() + x.getOrders());
        to.setUnits(to.getUnits() + x.getUnits());
    }

    /** 投放某天的数据拆到客户搜索词：点击按占比随机分配，花费按点击、订单给会转化的词，曝光按占比 */
    private void searchTerms(DemoContext ctx, StoreAds sa, Tgt t, int d, LocalDate date, List<EcomAdSearchTerm> out) {
        List<Term> active = t.terms.stream().filter(x -> x.minD() <= d).toList();
        if (active.isEmpty()) {
            return;
        }
        double[] share = active.stream().mapToDouble(Term::share).toArray();
        long[] clk = new long[active.size()];
        for (long i = 0; i < t.clk[d]; i++) {
            clk[pick(share, ctx.random())]++;
        }
        long[] imp = splitLong(t.imp[d], share);
        BigDecimal[] spend = splitMoney(t.spend[d], toDouble(clk), sa.scale);
        double[] convW = new double[active.size()];
        int firstConv = -1;
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).converts()) {
                convW[i] = clk[i];
                firstConv = firstConv < 0 ? i : firstConv;
            }
        }
        long[] ord = new long[active.size()];
        if (t.orders[d] > 0 && firstConv >= 0) {
            if (sum(convW) == 0) {
                convW[firstConv] = 1;
            }
            ord = splitLong(t.orders[d], convW);
        }
        BigDecimal[] sales = splitMoney(t.sales[d], toDouble(ord), sa.scale);
        for (int i = 0; i < active.size(); i++) {
            if (imp[i] == 0) {
                continue;
            }
            EcomAdSearchTerm s = new EcomAdSearchTerm();
            s.setStoreId(sa.storeId);
            s.setStatDate(date);
            s.setCampaignId(t.e.getCampaignId());
            s.setAdGroupId(t.e.getAdGroupId());
            s.setTargetId(t.e.getId());
            s.setKeywordText(t.e.getTargetText());
            s.setMatchType(Optional.ofNullable(t.e.getMatchType()).map(Enum::name).orElse(null));
            s.setSearchTerm(active.get(i).text());
            s.setImpressions(imp[i]);
            s.setClicks(clk[i]);
            s.setSpend(spend[i]);
            s.setSales(sales[i]);
            s.setOrders((int) ord[i]);
            s.setCurrency(sa.store.currency());
            out.add(s);
        }
    }

    private void persistRules(List<EcomAdRule> rules, List<LogPlan> plans) {
        for (EcomAdRule r : rules) {
            ruleMapper.insert(r);
        }
        // 结构写入后补上规则 / 手工修改对本地竞价、预算、状态的最终结果
        Set<Object> touched = new LinkedHashSet<>();
        List<EcomAdRuleLog> logs = new ArrayList<>();
        plans.sort(Comparator.comparing((LogPlan l) -> l.runTime));
        for (LogPlan p : plans) {
            EcomAdRuleLog l = new EcomAdRuleLog();
            l.setRuleId(p.rule == null ? null : rules.get(p.rule).getId());
            l.setSource(p.rule == null ? EcomAdLogSource.MANUAL : EcomAdLogSource.RULE);
            l.setRunTime(p.runTime);
            l.setStoreId(p.sa.storeId);
            l.setLevel(p.level);
            l.setEntityId(p.target != null ? p.target.e.getId() : p.camp.e.getId());
            l.setCampaignId(p.camp.e.getId());
            l.setEntityName(p.target != null ? p.target.e.getTargetText() : p.camp.e.getName());
            l.setAction(p.action);
            l.setOldValue(p.oldValue);
            l.setNewValue(p.newValue);
            l.setOldStatus(p.oldStatus);
            l.setNewStatus(p.newStatus);
            l.setMetrics(p.metrics == null ? null : snapshot(p.metrics));
            l.setStatus(p.status);
            l.setMessage(p.message);
            l.setAppliedTime(p.appliedTime);
            l.setAppliedBy(p.appliedBy);
            logs.add(l);
            if (p.status == EcomAdLogStatus.MANUAL) {
                touched.add(p.target != null ? p.target : p.camp);
            }
        }
        for (int i = 0; i < logs.size(); i += CHUNK) {
            ruleLogMapper.insertBatch(logs.subList(i, Math.min(logs.size(), i + CHUNK)));
        }
        for (Object o : touched) {
            if (o instanceof Tgt t) {
                targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getBid, t.e.getBid())
                        .set(EcomAdTarget::getBaseBid, t.e.getBaseBid()).set(EcomAdTarget::getStatus, t.e.getStatus()).eq(EcomAdTarget::getId, t.e.getId()));
            } else if (o instanceof Camp c) {
                campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getDailyBudget, c.e.getDailyBudget())
                        .set(EcomAdCampaign::getStatus, c.e.getStatus()).eq(EcomAdCampaign::getId, c.e.getId()));
            }
        }
    }

    /** 与 EcomAdRuleService 写入的指标快照字段一致 */
    private static String snapshot(AdsViews.Metrics m) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("impressions", m.getImpressions());
        snap.put("clicks", m.getClicks());
        snap.put("spend", m.getSpend());
        snap.put("sales", m.getSales());
        snap.put("orders", m.getOrders());
        snap.put("acos", m.getAcos());
        snap.put("roas", m.getRoas());
        snap.put("ctr", m.getCtr());
        snap.put("cpc", m.getCpc());
        snap.put("cvr", m.getCvr());
        return HttpJson.MAPPER.writeValueAsString(snap);
    }

    /** 分时策略：美国站（第一个店铺）手动关键词活动，凌晨 0.6、白天 1.0–1.2、晚高峰 1.3，周末白天再加 0.1 */
    private void schedule(StoreAds sa) {
        Camp c = sa.camps.stream().filter(x -> x.kind == Kind.KEYWORD).findFirst().orElse(null);
        if (c == null) {
            return;
        }
        List<List<BigDecimal>> factors = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            List<BigDecimal> row = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                double f = h < 6 ? 0.6 : h < 8 ? 0.8 : h < 10 ? 1.0 : h < 14 ? 1.2 : h < 19 ? 1.0 : h < 23 ? 1.3 : 0.8;
                if (day >= 5 && h >= 8 && h < 23) {
                    f += 0.1;
                }
                row.add(BigDecimal.valueOf(f).setScale(2, RoundingMode.HALF_UP));
            }
            factors.add(row);
        }
        BigDecimal min = c.groups.get(0).targets.get(0).e.getBid();
        EcomAdSchedule s = new EcomAdSchedule();
        s.setStoreId(sa.storeId);
        s.setCampaignId(c.e.getId());
        s.setStatus(EnableStatus.ENABLED);
        s.setFactorsJson(HttpJson.MAPPER.writeValueAsString(factors));
        s.setMinBid(sa.scale == 0 ? BigDecimal.valueOf(30) : new BigDecimal("0.30"));
        s.setMaxBid(min == null ? null : min.multiply(BigDecimal.valueOf(2)).setScale(sa.scale, RoundingMode.HALF_UP));
        ZonedDateTime local = ZonedDateTime.now(ZoneId.of(sa.store.timezone()));
        s.setLastAppliedTime(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0));
        s.setLastFactor(factors.get(local.getDayOfWeek().getValue() - 1).get(local.getHour()));
        s.setLastMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.scheduleNoApi"));
        s.setRemark("凌晨降价，白天和晚高峰加价，周末白天再加 10%"); // i18n-ignore 演示数据存库
        scheduleMapper.insert(s);
    }

    // ---------------------------------------------------------------- 工具

    private static List<Tgt> targets(StoreAds sa) {
        List<Tgt> r = new ArrayList<>();
        for (Camp c : sa.camps) {
            for (Grp g : c.groups) {
                r.addAll(g.targets);
            }
        }
        return r;
    }

    private static BigDecimal money(double v, int scale) {
        return BigDecimal.valueOf(Math.max(0, v)).setScale(scale, RoundingMode.HALF_UP);
    }

    /** 随机取整：小数部分作为进位概率（小量数据更自然） */
    private static long stochRound(double v, Random rnd) {
        if (v <= 0) {
            return 0;
        }
        long f = (long) Math.floor(v);
        return f + (rnd.nextDouble() < v - f ? 1 : 0);
    }

    private static int pick(List<Double> weights, Random rnd) {
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        if (total <= 0) {
            return -1;
        }
        double x = rnd.nextDouble() * total;
        for (int i = 0; i < weights.size(); i++) {
            x -= weights.get(i);
            if (x < 0 && weights.get(i) > 0) {
                return i;
            }
        }
        for (int i = weights.size() - 1; i >= 0; i--) {
            if (weights.get(i) > 0) {
                return i;
            }
        }
        return -1;
    }

    private static int pick(double[] weights, Random rnd) {
        List<Double> list = new ArrayList<>(weights.length);
        for (double w : weights) {
            list.add(w);
        }
        int k = pick(list, rnd);
        return k < 0 ? 0 : k;
    }

    private static double sum(double[] w) {
        double s = 0;
        for (double x : w) {
            s += x;
        }
        return s;
    }

    private static double[] toDouble(long[] v) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = v[i];
        }
        return r;
    }

    /** 整数按权重拆分（最大余数法），合计不变 */
    private static long[] splitLong(long total, double[] w) {
        long[] r = new long[w.length];
        double s = sum(w);
        if (total == 0 || s <= 0) {
            return r;
        }
        double[] frac = new double[w.length];
        long used = 0;
        for (int i = 0; i < w.length; i++) {
            double exact = total * w[i] / s;
            r[i] = (long) Math.floor(exact);
            frac[i] = exact - r[i];
            used += r[i];
        }
        for (long left = total - used; left > 0; left--) {
            int best = 0;
            for (int i = 1; i < w.length; i++) {
                if (frac[i] > frac[best]) {
                    best = i;
                }
            }
            r[best]++;
            frac[best] = -1;
        }
        return r;
    }

    /** 金额按权重拆分：前面各项向下取整，余数给最后一个有权重的项，合计不变；权重全为 0 时全部给第一项 */
    private static BigDecimal[] splitMoney(BigDecimal total, double[] w, int scale) {
        BigDecimal[] r = new BigDecimal[w.length];
        Arrays.fill(r, BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP));
        if (w.length == 0 || total.signum() == 0) {
            return r;
        }
        double s = sum(w);
        if (s <= 0) {
            r[0] = total;
            return r;
        }
        int last = -1;
        for (int i = 0; i < w.length; i++) {
            if (w[i] > 0) {
                last = i;
            }
        }
        BigDecimal used = BigDecimal.ZERO;
        for (int i = 0; i < w.length; i++) {
            if (w[i] <= 0 || i == last) {
                continue;
            }
            r[i] = total.multiply(BigDecimal.valueOf(w[i] / s)).setScale(scale, RoundingMode.DOWN);
            used = used.add(r[i]);
        }
        r[last] = total.subtract(used);
        return r;
    }

}
