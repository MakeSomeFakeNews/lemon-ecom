package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomProfitMapper;
import com.lemon.erp.modules.ecom.mapper.EcomStoreMapper;
import com.lemon.erp.modules.ecom.mapper.EcomTaxRateMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.TaxType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 电商税务：税率设置（ecom_tax_rate）、订单写入时匹配税率（{@link #resolve}）、按日期范围重算订单税额（{@link #reapply}）、
 * VAT 汇总（按收件国家 × 月份：应申报销售额、销项税、平台代扣、头程关税与进口增值税）
 */
@Service
@RequiredArgsConstructor
public class EcomTaxService {

    private final EcomTaxRateMapper rateMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomStoreMapper storeMapper;
    private final EcomProfitMapper profitMapper;
    private final DictLookup dictLookup;

    public PageResult<EcomTaxRate> page(long current, long size, String country, TaxType taxType, EnableStatus status) {
        Page<EcomTaxRate> page = Page.of(current, size);
        rateMapper.selectPage(page, Wrappers.<EcomTaxRate>lambdaQuery()
                .eq(StringUtils.hasText(country), EcomTaxRate::getCountry, country == null ? null : country.trim().toUpperCase(Locale.ROOT))
                .eq(taxType != null, EcomTaxRate::getTaxType, taxType)
                .eq(status != null, EcomTaxRate::getStatus, status)
                .orderByAsc(EcomTaxRate::getCountry).orderByAsc(EcomTaxRate::getMarketplace));
        return PageResult.of(page);
    }

    public Long create(EcomTaxRate r) {
        r.setId(null);
        normalize(r);
        checkUnique(r, null);
        r.setBuiltin(0);
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        rateMapper.insert(r);
        return r.getId();
    }

    public void update(Long id, EcomTaxRate r) {
        EcomTaxRate old = get(id);
        normalize(r);
        checkUnique(r, id);
        r.setId(id);
        r.setBuiltin(old.getBuiltin());
        rateMapper.updateById(r);
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        rateMapper.update(Wrappers.<EcomTaxRate>lambdaUpdate().set(EcomTaxRate::getStatus, status).eq(EcomTaxRate::getId, id));
    }

    public void delete(List<Long> ids) {
        rateMapper.deleteByIds(ids);
    }

    private EcomTaxRate get(Long id) {
        EcomTaxRate r = rateMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.tax.rateNotFound");
        }
        return r;
    }

    private void normalize(EcomTaxRate r) {
        r.setCountry(r.getCountry().trim().toUpperCase(Locale.ROOT));
        r.setMarketplace(r.getMarketplace() == null ? "" : r.getMarketplace().trim());
        dictLookup.check(EcomConstants.MARKETPLACE_DICT, r.getMarketplace(), "ecom.label.marketplace");
        r.setFacilitator(r.getFacilitator() != null && r.getFacilitator() == 1 ? 1 : 0);
        r.setRate(Decimals.money(r.getRate()));
    }

    private void checkUnique(EcomTaxRate r, Long excludeId) {
        if (rateMapper.exists(Wrappers.<EcomTaxRate>lambdaQuery().eq(EcomTaxRate::getCountry, r.getCountry())
                .eq(EcomTaxRate::getMarketplace, r.getMarketplace()).ne(excludeId != null, EcomTaxRate::getId, excludeId))) {
            throw new BizException("ecom.error.tax.rateExists", r.getCountry(), r.getMarketplace());
        }
    }

    /**
     * 订单适用的税率：收件国家相同的启用税率中，店铺站点相同的优先，其次为全部站点（站点为空）；没有时返回 null（不拆税）
     */
    public EcomTaxRate resolve(String marketplace, String country) {
        if (!StringUtils.hasText(country)) {
            return null;
        }
        List<EcomTaxRate> rates = rateMapper.selectList(Wrappers.<EcomTaxRate>lambdaQuery()
                .eq(EcomTaxRate::getCountry, country.trim().toUpperCase(Locale.ROOT)).eq(EcomTaxRate::getStatus, EnableStatus.ENABLED));
        String mkt = marketplace == null ? "" : marketplace;
        return rates.stream().filter(r -> mkt.equals(r.getMarketplace())).findFirst()
                .orElse(rates.stream().filter(r -> r.getMarketplace() == null || r.getMarketplace().isEmpty()).findFirst().orElse(null));
    }

    /**
     * 按当前税率重算一段下单时间内订单的税率、税额与利润（修改税率后使用），取消的订单不处理
     *
     * @return 处理的订单数
     */
    @Transactional
    public int reapply(LocalDate begin, LocalDate end, Long storeId) {
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("ecom.error.tax.rangeRequired");
        }
        Map<Long, EcomStore> stores = new HashMap<>();
        Map<String, EcomTaxRate> cache = new HashMap<>();
        int count = 0;
        long current = 1;
        while (true) {
            Page<EcomOrder> page = Page.of(current, 500);
            page.setSearchCount(false);
            orderMapper.selectPage(page, Wrappers.<EcomOrder>lambdaQuery().ge(EcomOrder::getOrderTime, begin.atStartOfDay())
                    .lt(EcomOrder::getOrderTime, end.plusDays(1).atStartOfDay()).eq(storeId != null, EcomOrder::getStoreId, storeId)
                    .ne(EcomOrder::getStatus, EcomOrderStatus.CANCELLED).orderByAsc(EcomOrder::getId));
            for (EcomOrder o : page.getRecords()) {
                EcomStore s = stores.computeIfAbsent(o.getStoreId(), storeMapper::selectById);
                String mkt = s == null ? "" : s.getMarketplace();
                EcomTaxRate rate = cache.computeIfAbsent(mkt + "|" + o.getShipCountry(), k -> {
                    EcomTaxRate r = resolve(mkt, o.getShipCountry());
                    return r == null ? NONE : r;
                });
                EcomOrderTax.assign(o, rate == NONE ? null : rate);
                BigDecimal profit = EcomOrderService.profitOf(o);
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getTaxType, o.getTaxType()).set(EcomOrder::getVatRate, o.getVatRate())
                        .set(EcomOrder::getTaxFacilitator, o.getTaxFacilitator()).set(EcomOrder::getVatAmount, o.getVatAmount())
                        .set(EcomOrder::getWithheldTax, o.getWithheldTax()).set(EcomOrder::getProfit, profit).eq(EcomOrder::getId, o.getId()));
                count++;
            }
            if (page.getRecords().size() < 500) {
                break;
            }
            current++;
        }
        return count;
    }

    private static final EcomTaxRate NONE = new EcomTaxRate();

    /**
     * VAT 汇总：订单（有税种的、未取消，按下单月份）的应申报销售额（不含税、扣除退款）、销项税、平台代扣；
     * 进项为已发运头程货件（按发运月份、店铺站点作为国家）的关税和进口增值税；应缴 = 销项税 − 进口增值税（本位币）
     */
    public List<EcomProfitViews.VatRow> vatSummary(LocalDate begin, LocalDate end, Long storeId) {
        if (begin == null || end == null) {
            throw new BizException("ecom.error.tax.rangeRequired");
        }
        Map<String, EcomProfitViews.VatRow> rows = new LinkedHashMap<>();
        Map<String, Integer> sellerRows = new HashMap<>();
        for (EcomProfitViews.VatRow r : profitMapper.selectVatSales(begin.atStartOfDay(), end.plusDays(1).atStartOfDay(), storeId)) {
            // 同一国家 × 月份 × 税种 × 是否代扣 合并不同币种（币种不同时不显示原币）
            String key = r.getCountry() + "|" + r.getPeriod() + "|" + r.getTaxType() + "|" + r.getFacilitator();
            EcomProfitViews.VatRow acc = rows.get(key);
            if (acc == null) {
                rows.put(key, r);
                if (!Objects.equals(r.getFacilitator(), 1)) {
                    sellerRows.putIfAbsent(r.getCountry() + "|" + r.getPeriod(), 1);
                }
                continue;
            }
            boolean sameCurrency = Objects.equals(acc.getCurrency(), r.getCurrency());
            acc.setOrders(acc.getOrders() + r.getOrders());
            acc.setTaxableBase(add(acc.getTaxableBase(), r.getTaxableBase()));
            acc.setVatBase(add(acc.getVatBase(), r.getVatBase()));
            acc.setWithheldBase(add(acc.getWithheldBase(), r.getWithheldBase()));
            if (sameCurrency) {
                acc.setTaxableFc(add(acc.getTaxableFc(), r.getTaxableFc()));
                acc.setVatFc(add(acc.getVatFc(), r.getVatFc()));
                acc.setWithheldFc(add(acc.getWithheldFc(), r.getWithheldFc()));
            } else {
                acc.setCurrency(null);
                acc.setTaxableFc(null);
                acc.setVatFc(null);
                acc.setWithheldFc(null);
            }
        }
        for (EcomProfitViews.VatRow in : profitMapper.selectVatInputs(begin, end, storeId)) {
            String ck = in.getCountry() + "|" + in.getPeriod();
            // 进项挂在该国家月份需卖家申报的行上；没有时单独一行
            EcomProfitViews.VatRow target = rows.values().stream()
                    .filter(r -> (r.getCountry() + "|" + r.getPeriod()).equals(ck) && (!sellerRows.containsKey(ck) || !Objects.equals(r.getFacilitator(), 1)))
                    .findFirst().orElse(null);
            if (target == null) {
                in.setOrders(0L);
                rows.put(ck + "|INPUT", in);
            } else {
                target.setDuty(add(target.getDuty(), in.getDuty()));
                target.setImportVat(add(target.getImportVat(), in.getImportVat()));
            }
        }
        List<EcomProfitViews.VatRow> list = new ArrayList<>(rows.values());
        for (EcomProfitViews.VatRow r : list) {
            r.setMonth(r.getPeriod() == null ? null : r.getPeriod() / 100 + "-" + String.format("%02d", r.getPeriod() % 100));
            r.setTaxableBase(Decimals.money(Decimals.nz(r.getTaxableBase())));
            r.setVatBase(Decimals.money(Decimals.nz(r.getVatBase())));
            r.setWithheldBase(Decimals.money(Decimals.nz(r.getWithheldBase())));
            r.setDuty(Decimals.money(Decimals.nz(r.getDuty())));
            r.setImportVat(Decimals.money(Decimals.nz(r.getImportVat())));
            r.setPayable(Objects.equals(r.getFacilitator(), 1) ? BigDecimal.ZERO.subtract(r.getImportVat()) : r.getVatBase().subtract(r.getImportVat()));
            if (r.getTaxableFc() != null) r.setTaxableFc(Decimals.money(r.getTaxableFc()));
            if (r.getVatFc() != null) r.setVatFc(Decimals.money(r.getVatFc()));
            if (r.getWithheldFc() != null) r.setWithheldFc(Decimals.money(r.getWithheldFc()));
        }
        list.sort(Comparator.comparing(EcomProfitViews.VatRow::getCountry, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EcomProfitViews.VatRow::getPeriod, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return Decimals.nz(a).add(Decimals.nz(b));
    }
}
