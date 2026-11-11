package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 跨境电商模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class EcomDocNos {

    /** 头程货件 */
    public static final String INBOUND = "HJ";
    /** 退货退款 */
    public static final String RETURN = "TK";

    @Bean
    @Order(2)
    public DocNoCatalog ecomDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.ecom",
                doc(INBOUND, "ecom.label.inbound"),
                doc(RETURN, "ecom.label.returnDoc"));
    }
}
