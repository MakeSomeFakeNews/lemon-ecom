package com.lemon.erp.framework.security.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;

/**
 * 业务模块提供的附加行级过滤（如电商按店铺授权过滤），注册为 Bean 后由 {@code MybatisPlusConfig} 加在数据权限之后、分页之前，
 * 分页 count 也会带上过滤条件。
 * <p>
 * 实现约定：只在当前线程启用了过滤时才解析、改写 SQL（参照 {@link LazyDataPermissionInterceptor}），
 * 不能依赖 Mapper 或 SqlSessionFactory（拦截器在 SqlSessionFactory 创建之前构造），需要查库的范围计算放到别处完成后通过 ThreadLocal 传入。
 */
public interface RowFilterInterceptor extends InnerInterceptor {
}
