package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 电商采购计划的单据编号（编码规则页面可配置）：采购计划 CJ
 */
@Configuration
public class EcomPurchaseDocNos {

    /** 采购计划 */
    public static final String PURCHASE_PLAN = "CJ";

    @Bean
    @Order(4)
    public DocNoCatalog ecomPurchaseDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.purchase.module", doc(PURCHASE_PLAN, "ecom.label.purchase.plan"));
    }
}
