package com.lemon.erp.modules.ecom.oper.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.connector.PromotionConnector;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.dto.PromotionQuery;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotionItem;
import com.lemon.erp.modules.ecom.oper.mapper.EcomPromotionItemMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomPromotionMapper;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoPhase;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoPushStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoStatus;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 促销活动：草稿 → 生效（计入日历与效果分析）→ 取消；店铺平台支持时可推送到平台（虾皮折扣活动、TikTok 限时价），推送过的活动取消时同步取消平台活动。
 * 效果分析比较活动前 / 中 / 后同等天数（活动天数）参与 SKU 的订单数、销量、销售额、利润（本位币），活动期利润扣除活动费用；
 * 活动没有登记 SKU（如店铺满减、全店优惠券）时按店铺全部订单统计
 */
@Service
public class EcomPromotionService {

    public static final String TYPE_DICT = "ecom_promo_type";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_ITEMS = 500;

    private final EcomPromotionMapper promotionMapper;
    private final EcomPromotionItemMapper itemMapper;
    private final EcomStoreService storeService;
    private final EcomListingService listingService;
    private final EcomSyncService syncService;
    private final ConnectorRegistry channelRegistry;
    private final ExchangeRateProvider exchangeRateProvider;
    private final DictLookup dictLookup;
    private final UserLookup userLookup;
    private final Map<EcomPlatform, PromotionConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public EcomPromotionService(EcomPromotionMapper promotionMapper, EcomPromotionItemMapper itemMapper, EcomStoreService storeService,
                                EcomListingService listingService, EcomSyncService syncService, ConnectorRegistry channelRegistry,
                                ExchangeRateProvider exchangeRateProvider, DictLookup dictLookup, UserLookup userLookup,
                                List<PromotionConnector> promotionConnectors) {
        this.promotionMapper = promotionMapper;
        this.itemMapper = itemMapper;
        this.storeService = storeService;
        this.listingService = listingService;
        this.syncService = syncService;
        this.channelRegistry = channelRegistry;
        this.exchangeRateProvider = exchangeRateProvider;
        this.dictLookup = dictLookup;
        this.userLookup = userLookup;
        promotionConnectors.forEach(c -> connectors.put(c.platform(), c));
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomPromotion> page(long current, long size, PromotionQuery q) {
        q.setNow(LocalDateTime.now());
        q.setEndNext(q.getEndDate() == null ? null : q.getEndDate().plusDays(1).atStartOfDay());
        Page<EcomPromotion> page = Page.of(current, size);
        promotionMapper.selectPageList(page, q);
        page.getRecords().forEach(this::decorate);
        return PageResult.of(page);
    }

    public EcomPromotion detail(Long id) {
        EcomPromotion p = require(id);
        p.setItems(items(id));
        return p;
    }

    public EcomPromotion require(Long id) {
        EcomPromotion p = EcomStoreScope.require(() -> promotionMapper.selectDetail(id), "ecom.error.oper.promoNotFound");
        decorate(p);
        return p;
    }

    private List<EcomPromotionItem> items(Long id) {
        return itemMapper.selectList(Wrappers.<EcomPromotionItem>lambdaQuery().eq(EcomPromotionItem::getPromotionId, id)
                .orderByAsc(EcomPromotionItem::getSort).orderByAsc(EcomPromotionItem::getId));
    }

    private void decorate(EcomPromotion p) {
        p.setPhase(phase(p, LocalDateTime.now()));
        p.setCanPush(p.getPlatform() != null && connectors.containsKey(p.getPlatform()));
    }

    /** 已生效活动的阶段，草稿 / 已取消为 null */
    static EcomPromoPhase phase(EcomPromotion p, LocalDateTime now) {
        if (p.getStatus() != EcomPromoStatus.ACTIVE) {
            return null;
        }
        if (now.isBefore(p.getStartTime())) {
            return EcomPromoPhase.NOT_STARTED;
        }
        return now.isAfter(p.getEndTime()) ? EcomPromoPhase.ENDED : EcomPromoPhase.ONGOING;
    }

    /** 促销日历：与 [begin, end] 有交集、未取消的活动 */
    public List<OperViews.CalendarItem> calendar(LocalDate begin, LocalDate end, Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionMapper.selectCalendar(begin.atStartOfDay(), end.plusDays(1).atStartOfDay(), storeId).stream().map(p -> {
            EcomPromoPhase ph = phase(p, now);
            return new OperViews.CalendarItem(p.getId(), p.getName(), p.getStoreId(), p.getStoreName(), p.getPlatform(), p.getPromoType(),
                    p.getStartTime(), p.getEndTime(), p.getStatus().name(), ph == null ? null : ph.name(), p.getItemCount());
        }).toList();
    }

    /** 能推送活动的平台 */
    public List<EcomPlatform> pushPlatforms() {
        return List.copyOf(connectors.keySet());
    }

    // ---------------------------------------------------------------- 维护

    @Transactional
    public Long create(EcomPromotion p) {
        p.setId(null);
        EcomStore store = validate(p);
        p.setStatus(EcomPromoStatus.DRAFT);
        p.setPushStatus(EcomPromoPushStatus.NONE);
        p.setPlatformPromoId(null);
        p.setCreateBy(SecurityUtils.getUserIdOrNull());
        if (p.getOwnerId() == null) {
            p.setOwnerId(store.getOwnerId() != null ? store.getOwnerId() : SecurityUtils.getUserIdOrNull());
        }
        promotionMapper.insert(p);
        saveItems(p.getId(), p.getItems());
        return p.getId();
    }

    /** 修改：已取消、已推送平台的活动不能修改（已推送的先取消再新建） */
    @Transactional
    public void update(Long id, EcomPromotion p) {
        EcomPromotion old = require(id);
        if (old.getStatus() == EcomPromoStatus.CANCELLED || old.getPushStatus() == EcomPromoPushStatus.PUSHED) {
            throw new BizException("ecom.error.oper.promoNotEditable", old.getName());
        }
        validate(p);
        p.setId(id);
        p.setStatus(null);
        p.setPushStatus(null);
        p.setPlatformPromoId(null);
        p.setCreateBy(null);
        promotionMapper.updateById(p);
        promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getDiscountPercent, p.getDiscountPercent())
                .set(EcomPromotion::getThresholdAmount, p.getThresholdAmount()).set(EcomPromotion::getReduceAmount, p.getReduceAmount())
                .set(EcomPromotion::getBudget, p.getBudget()).set(EcomPromotion::getRemark, p.getRemark()).eq(EcomPromotion::getId, id));
        itemMapper.delete(Wrappers.<EcomPromotionItem>lambdaQuery().eq(EcomPromotionItem::getPromotionId, id));
        saveItems(id, p.getItems());
    }

    /** 校验并补全：店铺币种、活动费用本位币、明细的在线商品信息与促销价 / 折扣 */
    private EcomStore validate(EcomPromotion p) {
        EcomStore store = storeService.get(p.getStoreId());
        dictLookup.check(TYPE_DICT, p.getPromoType(), "ecom.label.oper.promoType");
        if (!p.getEndTime().isAfter(p.getStartTime())) {
            throw new BizException("ecom.error.oper.promoTimeInvalid");
        }
        if (p.getOwnerId() != null) {
            userLookup.require(p.getOwnerId(), "ecom.error.oper.userNotFound");
        }
        p.setName(p.getName().trim());
        p.setCurrency(store.getCurrency());
        if (p.getFeeAmount() == null) {
            p.setFeeAmount(BigDecimal.ZERO);
        }
        p.setFeeBase(toBase(p.getFeeAmount(), store.getCurrency(), p.getStartTime().toLocalDate()));
        p.setRemark(StringUtils.hasText(p.getRemark()) ? p.getRemark().trim() : null);
        List<EcomPromotionItem> items = p.getItems() == null ? List.of() : p.getItems();
        if (items.size() > MAX_ITEMS) {
            throw new BizException("ecom.error.oper.promoTooManyItems", MAX_ITEMS);
        }
        Set<String> skus = new LinkedHashSet<>();
        for (EcomPromotionItem it : items) {
            it.setSellerSku(it.getSellerSku().trim());
            if (!skus.add(it.getSellerSku())) {
                throw new BizException("ecom.error.oper.promoSkuDuplicate", it.getSellerSku());
            }
        }
        Map<String, EcomListing> listings = listingService.byStoreSkus(store.getId(), skus);
        for (EcomPromotionItem it : items) {
            EcomListing l = listings.get(it.getSellerSku());
            if (l != null) {
                it.setListingId(l.getId());
                it.setPlatformItemId(l.getPlatformItemId());
                it.setParentItemId(l.getParentItemId());
                if (!StringUtils.hasText(it.getTitle())) it.setTitle(l.getTitle());
                if (it.getOriginalPrice() == null && l.getPrice() != null) it.setOriginalPrice(l.getPrice().setScale(2, RoundingMode.HALF_UP));
            } else {
                it.setListingId(null);
            }
            fillPrice(it, p.getDiscountPercent());
        }
        return store;
    }

    /** 促销价与折扣互算：只有折扣（或活动默认折扣）时按原价算促销价，只有促销价时按原价算折扣 */
    static void fillPrice(EcomPromotionItem it, BigDecimal defaultDiscount) {
        BigDecimal orig = it.getOriginalPrice();
        if (it.getPromoPrice() == null && it.getDiscountPercent() == null && defaultDiscount != null) {
            it.setDiscountPercent(defaultDiscount);
        }
        if (orig == null || orig.signum() <= 0) {
            return;
        }
        if (it.getPromoPrice() == null && it.getDiscountPercent() != null) {
            it.setPromoPrice(orig.multiply(HUNDRED.subtract(it.getDiscountPercent())).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        } else if (it.getPromoPrice() != null) {
            it.setDiscountPercent(orig.subtract(it.getPromoPrice()).multiply(HUNDRED).divide(orig, 2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal toBase(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = exchangeRateProvider.rate(currency, date);
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", currency, date.getYear() + "-" + String.format("%02d", date.getMonthValue()));
        }
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private void saveItems(Long promotionId, List<EcomPromotionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int sort = 0;
        for (EcomPromotionItem it : items) {
            it.setId(null);
            it.setPromotionId(promotionId);
            it.setSort(++sort);
        }
        itemMapper.insertBatch(items);
    }

    /** 生效：草稿 → 已生效 */
    public void confirm(Long id) {
        EcomPromotion p = require(id);
        if (p.getStatus() != EcomPromoStatus.DRAFT) {
            throw new BizException("ecom.error.oper.promoNotDraft", p.getName());
        }
        promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getStatus, EcomPromoStatus.ACTIVE).eq(EcomPromotion::getId, id));
    }

    /** 取消：推送过的同时取消平台活动（平台取消失败时不取消，提示原因） */
    public void cancel(Long id) {
        EcomPromotion p = require(id);
        if (p.getStatus() == EcomPromoStatus.CANCELLED) {
            throw new BizException("ecom.error.oper.promoCancelled", p.getName());
        }
        if (p.getPushStatus() == EcomPromoPushStatus.PUSHED && StringUtils.hasText(p.getPlatformPromoId())) {
            PromotionConnector connector = connectors.get(p.getPlatform());
            if (connector != null) {
                connector.cancel(context(p), p);
            }
        }
        promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getStatus, EcomPromoStatus.CANCELLED).eq(EcomPromotion::getId, id));
    }

    /** 删除：草稿或已取消的活动 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomPromotion p = require(id);
            if (p.getStatus() == EcomPromoStatus.ACTIVE) {
                throw new BizException("ecom.error.oper.promoActiveDelete", p.getName());
            }
            itemMapper.delete(Wrappers.<EcomPromotionItem>lambdaQuery().eq(EcomPromotionItem::getPromotionId, id));
            promotionMapper.deleteById(id);
        }
    }

    /**
     * 推送到平台：已生效、店铺平台支持、店铺有渠道授权。成功记为已推送并保存平台活动 ID；失败记为推送失败并保存原因，然后报错
     */
    public EcomPromotion push(Long id) {
        EcomPromotion p = require(id);
        if (p.getStatus() != EcomPromoStatus.ACTIVE) {
            throw new BizException("ecom.error.oper.promoNotActive", p.getName());
        }
        PromotionConnector connector = connectors.get(p.getPlatform());
        if (connector == null) {
            throw new BizException("ecom.error.oper.pushNotSupported", p.getPlatform().name());
        }
        try {
            String platformId = connector.push(context(p), p, items(id));
            promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getPushStatus, EcomPromoPushStatus.PUSHED)
                    .set(EcomPromotion::getPlatformPromoId, platformId).set(EcomPromotion::getPushMessage, null)
                    .set(EcomPromotion::getPushTime, LocalDateTime.now()).eq(EcomPromotion::getId, id));
        } catch (BizException e) {
            promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getPushStatus, EcomPromoPushStatus.FAILED)
                    .set(EcomPromotion::getPushMessage, MonitorAlertRules.cut(e.getMessage())).set(EcomPromotion::getPushTime, LocalDateTime.now())
                    .eq(EcomPromotion::getId, id));
            throw e;
        }
        return detail(id);
    }

    private ConnectorContext context(EcomPromotion p) {
        EcomStore store = storeService.get(p.getStoreId());
        ConnectorContext ctx = syncService.requireContext(store);
        channelRegistry.find(store.getPlatform()).ifPresent(c -> c.ensureToken(ctx));
        return ctx;
    }

    // ---------------------------------------------------------------- 效果分析

    /** 效果分析：活动前 / 中 / 后同等天数（活动天数，向上取整，至少 1 天），没有结束的时段统计到当前 */
    public OperViews.PromotionEffect effect(Long id) {
        EcomPromotion p = require(id);
        List<EcomPromotionItem> items = items(id);
        List<String> skus = items.stream().map(EcomPromotionItem::getSellerSku).toList();
        LocalDateTime now = LocalDateTime.now();
        long hours = Math.max(1, Duration.between(p.getStartTime(), p.getEndTime()).toHours());
        int days = (int) Math.max(1, (hours + 23) / 24);
        OperViews.Window before = window("before", p.getStartTime().minusDays(days), p.getStartTime(), now);
        OperViews.Window during = window("during", p.getStartTime(), p.getEndTime(), now);
        OperViews.Window after = window("after", p.getEndTime(), p.getEndTime().plusDays(days), now);
        TreeMap<LocalDate, OperViews.DailyRow> daily = new TreeMap<>();
        Map<String, OperViews.SkuEffect> bySku = new LinkedHashMap<>();
        items.forEach(it -> sku(bySku, it.getSellerSku(), it.getTitle()));
        for (OperViews.Window w : List.of(before, during, after)) {
            if (!w.getEnd().isAfter(w.getBegin())) {
                continue;
            }
            for (Map<String, Object> r : promotionMapper.selectSkuDaily(p.getStoreId(), skus, w.getBegin(), w.getEnd())) {
                LocalDate day = toDate(r.get("day"));
                long orders = ((Number) r.get("orders")).longValue();
                BigDecimal qty = dec(r.get("qty"));
                BigDecimal sales = dec(r.get("sales"));
                BigDecimal profit = dec(r.get("profit"));
                w.setOrders(w.getOrders() + orders);
                w.setQty(w.getQty().add(qty));
                w.setSales(w.getSales().add(sales));
                w.setGrossProfit(w.getGrossProfit().add(profit));
                daily.merge(day, new OperViews.DailyRow(day, orders, qty, sales, profit), (a, b) -> new OperViews.DailyRow(day,
                        a.orders() + b.orders(), a.qty().add(b.qty()), a.sales().add(b.sales()), a.profit().add(b.profit())));
            }
            for (Map<String, Object> r : promotionMapper.selectSkuTotals(p.getStoreId(), skus, w.getBegin(), w.getEnd())) {
                OperViews.SkuEffect e = sku(bySku, String.valueOf(r.get("sellerSku")), (String) r.get("title"));
                BigDecimal qty = dec(r.get("qty"));
                BigDecimal sales = dec(r.get("sales"));
                if (w == before) {
                    e.setBeforeQty(qty);
                    e.setBeforeSales(sales);
                } else if (w == during) {
                    e.setDuringQty(qty);
                    e.setDuringSales(sales);
                } else {
                    e.setAfterQty(qty);
                    e.setAfterSales(sales);
                }
            }
        }
        during.setFee(p.getFeeBase() == null ? BigDecimal.ZERO : p.getFeeBase());
        for (OperViews.Window w : List.of(before, during, after)) {
            finish(w);
        }
        bySku.values().forEach(e -> e.setQtyLift(lift(e.getDuringQty(), e.getBeforeQty())));
        BigDecimal roi = null;
        boolean started = elapsedDays(during).signum() > 0;
        if (started && during.getFee().signum() > 0) {
            BigDecimal elapsed = elapsedDays(during);
            BigDecimal incremental = during.getGrossProfit().subtract(before.getGrossProfit().divide(BigDecimal.valueOf(days), 6, RoundingMode.HALF_UP)
                    .multiply(elapsed));
            roi = incremental.subtract(during.getFee()).multiply(HUNDRED).divide(during.getFee(), 2, RoundingMode.HALF_UP);
        }
        p.setItems(items);
        return new OperViews.PromotionEffect(p, days, before, during, after,
                started ? lift(perDay(during.getQty(), during), perDay(before.getQty(), before)) : null,
                started ? lift(perDay(during.getSales(), during), perDay(before.getSales(), before)) : null,
                started ? lift(perDay(during.getProfit(), during), perDay(before.getProfit(), before)) : null, roi,
                new ArrayList<>(daily.values()), new ArrayList<>(bySku.values()));
    }

    private static OperViews.SkuEffect sku(Map<String, OperViews.SkuEffect> map, String sku, String title) {
        OperViews.SkuEffect e = map.computeIfAbsent(sku, k -> {
            OperViews.SkuEffect x = new OperViews.SkuEffect();
            x.setSellerSku(k);
            return x;
        });
        if (e.getTitle() == null && title != null) {
            e.setTitle(title);
        }
        return e;
    }

    private static OperViews.Window window(String key, LocalDateTime begin, LocalDateTime end, LocalDateTime now) {
        OperViews.Window w = new OperViews.Window();
        w.setKey(key);
        w.setBegin(begin);
        if (end.isAfter(now)) {
            w.setPartial(true);
            w.setEnd(now.isBefore(begin) ? begin : now);
        } else {
            w.setEnd(end);
        }
        return w;
    }

    /** 利润扣费用、日均、利润率 */
    private static void finish(OperViews.Window w) {
        w.setProfit(w.getGrossProfit().subtract(w.getFee()));
        BigDecimal d = elapsedDays(w);
        if (d.signum() > 0) {
            w.setDailyQty(w.getQty().divide(d, 2, RoundingMode.HALF_UP));
            w.setDailySales(w.getSales().divide(d, 2, RoundingMode.HALF_UP));
        }
        w.setProfitRate(w.getSales().signum() == 0 ? null : w.getProfit().multiply(HUNDRED).divide(w.getSales(), 2, RoundingMode.HALF_UP));
    }

    /** 时段已经过的天数（按小时折算） */
    static BigDecimal elapsedDays(OperViews.Window w) {
        long hours = Math.max(0, Duration.between(w.getBegin(), w.getEnd()).toHours());
        return BigDecimal.valueOf(hours).divide(BigDecimal.valueOf(24), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal perDay(BigDecimal v, OperViews.Window w) {
        BigDecimal d = elapsedDays(w);
        return d.signum() == 0 ? BigDecimal.ZERO : v.divide(d, 6, RoundingMode.HALF_UP);
    }

    /** 增长 %：基数为 0 时为空 */
    static BigDecimal lift(BigDecimal cur, BigDecimal base) {
        if (base == null || base.signum() == 0 || cur == null) {
            return null;
        }
        return cur.subtract(base).multiply(HUNDRED).divide(base.abs(), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }
}
