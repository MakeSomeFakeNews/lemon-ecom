package com.lemon.erp.modules.ecom.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据分析的 Mapper 在子包 analytics.mapper 中，不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.analytics.mapper")
public class AnalyticsMapperConfig {
}
