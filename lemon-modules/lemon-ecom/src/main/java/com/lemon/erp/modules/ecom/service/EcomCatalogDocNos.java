package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 电商商品体系的单据编号（编码规则页面可配置）：发货计划 FP
 */
@Configuration
public class EcomCatalogDocNos {

    /** 发货计划 */
    public static final String SHIPMENT_PLAN = "FP";

    @Bean
    @Order(4)
    public DocNoCatalog ecomCatalogDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.catalog.module", doc(SHIPMENT_PLAN, "ecom.label.catalog.shipmentPlan"));
    }
}
