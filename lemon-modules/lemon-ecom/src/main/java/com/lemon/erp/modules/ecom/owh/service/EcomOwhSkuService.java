package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.SkuRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhSku;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhSkuMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 海外仓 SKU 映射：账号下 物料 ↔ 服务商 SKU（一对一）。没有映射的物料按物料编码作为服务商 SKU；服务商返回的 SKU 先按映射、再按物料编码识别物料。
 * 库存同步、入库、出库、退件、调拨、费用都经这里转换
 */
@Service
@RequiredArgsConstructor
public class EcomOwhSkuService {

    private final EcomOwhSkuMapper skuMapper;
    private final EcomOwhStockMapper stockMapper;

    public List<EcomOwhSku> list(Long accountId, String keyword) {
        return skuMapper.selectByAccount(accountId, StringUtils.hasText(keyword) ? keyword.trim() : null);
    }

    /** 新增或修改映射（同一物料只有一条；同一服务商 SKU 只能对应一个物料） */
    public Long save(Long accountId, SkuRequest req) {
        String sku = req.owhSku().trim();
        EcomOwhSku dup = skuMapper.selectOne(Wrappers.<EcomOwhSku>lambdaQuery().eq(EcomOwhSku::getAccountId, accountId).eq(EcomOwhSku::getOwhSku, sku), false);
        if (dup != null && !Objects.equals(dup.getMaterialId(), req.materialId())) {
            throw new BizException("ecom.error.owh.skuMapped", sku);
        }
        if (stockMapper.selectMaterials(List.of(req.materialId())).isEmpty()) {
            throw new BizException("ecom.error.owh.materialNotFound");
        }
        EcomOwhSku old = skuMapper.selectOne(Wrappers.<EcomOwhSku>lambdaQuery().eq(EcomOwhSku::getAccountId, accountId)
                .eq(EcomOwhSku::getMaterialId, req.materialId()), false);
        if (old != null) {
            skuMapper.update(Wrappers.<EcomOwhSku>lambdaUpdate().set(EcomOwhSku::getOwhSku, sku).set(EcomOwhSku::getRemark, req.remark())
                    .eq(EcomOwhSku::getId, old.getId()));
            return old.getId();
        }
        EcomOwhSku k = new EcomOwhSku();
        k.setAccountId(accountId);
        k.setMaterialId(req.materialId());
        k.setOwhSku(sku);
        k.setRemark(req.remark());
        skuMapper.insert(k);
        return k.getId();
    }

    public void delete(Long id) {
        skuMapper.deleteById(id);
    }

    /** 物料 → 服务商 SKU（有映射用映射，否则物料编码） */
    public Map<Long, String> skus(Long accountId, Collection<Long> materialIds) {
        Map<Long, String> out = new HashMap<>();
        if (materialIds == null || materialIds.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : stockMapper.selectMaterials(new HashSet<>(materialIds))) {
            out.put(((Number) row.get("id")).longValue(), String.valueOf(row.get("code")));
        }
        for (EcomOwhSku k : skuMapper.selectList(Wrappers.<EcomOwhSku>lambdaQuery().eq(EcomOwhSku::getAccountId, accountId)
                .in(EcomOwhSku::getMaterialId, new HashSet<>(materialIds)))) {
            out.put(k.getMaterialId(), k.getOwhSku());
        }
        return out;
    }

    /** 服务商 SKU → 物料（先按映射，再按物料编码），识别不了的不在结果中 */
    public Map<String, Long> materials(Long accountId, Collection<String> skus) {
        Map<String, Long> out = new HashMap<>();
        Set<String> rest = new HashSet<>();
        if (skus == null || skus.isEmpty()) {
            return out;
        }
        Set<String> all = new HashSet<>(skus);
        all.remove(null);
        if (all.isEmpty()) {
            return out;
        }
        for (EcomOwhSku k : skuMapper.selectList(Wrappers.<EcomOwhSku>lambdaQuery().eq(EcomOwhSku::getAccountId, accountId).in(EcomOwhSku::getOwhSku, all))) {
            out.put(k.getOwhSku(), k.getMaterialId());
        }
        for (String s : all) {
            if (!out.containsKey(s)) {
                rest.add(s);
            }
        }
        if (!rest.isEmpty()) {
            // 按物料编码识别时，已映射到其他 SKU 的物料不再按编码识别
            Set<Long> mapped = new HashSet<>();
            skuMapper.selectList(Wrappers.<EcomOwhSku>lambdaQuery().select(EcomOwhSku::getMaterialId).eq(EcomOwhSku::getAccountId, accountId))
                    .forEach(k -> mapped.add(k.getMaterialId()));
            for (Map<String, Object> row : stockMapper.selectMaterialIds(rest)) {
                Long id = ((Number) row.get("id")).longValue();
                if (!mapped.contains(id)) {
                    out.put(String.valueOf(row.get("code")), id);
                }
            }
        }
        return out;
    }
}
