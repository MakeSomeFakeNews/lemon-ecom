package com.lemon.erp.modules.ecom.publish;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 刊登与定价的 Mapper 在子包 publish.mapper 中，不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.publish.mapper")
public class PublishMapperConfig {
}
