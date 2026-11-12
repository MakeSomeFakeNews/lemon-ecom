package com.lemon.erp.modules.ecom.owh.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 海外仓的单据编号（编码规则页面可配置）：入库单 OWI、出库单 OWO、退件单 OWR、调拨单 OWT
 */
@Configuration
public class EcomOwhDocNos {

    /** 海外仓入库单 */
    public static final String INBOUND = "OWI";
    /** 海外仓出库单 */
    public static final String OUTBOUND = "OWO";
    /** 海外仓退件单 */
    public static final String RETURN = "OWR";
    /** 海外仓调拨单 */
    public static final String TRANSFER = "OWT";

    @Bean
    @Order(6)
    public DocNoCatalog ecomOwhDocNoCatalog() {
        return DocNoCatalog.of("ecom.label.owh.module", doc(INBOUND, "ecom.label.owh.inbound"), doc(OUTBOUND, "ecom.label.owh.outbound"),
                doc(RETURN, "ecom.label.owh.return"), doc(TRANSFER, "ecom.label.owh.transfer"));
    }
}
