package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.entity.EcomBundleItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.mapper.EcomBundleItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组合商品（捆绑 / 套装）：一个平台 SKU 由多个本地物料组成。
 * <p>
 * {@link #componentsOf} 把单品和组合统一成「一件平台 SKU 含哪些物料、各多少」，补货建议、库存回传、发货计划等按物料折算时使用
 */
@Service
@RequiredArgsConstructor
public class EcomBundleService {

    private final EcomBundleItemMapper bundleItemMapper;
    private final EcomListingMapper listingMapper;
    private final WmsMaterialService materialService;

    /**
     * 在线商品的组成物料：单品为 映射的物料 × 多件装数量，组合为各组件 × 用量；没有映射（或组合没有组成）的不在结果中。
     * 可复用：补货建议按组件折算平台仓库存 / 在途 / 销量时调用
     *
     * @return 在线商品 ID → 组成物料（按组合中的顺序）
     */
    public Map<Long, List<BundleComponent>> componentsOf(Collection<Long> listingIds) {
        Map<Long, List<BundleComponent>> out = new LinkedHashMap<>();
        if (listingIds == null || listingIds.isEmpty()) {
            return out;
        }
        List<EcomListing> listings = listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery()
                .select(EcomListing::getId, EcomListing::getMaterialId, EcomListing::getPackQty, EcomListing::getComposeType)
                .in(EcomListing::getId, new HashSet<>(listingIds)));
        List<Long> bundles = new ArrayList<>();
        for (EcomListing l : listings) {
            if (l.getComposeType() == ListingComposeType.BUNDLE) {
                bundles.add(l.getId());
            } else if (l.getMaterialId() != null) {
                out.put(l.getId(), List.of(new BundleComponent(l.getMaterialId(), packOf(l.getPackQty()))));
            }
        }
        itemsOf(bundles).forEach((id, items) -> out.put(id, items.stream().map(i -> new BundleComponent(i.getMaterialId(), i.getQty())).toList()));
        return out;
    }

    /** 组合商品的组成（含物料编码、名称）：在线商品 ID → 组件，没有组成的不在结果中 */
    public Map<Long, List<EcomBundleItem>> itemsOf(Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Map.of();
        }
        return bundleItemMapper.selectByListingIds(listingIds).stream()
                .collect(Collectors.groupingBy(EcomBundleItem::getListingId, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 校验并整理组合的组成：至少一个组件，物料启用且不是款式、不重复，用量大于 0
     */
    public List<EcomBundleItem> normalize(List<EcomBundleItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException("ecom.error.catalog.bundleEmpty");
        }
        Set<Long> seen = new HashSet<>();
        for (EcomBundleItem i : items) {
            if (i.getMaterialId() == null) {
                throw new BizException("ecom.valid.catalog.bundleMaterialRequired");
            }
            if (i.getQty() == null || i.getQty().signum() <= 0) {
                throw new BizException("ecom.valid.catalog.bundleQtyPositive");
            }
            if (!seen.add(i.getMaterialId())) {
                throw new BizException("ecom.error.catalog.bundleDuplicate");
            }
        }
        Map<Long, WmsMaterial> materials = materialService.getEnabled(seen);
        List<EcomBundleItem> out = new ArrayList<>();
        int sort = 0;
        for (EcomBundleItem i : items) {
            WmsMaterial m = materials.get(i.getMaterialId());
            if (m.isTemplateMaterial()) {
                throw new BizException("ecom.error.materialIsTemplate", m.getName());
            }
            EcomBundleItem b = new EcomBundleItem();
            b.setMaterialId(i.getMaterialId());
            b.setQty(Decimals.qty(i.getQty()));
            b.setSort(++sort);
            out.add(b);
        }
        return out;
    }

    /** 保存组合的组成（整体替换，调用方的事务中执行）；items 为空时清空 */
    public void replace(Long listingId, List<EcomBundleItem> items) {
        bundleItemMapper.delete(Wrappers.<EcomBundleItem>lambdaQuery().eq(EcomBundleItem::getListingId, listingId));
        if (items == null || items.isEmpty()) {
            return;
        }
        items.forEach(i -> i.setListingId(listingId));
        bundleItemMapper.insertBatch(items);
    }

    /** 删除在线商品时一并删除组成（调用方的事务中执行） */
    public void deleteByListings(Collection<Long> listingIds) {
        if (listingIds != null && !listingIds.isEmpty()) {
            bundleItemMapper.delete(Wrappers.<EcomBundleItem>lambdaQuery().in(EcomBundleItem::getListingId, listingIds));
        }
    }

    static BigDecimal packOf(BigDecimal packQty) {
        return packQty == null || packQty.signum() <= 0 ? BigDecimal.ONE : packQty;
    }
}
