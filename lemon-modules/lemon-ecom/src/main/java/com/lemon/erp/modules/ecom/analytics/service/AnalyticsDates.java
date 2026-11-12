package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.common.exception.BizException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 日期范围的默认值与校验：结束为空取今天，开始为空取结束前 defaultDays − 1 天；开始不能晚于结束，跨度不超过 maxDays 天
 */
public record AnalyticsDates(LocalDate begin, LocalDate end) {

    public static AnalyticsDates of(LocalDate begin, LocalDate end, int defaultDays, int maxDays) {
        LocalDate e = end != null ? end : LocalDate.now();
        LocalDate b = begin != null ? begin : e.minusDays(defaultDays - 1L);
        if (b.isAfter(e)) {
            throw new BizException("ecom.error.analytics.rangeInvalid");
        }
        if (ChronoUnit.DAYS.between(b, e) + 1 > maxDays) {
            throw new BizException("ecom.error.analytics.rangeTooLong", maxDays);
        }
        return new AnalyticsDates(b, e);
    }

    /** 天数（含首尾） */
    public int days() {
        return (int) ChronoUnit.DAYS.between(begin, end) + 1;
    }

    /** 下单时间条件 [begin 0 点, end 次日 0 点) */
    public LocalDateTime from() {
        return begin.atStartOfDay();
    }

    public LocalDateTime to() {
        return end.plusDays(1).atStartOfDay();
    }

    /** 环比：紧挨在前面、同样长度的区间 */
    public AnalyticsDates previous() {
        int n = days();
        return new AnalyticsDates(begin.minusDays(n), begin.minusDays(1));
    }

    /** 同比：去年同期 */
    public AnalyticsDates lastYear() {
        return new AnalyticsDates(begin.minusYears(1), end.minusYears(1));
    }
}
