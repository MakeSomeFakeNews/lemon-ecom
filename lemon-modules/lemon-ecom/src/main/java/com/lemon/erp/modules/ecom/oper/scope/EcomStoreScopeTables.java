package com.lemon.erp.modules.ecom.oper.scope;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 需要按店铺过滤的电商表：启动后第一次使用时从数据库元数据中找出全部 {@code ecom_} 开头、带 {@code store_id} 列的表
 * （包括以后新增的表，不需要逐个登记），外加店铺表自身（按 {@code id}）。
 * 允许为空的店铺列（如适用于全部店铺的规则、不属于店铺的竞品监控）按「为空或在可见范围内」过滤。
 */
@Slf4j
public final class EcomStoreScopeTables {

    /** 店铺列 */
    public record Column(String name, boolean nullable) {
    }

    static final String STORE_TABLE = "ecom_store";
    static final String STORE_COLUMN = "store_id";
    private static final String PREFIX = "ecom_";

    private static volatile Map<String, Column> tables;

    private EcomStoreScopeTables() {
    }

    /** 需要过滤的表（小写表名 → 店铺列），第一次调用时读取元数据 */
    static Map<String, Column> get(DataSource dataSource) {
        Map<String, Column> t = tables;
        if (t == null) {
            synchronized (EcomStoreScopeTables.class) {
                t = tables;
                if (t == null) {
                    t = load(dataSource);
                    tables = t;
                }
            }
        }
        return t;
    }

    private static Map<String, Column> load(DataSource dataSource) {
        Map<String, Column> map = new HashMap<>();
        map.put(STORE_TABLE, new Column("id", false));
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            for (String column : new String[]{STORE_COLUMN, STORE_COLUMN.toUpperCase(Locale.ROOT)}) {
                try (ResultSet rs = meta.getColumns(catalog, null, "%", column)) {
                    while (rs.next()) {
                        String table = rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT);
                        if (table.startsWith(PREFIX) && !map.containsKey(table)) {
                            map.put(table, new Column(STORE_COLUMN, rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("读取店铺数据权限的表结构失败", e); // i18n-ignore 内部错误
        }
        log.info("店铺数据权限过滤的表：{}", map.keySet()); // i18n-ignore 日志
        return Collections.unmodifiableMap(map);
    }
}
