package com.lemon.erp.modules.ecom.oper.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitor;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorAlert;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import com.lemon.erp.modules.ecom.oper.mapper.EcomMonitorAlertMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomOperReportMapper;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorSource;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorType;
import com.lemon.erp.modules.ecom.oper.service.EcomMonitorService;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * 演示数据 · ASIN / 商品监控（order 151）：亚马逊美 / 德 / 日站各取销量最高的 3 个 ASIN 作为自己的商品监控，每个配 1–2 个竞品，
 * 虾皮、TikTok 各 2 个自己的商品（手工录入来源）；生成 60 天的每日快照（排名随机游走、评论数逐日增长、评分小幅波动），
 * 并安排几个事件触发预警：新增跟卖、排名大跌、评分下降、竞品改标题 / 换主图 / 改五点、竞品被下架。较早的预警已处理或忽略，最近几天的为未处理
 */
@Component
@RequiredArgsConstructor
public class OperMonitorDemoContributor implements DemoContributor {

    static final int DAYS = 60;

    private final EcomStoreService storeService;
    private final EcomListingService listingService;
    private final EcomMonitorService monitorService;
    private final EcomMonitorAlertMapper alertMapper;
    private final EcomOperReportMapper reportMapper;

    /** 一个监控的走势参数与要安排的事件（距今天数，-1 为不安排） */
    private static final class Plan {
        int rank;
        int subRank;
        double rating;
        int reviews;
        int dailyReviews;
        double price;
        int hijackDay;
        int rankDropDay;
        int ratingDropDay;
        int titleDay;
        int imageDay;
        int bulletsDay;
        int delistDay;
    }

    private static final String[] CATEGORIES = {"Electronics", "Home & Kitchen", "Sports & Outdoors", "Toys & Games", "Beauty & Personal Care"};
    private static final String[] SUBS = {"Earbud Headphones", "Kitchen Storage", "Yoga Mats", "Building Toys", "Hair Dryers", "Phone Cases", "LED Strip Lights"};
    private static final String[] COMPETITOR_BRANDS = {"Ankerly", "SoundPeak", "HomeNest", "FitPro", "Lumina", "ZenGear"};

    @Override
    public int order() {
        return 151;
    }

