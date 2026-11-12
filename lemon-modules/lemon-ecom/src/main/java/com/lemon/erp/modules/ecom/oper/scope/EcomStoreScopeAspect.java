package com.lemon.erp.modules.ecom.oper.scope;

import com.lemon.erp.modules.ecom.oper.service.EcomStoreMemberService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Set;

/**
 * 店铺数据权限的统一入口：电商模块的全部接口（RestController）、AI 工具（AiTool.execute）、首页待办（TodoProvider.todos）执行期间，
 * 按当前用户的可见店铺启用 {@link EcomStoreScope}。各 Service / Mapper 不需要逐个处理——带 store_id 列的电商表查询自动过滤，
 * 以后新增的表和页面同样生效。不限制的用户（超级管理员、全部数据权限）和定时任务不启用。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class EcomStoreScopeAspect {

    /** 延迟获取，避免切面过早实例化业务 Bean（使其失去事务代理） */
    private final ObjectProvider<EcomStoreMemberService> memberService;
    private final ObjectProvider<DataSource> dataSource;

    @Around("within(com.lemon.erp.modules.ecom..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object aroundController(ProceedingJoinPoint point) throws Throwable {
        return proceed(point);
    }

    @Around("within(com.lemon.erp.modules.ecom..*) && (execution(* com.lemon.erp.framework.ai.AiTool+.execute(..))"
            + " || execution(* com.lemon.erp.framework.todo.TodoProvider+.todos(..)))")
    public Object aroundTools(ProceedingJoinPoint point) throws Throwable {
        return proceed(point);
    }

    private Object proceed(ProceedingJoinPoint point) throws Throwable {
        if (EcomStoreScope.restricted()) {
            return point.proceed();
        }
        Set<Long> storeIds = memberService.getObject().visibleStoreIds();
        if (storeIds == null) {
            return point.proceed();
        }
        EcomStoreScope.Active previous = EcomStoreScope.enter(new EcomStoreScope.Active(storeIds, EcomStoreScopeTables.get(dataSource.getObject())));
        try {
            return point.proceed();
        } finally {
            EcomStoreScope.restore(previous);
        }
    }
}
