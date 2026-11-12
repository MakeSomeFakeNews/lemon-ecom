package com.lemon.erp.modules.ecom.oper.demo;

import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomStoreService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营监控与团队的演示数据公共方法：取演示店铺（优先用基础部分共享的店铺编码 → ID，没有时按编码查询）
 */
final class OperDemoSupport {

    /** 演示店铺编码（与基础演示数据 DemoCatalog.STORES 一致） */
    static final String AMZ_US = "DEMO-AMZ-US";
    static final String AMZ_DE = "DEMO-AMZ-DE";
    static final String AMZ_JP = "DEMO-AMZ-JP";
    static final String SHP_MY = "DEMO-SHP-MY";
    static final String SHP_SG = "DEMO-SHP-SG";
    static final String TT_US = "DEMO-TT-US";
    static final String TEMU_US = "DEMO-TEMU-US";
    static final String AE = "DEMO-AE";
    static final String SHOPIFY = "DEMO-SHOPIFY";
    static final String EBAY_US = "DEMO-EBAY-US";
    static final List<String> ALL = List.of(AMZ_US, AMZ_DE, AMZ_JP, SHP_MY, SHP_SG, TT_US, TEMU_US, AE, SHOPIFY, EBAY_US);

    private OperDemoSupport() {
    }

    /** 店铺编码 → 店铺（不存在的跳过），按 {@link #ALL} 的顺序 */
    static Map<String, EcomStore> stores(DemoContext ctx, EcomStoreService storeService) {
        Map<String, Long> ids = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<String, EcomStore> out = new LinkedHashMap<>();
        for (String code : ALL) {
            EcomStore s = ids != null && ids.get(code) != null ? storeService.get(ids.get(code)) : storeService.findByCode(code);
            if (s != null) {
                out.put(code, s);
            }
        }
        return out;
    }
}
