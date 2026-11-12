package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnector;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnectorRegistry;
import com.lemon.erp.modules.ecom.publish.connector.PublishPayload;
import com.lemon.erp.modules.ecom.publish.connector.PublishResult;
import com.lemon.erp.modules.ecom.publish.connector.PublishSku;
import com.lemon.erp.modules.ecom.publish.connector.PublishSupport;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceChange;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishVariant;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPriceChangeMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishLogMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishVariantMapper;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import com.lemon.erp.modules.ecom.publish.model.PublishAction;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行发布：领取一条待发布的刊登记录（待发布 → 发布中），调用平台接口（不在事务中），再在事务中写结果——
 * 成功时回写平台 ID、生成 / 关联在线商品并记录首次售价；平台异步处理时保持发布中并记下处理编号（定时任务查询）；失败时记录平台返回的错误。
 * 由 {@link EcomPublishExecutor} 在后台调用，定时任务 ecom.publishQueue 兜底处理遗漏的待发布记录、查询异步结果、把超时的记为失败
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcomPublishWorker {

    /** 发布中没有处理编号、超过这个时间仍未结束的记为失败 */
    static final int TIMEOUT_MINUTES = 30;

    private final EcomPublishItemMapper itemMapper;
    private final EcomPublishVariantMapper variantMapper;
    private final EcomPublishLogMapper logMapper;
    private final EcomPriceChangeMapper priceChangeMapper;
    private final EcomListingMapper listingMapper;
    private final EcomListingService listingService;
    private final EcomStoreService storeService;
    private final EcomPublishDraftService draftService;
    private final PublishConnectorRegistry registry;
    private final EcomPublishSupport support;
    private final TransactionTemplate transactionTemplate;

    /** 发布一条待发布的记录（已被其他线程领取时跳过） */
    public void process(Long itemId) {
        int claimed = itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.PUBLISHING)
                .eq(EcomPublishItem::getId, itemId).eq(EcomPublishItem::getStatus, PublishStatus.PENDING));
        if (claimed == 0) {
            return;
        }
        EcomPublishItem item = draftService.getItem(itemId);
        EcomStore store = storeService.get(item.getStoreId());
        PublishResult r;
        try {
            if (EcomPublishSupport.isDemoStore(store)) {
                throw new BizException("ecom.error.pub.demoStore", store.getName());
            }
            PublishConnector c = connector(store);
            ConnectorContext ctx = support.prepare(store);
            r = c.publish(ctx, payload(item, store));
        } catch (RuntimeException e) {
            if (!(e instanceof BizException)) {
                log.warn("Publish item {} failed", itemId, e);
            }
            r = PublishResult.failed(message(e));
        }
        apply(item, store, r, PublishAction.PUBLISH);
    }

    /** 查询一条发布中（平台异步处理）的记录 */
    public void check(EcomPublishItem row) {
        EcomPublishItem item = draftService.getItem(row.getId());
        EcomStore store = storeService.get(item.getStoreId());
        if (EcomPublishSupport.isDemoStore(store)) {
            return;
        }
        PublishResult r;
        try {
            PublishConnector c = connector(store);
            r = c.checkPublish(support.prepare(store), payload(item, store), item.getPublishRef());
        } catch (RuntimeException e) {
            log.debug("Check publish {} failed: {}", row.getId(), e.getMessage());
            return;
        }
        if (r.status() != PublishResult.Status.PENDING) {
            apply(item, store, r, PublishAction.CHECK);
        }
    }

    /** 定时任务：遗漏的待发布（提交超过 2 分钟）、平台异步处理中的、超时的 */
    public List<String> runQueue(LocalDateTime now) {
        List<String> out = new ArrayList<>();
        for (EcomPublishItem i : itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getStatus, PublishStatus.PENDING)
                .lt(EcomPublishItem::getSubmitTime, now.minusMinutes(2)).orderByAsc(EcomPublishItem::getId))) {
            safely(() -> process(i.getId()), i, out);
        }
        for (EcomPublishItem i : itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getStatus, PublishStatus.PUBLISHING)
                .isNotNull(EcomPublishItem::getPublishRef))) {
            safely(() -> check(i), i, out);
        }
        for (EcomPublishItem i : itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getStatus, PublishStatus.PUBLISHING)
                .isNull(EcomPublishItem::getPublishRef).lt(EcomPublishItem::getUpdateTime, now.minusMinutes(TIMEOUT_MINUTES)))) {
            safely(() -> {
                EcomStore store = storeService.get(i.getStoreId());
                apply(draftService.getItem(i.getId()), store, PublishResult.failed(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.timeout")),
                        PublishAction.CHECK);
            }, i, out);
        }
        return out;
    }

    private static void safely(Runnable r, EcomPublishItem i, List<String> out) {
        try {
            r.run();
            out.add(i.getId() + ":ok");
        } catch (RuntimeException e) {
            out.add(i.getId() + ": " + e.getMessage());
        }
    }

    PublishConnector connector(EcomStore store) {
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        if (c == null || !c.supports(PublishCapability.PUBLISH)) {
            throw new BizException("ecom.error.pub.publishUnsupported", store.getPlatform().name());
        }
        return c;
    }

    /** 刊登记录 → 发布内容（图片换成公网地址） */
    PublishPayload payload(EcomPublishItem item, EcomStore store) {
        List<EcomPublishVariant> variants = item.getVariants() == null ? List.of() : item.getVariants();
        List<PublishSku> skus = variants.stream().map(v -> new PublishSku(v.getSellerSku(), v.getVariantAttrs(), v.getVariantText(), v.getBarcode(),
                v.getPrice(), v.getQty() == null ? 0 : v.getQty(), v.getPlatformSkuId())).toList();
        String parentSku = item.getParentSku();
        if (parentSku == null && skus.size() > 1) {
            parentSku = PublishSupportNames.parentSku(skus.get(0).sellerSku());
        }
        return new PublishPayload(item.getTitle(), item.getBullets(), item.getDescription(), support.absoluteImages(item.getImages()), item.getBrand(),
                item.getBarcodeType() == null ? null : item.getBarcodeType().name(), item.getUpcExempt() != null && item.getUpcExempt() == 1,
                item.getExemptionNote(), item.getCategoryId(), item.getAttributes(), item.getCurrency(),
                item.getFulfillment() == null ? null : item.getFulfillment().name(), item.getShippingTemplate(), item.getReturnPolicy(),
                item.getHandlingDays(), item.getExtra(), parentSku, item.getPlatformItemId(), store.getMarketplace(), skus);
    }

    /** 写入一条刊登记录的发布结果（演示数据与测试也通过它模拟平台返回） */
    public void record(Long itemId, PublishResult r, PublishAction action) {
        EcomPublishItem item = draftService.getItem(itemId);
        apply(item, storeService.get(item.getStoreId()), r, action);
    }

    /** 写发布结果（一个事务） */
    void apply(EcomPublishItem item, EcomStore store, PublishResult r, PublishAction action) {
        transactionTemplate.executeWithoutResult(s -> {
            LocalDateTime now = LocalDateTime.now();
            switch (r.status()) {
                case SUCCESS -> {
                    itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.SUCCESS)
                            .set(EcomPublishItem::getPlatformItemId, r.platformItemId() != null ? r.platformItemId() : item.getPlatformItemId())
                            .set(EcomPublishItem::getPlatformUrl, r.url() != null ? r.url() : item.getPlatformUrl())
                            .set(EcomPublishItem::getPublishRef, null).set(EcomPublishItem::getErrorMessage, null)
                            .set(EcomPublishItem::getPublishTime, now).eq(EcomPublishItem::getId, item.getId()));
                    linkListings(item, store, r);
                    log(item, action, true, r.message());
                }
                case PENDING -> {
                    itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.PUBLISHING)
                            .set(EcomPublishItem::getPublishRef, r.ref()).set(EcomPublishItem::getErrorMessage, null)
                            .eq(EcomPublishItem::getId, item.getId()));
                    log(item, action, true, Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.pending", String.valueOf(r.message())));
                }
                default -> {
                    String msg = PublishSupport.cut(r.message() == null ? "-" : r.message(), 2000);
                    itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.FAILED)
                            .set(EcomPublishItem::getPublishRef, null).set(EcomPublishItem::getErrorMessage, msg)
                            .eq(EcomPublishItem::getId, item.getId()));
                    log(item, action, false, msg);
                }
            }
        });
    }

    /** 发布成功：每个变体生成或更新在线商品（映射物料、多件装 1），回写平台 SKU ID，记录首次售价 */
    private void linkListings(EcomPublishItem item, EcomStore store, PublishResult r) {
        List<EcomPublishVariant> variants = item.getVariants();
        boolean multi = variants.size() > 1;
        String image = item.getImages().isEmpty() ? null : PublishSupport.cut(item.getImages().get(0), 255);
        List<EcomPriceChange> history = new ArrayList<>();
        for (EcomPublishVariant v : variants) {
            String skuId = r.skuIds().get(v.getSellerSku());
            String platformItemId = multi ? skuId : skuId != null ? skuId : r.platformItemId();
            String parent = multi ? r.platformItemId() : null;
            EcomListing l = listingMapper.selectOne(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                    .eq(EcomListing::getSellerSku, v.getSellerSku()));
            Map<String, String> extra = new LinkedHashMap<>(l == null ? Map.of() : EcomPublishSupport.readMap(l.getPlatformExtra()));
            extra.putAll(r.skuExtra().getOrDefault(v.getSellerSku(), Map.of()));
            String title = PublishSupport.cut(item.getTitle(), 255);
            BigDecimal oldPrice = l == null ? null : l.getPrice();
            if (l == null) {
                l = new EcomListing();
                l.setStoreId(store.getId());
                l.setSellerSku(v.getSellerSku());
                l.setPlatformItemId(platformItemId);
                l.setParentItemId(parent);
                l.setTitle(title);
                l.setVariant(v.getVariantText());
                l.setPrice(v.getPrice());
                l.setCurrency(item.getCurrency());
                l.setFulfillment(item.getFulfillment());
                l.setPlatformQty(BigDecimal.valueOf(v.getQty() == null ? 0 : v.getQty()));
                l.setStatus(ListingStatus.ACTIVE);
                l.setMaterialId(v.getMaterialId());
                l.setPackQty(BigDecimal.ONE);
                l.setComposeType(ListingComposeType.SINGLE);
                l.setImageUrl(image);
                l.setUrl(PublishSupport.cut(item.getPlatformUrl() != null ? item.getPlatformUrl() : r.url(), 255));
                l.setPlatformExtra(extra.isEmpty() ? null : EcomPublishSupport.writeMap(extra));
                listingService.create(l);
            } else {
                listingMapper.update(Wrappers.<EcomListing>lambdaUpdate()
                        .set(platformItemId != null, EcomListing::getPlatformItemId, platformItemId)
                        .set(parent != null, EcomListing::getParentItemId, parent)
                        .set(EcomListing::getTitle, title).set(EcomListing::getPrice, v.getPrice()).set(EcomListing::getStatus, ListingStatus.ACTIVE)
                        .set(l.getMaterialId() == null && l.getComposeType() != ListingComposeType.BUNDLE, EcomListing::getMaterialId, v.getMaterialId())
                        .set(image != null && l.getImageUrl() == null, EcomListing::getImageUrl, image)
                        .set(r.url() != null, EcomListing::getUrl, PublishSupport.cut(r.url(), 255))
                        .set(!extra.isEmpty(), EcomListing::getPlatformExtra, EcomPublishSupport.writeMap(extra))
                        .eq(EcomListing::getId, l.getId()));
            }
            variantMapper.update(Wrappers.<EcomPublishVariant>lambdaUpdate().set(EcomPublishVariant::getListingId, l.getId())
                    .set(skuId != null, EcomPublishVariant::getPlatformSkuId, skuId).eq(EcomPublishVariant::getId, v.getId()));
            if (v.getPrice() != null) {
                EcomPriceChange c = new EcomPriceChange();
                c.setListingId(l.getId());
                c.setStoreId(store.getId());
                c.setSellerSku(v.getSellerSku());
                c.setChangeType(PriceChangeType.PRICE);
                c.setOldValue(oldPrice);
                c.setNewValue(v.getPrice());
                c.setCurrency(item.getCurrency());
                c.setSource(PriceChangeSource.PUBLISH);
                c.setStatus(PriceChangeStatus.SUCCESS);
                c.setCreateBy(item.getCreateBy());
                history.add(c);
            }
        }
        if (!history.isEmpty()) {
            priceChangeMapper.insertBatch(history);
        }
    }

    void log(EcomPublishItem item, PublishAction action, boolean success, String message) {
        EcomPublishLog l = new EcomPublishLog();
        l.setItemId(item.getId());
        l.setStoreId(item.getStoreId());
        l.setAction(action);
        l.setSuccess(success ? 1 : 0);
        l.setMessage(PublishSupport.cut(message, 2000));
        logMapper.insert(l);
    }

    static String message(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
