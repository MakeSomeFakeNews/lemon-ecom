package com.lemon.erp.modules.ecom.oper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 运营监控与团队的 Mapper 在子包 oper.mapper 中，不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.oper.mapper")
public class OperMapperConfig {
}
