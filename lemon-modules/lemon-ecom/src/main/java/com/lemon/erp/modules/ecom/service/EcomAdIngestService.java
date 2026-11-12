package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.ads.AdsReportRow;
import com.lemon.erp.modules.ecom.ads.AdsStructure;
import com.lemon.erp.modules.ecom.dto.AdReportImportRequest;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdSearchTerm;
import com.lemon.erp.modules.ecom.entity.EcomAdStat;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdDailyMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdSearchTermMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdStatMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.model.EcomAdDailyLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 广告报告与结构的写入（文件导入、广告接口同步共用）：
 * <ul>
 *   <li>活动报告 → 活动级日统计（ecom_ad_stat CAMPAIGN），同一店铺、日期、活动在广告日报中还没有推广商品数据时，
 *       另写一行活动级日报（ad_level = CAMPAIGN，SKU 为空），利润分析的店铺广告花费因此完整</li>
 *   <li>推广商品报告 → 广告日报（店铺 × 日期 × 活动 × SKU，覆盖；同一 SKU 在多个广告组时合并、广告组为空），并删除同一店铺、日期、活动的活动级日报，
 *       避免重复计算花费</li>
 *   <li>投放报告 → 投放级日统计（ecom_ad_stat TARGET）</li>
 *   <li>搜索词报告 → ecom_ad_search_term，按 店铺 + 活动 + 日期范围 整体替换</li>
 * </ul>
 * 活动 / 广告组 / 投放按平台 ID 或名称关联，没有时自动建立。所有数据重复导入都是覆盖，不会累加
 */
@Service
@RequiredArgsConstructor
public class EcomAdIngestService {

    private static final int BATCH = 500;

    private final EcomAdStructureService structureService;
    private final EcomStoreService storeService;
    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;
    private final EcomAdStatMapper statMapper;
    private final EcomAdSearchTermMapper searchTermMapper;
    private final EcomAdDailyMapper dailyMapper;

    /** 导入广告后台下载的报告文件（有错误的行时整批不导入） */
    @Transactional
    public AdsViews.ImportResult importFile(AdReportImportRequest req) {
        EcomStore store = storeService.get(req.getStoreId());
        EcomAdReportParser.Result parsed = EcomAdReportParser.parse(req.getReportType(), req.getHeaders(), req.getRows(),
                req.getFirstRowNum() == null ? 2 : req.getFirstRowNum());
        if (!parsed.errors().isEmpty()) {
            return new AdsViews.ImportResult(parsed.type(), parsed.rows().size(), 0, 0, 0, 0, parsed.errors());
        }
        if (parsed.rows().isEmpty()) {
            throw new BizException("ecom.error.ads.reportEmpty");
        }
        return ingest(store, parsed.type(), req.getAdType(), parsed.rows());
    }

    /** 写入一种报告的行（调用方在事务中） */
    @Transactional
    public AdsViews.ImportResult ingest(EcomStore store, EcomAdReportType type, EcomAdType adType, List<AdsReportRow> rows) {
        Resolver r = new Resolver(store, adType);
        List<AdsReportRow> sorted = rows.stream().filter(x -> x.getDate() != null).sorted(Comparator.comparing(AdsReportRow::getDate)).toList();
        int written = switch (type) {
            case CAMPAIGN -> writeCampaigns(store, r, sorted);
            case PRODUCT -> writeProducts(store, r, sorted);
            case TARGETING -> writeTargets(store, r, sorted);
            case SEARCH_TERM -> writeSearchTerms(store, r, sorted);
        };
        return new AdsViews.ImportResult(type, rows.size(), written, r.campaignsCreated, r.groupsCreated, r.targetsCreated, List.of());
    }

    // ---------------------------------------------------------------- 活动报告

