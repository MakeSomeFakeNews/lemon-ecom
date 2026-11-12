package com.lemon.erp.modules.ecom.dev;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 产品开发与 1688 采购的 Mapper 在子包 dev.mapper 中，不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.dev.mapper")
public class DevMapperConfig {
}
