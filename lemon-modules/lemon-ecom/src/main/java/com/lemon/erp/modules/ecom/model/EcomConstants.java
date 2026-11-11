package com.lemon.erp.modules.ecom.model;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;

/**
 * 跨境电商模块的常量：数据权限规则、业务字典名称
 */
public final class EcomConstants {

    private EcomConstants() {
    }

    /** 平台订单按制单人部门 + 制单人做数据权限 */
    public static final DataPermissionRule ORDER_RULE = new DataPermissionRule("ecom_order", "dept_id", "create_by");
    /** 头程货件 */
    public static final DataPermissionRule INBOUND_RULE = new DataPermissionRule("ecom_inbound", "dept_id", "create_by");
    /** 退货退款 */
    public static final DataPermissionRule RETURN_RULE = new DataPermissionRule("ecom_return", "dept_id", "create_by");

    /** 业务字典：站点 / 市场 */
    public static final String MARKETPLACE_DICT = "ecom_marketplace";
    /** 业务字典：运输方式 */
    public static final String TRANSPORT_MODE_DICT = "ecom_transport_mode";
    /** 业务字典：退货原因 */
    public static final String RETURN_REASON_DICT = "ecom_return_reason";

    /** 补货建议的日均销量统计天数 */
    public static final int VELOCITY_DAYS = 30;
}
