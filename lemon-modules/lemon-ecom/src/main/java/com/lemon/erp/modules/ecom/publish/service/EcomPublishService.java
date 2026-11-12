package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.publish.connector.ListingRef;
import com.lemon.erp.modules.ecom.publish.connector.PlatformAttribute;
import com.lemon.erp.modules.ecom.publish.connector.PlatformCategory;
import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnector;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnectorRegistry;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishVariant;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishLogMapper;
import com.lemon.erp.modules.ecom.publish.model.PublishAction;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 刊登的发布流程：提交发布（校验后变为待发布，后台异步执行）、重试（失败的重新提交）、下架、日志；
 * 以及按店铺查询连接器能力、平台类目与属性（页面选择类目、填写属性时调用）
 */
@Service
@RequiredArgsConstructor
public class EcomPublishService {

    /** 可以提交发布的状态 */
    static final Set<PublishStatus> SUBMITTABLE = EnumSet.of(PublishStatus.DRAFT, PublishStatus.FAILED, PublishStatus.OFFLINE);

    private final EcomPublishItemMapper itemMapper;
    private final EcomPublishLogMapper logMapper;
    private final EcomListingMapper listingMapper;
    private final EcomStoreService storeService;
    private final EcomPublishDraftService draftService;
    private final EcomPublishWorker worker;
    private final EcomPublishExecutor executor;
    private final PublishConnectorRegistry registry;
    private final EcomPublishSupport support;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 能力、类目与属性

    public PublishViews.StoreCapabilities capabilities(Long storeId) {
        EcomStore store = storeService.detail(storeId);
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        boolean hasCredentials = Boolean.TRUE.equals(store.getHasCredentials());
        if (c == null) {
            return new PublishViews.StoreCapabilities(storeId, store.getPlatform().name(), false, hasCredentials, Set.of(), false,
                    Messages.get("ecom.text.pub.note.unsupported"));
        }
        return new PublishViews.StoreCapabilities(storeId, store.getPlatform().name(), true, hasCredentials, c.capabilities(), c.categoryRequired(),
                c.note() == null ? null : Messages.get(c.note()));
    }

    public List<PlatformCategory> categories(Long storeId, String parentId, String keyword) {
        EcomStore store = storeService.requireEnabled(storeId);
        PublishConnector c = require(store, PublishCapability.CATEGORIES);
        return c.categories(support.prepare(store), parentId, keyword);
    }

    public List<PlatformAttribute> attributes(Long storeId, String categoryId) {
        EcomStore store = storeService.requireEnabled(storeId);
        PublishConnector c = require(store, PublishCapability.ATTRIBUTES);
        if (categoryId == null || categoryId.isBlank()) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        return c.attributes(support.prepare(store), categoryId.trim());
    }

    private PublishConnector require(EcomStore store, PublishCapability capability) {
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        if (c == null || !c.supports(capability)) {
            throw new BizException("ecom.error.pub.capabilityUnsupported", store.getPlatform().name(), capability.name());
        }
        return c;
    }

    // ---------------------------------------------------------------- 发布

