package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.PlatformStockImportRow;
import com.lemon.erp.modules.ecom.entity.EcomPlatformStock;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomPlatformStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 平台仓库存快照：导入平台库存报告（店铺 + SKU 只保留最近一次），与系统中平台仓库存对比差异
 */
@Service
@RequiredArgsConstructor
public class EcomPlatformStockService {

    private final EcomPlatformStockMapper stockMapper;
    private final EcomStoreService storeService;

    public PageResult<EcomPlatformStock> page(long current, long size, Long storeId, String keyword, Boolean diffOnly) {
        Page<EcomPlatformStock> page = Page.of(current, size);
        stockMapper.selectPageList(page, storeId, keyword, diffOnly);
        return PageResult.of(page);
    }

    @Transactional
    public void importRow(PlatformStockImportRow r) {
        EcomStore store = storeService.findByCode(r.getStoreCode());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", r.getStoreCode());
        }
        String sku = r.getSellerSku().trim();
        EcomPlatformStock existing = stockMapper.selectOne(Wrappers.<EcomPlatformStock>lambdaQuery()
                .eq(EcomPlatformStock::getStoreId, store.getId()).eq(EcomPlatformStock::getSellerSku, sku));
        EcomPlatformStock s = existing == null ? new EcomPlatformStock() : existing;
        s.setStoreId(store.getId());
        s.setSellerSku(sku);
        s.setPlatformItemId(r.getPlatformItemId());
        s.setFulfillableQty(Decimals.qty(r.getFulfillableQty()));
        s.setInboundQty(Decimals.qty(Decimals.nz(r.getInboundQty())));
        s.setReservedQty(Decimals.qty(Decimals.nz(r.getReservedQty())));
        s.setUnfulfillableQty(Decimals.qty(Decimals.nz(r.getUnfulfillableQty())));
        s.setSnapshotDate(r.getSnapshotDate() == null ? LocalDate.now() : r.getSnapshotDate());
        if (existing == null) {
            stockMapper.insert(s);
        } else {
            stockMapper.updateById(s);
        }
    }

    public void delete(List<Long> ids) {
        stockMapper.deleteByIds(ids);
    }
}
