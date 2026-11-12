package com.lemon.erp.modules.ecom.fba;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * FBA 运营的 Mapper 在子包 fba.mapper 中，不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.fba.mapper")
public class FbaMapperConfig {
}