    /**
     * 提交发布（草稿 / 失败 / 已下架的）：逐条校验，通过的变为待发布并在后台发布，未通过的返回原因
     */
    public PublishViews.ActionResult submit(List<Long> ids) {
        List<PublishViews.ActionError> errors = new ArrayList<>();
        List<Long> ok = new ArrayList<>();
        for (Long id : ids) {
            EcomPublishItem item;
            try {
                item = draftService.getItem(id);
            } catch (BizException e) {
                errors.add(new PublishViews.ActionError(id, null, e.getMessage()));
                continue;
            }
            String error = validate(item);
            if (error != null) {
                errors.add(new PublishViews.ActionError(id, item.getStoreName() + " / " + item.getTitle(), error));
                continue;
            }
            boolean retry = item.getStatus() == PublishStatus.FAILED;
            Boolean done = transactionTemplate.execute(s -> {
                int n = itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.PENDING)
                        .set(EcomPublishItem::getSubmitTime, LocalDateTime.now()).set(EcomPublishItem::getErrorMessage, null)
                        .set(EcomPublishItem::getPublishRef, null)
                        .set(EcomPublishItem::getRetryCount, (item.getRetryCount() == null ? 0 : item.getRetryCount()) + (retry ? 1 : 0))
                        .eq(EcomPublishItem::getId, id).in(EcomPublishItem::getStatus, SUBMITTABLE));
                if (n > 0) {
                    worker.log(item, PublishAction.SUBMIT, true, retry ? Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.retry") : null);
                }
                return n > 0;
            });
            if (Boolean.TRUE.equals(done)) {
                ok.add(id);
            } else {
                errors.add(new PublishViews.ActionError(id, item.getTitle(), Messages.get("ecom.error.pub.notSubmittable", item.getTitle())));
            }
        }
        if (!ok.isEmpty()) {
            executor.runAsync(ok);
        }
        return new PublishViews.ActionResult(ids.size(), ok.size(), errors);
    }

    /** 发布前的检查，通过返回 null，否则返回原因 */
    String validate(EcomPublishItem item) {
        if (!SUBMITTABLE.contains(item.getStatus())) {
            return Messages.get("ecom.error.pub.notSubmittable", item.getTitle());
        }
        EcomStore store = storeService.detail(item.getStoreId());
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        if (c == null || !c.supports(PublishCapability.PUBLISH)) {
            return Messages.get("ecom.error.pub.publishUnsupported", store.getPlatform().name());
        }
        if (!Boolean.TRUE.equals(store.getHasCredentials())) {
            return Messages.get("ecom.error.pub.storeNoCredentials", store.getName());
        }
        List<EcomPublishVariant> variants = item.getVariants();
        if (variants == null || variants.isEmpty()) {
            return Messages.get("ecom.error.pub.variantRequired");
        }
        for (EcomPublishVariant v : variants) {
            if (v.getPrice() == null || v.getPrice().signum() <= 0) {
                return Messages.get("ecom.error.pub.priceRequired", v.getSellerSku());
            }
        }
        if (c.categoryRequired() && (item.getCategoryId() == null || item.getCategoryId().isBlank())) {
            return Messages.get("ecom.error.pub.categoryRequired");
        }
        if (item.getImages().size() < c.minImages()) {
            return Messages.get("ecom.error.pub.imagesRequired", c.minImages());
        }
        try {
            support.absoluteImages(item.getImages());
        } catch (BizException e) {
            return e.getMessage();
        }
        return null;
    }

    /** 下架发布成功的记录：经平台接口下架，在线商品改为下架 */
    public PublishViews.ActionResult offline(List<Long> ids) {
        List<PublishViews.ActionError> errors = new ArrayList<>();
        int ok = 0;
        for (Long id : ids) {
            EcomPublishItem item = null;
            try {
                item = draftService.getItem(id);
                if (item.getStatus() != PublishStatus.SUCCESS) {
                    throw new BizException("ecom.error.pub.notOnline", item.getTitle());
                }
                EcomStore store = storeService.get(item.getStoreId());
                PublishConnector c = require(store, PublishCapability.OFFLINE);
                List<Long> listingIds = item.getVariants().stream().map(EcomPublishVariant::getListingId).filter(Objects::nonNull).toList();
                List<EcomListing> listings = listingIds.isEmpty() ? List.of() : listingMapper.selectByIds(listingIds);
                List<ListingRef> refs = listings.stream().map(support::ref).toList();
                if (!EcomPublishSupport.isDemoStore(store)) {
                    ConnectorContext ctx = support.prepare(store);
                    c.offline(ctx, worker.payload(item, store), refs);
                }
                EcomPublishItem it = item;
                transactionTemplate.executeWithoutResult(s -> {
                    itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.OFFLINE)
                            .set(EcomPublishItem::getOfflineTime, LocalDateTime.now()).eq(EcomPublishItem::getId, it.getId()));
                    if (!listingIds.isEmpty()) {
                        listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getStatus, ListingStatus.INACTIVE)
                                .in(EcomListing::getId, listingIds));
                    }
                    worker.log(it, PublishAction.OFFLINE, true, null);
                });
                ok++;
            } catch (BizException e) {
                if (item != null && item.getStatus() == PublishStatus.SUCCESS) {
                    EcomPublishItem it = item;
                    transactionTemplate.executeWithoutResult(s -> worker.log(it, PublishAction.OFFLINE, false, e.getMessage()));
                }
                errors.add(new PublishViews.ActionError(id, item == null ? null : item.getTitle(), e.getMessage()));
            }
        }
        return new PublishViews.ActionResult(ids.size(), ok, errors);
    }

    /** 刊登设置：是否配置了后端外网地址（页面据此提示图片能否被平台访问） */
    public PublishViews.Settings settings() {
        return new PublishViews.Settings(support.publicUrlConfigured(), support.publicUrl());
    }

    public List<EcomPublishLog> logs(Long itemId) {
        return logMapper.selectByItem(itemId);
    }
}
