package com.lemon.erp.modules.ecom.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * AI 运营助手（运营日报、日报订阅、违禁词库、问数工具）的 Mapper 在子包 assistant.mapper 中，
 * 不在启动类 {@code com.lemon.erp.modules.*.mapper} 的扫描范围内，单独登记
 */
@Configuration
@MapperScan("com.lemon.erp.modules.ecom.assistant.mapper")
public class AssistantMapperConfig {
}
