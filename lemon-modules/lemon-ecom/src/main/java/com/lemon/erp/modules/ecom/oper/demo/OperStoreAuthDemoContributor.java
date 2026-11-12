package com.lemon.erp.modules.ecom.oper.demo;

import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.oper.service.EcomStoreMemberService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 店铺授权（order 150）：三个运营演示账号除了负责的店铺，再互相授权部分店铺（备岗运营、客服、助理），
 * 用演示账号登录即可看到店铺数据权限的效果（只看到负责和被授权的店铺）
 */
@Component
@RequiredArgsConstructor
public class OperStoreAuthDemoContributor implements DemoContributor {

    static final String OPERATOR = "OPERATOR";
    static final String CS = "CS";
    static final String ASSISTANT = "ASSISTANT";

    private final EcomStoreService storeService;
    private final EcomStoreMemberService memberService;

    @Override
    public int order() {
        return 150;
    }

    @Override
    public String key() {
        return "oper-store-auth";
    }

    @Override
    public void generate(DemoContext ctx) {
        List<Long> ops = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        if (ops == null || ops.size() < 3) {
            return;
        }
        Map<String, EcomStore> stores = OperDemoSupport.stores(ctx, storeService);
        Long lily = ops.get(0);
        Long jack = ops.get(1);
        Long mia = ops.get(2);
        // 林晓雨（美国站、TikTok）：协助德国站，兼做虾皮马来西亚站客服
        grant(stores, OperDemoSupport.AMZ_DE, lily, ASSISTANT);
        grant(stores, OperDemoSupport.SHP_MY, lily, CS);
        // 周子航（欧日站）：美国站备岗运营
        grant(stores, OperDemoSupport.AMZ_US, jack, OPERATOR);
        // 陈思琪（新兴平台）：TikTok 客服、Shopify 运营
        grant(stores, OperDemoSupport.TT_US, mia, CS);
        grant(stores, OperDemoSupport.SHOPIFY, mia, OPERATOR);
    }

    private void grant(Map<String, EcomStore> stores, String code, Long userId, String role) {
        EcomStore s = stores.get(code);
        if (s != null && !userId.equals(s.getOwnerId())) {
            memberService.grant(s.getId(), userId, role);
        }
    }
}
