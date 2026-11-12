package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * 销量预测算法（纯计算，按天）：
 * <ul>
 *   <li>移动平均 MA：最近 window 天日均；加权移动平均 WMA：最近 window 天按 1..window 线性加权（越近越大）</li>
 *   <li>Holt 线性趋势 HOLT：水平 L = α·y + (1 − α)(L + T)，趋势 T = β(L − L′) + (1 − β)T，初始水平取前 7 天日均、趋势 0；第 h 天预测 L + h·T（不为负）</li>
 *   <li>星期季节性 SEASONAL：最近 seasonWeeks 周中每个星期几的日均 ÷ 总日均为星期系数，去掉星期因素后做 WMA，再乘目标日的星期系数</li>
 * </ul>
 * 促销 / 节假日系数（{@link Factors}）：历史销量先 ÷ 当天系数得到基线，预测结果再 × 目标日系数。
 * 回测：留出最近 {@link #BACKTEST_DAYS} 天，用之前的数据预测这 28 天，按天计算 MAPE = 平均(|实际 − 预测| ÷ 实际) × 100
 * （只计实际 &gt; 0 的天）；默认选 MAPE 最小的方法（相同时取 28 天绝对误差合计小的），全部没有 MAPE（回测期没有销量）时选绝对误差最小的，
 * 历史不足（少于 28 + 14 天）时用移动平均
 */
public final class ForecastEngine {

    /** 回测天数 */
    public static final int BACKTEST_DAYS = 28;
    /** 回测时训练数据至少的天数 */
    static final int MIN_TRAIN_DAYS = 14;
    /** 80% 预测区间的 z 值 */
    static final double Z80 = 1.2816;

    private ForecastEngine() {
    }

    /** 参数：Holt 的 α / β，移动平均窗口天数，星期系数取最近几周 */
    public record Params(double alpha, double beta, int window, int seasonWeeks) {
        public static final Params DEFAULT = new Params(0.3, 0.1, 28, 8);
    }

    /** 某天的促销 / 节假日系数（没有为 1） */
    @FunctionalInterface
    public interface Factors {
        Factors NONE = d -> 1.0;

        double at(LocalDate date);
    }

    /**
     * 运行结果
     *
     * @param forecasts 各方法从 start + history.length 天起 horizon 天的预测（已乘系数）
     * @param mape      各方法的回测 MAPE（%），无法计算为空
     * @param mae       各方法回测 28 天的绝对误差合计，历史不足时为空
     * @param method    选用的方法
     * @param sigma     选用方法回测的日误差标准差（没有回测时为最近 28 天实际销量的标准差）
     * @param backtest  选用方法对回测期（最近 28 天）的预测，没有回测时为空
     */
    public record Result(Map<ForecastMethod, double[]> forecasts, Map<ForecastMethod, Double> mape, Map<ForecastMethod, Double> mae,
                         ForecastMethod method, double sigma, double[] backtest) {

        public double[] selected() {
            return forecasts.get(method);
        }
    }

    /**
     * 计算各方法的预测、回测并选择方法
     *
     * @param history 每天的实际销量，history[i] 为 start + i 天（最后一天一般是昨天）
     * @param horizon 预测天数
     * @param fixed   手工指定的方法，为空按回测选择
     */
    public static Result run(double[] history, LocalDate start, int horizon, Params p, Factors factors, ForecastMethod fixed) {
        int n = history.length;
        double[] base = deflate(history, start, factors);
        Map<ForecastMethod, Double> mape = new EnumMap<>(ForecastMethod.class);
        Map<ForecastMethod, Double> mae = new EnumMap<>(ForecastMethod.class);
        Map<ForecastMethod, double[]> backtests = new EnumMap<>(ForecastMethod.class);
        boolean canBacktest = n >= BACKTEST_DAYS + MIN_TRAIN_DAYS;
        if (canBacktest) {
            int train = n - BACKTEST_DAYS;
            double[] trainBase = new double[train];
            System.arraycopy(base, 0, trainBase, 0, train);
            double[] actual = new double[BACKTEST_DAYS];
            System.arraycopy(history, train, actual, 0, BACKTEST_DAYS);
            for (ForecastMethod m : ForecastMethod.values()) {
                double[] fc = inflate(forecast(m, trainBase, start, BACKTEST_DAYS, p), start.plusDays(train), factors);
                backtests.put(m, fc);
                Double e = mape(actual, fc);
                if (e != null) {
                    mape.put(m, e);
                }
                mae.put(m, absError(actual, fc));
            }
        }
        ForecastMethod method = fixed != null ? fixed : select(mape, mae);
        Map<ForecastMethod, double[]> forecasts = new EnumMap<>(ForecastMethod.class);
        LocalDate first = start.plusDays(n);
        for (ForecastMethod m : ForecastMethod.values()) {
            forecasts.put(m, inflate(forecast(m, base, start, horizon, p), first, factors));
        }
        double sigma;
        double[] backtest = backtests.get(method);
        if (backtest != null) {
            double[] err = new double[BACKTEST_DAYS];
            for (int i = 0; i < BACKTEST_DAYS; i++) {
                err[i] = history[n - BACKTEST_DAYS + i] - backtest[i];
            }
            sigma = stddev(err);
        } else {
            double[] recent = new double[Math.min(BACKTEST_DAYS, n)];
            System.arraycopy(history, n - recent.length, recent, 0, recent.length);
            sigma = stddev(recent);
        }
        return new Result(forecasts, mape, mae, method, sigma, backtest);
    }

    /** 回测选方法：MAPE 最小（相同时绝对误差小的）；都没有 MAPE 时取绝对误差最小；都没有（历史不足）时用移动平均。再相同时按枚举顺序 */
    static ForecastMethod select(Map<ForecastMethod, Double> mape, Map<ForecastMethod, Double> mae) {
        ForecastMethod best = null;
        for (ForecastMethod m : ForecastMethod.values()) {
            if (mape.isEmpty() ? !mae.containsKey(m) : !mape.containsKey(m)) {
                continue;
            }
            if (best == null || better(m, best, mape, mae)) {
                best = m;
            }
        }
        return best == null ? ForecastMethod.MA : best;
    }

    private static boolean better(ForecastMethod m, ForecastMethod best, Map<ForecastMethod, Double> mape, Map<ForecastMethod, Double> mae) {
        if (!mape.isEmpty()) {
            double d = mape.get(m) - mape.get(best);
            if (Math.abs(d) > 1e-9) {
                return d < 0;
            }
        }
        double a = mae.getOrDefault(m, Double.MAX_VALUE);
        double b = mae.getOrDefault(best, Double.MAX_VALUE);
        return a < b - 1e-9;
    }

    /**
     * 单个方法的预测（不含系数）
     *
     * @param base  去掉系数后的历史，base[i] 为 start + i 天
     * @return 从 start + base.length 天起 horizon 天
     */
    public static double[] forecast(ForecastMethod method, double[] base, LocalDate start, int horizon, Params p) {
        double[] out = new double[horizon];
        switch (method) {
            case MA -> Arrays.fill(out, movingAverage(base, p.window()));
            case WMA -> Arrays.fill(out, weightedMovingAverage(base, p.window()));
            case HOLT -> {
                return holt(base, p.alpha(), p.beta(), horizon);
            }
            case SEASONAL -> {
                double[] index = weekdayIndex(base, start, p.seasonWeeks());
                double[] deseason = new double[base.length];
                for (int i = 0; i < base.length; i++) {
                    double idx = index[weekday(start.plusDays(i))];
                    deseason[i] = idx > 0 ? base[i] / idx : 0;
                }
                double level = weightedMovingAverage(deseason, p.window());
                LocalDate first = start.plusDays(base.length);
                for (int h = 0; h < horizon; h++) {
                    out[h] = level * index[weekday(first.plusDays(h))];
                }
            }
            default -> throw new IllegalArgumentException(method.name());
        }
        return out;
    }

    /** 最近 window 天的日均（不足 window 天按实际天数，没有数据为 0） */
    public static double movingAverage(double[] h, int window) {
        int n = Math.min(window, h.length);
        if (n <= 0) {
            return 0;
        }
        double sum = 0;
        for (int i = h.length - n; i < h.length; i++) {
            sum += h[i];
        }
        return sum / n;
    }

    /** 最近 window 天按 1..window 加权（最近一天权重最大） */
    public static double weightedMovingAverage(double[] h, int window) {
        int n = Math.min(window, h.length);
        if (n <= 0) {
            return 0;
        }
        double sum = 0;
        double weights = 0;
        for (int k = 1; k <= n; k++) {
            double v = h[h.length - n + k - 1];
            sum += v * k;
            weights += k;
        }
        return sum / weights;
    }

    /** Holt 线性趋势：返回之后 horizon 天的预测（不为负） */
    public static double[] holt(double[] h, double alpha, double beta, int horizon) {
        double[] out = new double[horizon];
        if (h.length == 0) {
            return out;
        }
        int init = Math.min(7, h.length);
        double level = 0;
        for (int i = 0; i < init; i++) {
            level += h[i];
        }
        level /= init;
        double trend = 0;
        for (double y : h) {
            double prev = level;
            level = alpha * y + (1 - alpha) * (level + trend);
            trend = beta * (level - prev) + (1 - beta) * trend;
        }
        for (int k = 0; k < horizon; k++) {
            out[k] = Math.max(0, level + (k + 1) * trend);
        }
        return out;
    }

    /**
     * 星期系数：最近 weeks 周（不足按实际天数）中每个星期几的日均 ÷ 总日均，下标 0 为星期一；
     * 不足 14 天或没有销量时全部为 1
     */
    public static double[] weekdayIndex(double[] h, LocalDate start, int weeks) {
        double[] index = new double[7];
        Arrays.fill(index, 1);
        int n = Math.min(h.length, weeks * 7);
        if (n < 14) {
            return index;
        }
        double[] sum = new double[7];
        int[] count = new int[7];
        double total = 0;
        for (int i = h.length - n; i < h.length; i++) {
            int wd = weekday(start.plusDays(i));
            sum[wd] += h[i];
            count[wd]++;
            total += h[i];
        }
        double mean = total / n;
        if (mean <= 0) {
            return index;
        }
        for (int d = 0; d < 7; d++) {
            index[d] = count[d] == 0 ? 1 : sum[d] / count[d] / mean;
        }
        return index;
    }

    /**
     * 按天计算的 MAPE（%）：只计实际 &gt; 0 的天，没有这样的天时为空
     */
    public static Double mape(double[] actual, double[] forecast) {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] > 0) {
                sum += Math.abs(actual[i] - forecast[i]) / actual[i];
                n++;
            }
        }
        return n == 0 ? null : sum / n * 100;
    }

    static double absError(double[] actual, double[] forecast) {
        double sum = 0;
        for (int i = 0; i < actual.length; i++) {
            sum += Math.abs(actual[i] - forecast[i]);
        }
        return sum;
    }

    static double stddev(double[] v) {
        if (v.length < 2) {
            return 0;
        }
        double mean = 0;
        for (double x : v) {
            mean += x;
        }
        mean /= v.length;
        double ss = 0;
        for (double x : v) {
            ss += (x - mean) * (x - mean);
        }
        return Math.sqrt(ss / (v.length - 1));
    }

    /** 80% 预测区间的下限 / 上限（下限不为负） */
    public static double lower(double forecast, double sigma) {
        return Math.max(0, forecast - Z80 * sigma);
    }

    public static double upper(double forecast, double sigma) {
        return forecast + Z80 * sigma;
    }

    /** 历史 ÷ 当天系数 */
    static double[] deflate(double[] history, LocalDate start, Factors factors) {
        double[] out = new double[history.length];
        for (int i = 0; i < history.length; i++) {
            double f = factors.at(start.plusDays(i));
            out[i] = f > 0 ? history[i] / f : history[i];
        }
        return out;
    }

    /** 预测 × 目标日系数 */
    static double[] inflate(double[] forecast, LocalDate first, Factors factors) {
        double[] out = new double[forecast.length];
        for (int i = 0; i < forecast.length; i++) {
            out[i] = Math.max(0, forecast[i] * factors.at(first.plusDays(i)));
        }
        return out;
    }

    /** 星期几的下标，星期一为 0 */
    static int weekday(LocalDate d) {
        return d.getDayOfWeek().getValue() - 1;
    }
}
