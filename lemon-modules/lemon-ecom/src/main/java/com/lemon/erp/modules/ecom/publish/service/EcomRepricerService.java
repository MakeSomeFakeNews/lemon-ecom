package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnectorRegistry;
import com.lemon.erp.modules.ecom.publish.connector.PublishSupport;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomCompetitorPrice;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceRule;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPricingMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomRepriceLogMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomRepriceRuleMapper;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.RepriceFloorMode;
import com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus;
import com.lemon.erp.modules.ecom.publish.model.RepriceStrategy;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动调价（Repricer）：规则维护、预览（只计算）、执行（手工立即执行或定时任务按执行间隔执行）、日志与建议的处理（应用 / 忽略）。
 * <p>
 * 执行：取规则适用的在售在线商品；需要竞品价格的策略，平台支持 Product Pricing API 的店铺先拉取竞品价格保存快照，其他平台使用手工录入 / 导入的快照
 * （{@link EcomCompetitorPriceService#FRESH_HOURS} 小时内最近的一条）；底价按固定值或「成本 + 最低利润率」推算，最高价可选；
 * 按 {@link RepriceEngine} 计算新价格。价格有变化的写日志：「仅建议」的为建议，否则经 {@link EcomPriceChangeService#applyPrices} 推送并写改价历史；
 * 没有依据的（缺竞品价格、成本）记为跳过；价格不变的不写日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomRepricerService {

    /** 一次执行最多处理的在线商品数 */
    static final int MAX_TARGETS = 2000;
    /** 按库存策略统计销量的天数 */
    static final int SALES_DAYS = 30;
    private static final Set<RepriceStrategy> NEEDS_COMPETITOR = EnumSet.of(RepriceStrategy.MATCH_LOWEST, RepriceStrategy.BEAT_LOWEST,
            RepriceStrategy.MATCH_BUYBOX);

    private final EcomRepriceRuleMapper ruleMapper;
    private final EcomRepriceLogMapper logMapper;
    private final EcomPricingMapper pricingMapper;
    private final EcomListingMapper listingMapper;
    private final EcomStoreService storeService;
    private final EcomPriceTemplateService priceTemplateService;
    private final EcomCompetitorPriceService competitorService;
    private final EcomPriceChangeService priceChangeService;
    private final PublishConnectorRegistry registry;
    private final EcomPublishSupport support;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 维护

    public PageResult<EcomRepriceRule> page(long current, long size, String keyword, EnableStatus status) {
        Page<EcomRepriceRule> page = Page.of(current, size);
        ruleMapper.selectPageList(page, StringUtils.hasText(keyword) ? keyword.trim() : null, status);
        return PageResult.of(page);
    }

    public EcomRepriceRule get(Long id) {
        EcomRepriceRule r = id == null ? null : ruleMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.pub.ruleNotFound");
        }
        return r;
    }

    public Long create(EcomRepriceRule r) {
        r.setId(null);
        normalize(r);
        checkName(r.getName(), null);
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        r.setLastRunTime(null);
        r.setLastRunMessage(null);
        ruleMapper.insert(r);
        return r.getId();
    }

    public void update(Long id, EcomRepriceRule r) {
        get(id);
        normalize(r);
        checkName(r.getName(), id);
        r.setId(id);
        r.setStatus(null);
        r.setLastRunTime(null);
        r.setLastRunMessage(null);
        // 全部字段一次写入（可清空的字段也写 null）
        ruleMapper.update(Wrappers.<EcomRepriceRule>lambdaUpdate().set(EcomRepriceRule::getName, r.getName())
                .set(EcomRepriceRule::getStoreIds, r.getStoreIds()).set(EcomRepriceRule::getSkus, r.getSkus())
                .set(EcomRepriceRule::getStrategy, r.getStrategy()).set(EcomRepriceRule::getAdjustMode, r.getAdjustMode())
                .set(EcomRepriceRule::getAdjustValue, r.getAdjustValue()).set(EcomRepriceRule::getTargetMargin, r.getTargetMargin())
                .set(EcomRepriceRule::getFloorMode, r.getFloorMode()).set(EcomRepriceRule::getMinPrice, r.getMinPrice())
                .set(EcomRepriceRule::getMaxPrice, r.getMaxPrice()).set(EcomRepriceRule::getMinMargin, r.getMinMargin())
                .set(EcomRepriceRule::getPriceTemplateId, r.getPriceTemplateId()).set(EcomRepriceRule::getStockHighDays, r.getStockHighDays())
                .set(EcomRepriceRule::getStockLowDays, r.getStockLowDays()).set(EcomRepriceRule::getIntervalMinutes, r.getIntervalMinutes())
                .set(EcomRepriceRule::getSuggestOnly, r.getSuggestOnly()).set(EcomRepriceRule::getRemark, r.getRemark())
                .eq(EcomRepriceRule::getId, id));
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        ruleMapper.update(Wrappers.<EcomRepriceRule>lambdaUpdate().set(EcomRepriceRule::getStatus, status).eq(EcomRepriceRule::getId, id));
    }

    public void delete(List<Long> ids) {
        transactionTemplate.executeWithoutResult(s -> {
            logMapper.delete(Wrappers.<EcomRepriceLog>lambdaQuery().in(EcomRepriceLog::getRuleId, ids));
            ruleMapper.deleteByIds(ids);
        });
    }

    private void normalize(EcomRepriceRule r) {
        r.setName(r.getName().trim());
        r.setStoreIds(cleanIds(r.getStoreIds()));
        r.setSkus(cleanList(r.getSkus()));
        r.setSuggestOnly(r.getSuggestOnly() == null || r.getSuggestOnly() == 1 ? 1 : 0);
        boolean needsAdjust = r.getStrategy() == RepriceStrategy.BEAT_LOWEST || r.getStrategy() == RepriceStrategy.BY_STOCK;
        if (needsAdjust) {
            if (r.getAdjustMode() != PriceAdjustMode.AMOUNT && r.getAdjustMode() != PriceAdjustMode.PERCENT) {
                throw new BizException("ecom.error.pub.adjustModeInvalid");
            }
            if (r.getAdjustValue() == null) {
                throw new BizException("ecom.error.pub.adjustValueRequired");
            }
        } else {
            r.setAdjustMode(null);
            r.setAdjustValue(null);
        }
        if (r.getStrategy() == RepriceStrategy.BY_STOCK && r.getStockHighDays() == null && r.getStockLowDays() == null) {
            throw new BizException("ecom.error.pub.stockDaysRequired");
        }
        if (r.getStrategy() == RepriceStrategy.TARGET_MARGIN || r.getFloorMode() == RepriceFloorMode.COST_MARGIN) {
            if (r.getPriceTemplateId() == null) {
                throw new BizException("ecom.error.pub.priceTemplateRequired");
            }
        }
        if (r.getPriceTemplateId() != null) {
            priceTemplateService.get(r.getPriceTemplateId());
        }
        if (r.getFloorMode() == RepriceFloorMode.COST_MARGIN && r.getMinMargin() == null) {
            throw new BizException("ecom.error.pub.minMarginRequired");
        }
        if (r.getFloorMode() == RepriceFloorMode.FIXED) {
            r.setMinMargin(null);
        } else {
            r.setMinPrice(null);
        }
        if (r.getMinPrice() != null && r.getMaxPrice() != null && r.getMinPrice().compareTo(r.getMaxPrice()) > 0) {
            throw new BizException("ecom.error.pub.minGreaterThanMax");
        }
    }

    private static String cleanIds(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (String p : s.split(",")) {
            if (!p.isBlank()) {
                try {
                    ids.add(String.valueOf(Long.parseLong(p.trim())));
                } catch (NumberFormatException e) {
                    throw new BizException("ecom.error.pub.storeIdsInvalid");
                }
            }
        }
        return ids.isEmpty() ? null : String.join(",", new LinkedHashSet<>(ids));
    }

    private static String cleanList(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        List<String> list = Arrays.stream(s.split("[,，\\n]")).map(String::trim).filter(v -> !v.isEmpty()).distinct().toList();
        return list.isEmpty() ? null : String.join(",", list);
    }

    private void checkName(String name, Long excludeId) {
        if (ruleMapper.exists(Wrappers.<EcomRepriceRule>lambdaQuery().eq(EcomRepriceRule::getName, name).ne(excludeId != null, EcomRepriceRule::getId, excludeId))) {
            throw new BizException("ecom.error.pub.nameExists", name);
        }
    }

    // ---------------------------------------------------------------- 执行

    /** 预览：按当前快照计算，不拉取竞品价格、不保存、不推送 */
    public PublishViews.RepriceResult preview(Long id) {
        EcomRepriceRule rule = get(id);
        List<PublishViews.RepriceRow> rows = compute(rule, false, new ArrayList<>());
        return summary(rule, rows, null);
    }

    /** 立即执行 */
    public PublishViews.RepriceResult run(Long id) {
        return execute(get(id));
    }

    /** 定时任务：启用且到了执行间隔的规则，逐条执行，单条失败不影响其他 */
    public List<String> runDue(LocalDateTime now) {
        List<String> out = new ArrayList<>();
        for (EcomRepriceRule r : ruleMapper.selectList(Wrappers.<EcomRepriceRule>lambdaQuery().eq(EcomRepriceRule::getStatus, EnableStatus.ENABLED))) {
            if (r.getLastRunTime() != null && r.getLastRunTime().plusMinutes(r.getIntervalMinutes() == null ? 60 : r.getIntervalMinutes()).isAfter(now)) {
                continue;
            }
            try {
                out.add(r.getName() + ": " + execute(r).message());
            } catch (RuntimeException e) {
                log.warn("Repricer rule {} failed", r.getId(), e);
                out.add(r.getName() + ": " + e.getMessage());
                ruleMapper.update(Wrappers.<EcomRepriceRule>lambdaUpdate().set(EcomRepriceRule::getLastRunTime, now)
                        .set(EcomRepriceRule::getLastRunMessage, PublishSupport.cut(e.getMessage(), 500)).eq(EcomRepriceRule::getId, r.getId()));
            }
        }
        return out;
    }

    private PublishViews.RepriceResult execute(EcomRepriceRule rule) {
        LocalDateTime runTime = LocalDateTime.now();
        List<String> notes = new ArrayList<>();
        List<PublishViews.RepriceRow> computed = compute(rule, true, notes);
        List<PublishViews.RepriceRow> rows = new ArrayList<>();
        Map<Long, List<PublishViews.RepriceRow>> changes = new LinkedHashMap<>();
        for (PublishViews.RepriceRow r : computed) {
            if (r.status() == RepriceLogStatus.SKIPPED) {
                rows.add(r);
            } else if (r.newPrice() != null) {
                if (rule.getSuggestOnly() != null && rule.getSuggestOnly() == 1) {
                    rows.add(with(r, RepriceLogStatus.SUGGESTED, null));
                } else {
                    changes.computeIfAbsent(r.storeId(), k -> new ArrayList<>()).add(r);
                }
            }
        }
        String batchNo = EcomPriceChangeService.newBatchNo();
        changes.forEach((storeId, list) -> rows.addAll(push(storeId, list, rule.getId(), batchNo)));
        Long operator = SecurityUtils.getUserIdOrNull();
        List<EcomRepriceLog> logs = rows.stream().map(r -> toLog(rule.getId(), r, runTime, operator)).toList();
        PublishViews.RepriceResult result = summary(rule, rows, notes.isEmpty() ? null : String.join("; ", notes));
        result = new PublishViews.RepriceResult(result.ruleId(), result.ruleName(), computed.size(), result.changed(), result.suggested(), result.pushed(),
                result.failed(), result.skipped(), result.message(), rows);
        String message = result.message();
        transactionTemplate.executeWithoutResult(s -> {
            if (!logs.isEmpty()) {
                logMapper.insertBatch(logs);
            }
            ruleMapper.update(Wrappers.<EcomRepriceRule>lambdaUpdate().set(EcomRepriceRule::getLastRunTime, runTime)
                    .set(EcomRepriceRule::getLastRunMessage, PublishSupport.cut(message, 500)).eq(EcomRepriceRule::getId, rule.getId()));
        });
        return result;
    }

    /** 推送一个店铺的调价，按改价结果设置日志状态 */
    private List<PublishViews.RepriceRow> push(Long storeId, List<PublishViews.RepriceRow> list, Long ruleId, String batchNo) {
        EcomStore store = storeService.get(storeId);
        Map<Long, EcomListing> listings = listingMapper.selectByIds(list.stream().map(PublishViews.RepriceRow::listingId).toList()).stream()
                .collect(Collectors.toMap(EcomListing::getId, Function.identity()));
        List<EcomPriceChangeService.PriceTarget> targets = list.stream()
                .map(r -> new EcomPriceChangeService.PriceTarget(listings.get(r.listingId()), r.newPrice())).toList();
        Map<Long, PublishViews.ChangeRow> results = priceChangeService.applyPrices(store, targets, PriceChangeSource.REPRICER, ruleId, batchNo, true)
                .stream().collect(Collectors.toMap(PublishViews.ChangeRow::listingId, Function.identity()));
        List<PublishViews.RepriceRow> out = new ArrayList<>();
        for (PublishViews.RepriceRow r : list) {
            PublishViews.ChangeRow c = results.get(r.listingId());
            RepriceLogStatus status = c == null || c.status() == PriceChangeStatus.FAILED ? RepriceLogStatus.FAILED
                    : c.status() == PriceChangeStatus.SUCCESS ? RepriceLogStatus.PUSHED : RepriceLogStatus.LOCAL;
            out.add(with(r, status, c == null ? null : c.message()));
        }
        return out;
    }

    /**
     * 计算规则适用的每个在线商品：价格有变化的 newPrice 不为空；没有依据的状态为 SKIPPED；不变的 newPrice 为空、状态为空
     *
     * @param fetch 先经平台接口拉取竞品价格（执行时）
     */
    List<PublishViews.RepriceRow> compute(EcomRepriceRule rule, boolean fetch, List<String> notes) {
        List<EcomListing> listings = targets(rule);
        if (listings.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, EcomStore> stores = storeService.byIds(listings.stream().map(EcomListing::getStoreId).distinct().toList()).stream()
                .collect(Collectors.toMap(EcomStore::getId, Function.identity()));
        if (fetch && NEEDS_COMPETITOR.contains(rule.getStrategy())) {
            Map<Long, List<EcomListing>> byStore = listings.stream().collect(Collectors.groupingBy(EcomListing::getStoreId, LinkedHashMap::new, Collectors.toList()));
            byStore.forEach((storeId, list) -> {
                EcomStore store = stores.get(storeId);
                if (store != null && !EcomPublishSupport.isDemoStore(store) && registry.supports(store.getPlatform(), PublishCapability.COMPETITIVE_PRICE)) {
                    try {
                        competitorService.fetch(store, list);
                    } catch (RuntimeException e) {
                        notes.add(store.getName() + ": " + e.getMessage());
                    }
                }
            });
        }
        List<Long> ids = listings.stream().map(EcomListing::getId).toList();
        Map<Long, EcomCompetitorPrice> snapshots = NEEDS_COMPETITOR.contains(rule.getStrategy()) ? competitorService.latest(ids) : Map.of();
        EcomPriceTemplate template = priceTemplateService.find(rule.getPriceTemplateId());
        PriceCalculator.Params params = template == null ? null : PriceCalculator.Params.of(template);
        Map<Long, BigDecimal> costs = params == null ? Map.of() : support.listingCosts(ids);
        Map<Long, BigDecimal> sales = new HashMap<>();
        if (rule.getStrategy() == RepriceStrategy.BY_STOCK) {
            pricingMapper.selectSales(ids, LocalDate.now().minusDays(SALES_DAYS).atStartOfDay())
                    .forEach(s -> sales.put(s.getListingId(), s.getQty().divide(BigDecimal.valueOf(SALES_DAYS), 4, RoundingMode.HALF_UP)));
        }
        Map<String, BigDecimal> rates = new HashMap<>();
        List<PublishViews.RepriceRow> rows = new ArrayList<>();
        for (EcomListing l : listings) {
            EcomStore store = stores.get(l.getStoreId());
            String currency = StringUtils.hasText(l.getCurrency()) ? l.getCurrency() : store == null ? null : store.getCurrency();
            EcomCompetitorPrice snap = snapshots.get(l.getId());
            BigDecimal lowest = snap == null || snap.getLowestPrice() == null ? null
                    : snap.getLowestPrice().add(snap.getLowestShipping() == null ? BigDecimal.ZERO : snap.getLowestShipping());
            BigDecimal buybox = snap == null ? null : snap.getBuyboxPrice();
            BigDecimal cost = costs.get(l.getId());
            BigDecimal floor = null;
            BigDecimal target = null;
            String skip = null;
            try {
                BigDecimal rate = params == null || currency == null ? null : rates.computeIfAbsent(currency, support::rate);
                if (rule.getFloorMode() == RepriceFloorMode.COST_MARGIN) {
                    if (cost == null) {
                        skip = "ecom.text.pub.reprice.noCost";
                    } else {
                        floor = PriceCalculator.price(cost, rate, params, rule.getMinMargin());
                    }
                } else {
                    floor = rule.getMinPrice();
                }
                if (rule.getStrategy() == RepriceStrategy.TARGET_MARGIN && cost != null) {
                    target = PriceCalculator.price(cost, rate, params, rule.getTargetMargin() != null ? rule.getTargetMargin() : params.targetMargin());
                }
            } catch (BizException e) {
                rows.add(row(l, store, currency, null, lowest, buybox, floor, RepriceLogStatus.SKIPPED, e.getMessage()));
                continue;
            }
            if (skip != null) {
                rows.add(row(l, store, currency, null, lowest, buybox, null, RepriceLogStatus.SKIPPED, text(skip)));
                continue;
            }
            RepriceEngine.Decision d = RepriceEngine.decide(new RepriceEngine.Input(rule.getStrategy(), rule.getAdjustMode(), rule.getAdjustValue(), l.getPrice(),
                    lowest, buybox, snap != null && snap.getBuyboxWinner() != null && snap.getBuyboxWinner() == 1, target, floor, rule.getMaxPrice(),
                    l.getPlatformQty(), sales.getOrDefault(l.getId(), BigDecimal.ZERO), rule.getStockHighDays(), rule.getStockLowDays()));
            String reason = text(d.reasonKey(), d.args());
            rows.add(row(l, store, currency, d.newPrice(), lowest, buybox, floor, d.skipped() ? RepriceLogStatus.SKIPPED : null, reason));
        }
        return rows;
    }

    /** 规则适用的在售在线商品（店铺启用，SKU 筛选），最多 {@link #MAX_TARGETS} 个 */
    private List<EcomListing> targets(EcomRepriceRule rule) {
        List<Long> storeIds = rule.getStoreIds() == null ? List.of()
                : Arrays.stream(rule.getStoreIds().split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        List<String> skus = rule.getSkus() == null ? List.of() : Arrays.stream(rule.getSkus().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        Set<Long> enabled = storeService.options().stream().map(EcomStore::getId).collect(Collectors.toSet());
        return listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStatus, ListingStatus.ACTIVE)
                        .in(!storeIds.isEmpty(), EcomListing::getStoreId, storeIds).in(!skus.isEmpty(), EcomListing::getSellerSku, skus)
                        .orderByAsc(EcomListing::getId))
                .stream().filter(l -> enabled.contains(l.getStoreId())).limit(MAX_TARGETS).toList();
    }

    private static PublishViews.RepriceRow row(EcomListing l, EcomStore store, String currency, BigDecimal newPrice, BigDecimal lowest, BigDecimal buybox,
                                               BigDecimal floor, RepriceLogStatus status, String reason) {
        return new PublishViews.RepriceRow(l.getId(), l.getStoreId(), store == null ? null : store.getName(), l.getSellerSku(), l.getTitle(), currency,
                l.getPrice(), newPrice, lowest, buybox, floor, status, reason, null);
    }

    private static PublishViews.RepriceRow with(PublishViews.RepriceRow r, RepriceLogStatus status, String message) {
        return new PublishViews.RepriceRow(r.listingId(), r.storeId(), r.storeName(), r.sellerSku(), r.title(), r.currency(), r.oldPrice(), r.newPrice(),
                r.lowestPrice(), r.buyboxPrice(), r.floorPrice(), status, r.reason(), message);
    }

    private static EcomRepriceLog toLog(Long ruleId, PublishViews.RepriceRow r, LocalDateTime runTime, Long operator) {
        EcomRepriceLog l = new EcomRepriceLog();
        l.setRuleId(ruleId);
        l.setListingId(r.listingId());
        l.setStoreId(r.storeId());
        l.setSellerSku(r.sellerSku());
        l.setOldPrice(r.oldPrice());
        l.setNewPrice(r.newPrice());
        l.setLowestPrice(r.lowestPrice());
        l.setBuyboxPrice(r.buyboxPrice());
        l.setFloorPrice(r.floorPrice());
        l.setCurrency(r.currency());
        l.setReason(PublishSupport.cut(r.reason(), 500));
        l.setStatus(r.status());
        l.setMessage(PublishSupport.cut(r.message(), 1000));
        l.setRunTime(runTime);
        l.setCreateBy(operator);
        return l;
    }

    private static PublishViews.RepriceResult summary(EcomRepriceRule rule, List<PublishViews.RepriceRow> rows, String note) {
        int changed = 0, suggested = 0, pushed = 0, failed = 0, skipped = 0;
        for (PublishViews.RepriceRow r : rows) {
            if (r.newPrice() != null) {
                changed++;
            }
            if (r.status() == RepriceLogStatus.SUGGESTED) {
                suggested++;
            } else if (r.status() == RepriceLogStatus.PUSHED || r.status() == RepriceLogStatus.LOCAL) {
                pushed++;
            } else if (r.status() == RepriceLogStatus.FAILED) {
                failed++;
            } else if (r.status() == RepriceLogStatus.SKIPPED) {
                skipped++;
            }
        }
        String message = text("ecom.text.pub.reprice.summary", rows.size(), changed, suggested, pushed, failed, skipped) + (note == null ? "" : "; " + note);
        return new PublishViews.RepriceResult(rule.getId(), rule.getName(), rows.size(), changed, suggested, pushed, failed, skipped, message, rows);
    }

    /** 日志与结果中的文字按默认语言（存库） */
    private static String text(String key, Object... args) {
        return Messages.get(Messages.DEFAULT_LOCALE, key, args);
    }

    // ---------------------------------------------------------------- 日志与建议

    public PageResult<EcomRepriceLog> logs(long current, long size, Long ruleId, Long storeId, RepriceLogStatus status, String keyword,
                                           LocalDate beginDate, LocalDate endDate) {
        Page<EcomRepriceLog> page = Page.of(current, size);
        logMapper.selectPageList(page, ruleId, storeId, status, StringUtils.hasText(keyword) ? keyword.trim() : null,
                beginDate == null ? null : beginDate.atStartOfDay(), endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        return PageResult.of(page);
    }

    /** 应用建议：按建议的新价格推送（建议之后在线商品价格已变化的也照常按建议价推送） */
    public PublishViews.RepriceResult applySuggestions(List<Long> logIds) {
        List<EcomRepriceLog> logs = logMapper.selectByIds(logIds).stream().filter(l -> l.getStatus() == RepriceLogStatus.SUGGESTED).toList();
        Map<Long, List<EcomRepriceLog>> byStore = logs.stream().collect(Collectors.groupingBy(EcomRepriceLog::getStoreId, LinkedHashMap::new, Collectors.toList()));
        List<PublishViews.RepriceRow> rows = new ArrayList<>();
        String batchNo = EcomPriceChangeService.newBatchNo();
        byStore.forEach((storeId, list) -> {
            Map<Long, List<EcomRepriceLog>> byRule = list.stream().collect(Collectors.groupingBy(EcomRepriceLog::getRuleId));
            byRule.forEach((ruleId, ruleLogs) -> {
                List<PublishViews.RepriceRow> input = ruleLogs.stream().map(l -> new PublishViews.RepriceRow(l.getListingId(), l.getStoreId(), null,
                        l.getSellerSku(), null, l.getCurrency(), l.getOldPrice(), l.getNewPrice(), l.getLowestPrice(), l.getBuyboxPrice(), l.getFloorPrice(),
                        null, l.getReason(), null)).toList();
                List<PublishViews.RepriceRow> pushed = push(storeId, input, ruleId, batchNo);
                Map<Long, PublishViews.RepriceRow> byListing = pushed.stream().collect(Collectors.toMap(PublishViews.RepriceRow::listingId, Function.identity(), (a, b) -> a));
                transactionTemplate.executeWithoutResult(s -> {
                    for (EcomRepriceLog l : ruleLogs) {
                        PublishViews.RepriceRow r = byListing.get(l.getListingId());
                        logMapper.update(Wrappers.<EcomRepriceLog>lambdaUpdate().set(EcomRepriceLog::getStatus, r.status())
                                .set(EcomRepriceLog::getMessage, PublishSupport.cut(r.message(), 1000)).eq(EcomRepriceLog::getId, l.getId()));
                    }
                });
                rows.addAll(pushed);
            });
        });
        EcomRepriceRule dummy = new EcomRepriceRule();
        return summary(dummy, rows, null);
    }

    public int ignore(List<Long> logIds) {
        return logMapper.update(Wrappers.<EcomRepriceLog>lambdaUpdate().set(EcomRepriceLog::getStatus, RepriceLogStatus.IGNORED)
                .in(EcomRepriceLog::getId, logIds).eq(EcomRepriceLog::getStatus, RepriceLogStatus.SUGGESTED));
    }
}
