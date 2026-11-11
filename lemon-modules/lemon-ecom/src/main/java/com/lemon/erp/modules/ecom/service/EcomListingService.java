package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
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

    public PageResult<EcomListing> page(long current, long size, Long storeId, String keyword, ListingStatus status, FulfillmentType fulfillment,
                                        Boolean mapped, Long materialId) {
        Page<EcomListing> page = Page.of(current, size);
        listingMapper.selectPageList(page, storeId, keyword, status, fulfillment, mapped, materialId);
        return PageResult.of(page);
    }

    public EcomListing detail(Long id) {
        EcomListing l = listingMapper.selectDetail(id);
        if (l == null) {
            throw new BizException("ecom.error.listingNotFound");
        }
        return l;
    }

    public EcomListing get(Long id) {
        EcomListing l = listingMapper.selectById(id);
        if (l == null) {
            throw new BizException("ecom.error.listingNotFound");
        }
        return l;
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
        normalize(l);
        if (findByStoreSku(l.getStoreId(), l.getSellerSku()) != null) {
            throw new BizException("ecom.error.listingSkuExists", l.getSellerSku());
        }
        listingMapper.insert(l);
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
    }

    /** 只修改物料映射（订单异常处理时使用） */
    @Transactional
    public void map(Long id, Long materialId, BigDecimal packQty) {
        get(id);
        checkMaterial(materialId);
        listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getMaterialId, materialId)
                .set(EcomListing::getPackQty, packQty == null || packQty.signum() <= 0 ? BigDecimal.ONE : Decimals.qty(packQty))
                .eq(EcomListing::getId, id));
    }

    public void delete(List<Long> ids) {
        listingMapper.deleteByIds(ids);
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
