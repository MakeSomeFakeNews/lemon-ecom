package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台渠道接口：每个平台一个实现（Spring Bean），按店铺的授权信息拉取订单、回传发货。
 * 没有实现的平台只能手工导入订单；新增平台时实现本接口即可，不需要改动订单服务
 */
public interface ChannelConnector {

    EcomPlatform platform();

    /**
     * 拉取 since 之后创建或更新的订单（含明细）
     */
    List<OrderDraft> fetchOrders(EcomStore store, StoreCredentials credentials, LocalDateTime since);

    /** 是否支持回传发货 */
    default boolean supportsShipmentConfirm() {
        return false;
    }

    /** 向平台回传发货（承运商、运单号），不支持时抛出异常 */
    default void confirmShipment(EcomStore store, StoreCredentials credentials, EcomOrder order, List<EcomOrderItem> items) {
        throw new BizException("ecom.error.connectorNoShipmentConfirm", platform().name());
    }

    /** 测试授权是否有效：默认拉取最近一天的订单 */
    default void test(EcomStore store, StoreCredentials credentials) {
        fetchOrders(store, credentials, LocalDateTime.now().minusDays(1));
    }
}
