package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.mapper.EcomReplenishMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 补货建议（平台仓 / FBA）：日均销量 = 最近 30 天销量 ÷ 30；可售天数 = (平台仓库存 + 在途) ÷ 日均；
 * 建议补货 = 日均 × (补货周期 + 安全天数) − 平台仓库存 − 在途；断货日期在补货周期内为紧急，在补货周期 + 安全天数内为预警
 */
@Service
@RequiredArgsConstructor
public class EcomReplenishService {

    private static final int DEFAULT_LEAD_DAYS = 30;
    private static final int DEFAULT_SAFETY_DAYS = 15;

    private final EcomReplenishMapper replenishMapper;

    /**
     * @param level 只看某个紧急程度，为空全部
     */
    public List<ReplenishRow> rows(Long storeId, String keyword, ReplenishLevel level) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(EcomConstants.VELOCITY_DAYS).atStartOfDay();
        List<ReplenishRow> rows = replenishMapper.selectRows(storeId, keyword, since);
        for (ReplenishRow r : rows) {
            compute(r, today);
        }
        return rows.stream().filter(r -> level == null || r.getLevel() == level)
                .sorted(Comparator.comparing((ReplenishRow r) -> r.getLevel().ordinal()).reversed()
                        .thenComparing(r -> r.getCoverDays() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : r.getCoverDays()))
                .toList();
    }

    static void compute(ReplenishRow r, LocalDate today) {
        int lead = r.getLeadDays() == null ? DEFAULT_LEAD_DAYS : r.getLeadDays();
        int safety = r.getSafetyDays() == null ? DEFAULT_SAFETY_DAYS : r.getSafetyDays();
        r.setLeadDays(lead);
        r.setSafetyDays(safety);
        BigDecimal sales = Decimals.nz(r.getSales30());
        BigDecimal daily = sales.divide(BigDecimal.valueOf(EcomConstants.VELOCITY_DAYS), 4, RoundingMode.HALF_UP);
        r.setDailySales(daily);
        BigDecimal platform = Decimals.nz(r.getPlatformQty());
        BigDecimal transit = Decimals.nz(r.getInTransitQty());
        BigDecimal available = platform.add(transit);
        if (daily.signum() <= 0) {
            r.setCoverDays(null);
            r.setSuggestQty(BigDecimal.ZERO);
            r.setStockoutDate(null);
            r.setLevel(available.signum() > 0 || sales.signum() == 0 ? ReplenishLevel.OK : ReplenishLevel.URGENT);
            return;
        }
        BigDecimal cover = available.divide(daily, 1, RoundingMode.DOWN);
        r.setCoverDays(cover);
        BigDecimal target = daily.multiply(BigDecimal.valueOf(lead + safety));
        BigDecimal suggest = target.subtract(available).setScale(0, RoundingMode.CEILING);
        r.setSuggestQty(suggest.signum() > 0 ? suggest : BigDecimal.ZERO);
        r.setStockoutDate(today.plusDays(cover.longValue()));
        if (cover.compareTo(BigDecimal.valueOf(lead)) <= 0) {
            r.setLevel(ReplenishLevel.URGENT);
        } else if (cover.compareTo(BigDecimal.valueOf(lead + safety)) <= 0) {
            r.setLevel(ReplenishLevel.WARN);
        } else {
            r.setLevel(ReplenishLevel.OK);
        }
    }

    public long countUrgent() {
        return rows(null, null, ReplenishLevel.URGENT).size();
    }
}
