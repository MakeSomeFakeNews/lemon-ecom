package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.ListingPair;
import com.lemon.erp.modules.ecom.entity.EcomBundleItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在线商品：店铺 × 平台 SKU 唯一，映射到本地物料；导入按 店铺编码 + 平台 SKU 新增或更新
 */
@Service
@RequiredArgsConstructor
public class EcomListingService {

    private final EcomListingMapper listingMapper;
    private final EcomStoreService storeService;
    private final WmsMaterialService materialService;
    private final EcomBundleService bundleService;

    public PageResult<EcomListing> page(long current, long size, Long storeId, String keyword, ListingStatus status, FulfillmentType fulfillment,
                                        Boolean mapped, Long materialId) {
        return page(current, size, storeId, keyword, status, fulfillment, mapped, materialId, null, null);
    }

    /**
     * @param composeType  只看单品 / 组合
     * @param parentItemId 只看某个父商品的变体（SPU 视图展开）
     */
    public PageResult<EcomListing> page(long current, long size, Long storeId, String keyword, ListingStatus status, FulfillmentType fulfillment,
                                        Boolean mapped, Long materialId, ListingComposeType composeType, String parentItemId) {
        Page<EcomListing> page = Page.of(current, size);
        listingMapper.selectPageList(page, storeId, keyword, status, fulfillment, mapped, materialId, composeType, parentItemId);
        attachBundles(page.getRecords());
        return PageResult.of(page);
    }

    /** 组合商品带上组成（列表、详情显示） */
    public void attachBundles(List<EcomListing> list) {
        List<Long> ids = list.stream().filter(l -> l.getComposeType() == ListingComposeType.BUNDLE).map(EcomListing::getId).toList();
        Map<Long, List<EcomBundleItem>> items = bundleService.itemsOf(ids);
        list.stream().filter(l -> l.getComposeType() == ListingComposeType.BUNDLE)
                .forEach(l -> l.setBundleItems(items.getOrDefault(l.getId(), List.of())));
    }

    public EcomListing detail(Long id) {
        EcomListing l = listingMapper.selectDetail(id);
        if (l == null) {
            throw new BizException("ecom.error.listingNotFound");
        }
        attachBundles(List.of(l));
        return l;
    }

    public EcomListing get(Long id) {
        EcomListing l = listingMapper.selectById(id);
        if (l == null) {
            throw new BizException("ecom.error.listingNotFound");
        }
        return l;
    }