    private int writeCampaigns(EcomStore store, Resolver r, List<AdsReportRow> rows) {
        Map<String, EcomAdStat> stats = new LinkedHashMap<>();
        for (AdsReportRow row : rows) {
            EcomAdCampaign c = r.campaign(row);
            boolean changed = false;
            if (row.getCampaignStatus() != null && row.getCampaignStatus() != c.getStatus()) {
                c.setStatus(row.getCampaignStatus());
                changed = true;
            }
            if (row.getCampaignBudget() != null && row.getCampaignBudget().signum() > 0 && !Decimals.money(row.getCampaignBudget()).equals(c.getDailyBudget())) {
                c.setDailyBudget(Decimals.money(row.getCampaignBudget()));
                changed = true;
            }
            if (changed) {
                campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getStatus, c.getStatus())
                        .set(EcomAdCampaign::getDailyBudget, c.getDailyBudget()).eq(EcomAdCampaign::getId, c.getId()));
            }
            EcomAdStat s = stats.computeIfAbsent(c.getId() + "|" + row.getDate(), k -> newStat(store, row, EcomAdLevel.CAMPAIGN, c.getId(), c.getId(), null));
            accumulate(s, row);
        }
        upsertStats(store, EcomAdLevel.CAMPAIGN, stats.values());
        // 活动级日报：同一店铺、日期、活动还没有推广商品数据时写入，有时不写
        if (!stats.isEmpty()) {
            LocalDate min = rows.get(0).getDate();
            LocalDate max = rows.get(rows.size() - 1).getDate();
            Map<Long, EcomAdCampaign> byId = r.campaignsById();
            List<String> names = byId.values().stream().map(EcomAdCampaign::getName).distinct().toList();
            Map<String, List<EcomAdDaily>> existing = new HashMap<>();
            for (EcomAdDaily d : dailyMapper.selectList(Wrappers.<EcomAdDaily>lambdaQuery().eq(EcomAdDaily::getStoreId, store.getId())
                    .between(EcomAdDaily::getAdDate, min, max).in(EcomAdDaily::getCampaign, names))) {
                existing.computeIfAbsent(d.getCampaign() + "|" + d.getAdDate(), k -> new ArrayList<>()).add(d);
            }
            for (EcomAdStat s : stats.values()) {
                EcomAdCampaign c = byId.get(s.getCampaignId());
                List<EcomAdDaily> list = existing.getOrDefault(c.getName() + "|" + s.getStatDate(), List.of());
                boolean hasProduct = list.stream().anyMatch(d -> d.getAdLevel() != EcomAdDailyLevel.CAMPAIGN);
                if (hasProduct) {
                    continue;
                }
                EcomAdDaily d = list.isEmpty() ? new EcomAdDaily() : list.get(0);
                d.setStoreId(store.getId());
                d.setAdDate(s.getStatDate());
                d.setCampaign(c.getName());
                d.setSellerSku("");
                d.setCampaignId(c.getId());
                d.setAdGroupId(null);
                d.setAdLevel(EcomAdDailyLevel.CAMPAIGN);
                d.setImpressions(s.getImpressions());
                d.setClicks(s.getClicks());
                d.setSpend(Decimals.money(s.getSpend()));
                d.setAdSales(Decimals.money(s.getSales()));
                d.setAdOrders(s.getOrders());
                d.setCurrency(s.getCurrency());
                if (d.getId() == null) {
                    dailyMapper.insert(d);
                } else {
                    dailyMapper.updateById(d);
                }
            }
        }
        return stats.size();
    }

    // ---------------------------------------------------------------- 推广商品报告 → 广告日报

    private int writeProducts(EcomStore store, Resolver r, List<AdsReportRow> rows) {
        Map<String, EcomAdDaily> agg = new LinkedHashMap<>();
        Map<String, Set<Long>> groupsOf = new HashMap<>();
        for (AdsReportRow row : rows) {
            EcomAdCampaign c = r.campaign(row);
            EcomAdGroup g = r.group(c, row);
            String sku = row.getSellerSku() == null ? "" : truncate(row.getSellerSku().trim(), 64);
            String key = c.getName() + "|" + row.getDate() + "|" + sku;
            EcomAdDaily d = agg.computeIfAbsent(key, k -> {
                EcomAdDaily x = new EcomAdDaily();
                x.setStoreId(store.getId());
                x.setAdDate(row.getDate());
                x.setCampaign(c.getName());
                x.setSellerSku(sku);
                x.setCampaignId(c.getId());
                x.setAdLevel(EcomAdDailyLevel.PRODUCT);
                x.setImpressions(0L);
                x.setClicks(0L);
                x.setSpend(BigDecimal.ZERO);
                x.setAdSales(BigDecimal.ZERO);
                x.setAdOrders(0);
                x.setCurrency(currency(store, row));
                return x;
            });
            if (row.getAsin() != null) {
                d.setPlatformItemId(truncate(row.getAsin(), 64));
            }
            d.setImpressions(d.getImpressions() + row.getImpressions());
            d.setClicks(d.getClicks() + row.getClicks());
            d.setSpend(d.getSpend().add(Decimals.nz(row.getSpend())));
            d.setAdSales(d.getAdSales().add(Decimals.nz(row.getSales())));
            d.setAdOrders(d.getAdOrders() + row.getOrders());
            groupsOf.computeIfAbsent(key, k -> new HashSet<>()).add(g == null ? 0L : g.getId());
        }
        if (agg.isEmpty()) {
            return 0;
        }
        LocalDate min = rows.get(0).getDate();
        LocalDate max = rows.get(rows.size() - 1).getDate();
        List<String> names = agg.values().stream().map(EcomAdDaily::getCampaign).distinct().toList();
        Map<String, EcomAdDaily> existing = new HashMap<>();
        Set<String> productKeys = new HashSet<>();
        for (EcomAdDaily d : dailyMapper.selectList(Wrappers.<EcomAdDaily>lambdaQuery().eq(EcomAdDaily::getStoreId, store.getId())
                .between(EcomAdDaily::getAdDate, min, max).in(EcomAdDaily::getCampaign, names))) {
            existing.put(d.getCampaign() + "|" + d.getAdDate() + "|" + d.getSellerSku(), d);
        }
        for (Map.Entry<String, EcomAdDaily> e : agg.entrySet()) {
            EcomAdDaily d = e.getValue();
            Set<Long> groups = groupsOf.get(e.getKey());
            d.setAdGroupId(groups.size() == 1 && !groups.contains(0L) ? groups.iterator().next() : null);
            d.setSpend(Decimals.money(d.getSpend()));
            d.setAdSales(Decimals.money(d.getAdSales()));
            EcomAdDaily old = existing.get(e.getKey());
            if (old == null) {
                dailyMapper.insert(d);
            } else {
                d.setId(old.getId());
                dailyMapper.updateById(d);
                if (d.getAdGroupId() == null) {
                    dailyMapper.update(Wrappers.<EcomAdDaily>lambdaUpdate().set(EcomAdDaily::getAdGroupId, null).eq(EcomAdDaily::getId, d.getId()));
                }
            }
            productKeys.add(d.getCampaign() + "|" + d.getAdDate());
        }
        // 删除同一活动、日期的活动级日报（SKU 为空的活动级行如果刚被推广商品数据覆盖，已改为 PRODUCT，不会删除）
        for (EcomAdDaily d : existing.values()) {
            if (d.getAdLevel() == EcomAdDailyLevel.CAMPAIGN && productKeys.contains(d.getCampaign() + "|" + d.getAdDate())
                    && !agg.containsKey(d.getCampaign() + "|" + d.getAdDate() + "|" + d.getSellerSku())) {
                dailyMapper.deleteById(d.getId());
            }
        }
        return agg.size();
    }

    /** 旧版广告日报导入的一行：关联（或建立）活动，并删除同一活动、日期的活动级日报 */
    public void linkLegacy(EcomStore store, EcomAdDaily a) {
        if (!StringUtils.hasText(a.getCampaign())) {
            return;
        }
        EcomAdCampaign c = structureService.ensureCampaign(store, null, a.getCampaign(), null, null);
        a.setCampaignId(c.getId());
        a.setAdLevel(EcomAdDailyLevel.PRODUCT);
        dailyMapper.delete(Wrappers.<EcomAdDaily>lambdaQuery().eq(EcomAdDaily::getStoreId, store.getId()).eq(EcomAdDaily::getAdDate, a.getAdDate())
                .eq(EcomAdDaily::getCampaign, a.getCampaign()).eq(EcomAdDaily::getAdLevel, EcomAdDailyLevel.CAMPAIGN)
                .ne(EcomAdDaily::getSellerSku, a.getSellerSku() == null ? "" : a.getSellerSku()));
    }

    // ---------------------------------------------------------------- 投放报告

    private int writeTargets(EcomStore store, Resolver r, List<AdsReportRow> rows) {
        Map<String, EcomAdStat> stats = new LinkedHashMap<>();
        for (AdsReportRow row : rows) {
            EcomAdCampaign c = r.campaign(row);
            EcomAdGroup g = r.groupRequired(c, row);
            EcomAdTarget t = r.target(g, row);
            EcomAdStat s = stats.computeIfAbsent(t.getId() + "|" + row.getDate(), k -> newStat(store, row, EcomAdLevel.TARGET, t.getId(), c.getId(), g.getId()));
            accumulate(s, row);
        }
        upsertStats(store, EcomAdLevel.TARGET, stats.values());
        return stats.size();
    }

    // ---------------------------------------------------------------- 搜索词报告

    private int writeSearchTerms(EcomStore store, Resolver r, List<AdsReportRow> rows) {
        Map<String, EcomAdSearchTerm> agg = new LinkedHashMap<>();
        for (AdsReportRow row : rows) {
            EcomAdCampaign c = r.campaign(row);
            EcomAdGroup g = r.group(c, row);
            EcomAdTarget t = g == null || row.getTargetText() == null ? null : r.target(g, row);
            String term = truncate(row.getSearchTerm().trim(), 255);
            String key = c.getId() + "|" + (g == null ? 0 : g.getId()) + "|" + (t == null ? 0 : t.getId()) + "|" + row.getDate() + "|" + term.toLowerCase();
            EcomAdSearchTerm s = agg.computeIfAbsent(key, k -> {
                EcomAdSearchTerm x = new EcomAdSearchTerm();
                x.setStoreId(store.getId());
                x.setStatDate(row.getDate());
                x.setCampaignId(c.getId());
                x.setAdGroupId(g == null ? null : g.getId());
                x.setTargetId(t == null ? null : t.getId());
                x.setKeywordText(row.getTargetText() == null ? null : truncate(row.getTargetText(), 255));
                x.setMatchType(row.getMatchType() == null ? (row.getTargetType() == null ? null : row.getTargetType().name()) : row.getMatchType().name());
                x.setSearchTerm(term);
                x.setImpressions(0L);
                x.setClicks(0L);
                x.setSpend(BigDecimal.ZERO);
                x.setSales(BigDecimal.ZERO);
                x.setOrders(0);
                x.setCurrency(currency(store, row));
                return x;
            });
            s.setImpressions(s.getImpressions() + row.getImpressions());
            s.setClicks(s.getClicks() + row.getClicks());
            s.setSpend(s.getSpend().add(Decimals.nz(row.getSpend())));
            s.setSales(s.getSales().add(Decimals.nz(row.getSales())));
            s.setOrders(s.getOrders() + row.getOrders());
        }
        if (agg.isEmpty()) {
            return 0;
        }
        LocalDate min = rows.get(0).getDate();
        LocalDate max = rows.get(rows.size() - 1).getDate();
        Set<Long> campaignIds = new HashSet<>();
        agg.values().forEach(s -> campaignIds.add(s.getCampaignId()));
        searchTermMapper.delete(Wrappers.<EcomAdSearchTerm>lambdaQuery().eq(EcomAdSearchTerm::getStoreId, store.getId())
                .in(EcomAdSearchTerm::getCampaignId, campaignIds).between(EcomAdSearchTerm::getStatDate, min, max));
        List<EcomAdSearchTerm> list = new ArrayList<>(agg.values());
        list.forEach(s -> {
            s.setSpend(Decimals.money(s.getSpend()));
            s.setSales(Decimals.money(s.getSales()));
        });
        for (int i = 0; i < list.size(); i += BATCH) {
            searchTermMapper.insertBatch(list.subList(i, Math.min(list.size(), i + BATCH)));
        }
        return list.size();
    }

    // ---------------------------------------------------------------- 统计

    private static EcomAdStat newStat(EcomStore store, AdsReportRow row, EcomAdLevel level, Long entityId, Long campaignId, Long adGroupId) {
        EcomAdStat s = new EcomAdStat();
        s.setStoreId(store.getId());
        s.setStatDate(row.getDate());
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
        s.setCurrency(currency(store, row));
        return s;
    }

    private static void accumulate(EcomAdStat s, AdsReportRow row) {
        s.setImpressions(s.getImpressions() + row.getImpressions());
        s.setClicks(s.getClicks() + row.getClicks());
        s.setSpend(s.getSpend().add(Decimals.nz(row.getSpend())));
        s.setSales(s.getSales().add(Decimals.nz(row.getSales())));
        s.setOrders(s.getOrders() + row.getOrders());
        s.setUnits(s.getUnits() + row.getUnits());
    }

    /** 按 店铺 × 日期 × 层级 × 对象 覆盖写入 */
    private void upsertStats(EcomStore store, EcomAdLevel level, java.util.Collection<EcomAdStat> stats) {
        if (stats.isEmpty()) {
            return;
        }
        LocalDate min = stats.stream().map(EcomAdStat::getStatDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate max = stats.stream().map(EcomAdStat::getStatDate).max(Comparator.naturalOrder()).orElseThrow();
        Set<Long> ids = new HashSet<>();
        stats.forEach(s -> ids.add(s.getEntityId()));
        Map<String, EcomAdStat> existing = new HashMap<>();
        for (EcomAdStat s : statMapper.selectList(Wrappers.<EcomAdStat>lambdaQuery().eq(EcomAdStat::getStoreId, store.getId())
                .eq(EcomAdStat::getLevel, level).in(EcomAdStat::getEntityId, ids).between(EcomAdStat::getStatDate, min, max))) {
            existing.put(s.getEntityId() + "|" + s.getStatDate(), s);
        }
        List<EcomAdStat> inserts = new ArrayList<>();
        for (EcomAdStat s : stats) {
            s.setSpend(Decimals.money(s.getSpend()));
            s.setSales(Decimals.money(s.getSales()));
            EcomAdStat old = existing.get(s.getEntityId() + "|" + s.getStatDate());
            if (old == null) {
                inserts.add(s);
            } else {
                s.setId(old.getId());
                statMapper.updateById(s);
            }
        }
        for (int i = 0; i < inserts.size(); i += BATCH) {
            statMapper.insertBatch(inserts.subList(i, Math.min(inserts.size(), i + BATCH)));
        }
    }

    private static String currency(EcomStore store, AdsReportRow row) {
        return StringUtils.hasText(row.getCurrency()) ? truncate(row.getCurrency(), 8) : store.getCurrency();
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    // ---------------------------------------------------------------- 结构同步

    /**
     * 写入从广告接口拉取的结构：按平台 ID（没有时按名称）对应本地记录并更新名称、状态、预算 / 竞价等，没有的新建；
     * 本地有平台 ID 但这次没有返回的（已归档或删除）标记为已归档
     */
    @Transactional
    public AdsViews.SyncResult applyStructure(EcomStore store, AdsStructure s) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, EcomAdCampaign> campaigns = new HashMap<>();
        for (AdsStructure.Campaign pc : s.campaigns()) {
            if (pc.platformId() == null) {
                continue;
            }
            EcomAdCampaign c = structureService.findCampaign(store.getId(), pc.platformId(), pc.name());
            boolean isNew = c == null;
            if (isNew) {
                c = new EcomAdCampaign();
                c.setStoreId(store.getId());
            }
            c.setPlatformCampaignId(pc.platformId());
            if (StringUtils.hasText(pc.name()) && (isNew || !pc.name().equals(c.getName()))
                    && structureService.findCampaign(store.getId(), null, pc.name()) == null) {
                c.setName(truncate(pc.name(), 128));
            } else if (isNew) {
                c.setName(pc.platformId());
            }
            c.setAdType(pc.adType() == null ? EcomAdStructureService.defaultType(store.getPlatform()) : pc.adType());
            c.setStatus(pc.status() == null ? EcomAdStatus.ENABLED : pc.status());
            c.setDailyBudget(pc.dailyBudget() == null ? c.getDailyBudget() : Decimals.money(pc.dailyBudget()));
            c.setBiddingStrategy(truncate(pc.biddingStrategy(), 32));
            c.setTargetingType(truncate(pc.targetingType(), 16));
            c.setStartDate(pc.startDate());
            c.setEndDate(pc.endDate());
            c.setSyncedTime(now);
            if (isNew) {
                campaignMapper.insert(c);
            } else {
                campaignMapper.updateById(c);
            }
            campaigns.put(pc.platformId(), c);
        }
        Map<String, EcomAdGroup> groups = new HashMap<>();
        for (AdsStructure.AdGroup pg : s.adGroups()) {
            EcomAdCampaign c = campaigns.get(pg.campaignPlatformId());
            if (c == null || pg.platformId() == null) {
                continue;
            }
            EcomAdGroup g = structureService.findGroup(c.getId(), pg.platformId(), pg.name());
            boolean isNew = g == null;
            if (isNew) {
                g = new EcomAdGroup();
                g.setStoreId(store.getId());
                g.setCampaignId(c.getId());
            }
            g.setPlatformAdGroupId(pg.platformId());
            g.setName(truncate(StringUtils.hasText(pg.name()) ? pg.name() : pg.platformId(), 128));
            g.setStatus(pg.status() == null ? EcomAdStatus.ENABLED : pg.status());
            g.setDefaultBid(pg.defaultBid() == null ? g.getDefaultBid() : Decimals.money(pg.defaultBid()));
            g.setSyncedTime(now);
            if (isNew) {
                groupMapper.insert(g);
            } else {
                groupMapper.updateById(g);
            }
            groups.put(pg.platformId(), g);
        }
        Set<Long> seenTargets = new HashSet<>();
        for (AdsStructure.Target pt : s.targets()) {
            EcomAdGroup g = groups.get(pt.adGroupPlatformId());
            if (g == null || pt.platformId() == null) {
                continue;
            }
            EcomAdTarget t = structureService.findTarget(g.getId(), pt.platformId(), pt.text(), pt.matchType(), pt.targetType());
            boolean isNew = t == null;
            if (isNew) {
                t = new EcomAdTarget();
                t.setStoreId(store.getId());
                t.setCampaignId(g.getCampaignId());
                t.setAdGroupId(g.getId());
            }
            t.setPlatformTargetId(pt.platformId());
            t.setTargetType(pt.targetType());
            t.setTargetText(truncate(StringUtils.hasText(pt.text()) ? pt.text() : pt.platformId(), 255));
            t.setMatchType(pt.matchType());
            t.setStatus(pt.status() == null ? EcomAdStatus.ENABLED : pt.status());
            if (pt.bid() != null) {
                BigDecimal bid = Decimals.money(pt.bid());
                t.setBid(bid);
                // 没有分时系数时平台竞价就是基准竞价；有系数时基准竞价保持不变（平台竞价 = 基准 × 系数）
                if (t.getScheduleFactor() == null || t.getBaseBid() == null) {
                    t.setBaseBid(bid);
                }
            }
            t.setSyncedTime(now);
            if (isNew) {
                targetMapper.insert(t);
            } else {
                targetMapper.updateById(t);
            }
            seenTargets.add(t.getId());
        }
        archiveMissing(store, campaigns, groups, seenTargets);
        return new AdsViews.SyncResult(campaigns.size(), groups.size(), seenTargets.size(), Map.of(), null);
    }

    private void archiveMissing(EcomStore store, Map<String, EcomAdCampaign> campaigns, Map<String, EcomAdGroup> groups, Set<Long> targets) {
        Set<Long> campaignIds = new HashSet<>();
        campaigns.values().forEach(c -> campaignIds.add(c.getId()));
        Set<Long> groupIds = new HashSet<>();
        groups.values().forEach(g -> groupIds.add(g.getId()));
        campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getStatus, EcomAdStatus.ARCHIVED)
                .eq(EcomAdCampaign::getStoreId, store.getId()).isNotNull(EcomAdCampaign::getPlatformCampaignId)
                .ne(EcomAdCampaign::getStatus, EcomAdStatus.ARCHIVED).notIn(!campaignIds.isEmpty(), EcomAdCampaign::getId, campaignIds));
        groupMapper.update(Wrappers.<EcomAdGroup>lambdaUpdate().set(EcomAdGroup::getStatus, EcomAdStatus.ARCHIVED)
                .eq(EcomAdGroup::getStoreId, store.getId()).isNotNull(EcomAdGroup::getPlatformAdGroupId)
                .ne(EcomAdGroup::getStatus, EcomAdStatus.ARCHIVED).notIn(!groupIds.isEmpty(), EcomAdGroup::getId, groupIds));
        targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getStatus, EcomAdStatus.ARCHIVED)
                .eq(EcomAdTarget::getStoreId, store.getId()).isNotNull(EcomAdTarget::getPlatformTargetId)
                .ne(EcomAdTarget::getStatus, EcomAdStatus.ARCHIVED).notIn(!targets.isEmpty(), EcomAdTarget::getId, targets));
    }

    /** 一批数据内的活动 / 广告组 / 投放查找缓存（找不到时建立） */
    private final class Resolver {
        private final EcomStore store;
        private final EcomAdType adType;
        private final Map<String, EcomAdCampaign> campaigns = new HashMap<>();
        private final Map<String, EcomAdGroup> groups = new HashMap<>();
        private final Map<String, EcomAdTarget> targets = new HashMap<>();
        int campaignsCreated;
        int groupsCreated;
        int targetsCreated;

        Resolver(EcomStore store, EcomAdType adType) {
            this.store = store;
            this.adType = adType;
        }

        EcomAdCampaign campaign(AdsReportRow row) {
            String key = Objects.toString(row.getCampaignPlatformId(), "") + "|" + Objects.toString(row.getCampaignName(), "");
            return campaigns.computeIfAbsent(key, k -> {
                boolean[] created = {false};
                EcomAdCampaign c = structureService.ensureCampaign(store, row.getCampaignPlatformId(), truncate(row.getCampaignName(), 128), adType, created);
                if (created[0]) {
                    campaignsCreated++;
                }
                return c;
            });
        }

        Map<Long, EcomAdCampaign> campaignsById() {
            Map<Long, EcomAdCampaign> m = new HashMap<>();
            campaigns.values().forEach(c -> m.put(c.getId(), c));
            return m;
        }

        EcomAdGroup group(EcomAdCampaign c, AdsReportRow row) {
            if (!StringUtils.hasText(row.getAdGroupPlatformId()) && !StringUtils.hasText(row.getAdGroupName())) {
                return null;
            }
            String key = c.getId() + "|" + Objects.toString(row.getAdGroupPlatformId(), "") + "|" + Objects.toString(row.getAdGroupName(), "");
            return groups.computeIfAbsent(key, k -> {
                boolean[] created = {false};
                EcomAdGroup g = structureService.ensureGroup(c, row.getAdGroupPlatformId(), truncate(row.getAdGroupName(), 128), created);
                if (created[0]) {
                    groupsCreated++;
                }
                return g;
            });
        }

        EcomAdGroup groupRequired(EcomAdCampaign c, AdsReportRow row) {
            EcomAdGroup g = group(c, row);
            if (g == null) {
                throw new BizException("ecom.error.ads.reportAdGroupMissing", row.getRowNum());
            }
            return g;
        }

        EcomAdTarget target(EcomAdGroup g, AdsReportRow row) {
            String key = g.getId() + "|" + Objects.toString(row.getTargetPlatformId(), "") + "|" + Objects.toString(row.getTargetText(), "")
                    + "|" + row.getMatchType() + "|" + row.getTargetType();
            return targets.computeIfAbsent(key, k -> {
                boolean[] created = {false};
                EcomAdTarget t = structureService.ensureTarget(g, row.getTargetPlatformId(), row.getTargetText(), row.getMatchType(), row.getTargetType(), created);
                if (t == null) {
                    throw new BizException("ecom.error.ads.reportTargetMissing");
                }
                if (created[0]) {
                    targetsCreated++;
                }
                return t;
            });
        }
    }
}
