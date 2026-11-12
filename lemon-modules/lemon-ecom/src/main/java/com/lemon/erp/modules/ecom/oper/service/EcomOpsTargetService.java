package com.lemon.erp.modules.ecom.oper.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.entity.EcomOpsTarget;
import com.lemon.erp.modules.ecom.oper.mapper.EcomOperReportMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomOpsTargetMapper;
import com.lemon.erp.modules.ecom.oper.model.EcomAchievementDimension;
import com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 运营目标与绩效：按月为店铺 / 运营人员设置销售额、利润、订单数目标（本位币）；达成率按店铺、按负责人（运营人员目标对比其负责店铺的合计）、
 * 按平台（店铺目标按平台合计）统计；绩效排行按负责人汇总销售额、利润、利润率、广告 ACOS、退货率、差评数。
 * 店铺数据权限下只统计可见店铺
 */
@Service
@RequiredArgsConstructor
public class EcomOpsTargetService {

    /** 绩效中差评的评分上限 */
    static final int BAD_RATING = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EcomOpsTargetMapper targetMapper;
    private final EcomOperReportMapper reportMapper;
    private final EcomStoreService storeService;
    private final UserLookup userLookup;
    private final ExchangeRateProvider exchangeRateProvider;

    static YearMonth month(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BizException("ecom.error.oper.monthInvalid", month);
        }
    }

    // ---------------------------------------------------------------- 目标设置

    /** 目标设置页：店铺（可见的全部）或运营人员（有负责店铺或已设置目标的用户）及本月目标、上月实际 */
    public List<OperViews.TargetEditRow> editRows(String monthText, EcomOpsTargetType type) {
        YearMonth month = month(monthText);
        Map<Long, EcomOpsTarget> targets = targetsOf(month, type);
        List<Map<String, Object>> stores = reportMapper.selectStores();
        Map<Long, StoreTotals> last = totals(month.minusMonths(1));
        List<OperViews.TargetEditRow> rows = new ArrayList<>();
        if (type == EcomOpsTargetType.STORE) {
            for (Map<String, Object> s : stores) {
                OperViews.TargetEditRow r = new OperViews.TargetEditRow();
                r.setRefId(id(s.get("storeId")));
                r.setRefName((String) s.get("name"));
                r.setCode((String) s.get("code"));
                r.setPlatform(platform(s.get("platform")));
                r.setMarketplace((String) s.get("marketplace"));
                r.setOwnerId(idOrNull(s.get("ownerId")));
                r.setOwnerName((String) s.get("ownerName"));
                StoreTotals t = last.get(r.getRefId());
                if (t != null) {
                    r.setLastSales(t.sales);
                    r.setLastProfit(t.profit);
                    r.setLastOrders(t.orders);
                }
                apply(r, targets.remove(r.getRefId()));
                rows.add(r);
            }
        } else {
            Map<Long, OperViews.TargetEditRow> byUser = new LinkedHashMap<>();
            for (Map<String, Object> u : reportMapper.selectOwners()) {
                OperViews.TargetEditRow r = new OperViews.TargetEditRow();
                r.setRefId(id(u.get("userId")));
                r.setRefName((String) u.get("nickname"));
                byUser.put(r.getRefId(), r);
            }
            for (Long uid : targets.keySet()) {
                byUser.computeIfAbsent(uid, k -> {
                    OperViews.TargetEditRow r = new OperViews.TargetEditRow();
                    r.setRefId(k);
                    r.setRefName(userLookup.nameOf(k));
                    return r;
                });
            }
            for (Map<String, Object> s : stores) {
                OperViews.TargetEditRow r = byUser.get(idOrNull(s.get("ownerId")));
                StoreTotals t = last.get(id(s.get("storeId")));
                if (r != null && t != null) {
                    r.setLastSales(r.getLastSales().add(t.sales));
                    r.setLastProfit(r.getLastProfit().add(t.profit));
                    r.setLastOrders(r.getLastOrders() + t.orders);
                }
            }
            byUser.values().forEach(r -> apply(r, targets.get(r.getRefId())));
            rows.addAll(byUser.values());
        }
        return rows;
    }

    private static void apply(OperViews.TargetEditRow r, EcomOpsTarget t) {
        if (t != null) {
            r.setSalesTarget(t.getSalesTarget());
            r.setProfitTarget(t.getProfitTarget());
            r.setOrderTarget(t.getOrderTarget());
            r.setRemark(t.getRemark());
        }
    }

    private Map<Long, EcomOpsTarget> targetsOf(YearMonth month, EcomOpsTargetType type) {
        Map<Long, EcomOpsTarget> map = new LinkedHashMap<>();
        targetMapper.selectList(Wrappers.<EcomOpsTarget>lambdaQuery().eq(EcomOpsTarget::getTargetMonth, month.toString())
                .eq(EcomOpsTarget::getTargetType, type)).forEach(t -> map.put(t.getRefId(), t));
        return map;
    }

    /** 保存一个月的目标：只处理提交的对象，三项都为 0 / 空时删除该对象的目标 */
    @Transactional
    public void save(OperRequests.TargetSaveRequest req) {
        YearMonth month = month(req.month());
        Map<Long, EcomOpsTarget> existing = targetsOf(month, req.targetType());
        for (OperRequests.TargetRow row : req.rows() == null ? List.<OperRequests.TargetRow>of() : req.rows()) {
            if (req.targetType() == EcomOpsTargetType.STORE) {
                storeService.get(row.refId());
            } else {
                userLookup.require(row.refId(), "ecom.error.oper.userNotFound");
            }
            BigDecimal sales = nz(row.salesTarget());
            BigDecimal profit = nz(row.profitTarget());
            int orders = row.orderTarget() == null ? 0 : row.orderTarget();
            EcomOpsTarget old = existing.get(row.refId());
            if (sales.signum() == 0 && profit.signum() == 0 && orders == 0) {
                if (old != null) {
                    targetMapper.deleteById(old.getId());
                }
                continue;
            }
            EcomOpsTarget t = old != null ? old : new EcomOpsTarget();
            t.setTargetMonth(month.toString());
            t.setTargetType(req.targetType());
            t.setRefId(row.refId());
            t.setStoreId(req.targetType() == EcomOpsTargetType.STORE ? row.refId() : null);
            t.setSalesTarget(sales);
            t.setProfitTarget(profit);
            t.setOrderTarget(orders);
            t.setRemark(StringUtils.hasText(row.remark()) ? row.remark().trim() : null);
            if (old == null) {
                t.setCreateBy(SecurityUtils.getUserIdOrNull());
                targetMapper.insert(t);
            } else {
                targetMapper.updateById(t);
                if (t.getRemark() == null) {
                    targetMapper.update(Wrappers.<EcomOpsTarget>lambdaUpdate().set(EcomOpsTarget::getRemark, null).eq(EcomOpsTarget::getId, t.getId()));
                }
            }
        }
    }

    /** 复制上月目标：本月已设置的对象不覆盖，返回复制的条数 */
    @Transactional
    public int copyLastMonth(OperRequests.TargetCopyRequest req) {
        YearMonth month = month(req.month());
        Map<Long, EcomOpsTarget> existing = targetsOf(month, req.targetType());
        int n = 0;
        for (EcomOpsTarget last : targetsOf(month.minusMonths(1), req.targetType()).values()) {
            if (existing.containsKey(last.getRefId())) {
                continue;
            }
            EcomOpsTarget t = new EcomOpsTarget();
            t.setTargetMonth(month.toString());
            t.setTargetType(req.targetType());
            t.setRefId(last.getRefId());
            t.setStoreId(last.getStoreId());
            t.setSalesTarget(last.getSalesTarget());
            t.setProfitTarget(last.getProfitTarget());
            t.setOrderTarget(last.getOrderTarget());
            t.setRemark(last.getRemark());
            t.setCreateBy(SecurityUtils.getUserIdOrNull());
            targetMapper.insert(t);
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- 达成率

    /** 月度达成率：按店铺、按负责人或按平台 */
    public OperViews.Achievement achievement(String monthText, EcomAchievementDimension dimension) {
        YearMonth month = month(monthText);
        Map<Long, StoreTotals> actual = totals(month);
        List<Map<String, Object>> stores = reportMapper.selectStores();
        Map<Long, EcomOpsTarget> storeTargets = targetsOf(month, EcomOpsTargetType.STORE);
        Map<String, OperViews.AchievementRow> rows = new LinkedHashMap<>();
        if (dimension == EcomAchievementDimension.OWNER) {
            Map<Long, EcomOpsTarget> userTargets = targetsOf(month, EcomOpsTargetType.USER);
            for (Map<String, Object> s : stores) {
                Long owner = idOrNull(s.get("ownerId"));
                OperViews.AchievementRow r = rows.computeIfAbsent(owner == null ? "-" : String.valueOf(owner), k -> {
                    OperViews.AchievementRow x = new OperViews.AchievementRow();
                    x.setKey(k);
                    x.setName((String) s.get("ownerName"));
                    return x;
                });
                r.setStoreCount(r.getStoreCount() + 1);
                add(r, actual.get(id(s.get("storeId"))));
            }
            for (EcomOpsTarget t : userTargets.values()) {
                OperViews.AchievementRow r = rows.computeIfAbsent(String.valueOf(t.getRefId()), k -> {
                    OperViews.AchievementRow x = new OperViews.AchievementRow();
                    x.setKey(k);
                    x.setName(userLookup.nameOf(t.getRefId()));
                    return x;
                });
                addTarget(r, t);
            }
        } else {
            for (Map<String, Object> s : stores) {
                Long storeId = id(s.get("storeId"));
                EcomOpsTarget t = storeTargets.get(storeId);
                StoreTotals a = actual.get(storeId);
                if (dimension == EcomAchievementDimension.STORE && t == null && a == null) {
                    continue;
                }
                EcomPlatform platform = platform(s.get("platform"));
                String key = dimension == EcomAchievementDimension.STORE ? String.valueOf(storeId) : String.valueOf(platform);
                OperViews.AchievementRow r = rows.computeIfAbsent(key, k -> {
                    OperViews.AchievementRow x = new OperViews.AchievementRow();
                    x.setKey(k);
                    x.setName(dimension == EcomAchievementDimension.STORE ? (String) s.get("name") : null);
                    x.setPlatform(platform);
                    return x;
                });
                r.setStoreCount(r.getStoreCount() + 1);
                add(r, a);
                addTarget(r, t);
            }
        }
        OperViews.AchievementRow total = new OperViews.AchievementRow();
        total.setKey("total");
        for (OperViews.AchievementRow r : rows.values()) {
            rates(r);
            total.setSalesTarget(total.getSalesTarget().add(r.getSalesTarget()));
            total.setSalesActual(total.getSalesActual().add(r.getSalesActual()));
            total.setProfitTarget(total.getProfitTarget().add(r.getProfitTarget()));
            total.setProfitActual(total.getProfitActual().add(r.getProfitActual()));
            total.setOrderTarget(total.getOrderTarget() + r.getOrderTarget());
            total.setOrderActual(total.getOrderActual() + r.getOrderActual());
            total.setStoreCount(total.getStoreCount() + r.getStoreCount());
        }
        rates(total);
        List<OperViews.AchievementRow> list = new ArrayList<>(rows.values());
        list.sort(Comparator.comparing(OperViews.AchievementRow::getSalesActual).reversed());
        return new OperViews.Achievement(month.toString(), timeProgress(month), total, list);
    }

    private static void add(OperViews.AchievementRow r, StoreTotals a) {
        if (a != null) {
            r.setSalesActual(r.getSalesActual().add(a.sales));
            r.setProfitActual(r.getProfitActual().add(a.profit));
            r.setOrderActual(r.getOrderActual() + a.orders);
        }
    }

    private static void addTarget(OperViews.AchievementRow r, EcomOpsTarget t) {
        if (t != null) {
            r.setSalesTarget(r.getSalesTarget().add(nz(t.getSalesTarget())));
            r.setProfitTarget(r.getProfitTarget().add(nz(t.getProfitTarget())));
            r.setOrderTarget(r.getOrderTarget() + (t.getOrderTarget() == null ? 0 : t.getOrderTarget()));
        }
    }

    private static void rates(OperViews.AchievementRow r) {
        r.setSalesRate(rate(r.getSalesActual(), r.getSalesTarget()));
        r.setProfitRate(rate(r.getProfitActual(), r.getProfitTarget()));
        r.setOrderRate(rate(BigDecimal.valueOf(r.getOrderActual()), BigDecimal.valueOf(r.getOrderTarget())));
    }

    /** 达成率 %：目标为 0 时为空 */
    static BigDecimal rate(BigDecimal actual, BigDecimal target) {
        if (target == null || target.signum() <= 0) {
            return null;
        }
        return actual.multiply(HUNDRED).divide(target, 2, RoundingMode.HALF_UP);
    }

    /** 时间进度 %：本月按今天计算，过去的月份 100，未来的月份 0 */
    static BigDecimal timeProgress(YearMonth month) {
        YearMonth now = YearMonth.now();
        if (month.isBefore(now)) {
            return HUNDRED;
        }
        if (month.isAfter(now)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(LocalDate.now().getDayOfMonth()).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 绩效

    /**
     * 运营人员绩效：[begin, end] 期间按负责人汇总（没有负责人的店铺不计入），按销售额排名；查询范围正好是一个自然月时附带销售额目标与达成率。
     * 广告花费、广告销售额按期初所在月汇率折算本位币
     */
    public List<OperViews.PerformanceRow> performance(LocalDate begin, LocalDate end) {
        if (end.isBefore(begin)) {
            throw new BizException("ecom.error.oper.dateRangeInvalid");
        }
        Map<Long, StoreTotals> orders = totals(begin, end);
        Map<Long, Map<String, Object>> ads = byStore(reportMapper.selectAdTotals(begin, end));
        Map<Long, Long> returns = counts(reportMapper.selectReturnCounts(begin.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        Map<Long, Long> bad = counts(reportMapper.selectBadReviewCounts(begin, end, BAD_RATING));
        Map<Long, OperViews.PerformanceRow> byOwner = new LinkedHashMap<>();
        Map<String, BigDecimal> rates = new HashMap<>();
        for (Map<String, Object> s : reportMapper.selectStores()) {
            Long owner = idOrNull(s.get("ownerId"));
            if (owner == null) {
                continue;
            }
            Long storeId = id(s.get("storeId"));
            OperViews.PerformanceRow r = byOwner.computeIfAbsent(owner, k -> {
                OperViews.PerformanceRow x = new OperViews.PerformanceRow();
                x.setUserId(k);
                x.setUserName((String) s.get("ownerName"));
                return x;
            });
            r.setStoreCount(r.getStoreCount() + 1);
            StoreTotals t = orders.get(storeId);
            if (t != null) {
                r.setOrders(r.getOrders() + t.orders);
                r.setSales(r.getSales().add(t.sales));
                r.setProfit(r.getProfit().add(t.profit));
            }
            Map<String, Object> ad = ads.get(storeId);
            if (ad != null) {
                String currency = (String) s.get("currency");
                BigDecimal rate = rates.computeIfAbsent(currency, c -> {
                    BigDecimal x = exchangeRateProvider.rate(c, begin);
                    return x == null ? BigDecimal.ONE : x;
                });
                r.setAdSpend(r.getAdSpend().add(dec(ad.get("spend")).multiply(rate).setScale(2, RoundingMode.HALF_UP)));
                r.setAdSales(r.getAdSales().add(dec(ad.get("adSales")).multiply(rate).setScale(2, RoundingMode.HALF_UP)));
            }
            r.setReturns(r.getReturns() + returns.getOrDefault(storeId, 0L));
            r.setBadReviews(r.getBadReviews() + bad.getOrDefault(storeId, 0L));
        }
        boolean wholeMonth = begin.getDayOfMonth() == 1 && end.equals(begin.withDayOfMonth(begin.lengthOfMonth()));
        Map<Long, EcomOpsTarget> targets = wholeMonth ? targetsOf(YearMonth.from(begin), EcomOpsTargetType.USER) : Map.of();
        List<OperViews.PerformanceRow> list = new ArrayList<>(byOwner.values());
        for (OperViews.PerformanceRow r : list) {
            r.setProfitRate(r.getSales().signum() == 0 ? null : r.getProfit().multiply(HUNDRED).divide(r.getSales(), 2, RoundingMode.HALF_UP));
            r.setAcos(r.getAdSales().signum() == 0 ? null : r.getAdSpend().multiply(HUNDRED).divide(r.getAdSales(), 2, RoundingMode.HALF_UP));
            r.setReturnRate(r.getOrders() == 0 ? null : BigDecimal.valueOf(r.getReturns()).multiply(HUNDRED)
                    .divide(BigDecimal.valueOf(r.getOrders()), 2, RoundingMode.HALF_UP));
            EcomOpsTarget t = targets.get(r.getUserId());
            if (t != null) {
                r.setSalesTarget(t.getSalesTarget());
                r.setSalesRate(rate(r.getSales(), t.getSalesTarget()));
            }
        }
        list.sort(Comparator.comparing(OperViews.PerformanceRow::getSales).reversed().thenComparing(OperViews.PerformanceRow::getUserId));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }
        return list;
    }

    // ---------------------------------------------------------------- 工具

    /** 店铺在一段时间内的订单数、销售额、利润（本位币） */
    record StoreTotals(long orders, BigDecimal sales, BigDecimal profit) {
    }

    private Map<Long, StoreTotals> totals(YearMonth month) {
        return totals(month.atDay(1), month.atEndOfMonth());
    }

    private Map<Long, StoreTotals> totals(LocalDate begin, LocalDate end) {
        Map<Long, StoreTotals> map = new HashMap<>();
        for (Map<String, Object> r : reportMapper.selectOrderTotals(begin.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            map.put(id(r.get("storeId")), new StoreTotals(((Number) r.get("orders")).longValue(), dec(r.get("sales")), dec(r.get("profit"))));
        }
        return map;
    }

    private static Map<Long, Map<String, Object>> byStore(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        rows.forEach(r -> map.put(id(r.get("storeId")), r));
        return map;
    }

    private static Map<Long, Long> counts(List<Map<String, Object>> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(r -> map.put(id(r.get("storeId")), ((Number) r.get("cnt")).longValue()));
        return map;
    }

    private static Long id(Object v) {
        return ((Number) Objects.requireNonNull(v)).longValue();
    }

    private static Long idOrNull(Object v) {
        return v == null ? null : ((Number) v).longValue();
    }

    private static EcomPlatform platform(Object v) {
        return v == null ? null : EcomPlatform.valueOf(String.valueOf(v));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }
}
