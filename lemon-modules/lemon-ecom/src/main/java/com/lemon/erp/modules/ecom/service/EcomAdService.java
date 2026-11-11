package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.AdImportRow;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdDailyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 广告日报：导入广告报告（店铺 × 日期 × 活动 × SKU，重复导入覆盖），汇总 ACOS、点击率、单次点击成本
 */
@Service
@RequiredArgsConstructor
public class EcomAdService {

    private final EcomAdDailyMapper adMapper;
    private final EcomStoreService storeService;

    public PageResult<EcomAdDaily> page(long current, long size, Long storeId, LocalDate beginDate, LocalDate endDate, String keyword) {
        Page<EcomAdDaily> page = Page.of(current, size);
        adMapper.selectPageList(page, storeId, beginDate, endDate, keyword);
        return PageResult.of(page);
    }

    /** 导入一行：同一店铺、日期、活动、SKU 已有记录时覆盖 */
    @Transactional
    public void importRow(AdImportRow r) {
        EcomStore store = storeService.findByCode(r.getStoreCode());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", r.getStoreCode());
        }
        String campaign = r.getCampaign() == null ? "" : r.getCampaign().trim();
        String sku = r.getSellerSku() == null ? "" : r.getSellerSku().trim();
        EcomAdDaily existing = adMapper.selectOne(Wrappers.<EcomAdDaily>lambdaQuery().eq(EcomAdDaily::getStoreId, store.getId())
                .eq(EcomAdDaily::getAdDate, r.getAdDate()).eq(EcomAdDaily::getCampaign, campaign).eq(EcomAdDaily::getSellerSku, sku));
        EcomAdDaily a = existing == null ? new EcomAdDaily() : existing;
        a.setStoreId(store.getId());
        a.setAdDate(r.getAdDate());
        a.setCampaign(campaign);
        a.setSellerSku(sku);
        a.setPlatformItemId(r.getPlatformItemId());
        a.setImpressions(r.getImpressions() == null ? 0L : r.getImpressions());
        a.setClicks(r.getClicks() == null ? 0L : r.getClicks());
        a.setSpend(Decimals.money(r.getSpend()));
        a.setCurrency(r.getCurrency() == null || r.getCurrency().isBlank() ? store.getCurrency() : r.getCurrency().trim().toUpperCase());
        a.setAdSales(Decimals.money(Decimals.nz(r.getAdSales())));
        a.setAdOrders(r.getAdOrders() == null ? 0 : r.getAdOrders());
        if (existing == null) {
            adMapper.insert(a);
        } else {
            adMapper.updateById(a);
        }
    }

    public void delete(List<Long> ids) {
        adMapper.deleteByIds(ids);
    }

    /**
     * 汇总：ACOS = 花费 / 广告销售额（%），点击率 = 点击 / 曝光（%），单次点击成本 = 花费 / 点击
     *
     * @param groupBy store / campaign / sku
     */
    public List<EcomReports.AdRow> summary(Long storeId, LocalDate beginDate, LocalDate endDate, String groupBy) {
        String g = "campaign".equals(groupBy) || "sku".equals(groupBy) ? groupBy : "store";
        List<EcomReports.AdRow> rows = adMapper.selectSummary(storeId, beginDate, endDate, g);
        for (EcomReports.AdRow r : rows) {
            r.setAcos(ratio(r.getSpend(), r.getAdSales(), true));
            r.setCtr(ratio(r.getClicks() == null ? null : BigDecimal.valueOf(r.getClicks()), r.getImpressions() == null ? null : BigDecimal.valueOf(r.getImpressions()), true));
            r.setCpc(ratio(r.getSpend(), r.getClicks() == null ? null : BigDecimal.valueOf(r.getClicks()), false));
        }
        return rows;
    }

    private static BigDecimal ratio(BigDecimal part, BigDecimal whole, boolean percent) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        BigDecimal v = part.divide(whole, 6, RoundingMode.HALF_UP);
        return percent ? v.multiply(Decimals.HUNDRED).setScale(2, RoundingMode.HALF_UP) : v.setScale(2, RoundingMode.HALF_UP);
    }
}
