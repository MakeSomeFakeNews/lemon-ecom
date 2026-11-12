package com.lemon.erp.modules.ecom.demo;

/**
 * {@link DemoContext} 中基础组成部分放入的共享值的键。值的类型在各常量的注释中说明。
 */
public final class DemoKeys {

    /** 演示店铺 ID 列表：List&lt;Long&gt; */
    public static final String STORE_IDS = "storeIds";
    /** 演示物料 ID 列表：List&lt;Long&gt; */
    public static final String MATERIAL_IDS = "materialIds";
    /** 本地发货仓 ID：Long */
    public static final String LOCAL_WAREHOUSE_ID = "localWarehouseId";
    /** 演示供应商 ID 列表：List&lt;Long&gt; */
    public static final String SUPPLIER_IDS = "supplierIds";

    /** 店铺编码 → 店铺 ID：Map&lt;String, Long&gt;（编码见 {@code DemoCatalog.STORES}，如 DEMO-AMZ-US） */
    public static final String STORE_BY_CODE = "storeByCode";
    /** 物料编码 → 物料 ID：Map&lt;String, Long&gt;（编码 DM- 开头） */
    public static final String MATERIAL_BY_CODE = "materialByCode";
    /** 演示在线商品 ID 列表（已配对的，含组合商品）：List&lt;Long&gt; */
    public static final String LISTING_IDS = "listingIds";
    /** 平台仓（FBA / 海外仓）ID 列表：List&lt;Long&gt; */
    public static final String PLATFORM_WAREHOUSE_IDS = "platformWarehouseIds";
    /** 不良品仓 ID：Long */
    public static final String DEFECT_WAREHOUSE_ID = "defectWarehouseId";
    /** 物流渠道编码 → ID：Map&lt;String, Long&gt;（头程 DEMO-AIR-US 等，尾程 DEMO-EXP 等） */
    public static final String CHANNEL_BY_CODE = "channelByCode";
    /** 运营演示用户 ID 列表（角色电商运营；创建失败时为空）：List&lt;Long&gt; */
    public static final String OPERATOR_USER_IDS = "operatorUserIds";
    /** 演示订单 ID 列表（全部，按下单时间）：List&lt;Long&gt; */
    public static final String ORDER_IDS = "orderIds";

    /** 演示物料编码前缀 */
    public static final String MATERIAL_CODE_PREFIX = "DM-";

    /** 演示店铺编码前缀 */
    public static final String STORE_CODE_PREFIX = "DEMO-";

    private DemoKeys() {
    }
}
