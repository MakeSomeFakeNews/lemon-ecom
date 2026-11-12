package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 电商订单作业的单据编号（编码规则页面可配置）：包裹 PK
 */
@Configuration
public class EcomOpsDocNos {

    /** 包裹 */
    public static final String PACKAGE = "PK";

    @Bean
    @Order(3)
    public DocNoCatalog ecomOpsDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.ops.module", doc(PACKAGE, "ecom.label.ops.package"));
    }
}
