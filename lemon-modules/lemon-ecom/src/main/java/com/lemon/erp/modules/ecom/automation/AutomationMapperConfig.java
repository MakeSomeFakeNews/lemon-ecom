package com.lemon.erp.modules.ecom.automation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 自动化规则中心的 Mapper 在子包 automation.mapper 中，不在启动类的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.automation.mapper")
public class AutomationMapperConfig {
}
