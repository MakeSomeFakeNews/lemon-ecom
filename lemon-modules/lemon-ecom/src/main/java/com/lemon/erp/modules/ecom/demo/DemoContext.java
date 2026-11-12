package com.lemon.erp.modules.ecom.demo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 一次演示数据生成的上下文：固定种子的随机数、基准日期（今天）、历史天数，以及组成部分之间共享的值。
 */
public class DemoContext {

    private final Random random;
    private final LocalDate today;
    private final int days;
    private final Map<String, Object> shared = new HashMap<>();

    public DemoContext(long seed, LocalDate today, int days) {
        this.random = new Random(seed);
        this.today = today;
        this.days = days;
    }

    public Random random() {
        return random;
    }

    /** 基准日期（生成时的今天），历史数据在 [today - days, today] 之间 */
    public LocalDate today() {
        return today;
    }

    /** 历史天数（默认 120 天） */
    public int days() {
        return days;
    }

    public void put(String key, Object value) {
        shared.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) shared.get(key);
    }

    /** [min, max] 之间的随机整数 */
    public int between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /** 按概率返回 true */
    public boolean chance(double p) {
        return random.nextDouble() < p;
    }

    /** 从数组中随机取一个 */
    @SafeVarargs
    public final <T> T pick(T... values) {
        return values[random.nextInt(values.length)];
    }
}
