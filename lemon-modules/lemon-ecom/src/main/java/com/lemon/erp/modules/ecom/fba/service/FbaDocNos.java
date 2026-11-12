package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * FBA 运营的单据编号（编码规则页面可配置）：移除订单 YC、FBA 索赔 SP
 */
@Configuration
public class FbaDocNos {

    /** 移除订单 */
    public static final String REMOVAL = "YC";
    /** FBA 索赔 */
    public static final String CLAIM = "SP";

    @Bean
    @Order(8)
    public DocNoCatalog ecomFbaDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.fba.module", doc(REMOVAL, "ecom.label.fba.removal"), doc(CLAIM, "ecom.label.fba.claim"));
    }
}
