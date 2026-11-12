package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.InventoryUpdate;
import com.lemon.erp.modules.ecom.connector.ListingDraft;
import com.lemon.erp.modules.ecom.dto.SyncResult;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomChannelMapper;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.model.WarehouseType;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 渠道同步中心的商品侧：拉取在线商品（能力 LISTINGS）与回传可售库存（能力 INVENTORY_PUSH）。
 * 平台接口在事务之外调用；写入在线商品每个店铺一个事务，回传水位每批一个更新，单个店铺失败不影响其他店铺
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomListingSyncService {

    /** 每次调用平台回传库存接口的 SKU 数 */
    static final int PUSH_BATCH = 50;
    /** 一次查询在线商品的 SKU 数 */
    private static final int QUERY_BATCH = 500;

    private final EcomStoreService storeService;
    private final EcomSyncService syncService;
    private final ConnectorRegistry registry;
    private final EcomListingMapper listingMapper;
    private final EcomChannelMapper channelMapper;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final StockReservationService reservationService;
    private final TransactionTemplate transactionTemplate;
    private final EcomBundleService bundleService;

    // ---------------------------------------------------------------- 拉取在线商品

    /** 手工同步一个店铺的在线商品 */
    public SyncResult syncListings(Long storeId) {
        return syncListings(storeService.requireEnabled(storeId), EcomSyncService.MANUAL);
    }

    /** 同步全部有授权、平台支持拉取商品的启用店铺（定时任务）；平台不支持的店铺跳过 */
    public List<SyncResult> syncListingsAll() {
        List<SyncResult> results = new ArrayList<>();
        for (EcomStore store : storeService.syncable()) {
            if (supports(store, ConnectorCapability.LISTINGS)) {
                results.add(syncListings(store, EcomSyncService.JOB));
            }
        }
        return results;
    }

    private SyncResult syncListings(EcomStore store, String trigger) {
        LocalDateTime start = LocalDateTime.now();
        ChannelConnector connector = registry.find(store.getPlatform()).orElse(null);
        if (connector == null || !connector.supports(ConnectorCapability.LISTINGS)) {
            return failed(store, SyncType.LISTINGS, trigger, start, Messages.get("ecom.error.connectorNoCapability",
                    store.getPlatform().name(), ConnectorCapability.LISTINGS.name()));
        }
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            return failed(store, SyncType.LISTINGS, trigger, start, Messages.get("ecom.error.storeCredentialsMissing", store.getName()));
        }
        List<ListingDraft> drafts;
        try {
            drafts = connector.fetchListings(ctx);
        } catch (RuntimeException e) {
            if (!(e instanceof BizException)) {
                log.warn("Listing sync failed for store {}", store.getCode(), e);
            }
            return failed(store, SyncType.LISTINGS, trigger, start, e.getMessage());
        }
        // 同一 SKU 多条时取最后一条
        Map<String, ListingDraft> bySku = new LinkedHashMap<>();
        for (ListingDraft d : drafts == null ? List.<ListingDraft>of() : drafts) {
            if (d != null && StringUtils.hasText(d.sellerSku())) {
                bySku.put(cut(d.sellerSku().trim(), 64), d);
            }
        }
        int[] counts;
        try {
            counts = transactionTemplate.execute(status -> upsert(store, bySku));
        } catch (BizException e) {
            return failed(store, SyncType.LISTINGS, trigger, start, e.getMessage());
        }
        syncService.writeLog(store.getId(), SyncType.LISTINGS, trigger, start, true, bySku.size(), counts[0], counts[1], null);
        return new SyncResult(store.getId(), store.getName(), bySku.size(), counts[0], counts[1], null);
    }

    /**
     * 按 店铺 + 平台 SKU 新增或更新在线商品：已有的只更新平台侧信息（平台没有返回的字段保留原值），不动物料映射与多件装；
     * 新增的按「平台 SKU = 物料编码」自动映射物料（物料是多规格款式时不映射）
     *
     * @return [新增数, 更新数]
     */
    private int[] upsert(EcomStore store, Map<String, ListingDraft> bySku) {
        List<String> skus = new ArrayList<>(bySku.keySet());
        Map<String, EcomListing> existing = new LinkedHashMap<>();
        for (int i = 0; i < skus.size(); i += QUERY_BATCH) {
            List<String> chunk = skus.subList(i, Math.min(skus.size(), i + QUERY_BATCH));
            listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery()
                            .select(EcomListing::getId, EcomListing::getSellerSku, EcomListing::getPlatformExtra)
                            .eq(EcomListing::getStoreId, store.getId()).in(EcomListing::getSellerSku, chunk))
                    .forEach(l -> existing.put(l.getSellerSku(), l));
        }
        List<String> newSkus = skus.stream().filter(s -> !existing.containsKey(s)).toList();
        Map<String, WmsMaterial> materials = new LinkedHashMap<>();
        for (int i = 0; i < newSkus.size(); i += QUERY_BATCH) {
            materials.putAll(materialService.mapByCodes(newSkus.subList(i, Math.min(newSkus.size(), i + QUERY_BATCH))));
        }
        int created = 0;
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, ListingDraft> e : bySku.entrySet()) {
            ListingDraft d = e.getValue();
            EcomListing old = existing.get(e.getKey());
            if (old == null) {
                EcomListing l = new EcomListing();
                l.setStoreId(store.getId());
                l.setSellerSku(e.getKey());
                apply(l, d);
                if (l.getFulfillment() == null) l.setFulfillment(FulfillmentType.SELLER);
                if (l.getStatus() == null) l.setStatus(ListingStatus.ACTIVE);
                if (l.getCurrency() == null) l.setCurrency(store.getCurrency());
                l.setPackQty(BigDecimal.ONE);
                WmsMaterial m = materials.get(e.getKey());
                if (m != null && !m.isTemplateMaterial()) {
                    l.setMaterialId(m.getId());
                }
                listingMapper.insert(l);
                created++;
            } else {
                EcomListing l = new EcomListing();
                apply(l, d);
                // 平台附加 ID 与已保存的合并（同名键覆盖），平台这次没有返回时保留原值
                l.setPlatformExtra(d.extra().isEmpty() ? null : extraJson(mergeExtra(parseExtra(old.getPlatformExtra()), d.extra())));
                listingMapper.update(Wrappers.<EcomListing>lambdaUpdate()
                        .set(l.getPlatformItemId() != null, EcomListing::getPlatformItemId, l.getPlatformItemId())
                        .set(l.getParentItemId() != null, EcomListing::getParentItemId, l.getParentItemId())
                        .set(l.getTitle() != null, EcomListing::getTitle, l.getTitle())
                        .set(l.getVariant() != null, EcomListing::getVariant, l.getVariant())
                        .set(l.getPrice() != null, EcomListing::getPrice, l.getPrice())
                        .set(l.getCurrency() != null, EcomListing::getCurrency, l.getCurrency())
                        .set(l.getPlatformQty() != null, EcomListing::getPlatformQty, l.getPlatformQty())
                        .set(l.getFulfillment() != null, EcomListing::getFulfillment, l.getFulfillment())
                        .set(l.getStatus() != null, EcomListing::getStatus, l.getStatus())
                        .set(l.getImageUrl() != null, EcomListing::getImageUrl, l.getImageUrl())
                        .set(l.getUrl() != null, EcomListing::getUrl, l.getUrl())
                        .set(l.getPlatformExtra() != null, EcomListing::getPlatformExtra, l.getPlatformExtra())
                        .set(EcomListing::getUpdateTime, now)
                        .eq(EcomListing::getId, old.getId()));
                updated++;
            }
        }
        return new int[]{created, updated};
    }

    /** 平台返回的字段写入在线商品（超长的截断，取值不认识的配送方式 / 状态忽略） */
    private static void apply(EcomListing l, ListingDraft d) {
        l.setPlatformItemId(cut(d.platformItemId(), 64));
        l.setParentItemId(cut(d.parentItemId(), 64));
        l.setTitle(cut(d.title(), 255));
        l.setVariant(cut(d.variant(), 128));
        l.setPrice(d.price() == null || d.price().signum() < 0 ? null : d.price().setScale(Decimals.PRICE_SCALE, Decimals.ROUNDING));
        l.setCurrency(StringUtils.hasText(d.currency()) ? cut(d.currency().trim().toUpperCase(), 8) : null);
        l.setPlatformQty(d.platformQty() == null ? null : Decimals.qty(d.platformQty()));
        l.setFulfillment(parse(FulfillmentType.class, d.fulfillment()));
        l.setStatus(parse(ListingStatus.class, d.status()));
        l.setImageUrl(cut(d.imageUrl(), 255));
        l.setUrl(cut(d.url(), 255));
        l.setPlatformExtra(d.extra().isEmpty() ? null : extraJson(d.extra()));
    }

    /** 在线商品的平台附加 ID（platform_extra，JSON 对象），为空或格式不对时为空 Map */
    static Map<String, String> parseExtra(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, String> map = HttpJson.MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
            });
            return ConnectorJson.compact(map);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    static String extraJson(Map<String, String> extra) {
        Map<String, String> m = ConnectorJson.compact(extra);
        return m.isEmpty() ? null : HttpJson.MAPPER.writeValueAsString(new TreeMap<>(m));
    }

    private static Map<String, String> mergeExtra(Map<String, String> saved, Map<String, String> fresh) {
        Map<String, String> m = new LinkedHashMap<>(saved);
        m.putAll(fresh);
        return m;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value.trim())) {
                return e;
            }
        }
        return null;
    }

    private static String cut(String v, int max) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    // ---------------------------------------------------------------- 回传可售库存

    /**
     * 手工回传一个店铺的可售库存（不要求开启自动回传）
     *
     * @param all true 时全部回传，false 只回传和上次不同的
     */
    public SyncResult pushInventory(Long storeId, boolean all) {
        return pushInventory(storeService.requireEnabled(storeId), EcomSyncService.MANUAL, all);
    }

    /** 回传全部开启了自动回传库存、有授权的启用店铺（定时任务），只回传有变化的；平台不支持回传库存的店铺跳过 */
    public List<SyncResult> pushInventoryAll() {
        List<SyncResult> results = new ArrayList<>();
        for (EcomStore store : storeService.inventorySyncable()) {
            if (supports(store, ConnectorCapability.INVENTORY_PUSH)) {
                results.add(pushInventory(store, EcomSyncService.JOB, false));
            }
        }
        return results;
    }

    /**
     * 可售数 = 默认发货仓的空闲库存（库存 − 冻结 − 已预留，不小于 0）÷ 多件装 − 店铺保留数量，向下取整、不小于 0。
     * 定时任务没有变化时不写日志，避免每 15 分钟一条
     */
    private SyncResult pushInventory(EcomStore store, String trigger, boolean all) {
        LocalDateTime start = LocalDateTime.now();
        ChannelConnector connector = registry.find(store.getPlatform()).orElse(null);
        if (connector == null || !connector.supports(ConnectorCapability.INVENTORY_PUSH)) {
            return failed(store, SyncType.INVENTORY_PUSH, trigger, start, Messages.get("ecom.error.connectorNoCapability",
                    store.getPlatform().name(), ConnectorCapability.INVENTORY_PUSH.name()));
        }
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            return failed(store, SyncType.INVENTORY_PUSH, trigger, start, Messages.get("ecom.error.storeCredentialsMissing", store.getName()));
        }
        List<EcomListing> candidates;
        try {
            candidates = sellable(store);
        } catch (BizException e) {
            return failed(store, SyncType.INVENTORY_PUSH, trigger, start, e.getMessage());
        }
        List<EcomListing> changed = candidates.stream()
                .filter(l -> all || l.getLastPushedQty() == null || l.getLastPushedQty().compareTo(l.getPlatformQty()) != 0).toList();
        if (changed.isEmpty()) {
            if (!EcomSyncService.JOB.equals(trigger)) {
                syncService.writeLog(store.getId(), SyncType.INVENTORY_PUSH, trigger, start, true, candidates.size(), 0, 0, null);
            }
            return new SyncResult(store.getId(), store.getName(), candidates.size(), 0, 0, null);
        }
        int pushed = 0;
        for (int i = 0; i < changed.size(); i += PUSH_BATCH) {
            List<EcomListing> batch = changed.subList(i, Math.min(changed.size(), i + PUSH_BATCH));
            List<InventoryUpdate> updates = batch.stream()
                    .map(EcomListingSyncService::inventoryUpdate).toList();
            try {
                connector.pushInventory(ctx, updates);
            } catch (RuntimeException e) {
                if (!(e instanceof BizException)) {
                    log.warn("Inventory push failed for store {}", store.getCode(), e);
                }
                String msg = pushed == 0 ? e.getMessage()
                        : Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.hub.pushPartial", pushed, e.getMessage());
                storeService.markSyncMessage(store.getId(), msg);
                syncService.writeLog(store.getId(), SyncType.INVENTORY_PUSH, trigger, start, false, candidates.size(), 0, pushed, msg);
                return new SyncResult(store.getId(), store.getName(), candidates.size(), 0, pushed, msg);
            }
            LocalDateTime now = LocalDateTime.now();
            List<EcomListing> marks = batch.stream().map(l -> {
                EcomListing m = new EcomListing();
                m.setId(l.getId());
                m.setLastPushedQty(l.getPlatformQty());
                return m;
            }).toList();
            channelMapper.updatePushed(marks, now);
            pushed += batch.size();
        }
        syncService.writeLog(store.getId(), SyncType.INVENTORY_PUSH, trigger, start, true, candidates.size(), 0, pushed, null);
        return new SyncResult(store.getId(), store.getName(), candidates.size(), 0, pushed, null);
    }

    /**
     * 店铺需要回传的在线商品及其可售数（借用 platformQty 字段带出本次要回传的数量，不写库）
     */
    List<EcomListing> sellable(EcomStore store) {
        List<EcomListing> list = channelMapper.selectPushCandidates(store.getId());
        if (list.isEmpty()) {
            return list;
        }
        WmsWarehouse wh = warehouseService.get(store.getWarehouseId());
        if (wh.getType() != null && WarehouseType.UNAVAILABLE.contains(wh.getType())) {
            throw new BizException("ecom.error.hub.warehouseUnavailable", wh.getName());
        }
        // 单品为 物料 × 多件装，组合为各组件 × 用量
        Map<Long, List<com.lemon.erp.modules.ecom.dto.BundleComponent>> comps = bundleService.componentsOf(list.stream().map(EcomListing::getId).toList());
        Set<Long> materialIds = comps.values().stream().flatMap(List::stream).map(com.lemon.erp.modules.ecom.dto.BundleComponent::materialId)
                .collect(Collectors.toSet());
        Map<Long, BigDecimal> free = materialIds.isEmpty() ? Map.of() : reservationService.free(store.getWarehouseId(), materialIds);
        BigDecimal buffer = Decimals.nz(store.getInventoryBuffer());
        List<EcomListing> out = new ArrayList<>();
        for (EcomListing l : list) {
            List<com.lemon.erp.modules.ecom.dto.BundleComponent> c = comps.get(l.getId());
            if (c == null || c.isEmpty()) {
                continue;
            }
            l.setPlatformQty(sellableQty(free, c, buffer));
            out.add(l);
        }
        return out;
    }

    /** 组合（或单品）的可售数 = min(⌊各组件空闲库存 ÷ 用量⌋) − 保留，不小于 0 */
    public static BigDecimal sellableQty(Map<Long, BigDecimal> free, List<com.lemon.erp.modules.ecom.dto.BundleComponent> components, BigDecimal buffer) {
        BigDecimal min = null;
        for (com.lemon.erp.modules.ecom.dto.BundleComponent c : components) {
            BigDecimal q = sellableQty(free.get(c.materialId()), c.qty(), BigDecimal.ZERO);
            min = min == null ? q : min.min(q);
        }
        return Decimals.nz(min).subtract(Decimals.nz(buffer)).setScale(0, RoundingMode.DOWN).max(BigDecimal.ZERO);
    }

    /** 回传给连接器的一条：平台 SKU、商品 ID、父商品 ID、平台仓库（附加 ID 中的 warehouseId）、可售数与附加 ID */
    static InventoryUpdate inventoryUpdate(EcomListing l) {
        Map<String, String> extra = parseExtra(l.getPlatformExtra());
        return new InventoryUpdate(l.getSellerSku(), l.getPlatformItemId(), l.getParentItemId(), extra.get(ListingDraft.EXTRA_WAREHOUSE_ID),
                l.getPlatformQty(), extra);
    }

    /** 可售数 = ⌊max(空闲库存, 0) ÷ 多件装⌋ − 保留，不小于 0，向下取整 */
    static BigDecimal sellableQty(BigDecimal free, BigDecimal packQty, BigDecimal buffer) {
        BigDecimal f = Decimals.nz(free).max(BigDecimal.ZERO);
        BigDecimal pack = packQty == null || packQty.signum() <= 0 ? BigDecimal.ONE : packQty;
        BigDecimal qty = f.divide(pack, 0, RoundingMode.DOWN).subtract(Decimals.nz(buffer)).setScale(0, RoundingMode.DOWN);
        return qty.max(BigDecimal.ZERO);
    }

    private boolean supports(EcomStore store, ConnectorCapability capability) {
        return registry.find(store.getPlatform()).map(c -> c.supports(capability)).orElse(false);
    }

    private SyncResult failed(EcomStore store, SyncType type, String trigger, LocalDateTime start, String message) {
        storeService.markSyncMessage(store.getId(), message);
        syncService.writeLog(store.getId(), type, trigger, start, false, 0, 0, 0, message);
        return SyncResult.failed(store.getId(), store.getName(), message);
    }
}
