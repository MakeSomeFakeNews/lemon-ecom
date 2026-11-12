package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

/**
 * 商品监控的平台取数（Spring Bean，每个平台一个实现）：用店铺的渠道授权（调用前由服务端刷新令牌）查询商品的排名、属性、图片、在售卖家数。
 * 没有实现的平台只能手工录入或 Excel 导入快照
 */
public interface MonitorConnector {

    EcomPlatform platform();

    /**
     * 查询一个商品（可以是竞品）在授权店铺所在站点的数据
     *
     * @param itemId ASIN / 平台商品 ID
     */
    MonitorData fetch(ConnectorContext ctx, String itemId);
}
