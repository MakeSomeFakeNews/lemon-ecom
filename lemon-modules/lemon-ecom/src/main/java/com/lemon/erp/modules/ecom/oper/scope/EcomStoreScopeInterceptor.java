package com.lemon.erp.modules.ecom.oper.scope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.lemon.erp.framework.security.datascope.RowFilterInterceptor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 店铺数据权限的 SQL 改写：当前线程设置了 {@link EcomStoreScope} 时，查询中出现的每张带店铺列的电商表都追加
 * {@code 别名.store_id IN (可见店铺)}（主表加在 WHERE，LEFT JOIN 的表加在 ON，子查询同样处理，分页 count 也生效）。
 * 只改写查询；写操作先按 ID 读取（读不到即视为不存在），不在 UPDATE / DELETE 上追加条件。
 */
@Component
public class EcomStoreScopeInterceptor extends DataPermissionInterceptor implements RowFilterInterceptor {

    private static final String DENY_ALL = "1 = 0";

    public EcomStoreScopeInterceptor() {
        super(new Handler());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (EcomStoreScope.active() != null) {
            super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        }
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // 只过滤查询
    }

    static final class Handler implements MultiDataPermissionHandler {

        @Override
        public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
            EcomStoreScope.Active scope = EcomStoreScope.active();
            if (scope == null || table.getName() == null) {
                return null;
            }
            EcomStoreScopeTables.Column column = scope.tables().get(table.getName().replace("`", "").toLowerCase(Locale.ROOT));
            if (column == null) {
                return null;
            }
            String condition = condition(scope, column, table.getAlias() != null ? table.getAlias().getName() : table.getName());
            try {
                return CCJSqlParserUtil.parseCondExpression(condition);
            } catch (JSQLParserException e) {
                throw new IllegalStateException("店铺数据权限条件解析失败: " + condition, e); // i18n-ignore 内部错误
            }
        }

        /** 拼接的只有表结构中的列名和 Long 型店铺 ID，不存在 SQL 注入 */
        static String condition(EcomStoreScope.Active scope, EcomStoreScopeTables.Column column, String qualifier) {
            String col = qualifier + "." + column.name();
            String in = scope.storeIds().isEmpty() ? DENY_ALL
                    : col + " IN (" + scope.storeIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            return column.nullable() ? "(" + col + " IS NULL OR " + in + ")" : in;
        }
    }
}
