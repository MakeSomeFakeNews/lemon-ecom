package com.lemon.erp.modules.ecom.oper.scope;

import com.lemon.erp.common.exception.BizException;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 店铺数据权限的当前范围（线程内）：电商的接口、AI 工具、首页待办执行期间由 {@link EcomStoreScopeAspect} 按当前用户设置，
 * {@link EcomStoreScopeInterceptor} 据此给带店铺列的电商表追加 {@code store_id IN (可见店铺)} 条件。
 * <p>
 * 没有设置（定时任务、超级管理员、角色数据权限为「全部数据」的用户）时不限制。
 */
public final class EcomStoreScope {

    /**
     * @param storeIds 可见的店铺 ID（为空集合时看不到任何店铺的数据）
     * @param tables   需要过滤的表（小写表名 → 店铺列）
     */
    record Active(Set<Long> storeIds, Map<String, EcomStoreScopeTables.Column> tables) {
    }

    private static final ThreadLocal<Active> CURRENT = new ThreadLocal<>();

    private EcomStoreScope() {
    }

    /** 当前生效的范围，没有限制时为 null */
    static Active active() {
        return CURRENT.get();
    }

    static Active enter(Active scope) {
        Active previous = CURRENT.get();
        CURRENT.set(scope);
        return previous;
    }

    static void restore(Active previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /** 当前是否按店铺限制 */
    public static boolean restricted() {
        return CURRENT.get() != null;
    }

    /** 当前可见的店铺 ID，不限制时为 null */
    public static Set<Long> visibleStoreIds() {
        Active a = CURRENT.get();
        return a == null ? null : a.storeIds();
    }

    /** 能否访问店铺的数据（不限制、或店铺在可见范围内；storeId 为空视为不属于任何店铺，可以访问） */
    public static boolean canAccess(Long storeId) {
        Active a = CURRENT.get();
        return a == null || storeId == null || a.storeIds().contains(storeId);
    }

    /**
     * 暂停店铺过滤执行（如写操作中的唯一性校验需要看到全部店铺的数据），结束后恢复
     */
    public static <T> T unrestricted(Supplier<T> action) {
        Active previous = CURRENT.get();
        CURRENT.remove();
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    /**
     * 按 ID 读取单条电商数据：店铺范围已在 SQL 中过滤，读不到（不存在或店铺不可见）时抛出 BizException(notFoundKey)
     */
    public static <T> T require(Supplier<T> loader, String notFoundKey, Object... args) {
        T value = loader.get();
        if (value == null) {
            throw new BizException(notFoundKey, args);
        }
        return value;
    }
}
