package com.lemon.erp.modules.ecom.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.dto.DailyForecast;
import com.lemon.erp.modules.ecom.analytics.dto.ForecastDetail;
import com.lemon.erp.modules.ecom.analytics.dto.GenerateResult;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecast;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastEvent;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSetting;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSku;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomAnalyticsMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomForecastEventMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomForecastMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomForecastSettingMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomForecastSkuMapper;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 销量预测（{@link ForecastEngine}）：每个在售 SKU 用最近 {@link #HISTORY_DAYS} 天（从首次有销量的那天起）的日销量，
 * 按移动平均、加权移动平均、Holt、星期季节性预测从今天起 {@link #HORIZON} 天，叠加促销 / 节假日系数，用最近 28 天回测；
 * 默认用回测 MAPE 最小的方法，可按 SKU 手工指定。结果存 ecom_forecast（每天）与 ecom_forecast_sku（汇总），
 * 定时任务每天生成；分批（每批 {@link #BATCH} 个 SKU）计算，每批一个事务，一批失败时逐个重试，单个失败不影响其他。
 * <p>
 * 补货建议等按 {@link #dailyForecast} 取未来每天的预测量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomForecastService {

    /** 预测天数（存库的天数） */
    public static final int HORIZON = 90;
    /** 历史天数 */
    static final int HISTORY_DAYS = 120;
    static final int BATCH = 200;
    static final int INSERT_CHUNK = 1000;
    static final BigDecimal DEFAULT_ALPHA = new BigDecimal("0.3");
    static final BigDecimal DEFAULT_BETA = new BigDecimal("0.1");
    static final int DEFAULT_WINDOW = 28;
    static final int DEFAULT_SEASON_WEEKS = 8;

    private final EcomAnalyticsMapper analyticsMapper;
    private final EcomForecastMapper forecastMapper;
    private final EcomForecastSkuMapper skuMapper;
    private final EcomForecastEventMapper eventMapper;
    private final EcomForecastSettingMapper settingMapper;
    private final EcomStoreService storeService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 参数

    /** 当前租户的预测参数，没有保存过时为默认值（id 为空） */
    public EcomForecastSetting setting() {
        EcomForecastSetting s = Queries.first(settingMapper, Wrappers.<EcomForecastSetting>lambdaQuery().orderByAsc(EcomForecastSetting::getId));
        if (s != null) {
            return s;
        }
        EcomForecastSetting d = new EcomForecastSetting();
        d.setAlpha(DEFAULT_ALPHA);
        d.setBeta(DEFAULT_BETA);
        d.setWindowDays(DEFAULT_WINDOW);
        d.setSeasonWeeks(DEFAULT_SEASON_WEEKS);
        return d;
    }

    /** 保存预测参数（下次生成时生效） */
    public void saveSetting(EcomForecastSetting input) {
        EcomForecastSetting current = setting();
        input.setId(current.getId());
        if (current.getId() == null) {
            settingMapper.insert(input);
        } else {
            settingMapper.updateById(input);
        }
    }

    static ForecastEngine.Params params(EcomForecastSetting s) {
        return new ForecastEngine.Params(s.getAlpha().doubleValue(), s.getBeta().doubleValue(), s.getWindowDays(), s.getSeasonWeeks());
    }

    // ---------------------------------------------------------------- 促销 / 节假日

    /** 与 [from, to] 有重叠的事件（都为空时全部），storeId 不为空时只看该店铺和全部店铺的 */
    public List<EcomForecastEvent> events(LocalDate from, LocalDate to, Long storeId) {
        return eventMapper.selectOverlap(from, to, storeId);
    }

    public Long saveEvent(Long id, EcomForecastEvent e) {
        if (e.getStartDate().isAfter(e.getEndDate())) {
            throw new BizException("ecom.error.analytics.rangeInvalid");
        }
        if (e.getStoreId() != null) {
            storeService.get(e.getStoreId());
        }
        if (id == null) {
            e.setId(null);
            eventMapper.insert(e);
            return e.getId();
        }
        if (eventMapper.selectById(id) == null) {
            throw new BizException("ecom.error.analytics.eventNotFound");
        }
        e.setId(id);
        eventMapper.updateById(e);
        return id;
    }

    public void deleteEvent(Long id) {
        if (eventMapper.deleteById(id) == 0) {
            throw new BizException("ecom.error.analytics.eventNotFound");
        }
    }

    /** 店铺的事件系数：日期落在事件区间内的系数相乘（店铺为空的事件对所有店铺生效） */
    static ForecastEngine.Factors factorsOf(List<EcomForecastEvent> events, Long storeId) {
        List<EcomForecastEvent> mine = events.stream().filter(e -> e.getStoreId() == null || e.getStoreId().equals(storeId)).toList();
        if (mine.isEmpty()) {
            return ForecastEngine.Factors.NONE;
        }
        return d -> {
            double f = 1;
            for (EcomForecastEvent e : mine) {
                if (!d.isBefore(e.getStartDate()) && !d.isAfter(e.getEndDate())) {
                    f *= e.getFactor().doubleValue();
                }
            }
            return f;
        };
    }

    // ---------------------------------------------------------------- 生成

    /**
     * 生成预测：storeId 为空时全部在售 SKU（并清理已停售 SKU 的旧结果和已过去日期的预测），否则只生成该店铺的
     */
    public GenerateResult generate(Long storeId) {
        LocalDateTime runStart = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        GenerateResult r = generateListings(storeId, null, runStart);
        LocalDate today = LocalDate.now();
        forecastMapper.deleteBefore(today);
        if (storeId == null && r.failed() == 0) {
            forecastMapper.deleteGeneratedBefore(runStart);
            skuMapper.deleteGeneratedBefore(runStart);
        }
        return r;
    }

    /** 重新生成一个 SKU 的预测（修改了方法或事件后） */
    public void regenerate(Long listingId) {
        GenerateResult r = generateListings(null, List.of(listingId), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        if (r.failed() > 0) {
            throw new BizException("ecom.error.analytics.forecastFailed", r.firstError());
        }
        if (r.total() == 0) {
            throw new BizException("ecom.error.analytics.listingNotActive");
        }
    }

    /** 手工指定 SKU 的预测方法（为空恢复按回测自动选择），并重新生成 */
    @Transactional
    public void fixMethod(Long listingId, ForecastMethod method) {
        EcomForecastSku row = Queries.first(skuMapper, Wrappers.<EcomForecastSku>lambdaQuery().eq(EcomForecastSku::getListingId, listingId));
        if (row == null) {
            row = new EcomForecastSku();
            row.setListingId(listingId);
            row.setStoreId(listingInfo(listingId).getStoreId());
            row.setMethod(method != null ? method : ForecastMethod.MA);
            row.setFixedMethod(method);
            row.setGeneratedTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            skuMapper.insertBatch(List.of(row));
        } else {
            skuMapper.update(null, Wrappers.<EcomForecastSku>lambdaUpdate().set(EcomForecastSku::getFixedMethod, method)
                    .eq(EcomForecastSku::getListingId, listingId));
        }
        regenerate(listingId);
    }

    private GenerateResult generateListings(Long storeId, List<Long> listingIds, LocalDateTime runStart) {
        LocalDate today = runStart.toLocalDate();
        ForecastEngine.Params p = params(setting());
        List<EcomForecastEvent> events = events(today.minusDays(HISTORY_DAYS), today.plusDays(HORIZON), null);
        int total = 0;
        int failed = 0;
        String firstError = null;
        long after = 0;
        while (true) {
            List<AnalyticsRows.ListingRef> refs = analyticsMapper.selectActiveListings(storeId, listingIds, after, BATCH);
            if (refs.isEmpty()) {
                break;
            }
            after = refs.get(refs.size() - 1).getListingId();
            try {
                persist(compute(refs, today, p, events), today, runStart);
                total += refs.size();
            } catch (RuntimeException e) {
                log.warn("Forecast batch failed, retrying one by one", e);
                for (AnalyticsRows.ListingRef ref : refs) {
                    try {
                        persist(compute(List.of(ref), today, p, events), today, runStart);
                        total++;
                    } catch (RuntimeException ex) {
                        log.warn("Forecast failed for listing {}", ref.getListingId(), ex);
                        failed++;
                        firstError = firstError == null ? ref.getSellerSku() + ": " + ex.getMessage() : firstError;
                    }
                }
            }
            if (refs.size() < BATCH) {
                break;
            }
        }
        return new GenerateResult(total, failed, firstError);
    }

    /** 一个 SKU 的计算结果 */
    record Computed(Long listingId, Long storeId, ForecastMethod fixed, LocalDate historyStart, double[] fullHistory,
                    ForecastEngine.Result result) {
    }

    /** 计算一批 SKU（读取历史、手工指定的方法） */
    private List<Computed> compute(List<AnalyticsRows.ListingRef> refs, LocalDate today, ForecastEngine.Params p, List<EcomForecastEvent> events) {
        List<Long> ids = refs.stream().map(AnalyticsRows.ListingRef::getListingId).toList();
        Map<Long, double[]> history = history(ids, today);
        Map<Long, ForecastMethod> fixed = new HashMap<>();
        for (EcomForecastSku s : skuMapper.selectList(Wrappers.<EcomForecastSku>lambdaQuery()
                .select(EcomForecastSku::getListingId, EcomForecastSku::getFixedMethod).in(EcomForecastSku::getListingId, ids))) {
            if (s.getFixedMethod() != null) {
                fixed.put(s.getListingId(), s.getFixedMethod());
            }
        }
        LocalDate start = today.minusDays(HISTORY_DAYS);
        List<Computed> out = new ArrayList<>();
        for (AnalyticsRows.ListingRef ref : refs) {
            double[] full = history.getOrDefault(ref.getListingId(), new double[HISTORY_DAYS]);
            out.add(computeOne(ref.getListingId(), ref.getStoreId(), fixed.get(ref.getListingId()), start, full, today, p, events));
        }
        return out;
    }

    /** 按首次有销量的那天截掉前面的空白后计算 */
    static Computed computeOne(Long listingId, Long storeId, ForecastMethod fixed, LocalDate start, double[] full, LocalDate today,
                               ForecastEngine.Params p, List<EcomForecastEvent> events) {
        int first = 0;
        while (first < full.length && full[first] <= 0) {
            first++;
        }
        double[] trimmed = new double[full.length - first];
        System.arraycopy(full, first, trimmed, 0, trimmed.length);
        LocalDate trimmedStart = start.plusDays(first);
        ForecastEngine.Result result = ForecastEngine.run(trimmed, trimmedStart, HORIZON, p, factorsOf(events, storeId), fixed);
        return new Computed(listingId, storeId, fixed, start, full, result);
    }

    /** 每个 SKU 最近 HISTORY_DAYS 天（到昨天）每天的销量 */
    private Map<Long, double[]> history(List<Long> ids, LocalDate today) {
        LocalDate start = today.minusDays(HISTORY_DAYS);
        Map<Long, double[]> map = new HashMap<>();
        for (AnalyticsRows.ListingDay d : analyticsMapper.selectListingDaily(ids, start.atStartOfDay(), today.atStartOfDay())) {
            int idx = (int) ChronoUnit.DAYS.between(start, d.getSaleDay());
            if (idx >= 0 && idx < HISTORY_DAYS) {
                map.computeIfAbsent(d.getListingId(), k -> new double[HISTORY_DAYS])[idx] += d.getQty() == null ? 0 : d.getQty().doubleValue();
            }
        }
        return map;
    }

    /** 写入一批结果：先删这些 SKU 从今天起的预测和汇总，再批量插入（同一事务） */
    private void persist(List<Computed> list, LocalDate today, LocalDateTime runStart) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(Computed::listingId).toList();
        List<EcomForecast> days = new ArrayList<>();
        List<EcomForecastSku> skus = new ArrayList<>();
        for (Computed c : list) {
            double[] selected = c.result().selected();
            for (int i = 0; i < selected.length; i++) {
                EcomForecast f = new EcomForecast();
                f.setListingId(c.listingId());
                f.setStoreId(c.storeId());
                f.setForecastDate(today.plusDays(i));
                f.setQty(qty(selected[i]));
                f.setMethod(c.result().method());
                f.setGeneratedTime(runStart);
                days.add(f);
            }
            skus.add(summary(c, runStart));
        }
        transactionTemplate.executeWithoutResult(st -> {
            forecastMapper.deleteFrom(ids, today);
            for (int i = 0; i < days.size(); i += INSERT_CHUNK) {
                forecastMapper.insertBatch(days.subList(i, Math.min(days.size(), i + INSERT_CHUNK)));
            }
            skuMapper.deleteByListings(ids);
            skuMapper.insertBatch(skus);
        });
    }

    private static EcomForecastSku summary(Computed c, LocalDateTime runStart) {
        ForecastEngine.Result r = c.result();
        EcomForecastSku s = new EcomForecastSku();
        s.setListingId(c.listingId());
        s.setStoreId(c.storeId());
        s.setMethod(r.method());
        s.setFixedMethod(c.fixed());
        s.setMapeMa(percent(r.mape().get(ForecastMethod.MA)));
        s.setMapeWma(percent(r.mape().get(ForecastMethod.WMA)));
        s.setMapeHolt(percent(r.mape().get(ForecastMethod.HOLT)));
        s.setMapeSeasonal(percent(r.mape().get(ForecastMethod.SEASONAL)));
        s.setHistoryDaily(qty(ForecastEngine.movingAverage(c.fullHistory(), ForecastEngine.BACKTEST_DAYS)));
        s.setForecast30(qty(sum(r.selected(), 30)));
        s.setForecast60(qty(sum(r.selected(), 60)));
        s.setForecast90(qty(sum(r.selected(), 90)));
        s.setSigma(qty(r.sigma()));
        s.setGeneratedTime(runStart);
        return s;
    }

    // ---------------------------------------------------------------- 查询

    /** 预测列表（每个 SKU 的方法、各方法 MAPE、30 / 60 / 90 天合计） */
    public PageResult<EcomForecastSku> page(long current, long size, Long storeId, String keyword, ForecastMethod method) {
        return PageResult.of(skuMapper.selectRows(new Page<>(current, size), storeId, EcomSalesStatsService.blankToNull(keyword), method, null));
    }

    /**
     * SKU 的预测摘要（AI 工具）：按店铺 / 关键字筛选预测汇总，未来 days 天的预测合计与日均；还没有生成预测的在售 SKU 现算
     */
    public List<ForecastSummary> summaries(Long storeId, String keyword, int days, int limit) {
        String kw = EcomSalesStatsService.blankToNull(keyword);
        List<EcomForecastSku> rows = skuMapper.selectRows(new Page<>(1, limit, false), storeId, kw, null, null).getRecords();
        Map<Long, EcomForecastSku> byId = new LinkedHashMap<>();
        rows.forEach(r -> byId.put(r.getListingId(), r));
        List<Long> ids = new ArrayList<>(byId.keySet());
        if (ids.isEmpty() && kw != null) {
            ids = analyticsMapper.selectListingIdsByKeyword(kw, storeId, limit);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, AnalyticsRows.ListingInfo> infos = new HashMap<>();
        analyticsMapper.selectListingInfo(ids).forEach(i -> infos.put(i.getListingId(), i));
        List<ForecastSummary> out = new ArrayList<>();
        dailyForecast(ids, days).forEach((id, f) -> {
            AnalyticsRows.ListingInfo info = infos.get(id);
            EcomForecastSku sku = byId.get(id);
            out.add(new ForecastSummary(info == null ? null : info.getStoreName(), info == null ? null : info.getSellerSku(),
                    info == null ? null : info.getTitle(), f.method(), sku == null ? null : mapeOf(sku, f.method()),
                    sku == null ? null : sku.getHistoryDaily(), days, f.total().setScale(2, RoundingMode.HALF_UP), f.average(),
                    sku == null ? null : sku.getForecast30(), sku == null ? null : sku.getForecast60(), sku == null ? null : sku.getForecast90(),
                    f.stored()));
        });
        return out;
    }

    /**
     * 预测摘要
     *
     * @param mapePercent    所选方法的回测 MAPE（%）
     * @param historyDaily   最近 28 天实际日均
     * @param forecastQty    未来 days 天的预测合计
     * @param forecastDaily  未来 days 天的预测日均
     * @param stored         是否为定时任务已生成的结果
     */
    public record ForecastSummary(String storeName, String sellerSku, String title, ForecastMethod method, BigDecimal mapePercent,
                                  BigDecimal historyDaily, int days, BigDecimal forecastQty, BigDecimal forecastDaily,
                                  BigDecimal forecast30, BigDecimal forecast60, BigDecimal forecast90, boolean stored) {
    }

    static BigDecimal mapeOf(EcomForecastSku s, ForecastMethod m) {
        return switch (m) {
            case MA -> s.getMapeMa();
            case WMA -> s.getMapeWma();
            case HOLT -> s.getMapeHolt();
            case SEASONAL -> s.getMapeSeasonal();
        };
    }

    /** 单个 SKU 的预测详情（按当前参数和事件现算，与存库结果可能有一天的差别） */
    public ForecastDetail detail(Long listingId) {
        AnalyticsRows.ListingInfo info = listingInfo(listingId);
        LocalDate today = LocalDate.now();
        EcomForecastSetting setting = setting();
        ForecastEngine.Params p = params(setting);
        List<EcomForecastEvent> events = events(today.minusDays(HISTORY_DAYS), today.plusDays(HORIZON), info.getStoreId());
        EcomForecastSku row = Queries.first(skuMapper, Wrappers.<EcomForecastSku>lambdaQuery().eq(EcomForecastSku::getListingId, listingId));
        ForecastMethod fixed = row == null ? null : row.getFixedMethod();
        double[] full = history(List.of(listingId), today).getOrDefault(listingId, new double[HISTORY_DAYS]);
        LocalDate start = today.minusDays(HISTORY_DAYS);
        Computed c = computeOne(listingId, info.getStoreId(), fixed, start, full, today, p, events);
        ForecastEngine.Result r = c.result();
        List<ForecastDetail.MethodLine> lines = new ArrayList<>();
        for (ForecastMethod m : ForecastMethod.values()) {
            double[] fc = r.forecasts().get(m);
            Double mae = r.mae().get(m);
            lines.add(new ForecastDetail.MethodLine(m, percent(r.mape().get(m)), mae == null ? null : qty(mae), qty(sum(fc, 30)), qty(sum(fc, 60)),
                    qty(sum(fc, 90)), m == r.method(), toList(fc)));
        }
        double[] selected = r.selected();
        List<BigDecimal> lower = new ArrayList<>();
        List<BigDecimal> upper = new ArrayList<>();
        for (double v : selected) {
            lower.add(qty(ForecastEngine.lower(v, r.sigma())));
            upper.add(qty(ForecastEngine.upper(v, r.sigma())));
        }
        return new ForecastDetail(info, r.method(), fixed, setting, start, toList(full), today, lines, lower, upper, qty(r.sigma()),
                r.backtest() == null ? null : toList(r.backtest()), events);
    }

    /**
     * 未来每天的预测销量（供补货建议等使用）：取定时任务生成的结果（今天起 days 天）；没有结果（新上架、还没生成、已停售）的 SKU
     * 按当前参数现算（不保存）。不存在的在线商品不在结果中
     *
     * @param listingIds 在线商品 ID
     * @param days       天数，1 ~ {@link #HORIZON}
     * @return 在线商品 ID → 每天的预测
     */
    public Map<Long, DailyForecast> dailyForecast(Collection<Long> listingIds, int days) {
        if (days < 1 || days > HORIZON) {
            throw new BizException("ecom.error.analytics.forecastDaysRange", HORIZON);
        }
        Map<Long, DailyForecast> out = new LinkedHashMap<>();
        if (listingIds == null || listingIds.isEmpty()) {
            return out;
        }
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(listingIds));
        LocalDate today = LocalDate.now();
        Map<Long, List<EcomForecast>> stored = new HashMap<>();
        for (int i = 0; i < ids.size(); i += BATCH) {
            for (EcomForecast f : forecastMapper.selectRange(ids.subList(i, Math.min(ids.size(), i + BATCH)), today, today.plusDays(days))) {
                stored.computeIfAbsent(f.getListingId(), k -> new ArrayList<>()).add(f);
            }
        }
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) {
            List<EcomForecast> rows = stored.get(id);
            if (rows != null && rows.size() == days) {
                out.put(id, new DailyForecast(id, rows.get(0).getMethod(), today, rows.stream().map(EcomForecast::getQty).toList(), true));
            } else {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            ForecastEngine.Params p = params(setting());
            List<EcomForecastEvent> events = events(today.minusDays(HISTORY_DAYS), today.plusDays(HORIZON), null);
            Map<Long, ForecastMethod> fixed = new HashMap<>();
            for (EcomForecastSku s : skuMapper.selectList(Wrappers.<EcomForecastSku>lambdaQuery().in(EcomForecastSku::getListingId, missing))) {
                if (s.getFixedMethod() != null) {
                    fixed.put(s.getListingId(), s.getFixedMethod());
                }
            }
            for (int i = 0; i < missing.size(); i += BATCH) {
                List<Long> part = missing.subList(i, Math.min(missing.size(), i + BATCH));
                Map<Long, double[]> history = history(part, today);
                for (AnalyticsRows.ListingInfo info : analyticsMapper.selectListingInfo(part)) {
                    Computed c = computeOne(info.getListingId(), info.getStoreId(), fixed.get(info.getListingId()), today.minusDays(HISTORY_DAYS),
                            history.getOrDefault(info.getListingId(), new double[HISTORY_DAYS]), today, p, events);
                    List<BigDecimal> daily = toList(c.result().selected()).subList(0, days);
                    out.put(info.getListingId(), new DailyForecast(info.getListingId(), c.result().method(), today, List.copyOf(daily), false));
                }
            }
        }
        // 按请求的顺序返回
        Map<Long, DailyForecast> ordered = new LinkedHashMap<>();
        ids.stream().filter(out::containsKey).forEach(id -> ordered.put(id, out.get(id)));
        return ordered;
    }

    /** 未来 days 天的预测日均（{@link #dailyForecast} 的平均值） */
    public Map<Long, BigDecimal> averageDaily(Collection<Long> listingIds, int days) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        dailyForecast(listingIds, days).forEach((id, f) -> out.put(id, f.average()));
        return out;
    }

    private AnalyticsRows.ListingInfo listingInfo(Long listingId) {
        List<AnalyticsRows.ListingInfo> list = listingId == null ? List.of() : analyticsMapper.selectListingInfo(List.of(listingId));
        if (list.isEmpty()) {
            throw new BizException("ecom.error.listingNotFound");
        }
        return list.get(0);
    }

    // ---------------------------------------------------------------- 内部

    private static double sum(double[] v, int n) {
        double s = 0;
        for (int i = 0; i < Math.min(n, v.length); i++) {
            s += v[i];
        }
        return s;
    }

    private static BigDecimal qty(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static List<BigDecimal> toList(double[] v) {
        List<BigDecimal> out = new ArrayList<>(v.length);
        for (double x : v) {
            out.add(qty(x));
        }
        return out;
    }
}
