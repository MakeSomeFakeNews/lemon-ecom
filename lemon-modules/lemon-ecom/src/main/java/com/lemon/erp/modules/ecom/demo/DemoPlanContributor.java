package com.lemon.erp.modules.ecom.demo;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 演示数据 · 订单计划（order 30）：只在内存中生成 120 天的订单计划和各仓库的物料需求（不写库），供采购、头程备货和订单写入使用
 */
@Component
public class DemoPlanContributor implements DemoContributor {

    @Override
    public int order() {
        return 30;
    }

    @Override
    public String key() {
        return "plan";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<Long, Map<String, DemoListings.Item>> listings = ctx.get(DemoContributors.LISTINGS);
        Map<String, Long> warehouses = ctx.get(DemoContributors.WAREHOUSE_BY_CODE);
        Long localWh = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        ctx.put(DemoContributors.ORDER_PLAN, DemoOrderPlan.build(ctx, stores, listings, warehouses, localWh,
                materials.get(DemoOrderPlan.SHORT_MATERIAL)));
    }
}
