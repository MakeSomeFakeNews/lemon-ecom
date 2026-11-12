package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 产品开发的单据编号（编码规则页面可配置）：开发项目 KF
 */
@Configuration
public class EcomDevDocNos {

    /** 开发项目 */
    public static final String PROJECT = "KF";

    @Bean
    @Order(4)
    public DocNoCatalog ecomDevDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.dev.module", doc(PROJECT, "ecom.label.dev.project"));
    }
}
