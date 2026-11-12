package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.modules.ecom.model.EcomCarrier;

import java.util.List;

/**
 * 物流商面单接口（SPI）：每个对接的物流商实现一个，注册为 Bean，由 {@link CarrierRegistry} 按 {@link EcomCarrier} 查找。
 * 与平台连接器（connector/ChannelConnector）相互独立：平台连接器拉单、回传发货，物流商接口下单取号、取面单。
 * <p>
 * 调用方保证不在数据库事务中调用；接口错误统一抛出 {@code ecom.error.ops.carrier*} 业务异常
 */
public interface CarrierConnector {

    EcomCarrier carrier();

    /** 授权字段、正式 / 测试环境地址 */
    CarrierInfo info();

    /** 向物流商下单，返回物流商订单号与运单号（部分物流商同时返回面单地址） */
    CarrierWaybill createOrder(CarrierContext ctx, CarrierShipment shipment);

    /** 面单 PDF */
    byte[] label(CarrierContext ctx, CarrierWaybill waybill);

    /** 取消（作废）物流商订单 */
    void cancel(CarrierContext ctx, CarrierWaybill waybill);

    /** 轨迹 */
    List<CarrierTrackEvent> track(CarrierContext ctx, String trackingNo);

    /**
     * 轨迹与归一后的状态（物流跟踪定时拉取使用）：默认按最新一条轨迹的描述判断状态；物流商返回状态码时覆盖本方法
     */
    default CarrierTrackInfo trackInfo(CarrierContext ctx, String trackingNo) {
        return CarrierTrackInfo.of(track(ctx, trackingNo));
    }
}