    @Override
    public String key() {
        return "oper-monitor";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, EcomStore> stores = OperDemoSupport.stores(ctx, storeService);
        Random rnd = ctx.random();
        LocalDate today = ctx.today();
        int seq = 0;
        Set<String> used = new HashSet<>();
        for (String code : List.of(OperDemoSupport.AMZ_US, OperDemoSupport.AMZ_DE, OperDemoSupport.AMZ_JP, OperDemoSupport.SHP_MY, OperDemoSupport.TT_US)) {
            EcomStore store = stores.get(code);
            if (store == null) {
                continue;
            }
            boolean amazon = store.getPlatform() == EcomPlatform.AMAZON;
            List<Map<String, Object>> top = reportMapper.selectTopSkus(store.getId(), today.minusDays(90).atStartOfDay(), today.plusDays(1).atStartOfDay(), amazon ? 3 : 2);
            Map<String, EcomListing> listings = listingService.byStoreSkus(store.getId(), top.stream().map(r -> String.valueOf(r.get("sellerSku"))).toList());
            for (Map<String, Object> row : top) {
                seq++;
                EcomListing l = listings.get(String.valueOf(row.get("sellerSku")));
                String itemId = l != null && l.getPlatformItemId() != null ? l.getPlatformItemId() : Objects.toString(row.get("platformItemId"), null);
                if (itemId == null || !used.add(store.getPlatform() + "|" + store.getMarketplace() + "|" + itemId)) {
                    itemId = (amazon ? "B0DM" : "") + String.format("%06d", 100000 + seq * 37);
                    used.add(store.getPlatform() + "|" + store.getMarketplace() + "|" + itemId);
                }
                EcomMonitor own = new EcomMonitor();
                own.setMonitorType(EcomMonitorType.OWN);
                own.setPlatform(store.getPlatform());
                own.setMarketplace(store.getMarketplace());
                own.setItemId(itemId);
                own.setStoreId(store.getId());
                own.setListingId(l == null ? null : l.getId());
                own.setSellerSku(l == null ? String.valueOf(row.get("sellerSku")) : null);
                own.setBrand("Lemon Home");
                own.setAutoFetch(amazon ? 1 : 0);
                own.setRemark("演示监控"); // i18n-ignore 演示数据存库
                Long ownId = monitorService.create(own);
                String title = l != null && l.getTitle() != null ? l.getTitle() : Objects.toString(row.get("title"), null);
                String image = l == null ? null : l.getImageUrl();
                double price = l != null && l.getPrice() != null ? l.getPrice().doubleValue() : 19.99;
                Plan p = basePlan(rnd, price, amazon);
                if (seq == 1) {
                    p.hijackDay = 4;
                } else if (seq == 2) {
                    p.ratingDropDay = 12;
                } else if (seq == 3) {
                    p.titleDay = 2;
                } else if (seq == 4) {
                    p.rankDropDay = 2;
                } else if (seq == 5) {
                    p.imageDay = 1;
                } else if (seq == 7) {
                    p.hijackDay = 25;
                }
                save(ownId, history(rnd, today, p, title, image, bullets(title), amazon ? EcomMonitorSource.API : EcomMonitorSource.MANUAL), today);
                if (!amazon) {
                    continue;
                }
                int competitors = seq % 2 == 0 ? 1 : 2;
                for (int c = 0; c < competitors; c++) {
                    EcomMonitor comp = new EcomMonitor();
                    comp.setMonitorType(EcomMonitorType.COMPETITOR);
                    comp.setPlatform(store.getPlatform());
                    comp.setMarketplace(store.getMarketplace());
                    comp.setItemId("B0C" + String.format("%07d", 1000000 + seq * 131 + c * 17));
                    comp.setBenchmarkId(ownId);
                    String brand = COMPETITOR_BRANDS[(seq + c) % COMPETITOR_BRANDS.length];
                    comp.setBrand(brand);
                    comp.setAutoFetch(1);
                    comp.setRemark("演示竞品"); // i18n-ignore 演示数据存库
                    Long compId = monitorService.create(comp);
                    Plan cp = basePlan(rnd, price * (0.85 + rnd.nextDouble() * 0.3), true);
                    cp.rank = Math.max(200, (int) (p.rank * (0.5 + rnd.nextDouble())));
                    if (seq == 1 && c == 0) {
                        cp.titleDay = 3;
                        cp.imageDay = 20;
                    } else if (seq == 3 && c == 0) {
                        cp.bulletsDay = 8;
                    } else if (seq == 5 && c == 0) {
                        cp.rankDropDay = 15;
                    } else if (seq == 7 && c == 0) {
                        cp.delistDay = 1;
                    }
                    String compTitle = brand + " " + (title == null ? "" : title.replaceAll("(?i)lemon\\s*home", "").trim());
                    save(compId, history(rnd, today, cp, compTitle, null, bullets(compTitle), EcomMonitorSource.API), today);
                }
            }
        }
    }

    private static Plan basePlan(Random rnd, double price, boolean amazon) {
        Plan p = new Plan();
        p.rank = amazon ? 600 + rnd.nextInt(15000) : 0;
        p.subRank = amazon ? 3 + rnd.nextInt(120) : 0;
        p.rating = 4.2 + rnd.nextInt(6) * 0.1;
        p.reviews = 150 + rnd.nextInt(3000);
        p.dailyReviews = 1 + rnd.nextInt(6);
        p.price = price;
        p.hijackDay = -1;
        p.rankDropDay = -1;
        p.ratingDropDay = -1;
        p.titleDay = -1;
        p.imageDay = -1;
        p.bulletsDay = -1;
        p.delistDay = -1;
        return p;
    }

    private static String bullets(String title) {
        String t = title == null ? "Product" : title.length() > 40 ? title.substring(0, 40) : title;
        return String.join("\n",
                "PREMIUM QUALITY: " + t + " made with durable, eco-friendly materials for daily use",
                "EASY TO USE: Ready out of the box, no tools required, fits most spaces",
                "WHAT YOU GET: 1 x " + t + ", user manual and 24-month worry-free warranty",
                "GIFT IDEA: Perfect for birthdays, holidays and housewarming",
                "SUPPORT: Contact us anytime, we reply within 24 hours");
    }

