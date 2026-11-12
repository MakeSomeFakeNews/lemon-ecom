package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.InventoryUpdate;
import com.lemon.erp.modules.ecom.connector.ListingDraft;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.publish.connector.PriceResult;
import com.lemon.erp.modules.ecom.publish.connector.PriceUpdate;
import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnector;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnectorRegistry;
import com.lemon.erp.modules.ecom.publish.connector.PublishSupport;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceChange;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPriceChangeMapper;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量改价 / 改库存与改价历史：
 * <ul>
 *   <li>计算：价格按固定值、百分比、金额加减或价格模板公式（成本取映射物料的参考成本），四舍五入两位小数；库存按固定值、加减，取非负整数</li>
 *   <li>推送：按店铺分组，价格经刊登连接器 updatePrices（逐条结果），库存经订单连接器 pushInventory（整批结果）；不推送或平台不支持时只改本地</li>
 *   <li>结果：推送成功或只改本地的更新在线商品的价格 / 平台可售数量，每条写改价历史（失败也记录，在线商品不变）</li>
 * </ul>
 * 平台调用不在事务中，每个店铺的结果在一个事务中写入。自动调价也通过 {@link #applyPrices} 推送与记录
 */
@Service
@RequiredArgsConstructor
public class EcomPriceChangeService {

    private static final DateTimeFormatter BATCH = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final EcomPriceChangeMapper changeMapper;
    private final EcomListingMapper listingMapper;
    private final EcomStoreService storeService;
    private final EcomPriceTemplateService priceTemplateService;
    private final PublishConnectorRegistry registry;
    private final EcomPublishSupport support;
    private final TransactionTemplate transactionTemplate;

    /** 一个改价目标 */
    public record PriceTarget(EcomListing listing, BigDecimal newPrice) {
    }

    public PageResult<EcomPriceChange> page(long current, long size, Long storeId, Long listingId, String keyword, PriceChangeType changeType,
                                            PriceChangeSource source, PriceChangeStatus status, String batchNo, LocalDate beginDate, LocalDate endDate) {
        Page<EcomPriceChange> page = Page.of(current, size);
        changeMapper.selectPageList(page, storeId, listingId, StringUtils.hasText(keyword) ? keyword.trim() : null, changeType, source, status,
                StringUtils.hasText(batchNo) ? batchNo.trim() : null, beginDate == null ? null : beginDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 批量修改

    /** 预览：计算新值，不保存、不推送 */
    public PublishViews.ChangeResult preview(PublishRequests.BatchChange req) {
        List<PublishViews.ChangeRow> rows = compute(req);
        int invalid = (int) rows.stream().filter(r -> r.status() == PriceChangeStatus.FAILED).count();
        return new PublishViews.ChangeResult(null, rows.size(), 0, invalid, 0, rows);
    }

    /** 执行：计算、推送、写历史 */
    public PublishViews.ChangeResult apply(PublishRequests.BatchChange req) {
        List<PublishViews.ChangeRow> computed = compute(req);
        String batchNo = newBatchNo();
        boolean push = !Boolean.FALSE.equals(req.push());
        Map<Long, EcomListing> listings = listingMapper.selectByIds(req.listingIds()).stream().collect(Collectors.toMap(EcomListing::getId, Function.identity()));
        Map<Long, List<PublishViews.ChangeRow>> byStore = new LinkedHashMap<>();
        List<PublishViews.ChangeRow> out = new ArrayList<>();
        for (PublishViews.ChangeRow r : computed) {
            if (r.status() == PriceChangeStatus.FAILED) {
                out.add(r);
            } else {
                byStore.computeIfAbsent(r.storeId(), k -> new ArrayList<>()).add(r);
            }
        }
        byStore.forEach((storeId, rows) -> {
            EcomStore store = storeService.get(storeId);
            if (req.changeType() == PriceChangeType.PRICE) {
                List<PriceTarget> targets = rows.stream().map(r -> new PriceTarget(listings.get(r.listingId()), r.newValue())).toList();
                Map<Long, PublishViews.ChangeRow> results = applyPrices(store, targets, PriceChangeSource.BATCH, null, batchNo, push).stream()
                        .collect(Collectors.toMap(PublishViews.ChangeRow::listingId, Function.identity()));
                rows.forEach(r -> out.add(withMargin(results.get(r.listingId()), r.estMargin())));
            } else {
                out.addAll(applyStock(store, rows, listings, batchNo, push, !Boolean.FALSE.equals(req.lockStock())));
            }
        });
        int ok = (int) out.stream().filter(r -> r.status() == PriceChangeStatus.SUCCESS).count();
        int failed = (int) out.stream().filter(r -> r.status() == PriceChangeStatus.FAILED).count();
        int local = (int) out.stream().filter(r -> r.status() == PriceChangeStatus.LOCAL).count();
        return new PublishViews.ChangeResult(batchNo, out.size(), ok, failed, local, out);
    }

    private static PublishViews.ChangeRow withMargin(PublishViews.ChangeRow r, BigDecimal margin) {
        return new PublishViews.ChangeRow(r.listingId(), r.storeId(), r.storeName(), r.sellerSku(), r.title(), r.currency(), r.oldValue(), r.newValue(),
                margin, r.status(), r.message());
    }

    /** 计算每个在线商品的新值；不能计算的行状态为 FAILED（message 为原因） */
    List<PublishViews.ChangeRow> compute(PublishRequests.BatchChange req) {
        if (req.mode() != PriceAdjustMode.FORMULA && req.value() == null) {
            throw new BizException("ecom.error.pub.adjustValueRequired");
        }
        if (req.mode() == PriceAdjustMode.FORMULA && req.changeType() == PriceChangeType.STOCK) {
            throw new BizException("ecom.error.pub.formulaPriceOnly");
        }
        EcomPriceTemplate template = req.priceTemplateId() == null ? null : priceTemplateService.get(req.priceTemplateId());
        if (req.mode() == PriceAdjustMode.FORMULA && template == null) {
            throw new BizException("ecom.error.pub.priceTemplateRequired");
        }
        List<EcomListing> listings = listingMapper.selectByIds(req.listingIds());
        Map<Long, EcomStore> stores = storeService.byIds(listings.stream().map(EcomListing::getStoreId).distinct().toList()).stream()
                .collect(Collectors.toMap(EcomStore::getId, Function.identity()));
        Map<Long, BigDecimal> costs = template == null ? Map.of() : support.listingCosts(listings.stream().map(EcomListing::getId).toList());
        Map<String, BigDecimal> rates = new HashMap<>();
        PriceCalculator.Params params = template == null ? null : PriceCalculator.Params.of(template);
        List<PublishViews.ChangeRow> rows = new ArrayList<>();
        for (EcomListing l : listings) {
            EcomStore store = stores.get(l.getStoreId());
            String currency = StringUtils.hasText(l.getCurrency()) ? l.getCurrency() : store == null ? null : store.getCurrency();
            String storeName = store == null ? null : store.getName();
            BigDecimal old = req.changeType() == PriceChangeType.PRICE ? l.getPrice() : l.getPlatformQty();
            BigDecimal value = null;
            BigDecimal margin = null;
            String error = null;
            try {
                BigDecimal rate = params == null || currency == null ? null : rates.computeIfAbsent(currency, support::rate);
                if (req.changeType() == PriceChangeType.PRICE) {
                    if (req.mode() == PriceAdjustMode.FORMULA) {
                        BigDecimal cost = costs.get(l.getId());
                        if (cost == null) {
                            throw new BizException("ecom.error.pub.noCost", l.getSellerSku());
                        }
                        value = PriceCalculator.price(cost, rate, params);
                    } else {
                        value = adjust(old, req.mode(), req.value()).setScale(2, RoundingMode.HALF_UP);
                    }
                    if (value.signum() <= 0) {
                        throw new BizException("ecom.error.pub.newPriceInvalid");
                    }
                    BigDecimal cost = costs.get(l.getId());
                    if (params != null && cost != null) {
                        margin = PriceCalculator.profit(value, cost, rate, params).margin();
                    }
                } else {
                    value = adjust(old, req.mode(), req.value()).setScale(0, RoundingMode.DOWN).max(BigDecimal.ZERO);
                }
            } catch (BizException e) {
                error = e.getMessage();
            }
            rows.add(new PublishViews.ChangeRow(l.getId(), l.getStoreId(), storeName, l.getSellerSku(), l.getTitle(), currency, old, value, margin,
                    error == null ? null : PriceChangeStatus.FAILED, error));
        }
        return rows;
    }

    /** 固定值 / 百分比 / 金额加减（原值为空时只能用固定值） */
    static BigDecimal adjust(BigDecimal old, PriceAdjustMode mode, BigDecimal value) {
        if (mode == PriceAdjustMode.FIXED) {
            return value;
        }
        if (old == null) {
            throw new BizException("ecom.error.pub.oldValueMissing");
        }
        if (mode == PriceAdjustMode.PERCENT) {
            return old.multiply(BigDecimal.ONE.add(value.divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)), MathContext.DECIMAL64);
        }
        return old.add(value);
    }

    // ---------------------------------------------------------------- 推送与记录

    /**
     * 推送一个店铺的若干改价并记录：push 为 false 或平台不支持改价时只改本地；推送成功 / 只改本地的更新在线商品价格，全部写改价历史
     */
    public List<PublishViews.ChangeRow> applyPrices(EcomStore store, List<PriceTarget> targets, PriceChangeSource source, Long ruleId, String batchNo,
                                                   boolean push) {
        Map<String, PriceResult> results = new HashMap<>();
        PriceChangeStatus fallback = PriceChangeStatus.LOCAL;
        String fallbackMessage = null;
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        if (push && EcomPublishSupport.isDemoStore(store)) {
            fallbackMessage = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.demoLocal");
        } else if (push && c != null && c.supports(PublishCapability.PRICE)) {
            try {
                ConnectorContext ctx = support.prepare(store);
                List<PriceUpdate> updates = targets.stream().map(t -> new PriceUpdate(support.ref(t.listing()), t.newPrice(),
                        StringUtils.hasText(t.listing().getCurrency()) ? t.listing().getCurrency() : store.getCurrency())).toList();
                c.updatePrices(ctx, updates).forEach(r -> results.put(r.sellerSku(), r));
                fallback = PriceChangeStatus.FAILED;
                fallbackMessage = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.noResult");
            } catch (BizException e) {
                fallback = PriceChangeStatus.FAILED;
                fallbackMessage = e.getMessage();
            }
        } else if (push) {
            fallbackMessage = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.priceLocalOnly");
        }
        List<PublishViews.ChangeRow> rows = new ArrayList<>();
        List<EcomPriceChange> history = new ArrayList<>();
        Long operator = SecurityUtils.getUserIdOrNull();
        for (PriceTarget t : targets) {
            EcomListing l = t.listing();
            PriceResult r = results.get(l.getSellerSku());
            PriceChangeStatus status = r == null ? fallback : r.success() ? PriceChangeStatus.SUCCESS : PriceChangeStatus.FAILED;
            String message = r == null ? fallbackMessage : r.message();
            String currency = StringUtils.hasText(l.getCurrency()) ? l.getCurrency() : store.getCurrency();
            rows.add(new PublishViews.ChangeRow(l.getId(), store.getId(), store.getName(), l.getSellerSku(), l.getTitle(), currency, l.getPrice(),
                    t.newPrice(), null, status, message));
            history.add(change(batchNo, l, store, PriceChangeType.PRICE, l.getPrice(), t.newPrice(), currency, source, status, message, ruleId, operator));
        }
        transactionTemplate.executeWithoutResult(s -> {
            for (PublishViews.ChangeRow r : rows) {
                if (r.status() != PriceChangeStatus.FAILED) {
                    listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getPrice, r.newValue()).eq(EcomListing::getId, r.listingId()));
                }
            }
            if (!history.isEmpty()) {
                changeMapper.insertBatch(history);
            }
        });
        return rows;
    }

    /** 改库存：经订单连接器回传可售数量（整批成功或失败），成功 / 只改本地的更新平台可售数量 */
    private List<PublishViews.ChangeRow> applyStock(EcomStore store, List<PublishViews.ChangeRow> rows, Map<Long, EcomListing> listings, String batchNo,
                                                   boolean push, boolean lock) {
        PriceChangeStatus status = PriceChangeStatus.LOCAL;
        String message = null;
        ChannelConnector c = support.channelRegistry().find(store.getPlatform()).orElse(null);
        if (push && EcomPublishSupport.isDemoStore(store)) {
            message = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.demoLocal");
        } else if (push && c != null && c.supports(ConnectorCapability.INVENTORY_PUSH)) {
            try {
                ConnectorContext ctx = support.prepare(store);
                List<InventoryUpdate> updates = new ArrayList<>();
                for (PublishViews.ChangeRow r : rows) {
                    EcomListing l = listings.get(r.listingId());
                    Map<String, String> extra = EcomPublishSupport.readMap(l.getPlatformExtra());
                    updates.add(new InventoryUpdate(l.getSellerSku(), l.getPlatformItemId(), l.getParentItemId(), extra.get(ListingDraft.EXTRA_WAREHOUSE_ID),
                            r.newValue(), extra));
                }
                c.pushInventory(ctx, updates);
                status = PriceChangeStatus.SUCCESS;
            } catch (BizException e) {
                status = PriceChangeStatus.FAILED;
                message = PublishSupport.cut(e.getMessage(), 1000);
            }
        } else if (push) {
            message = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.stockLocalOnly");
        }
        Long operator = SecurityUtils.getUserIdOrNull();
        List<PublishViews.ChangeRow> out = new ArrayList<>();
        List<EcomPriceChange> history = new ArrayList<>();
        for (PublishViews.ChangeRow r : rows) {
            EcomListing l = listings.get(r.listingId());
            out.add(new PublishViews.ChangeRow(r.listingId(), r.storeId(), r.storeName(), r.sellerSku(), r.title(), r.currency(), r.oldValue(), r.newValue(),
                    null, status, message));
            history.add(change(batchNo, l, store, PriceChangeType.STOCK, r.oldValue(), r.newValue(), null, PriceChangeSource.BATCH, status, message, null,
                    operator));
        }
        PriceChangeStatus finalStatus = status;
        transactionTemplate.executeWithoutResult(s -> {
            if (finalStatus != PriceChangeStatus.FAILED) {
                LocalDateTime now = LocalDateTime.now();
                for (PublishViews.ChangeRow r : out) {
                    listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getPlatformQty, r.newValue())
                            .set(lock, EcomListing::getStockLocked, 1)
                            .set(finalStatus == PriceChangeStatus.SUCCESS, EcomListing::getLastPushedQty, r.newValue())
                            .set(finalStatus == PriceChangeStatus.SUCCESS, EcomListing::getLastPushedTime, now)
                            .eq(EcomListing::getId, r.listingId()));
                }
            }
            changeMapper.insertBatch(history);
        });
        return out;
    }

    /** 锁定 / 解除手工库存：锁定的商品自动库存回传跳过，解除后下一次回传按可售数重新计算 */
    public int lockStock(List<Long> listingIds, boolean locked) {
        if (listingIds.isEmpty()) {
            return 0;
        }
        return listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getStockLocked, locked ? 1 : 0)
                .set(!locked, EcomListing::getLastPushedQty, null).in(EcomListing::getId, listingIds));
    }

    private static EcomPriceChange change(String batchNo, EcomListing l, EcomStore store, PriceChangeType type, BigDecimal old, BigDecimal value,
                                          String currency, PriceChangeSource source, PriceChangeStatus status, String message, Long ruleId, Long operator) {
        EcomPriceChange c = new EcomPriceChange();
        c.setBatchNo(batchNo);
        c.setListingId(l.getId());
        c.setStoreId(store.getId());
        c.setSellerSku(l.getSellerSku());
        c.setChangeType(type);
        c.setOldValue(old);
        c.setNewValue(value);
        c.setCurrency(currency);
        c.setSource(source);
        c.setStatus(status);
        c.setMessage(PublishSupport.cut(message, 1000));
        c.setRuleId(ruleId);
        c.setCreateBy(operator);
        return c;
    }

    /** 批次号：年月日时分秒 + 3 位随机数 */
    public static String newBatchNo() {
        return LocalDateTime.now().format(BATCH) + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
