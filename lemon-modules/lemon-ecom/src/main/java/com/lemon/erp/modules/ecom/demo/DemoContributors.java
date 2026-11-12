package com.lemon.erp.modules.ecom.demo;

/**
 * 基础组成部分（order 0–99）之间内部使用的共享值键（其他开发线用 {@link DemoKeys}）
 */
final class DemoContributors {

    /** 仓库编码 → ID：Map&lt;String, Long&gt; */
    static final String WAREHOUSE_BY_CODE = "demo.warehouseByCode";
    /** 头程货代供应商 ID：Long */
    static final String FORWARDER_ID = "demo.forwarderId";
    /** 供应商编码 → ID：Map&lt;String, Long&gt; */
    static final String SUPPLIER_BY_CODE = "demo.supplierByCode";
    /** 店铺编码 → 店铺负责人 ID：Map&lt;String, Long&gt; */
    static final String STORE_OWNER = "demo.storeOwner";
    /** 订单计划：DemoOrderPlan */
    static final String ORDER_PLAN = "demo.orderPlan";
    /** 在线商品：店铺 ID → (平台 SKU → 在线商品)：Map&lt;Long, Map&lt;String, DemoListings.Item&gt;&gt; */
    static final String LISTINGS = "demo.listings";
    /** 头程单件成本：店铺 ID → (物料 ID → 单件头程成本)：Map&lt;Long, Map&lt;Long, BigDecimal&gt;&gt; */
    static final String LANDED = "demo.landed";

    private DemoContributors() {
    }
}
