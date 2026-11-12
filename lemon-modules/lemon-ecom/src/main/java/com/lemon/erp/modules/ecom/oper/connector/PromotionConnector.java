package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotionItem;

import java.util.List;

/**
 * 促销活动推送（Spring Bean，每个平台一个实现）：用店铺的渠道授权（调用前由服务端刷新令牌）在平台创建活动并添加参与商品。
 * 没有实现的平台（亚马逊的优惠券、秒杀、Prime 折扣等没有开放创建接口）只能在平台后台创建、在系统中手工维护
 */
public interface PromotionConnector {

    EcomPlatform platform();

    /**
     * 创建平台活动并添加参与商品；活动已推送过（platformPromoId 不为空）时只重新提交商品
     *
     * @return 平台活动 ID
     */
    String push(ConnectorContext ctx, EcomPromotion promotion, List<EcomPromotionItem> items);

    /** 取消平台活动：未开始的删除，进行中的提前结束 */
    void cancel(ConnectorContext ctx, EcomPromotion promotion);
}