    /** 生成 60 天快照：排名每天 ±8% 内随机游走，评论数逐日增加，按计划插入事件 */
    private static List<EcomMonitorSnapshot> history(Random rnd, LocalDate today, Plan p, String title, String image, String bullets,
                                                     EcomMonitorSource source) {
        List<EcomMonitorSnapshot> list = new ArrayList<>();
        double rank = p.rank;
        double sub = p.subRank;
        double rating = p.rating;
        int reviews = p.reviews - p.dailyReviews * DAYS;
        int sellers = 1;
        String curTitle = title;
        String curImage = image == null ? "https://images.demo.lemon-ecom.local/" + Math.abs(String.valueOf(title).hashCode()) + "/main-v1.jpg" : image;
        String curBullets = bullets;
        ListingStatus status = ListingStatus.ACTIVE;
        for (int d = DAYS - 1; d >= 0; d--) {
            if (rank > 0) {
                rank = Math.max(50, rank * (0.93 + rnd.nextDouble() * 0.14));
                sub = Math.max(1, sub * (0.92 + rnd.nextDouble() * 0.16));
            }
            reviews += rnd.nextInt(p.dailyReviews * 2 + 1);
            if (d == p.rankDropDay && rank > 0) {
                rank = rank * 1.6;
                sub = sub * 1.8;
            }
            if (d == p.ratingDropDay) {
                rating = rating - 0.2;
            }
            if (d == p.hijackDay) {
                sellers = 2;
            } else if (p.hijackDay > 10 && d == p.hijackDay - 3) {
                sellers = 1;
            }
            if (d == p.titleDay && curTitle != null) {
                curTitle = curTitle + " - 2026 Upgraded Version";
            }
            if (d == p.imageDay) {
                curImage = curImage.contains("main-v1") ? curImage.replace("main-v1", "main-v2") : curImage + (curImage.contains("?") ? "&" : "?") + "v=2";
            }
            if (d == p.bulletsDay) {
                curBullets = "NEW 2026 MODEL: faster, lighter and 30% longer battery life\n" + curBullets;
            }
            if (d == p.delistDay) {
                status = ListingStatus.INACTIVE;
            }
            EcomMonitorSnapshot s = new EcomMonitorSnapshot();
            s.setSnapshotDate(today.minusDays(d));
            if (rank > 0) {
                s.setCategory(CATEGORIES[Math.abs(String.valueOf(title).hashCode()) % CATEGORIES.length]);
                s.setRankMain((int) Math.round(rank));
                s.setSubCategory(SUBS[Math.abs(String.valueOf(title).hashCode()) % SUBS.length]);
                s.setRankSub((int) Math.round(sub));
            }
            s.setRating(BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP));
            s.setReviewCount(reviews);
            s.setSellerCount(status == ListingStatus.INACTIVE ? 0 : sellers);
            s.setPrice(BigDecimal.valueOf(p.price * (d % 7 == 0 ? 0.95 : 1)).setScale(2, RoundingMode.HALF_UP));
            s.setTitle(curTitle == null ? null : curTitle.length() > 500 ? curTitle.substring(0, 500) : curTitle);
            s.setMainImage(curImage);
            s.setBullets(curBullets);
            s.setListingStatus(status);
            s.setSource(source);
            list.add(s);
        }
        return list;
    }

    /** 写入历史并把 5 天前的预警标为已处理 / 已忽略 */
    private void save(Long monitorId, List<EcomMonitorSnapshot> history, LocalDate today) {
        List<EcomMonitorAlert> alerts = monitorService.saveHistory(monitorId, history);
        for (EcomMonitorAlert a : alerts) {
            if (a.getAlertDate().isBefore(today.minusDays(5))) {
                EcomMonitorAlertStatus st = a.getId() % 3 == 0 ? EcomMonitorAlertStatus.IGNORED : EcomMonitorAlertStatus.HANDLED;
                alertMapper.update(Wrappers.<EcomMonitorAlert>lambdaUpdate().set(EcomMonitorAlert::getStatus, st)
                        .set(EcomMonitorAlert::getHandleTime, a.getAlertDate().plusDays(1).atTime(10, 30))
                        .set(EcomMonitorAlert::getHandleNote, st == EcomMonitorAlertStatus.HANDLED ? "已跟进处理" : "正常波动，忽略") // i18n-ignore 演示数据存库
                        .eq(EcomMonitorAlert::getId, a.getId()));
            }
        }
    }
}
