package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomFeeAllocation;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.entity.EcomSettlementItem;
import com.lemon.erp.modules.ecom.mapper.EcomFeeAllocationMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementItemMapper;
import com.lemon.erp.modules.ecom.model.FeeAllocMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 不挂订单的平台费用分摊：结算明细没有平台订单号（或订单不在系统中）、类型可分摊（仓储费、订阅费、移除费等，见
 * {@link com.lemon.erp.modules.ecom.model.SettlementItemType#allocatable}）时，
 * <ul>
 *     <li>明细带平台 SKU：直接归属该 SKU（SKU）</li>
 *     <li>否则按结算单的分摊方式（为空按类型默认：仓储、移除、入库、配送、退货类按销量，其余按销售额），
 *     以店铺在结算周期内（下单时间）各平台 SKU 的销量或本位币销售额为权重分摊，尾差记到最后一个 SKU</li>
 *     <li>周期内没有销量：留在店铺（STORE，SKU 为空），利润报表按店铺计入</li>
 * </ul>
 * 原币与本位币（按结算单汇率）分别按权重分摊，各自合计等于明细金额。导入、确认、修改分摊方式时重新分摊（先删后插）
 */
@Service
@RequiredArgsConstructor
public class EcomFeeAllocationService {

    private final EcomFeeAllocationMapper allocationMapper;
    private final EcomSettlementItemMapper itemMapper;

    public List<EcomFeeAllocation> list(Long settlementId) {
        return allocationMapper.selectBySettlement(settlementId);
    }

    public void deleteBySettlement(Long settlementId) {
        allocationMapper.delete(Wrappers.<EcomFeeAllocation>lambdaQuery().eq(EcomFeeAllocation::getSettlementId, settlementId));
    }

    /** 重新分摊结算单中不挂订单的费用，返回分摊结果行数（调用方负责事务） */
    public int allocate(EcomSettlement s) {
        FeeAllocMethod override = s.getAllocMethod();
        if (override != null && override != FeeAllocMethod.QTY && override != FeeAllocMethod.SALES) {
            throw new BizException("ecom.error.fee.allocMethodInvalid");
        }
        deleteBySettlement(s.getId());
        List<EcomSettlementItem> items = itemMapper.selectUnlinked(s.getId()).stream()
                .filter(i -> i.getItemType() != null && i.getItemType().allocatable() && Decimals.nz(i.getAmount()).signum() != 0).toList();
        if (items.isEmpty()) {
            return 0;
        }
        Map<String, BigDecimal[]> basis = new LinkedHashMap<>();
        for (Map<String, Object> m : allocationMapper.selectSkuBasis(s.getStoreId(), s.getPeriodStart().atStartOfDay(), s.getPeriodEnd().plusDays(1).atStartOfDay())) {
            basis.put((String) m.get("sellerSku"), new BigDecimal[]{dec(m.get("qty")), dec(m.get("sales"))});
        }
        BigDecimal rate = Decimals.nz(s.getExchangeRate());
        List<EcomFeeAllocation> rows = new ArrayList<>();
        for (EcomSettlementItem it : items) {
            BigDecimal amount = Decimals.money(it.getAmount());
            BigDecimal base = Decimals.money(amount.multiply(rate));
            if (StringUtils.hasText(it.getSellerSku())) {
                rows.add(row(s, it, it.getSellerSku().trim(), FeeAllocMethod.SKU, null, amount, base));
                continue;
            }
            FeeAllocMethod method = override != null ? override : it.getItemType().defaultAllocMethod();
            int idx = method == FeeAllocMethod.QTY ? 0 : 1;
            Map<String, BigDecimal> weights = new LinkedHashMap<>();
            basis.forEach((sku, v) -> {
                if (v[idx].signum() > 0) {
                    weights.put(sku, v[idx]);
                }
            });
            if (weights.isEmpty()) {
                rows.add(row(s, it, null, FeeAllocMethod.STORE, null, amount, base));
                continue;
            }
            BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal restFc = amount, restBase = base;
            int n = 0;
            for (Map.Entry<String, BigDecimal> e : weights.entrySet()) {
                n++;
                BigDecimal fc, b;
                if (n == weights.size()) {
                    fc = restFc;
                    b = restBase;
                } else {
                    BigDecimal ratio = e.getValue().divide(total, 10, RoundingMode.HALF_UP);
                    fc = Decimals.money(amount.multiply(ratio));
                    b = Decimals.money(base.multiply(ratio));
                    restFc = restFc.subtract(fc);
                    restBase = restBase.subtract(b);
                }
                rows.add(row(s, it, e.getKey(), method, e.getValue(), fc, b));
            }
        }
        for (int i = 0; i < rows.size(); i += 500) {
            allocationMapper.insertBatch(rows.subList(i, Math.min(rows.size(), i + 500)));
        }
        return rows.size();
    }

    private static EcomFeeAllocation row(EcomSettlement s, EcomSettlementItem it, String sku, FeeAllocMethod method, BigDecimal basis,
                                         BigDecimal amount, BigDecimal base) {
        EcomFeeAllocation a = new EcomFeeAllocation();
        a.setSettlementId(s.getId());
        a.setSettlementItemId(it.getId());
        a.setStoreId(s.getStoreId());
        a.setSellerSku(sku);
        a.setItemType(it.getItemType());
        a.setMethod(method);
        a.setAllocDate(it.getPostedDate() != null ? it.getPostedDate() : s.getPeriodEnd());
        a.setBasisValue(basis == null ? null : basis.setScale(4, RoundingMode.HALF_UP));
        a.setAmount(amount);
        a.setAmountBase(base);
        return a;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