    /** 按 ID 批量取在线商品：ID → 在线商品 */
    public Map<Long, EcomListing> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return listingMapper.selectByIds(ids).stream().collect(Collectors.toMap(EcomListing::getId, Function.identity()));
    }

    /** 店铺中若干平台 SKU 的在线商品：SKU → 在线商品（SKU 大小写按原样匹配） */
    public Map<String, EcomListing> byStoreSkus(Long storeId, Collection<String> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        return listingMapper.selectByStoreSkus(storeId, skus).stream()
                .collect(Collectors.toMap(EcomListing::getSellerSku, Function.identity(), (a, b) -> a));
    }

    @Transactional
    public Long create(EcomListing l) {
        l.setId(null);
        if (l.getComposeType() == ListingComposeType.BUNDLE && l.getBundleItems() == null) {
            throw new BizException("ecom.error.catalog.bundleEmpty");
        }
        normalize(l);
        if (findByStoreSku(l.getStoreId(), l.getSellerSku()) != null) {
            throw new BizException("ecom.error.listingSkuExists", l.getSellerSku());
        }
        listingMapper.insert(l);
        saveBundle(l);
        return l.getId();
    }

    @Transactional
    public void update(Long id, EcomListing l) {
        EcomListing old = get(id);
        l.setStoreId(old.getStoreId());
        normalize(l);
        EcomListing dup = findByStoreSku(l.getStoreId(), l.getSellerSku());
        if (dup != null && !dup.getId().equals(id)) {
            throw new BizException("ecom.error.listingSkuExists", l.getSellerSku());
        }
        l.setId(id);
        listingMapper.updateById(l);
        if (l.getMaterialId() == null && old.getMaterialId() != null) {
            listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getMaterialId, null).eq(EcomListing::getId, id));
        }
        if (l.getFnsku() == null && old.getFnsku() != null) {
            listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getFnsku, null).eq(EcomListing::getId, id));
        }
        // 导入更新（没有带组成）时保留原组合的组成
        if (l.getComposeType() != ListingComposeType.BUNDLE || l.getBundleItems() != null) {
            saveBundle(l);
        }
    }

    /** 组合商品保存组成，单品清空组成 */
    private void saveBundle(EcomListing l) {
        bundleService.replace(l.getId(), l.getComposeType() == ListingComposeType.BUNDLE ? l.getBundleItems() : null);
    }

    /** 只修改物料映射（订单异常处理、SKU 配对时使用）：映射为单品，原来是组合的清空组成 */
    @Transactional
    public void map(Long id, Long materialId, BigDecimal packQty) {
        get(id);
        checkMaterial(materialId);
        listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getMaterialId, materialId)
                .set(EcomListing::getPackQty, packQty == null || packQty.signum() <= 0 ? BigDecimal.ONE : Decimals.qty(packQty))
                .set(EcomListing::getComposeType, ListingComposeType.SINGLE)
                .eq(EcomListing::getId, id));
        bundleService.replace(id, null);
    }

    /** 批量配对（推荐一键确认、按款式批量映射） */
    @Transactional
    public int mapAll(List<ListingPair> pairs) {
        for (ListingPair p : pairs) {
            map(p.listingId(), p.materialId(), p.packQty());
        }
        return pairs.size();
    }

    /** 解除配对：单品清空映射，组合清空组成并改为单品 */
    @Transactional
    public void unpair(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getMaterialId, null).set(EcomListing::getPackQty, BigDecimal.ONE)
                .set(EcomListing::getComposeType, ListingComposeType.SINGLE).in(EcomListing::getId, ids));
        bundleService.deleteByListings(ids);
    }

    @Transactional
    public void delete(List<Long> ids) {
        listingMapper.deleteByIds(ids);
        bundleService.deleteByListings(ids);
    }

    /**
     * 导入一行：按店铺编码找店铺，物料编码找物料；店铺 + SKU 已存在时更新（物料编码为空时保留原映射）
     */
    @Transactional
    public void importRow(EcomListing row) {
        EcomStore store = storeService.findByCode(row.getStoreCode());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", row.getStoreCode());
        }
        row.setStoreId(store.getId());
        if (StringUtils.hasText(row.getMaterialCodeInput())) {
            WmsMaterial m = materialService.mapByCodes(List.of(row.getMaterialCodeInput().trim())).get(row.getMaterialCodeInput().trim());
            if (m == null) {
                throw new BizException("ecom.error.materialCodeNotFound", row.getMaterialCodeInput());
            }
            row.setMaterialId(m.getId());
        }
        EcomListing existing = findByStoreSku(store.getId(), row.getSellerSku().trim());
        if (existing == null) {
            if (row.getCurrency() == null) row.setCurrency(store.getCurrency());
            create(row);
            return;
        }
        if (row.getComposeType() == null) {
            // 导入只能映射单品：填了物料编码的改为单品，否则保持原组成方式（组合的组成不变）
            row.setComposeType(row.getMaterialId() != null ? ListingComposeType.SINGLE : existing.getComposeType());
        }
        if (row.getFnsku() == null) row.setFnsku(existing.getFnsku());
        if (row.getMaterialId() == null) {
            row.setMaterialId(existing.getMaterialId());
            if (row.getPackQty() == null) row.setPackQty(existing.getPackQty());
        }
        if (row.getStatus() == null) row.setStatus(existing.getStatus());
        if (row.getFulfillment() == null) row.setFulfillment(existing.getFulfillment());
        if (row.getCurrency() == null) row.setCurrency(existing.getCurrency());
        update(existing.getId(), row);
    }

    private EcomListing findByStoreSku(Long storeId, String sku) {
        return listingMapper.selectOne(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, storeId).eq(EcomListing::getSellerSku, sku));
    }

    private void normalize(EcomListing l) {
        EcomStore store = storeService.get(l.getStoreId());
        l.setSellerSku(l.getSellerSku().trim());
        if (l.getStatus() == null) l.setStatus(ListingStatus.ACTIVE);
        if (l.getFulfillment() == null) l.setFulfillment(FulfillmentType.SELLER);
        if (!StringUtils.hasText(l.getCurrency())) l.setCurrency(store.getCurrency());
        l.setPackQty(l.getPackQty() == null || l.getPackQty().signum() <= 0 ? BigDecimal.ONE : Decimals.qty(l.getPackQty()));
        if (l.getPlatformQty() != null) l.setPlatformQty(Decimals.qty(l.getPlatformQty()));
        if (StringUtils.hasText(l.getFnsku())) {
            l.setFnsku(l.getFnsku().trim().toUpperCase());
        } else {
            l.setFnsku(null);
        }
        if (l.getComposeType() == null) l.setComposeType(ListingComposeType.SINGLE);
        if (l.getComposeType() == ListingComposeType.BUNDLE) {
            // 组合：物料在组成中，没有带组成（导入更新）时保留原组成
            l.setMaterialId(null);
            l.setPackQty(BigDecimal.ONE);
            if (l.getBundleItems() != null) {
                l.setBundleItems(bundleService.normalize(l.getBundleItems()));
            }
            return;
        }
        if (l.getMaterialId() != null) {
            checkMaterial(l.getMaterialId());
        }
    }

    /** 映射的物料必须启用，且不能是多规格款式（款式本身不能出入库） */
    private void checkMaterial(Long materialId) {
        WmsMaterial m = materialService.get(materialId);
        if (m.isTemplateMaterial()) {
            throw new BizException("ecom.error.materialIsTemplate", m.getName());
        }
    }

    /** 物料 ID → 在线商品数（删除物料前的引用检查等） */
    public Map<Long, Long> countByMaterials(Collection<Long> materialIds) {
        Map<Long, Long> map = new HashMap<>();
        if (materialIds.isEmpty()) {
            return map;
        }
        listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery().select(EcomListing::getMaterialId).in(EcomListing::getMaterialId, materialIds))
                .forEach(l -> map.merge(l.getMaterialId(), 1L, Long::sum));
        return map;
    }
}
