package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台渠道接口：每个平台一个实现（Spring Bean），一个 ERP 同时管理多个平台、每个平台多个店铺——
 * 店铺记录平台、授权信息（加密）和环境（沙箱 / 正式），调用时由框架组装 {@link ConnectorContext} 传入。
 * <p>
 * 新增平台：实现本接口，{@link #info()} 声明授权方式、授权字段、接口地址与能力，按能力实现对应方法；
 * 不需要改动订单、库存、店铺等服务。OAuth 平台还要实现 {@link #authorizeUrl} 与 {@link #exchangeCode}，
 * 令牌会过期的在每次调用前用 {@link #ensureToken} 刷新（刷新后调用 {@link ConnectorContext#updateCredentials} 保存）。
 * 平台接口返回错误时抛出 {@code BizException}（ecom.error.connector*），错误信息会显示在同步日志和店铺页面。
 * <p>
 * HTTP 调用统一用 {@link HttpJson}（非 2xx 抛 {@link ConnectorHttpException}，可按状态码判断；429 / 503 自动重试），
 * 取值与时间换算用 {@link ConnectorJson}；时间一律换算为服务器默认时区。错误信息、日志中不要带令牌、密钥与签名
 * （{@link HttpJson#abbreviate} 会遮盖常见的键）。连接器自己缓存的令牌、过期时间在 {@link CredentialField#cached} 中声明
 */
public interface ChannelConnector {

    ConnectorInfo info();

    default EcomPlatform platform() {
        return info().platform();
    }

    // ---------------------------------------------------------------- 授权

    /**
     * OAuth 授权页地址：卖家打开后登录平台、同意授权，平台回调 redirectUri（带 code 与 state）
     *
     * @param state 框架生成的签名参数（含租户、店铺），回调时原样带回
     */
    default String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        throw new BizException("ecom.error.connectorNoOAuth", platform().name());
    }

    /**
     * OAuth 回调：用授权码换取令牌
     *
     * @param params 回调地址上的全部参数（code、shop_id 等，各平台不同）
     */
    default OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        throw new BizException("ecom.error.connectorNoOAuth", platform().name());
    }

    /** 令牌快过期时刷新（默认不需要），每次调用接口前由连接器自己调用 */
    default void ensureToken(ConnectorContext ctx) {
    }

    /** 测试授权是否有效：默认拉取最近一天的订单 */
    default void test(ConnectorContext ctx) {
        fetchOrders(ctx, LocalDateTime.now().minusDays(1));
    }

    // ---------------------------------------------------------------- 能力

    /** 拉取 since 之后创建或更新的订单（含明细），能力 ORDERS */
    List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since);

    /**
     * 回传发货（承运商、运单号），能力 SHIP_CONFIRM。
     * 平台物流托管（运单号由平台分配）时返回 {@link ShipmentResult#platformManaged}，尽量带回平台运单号与承运商，
     * 框架会写入订单（以及还没有运单号的包裹）；没有需要回传的内容时返回 {@link ShipmentResult#skipped}。失败时抛出异常
     */
    default ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        throw new BizException("ecom.error.connectorNoCapability", platform().name(), ConnectorCapability.SHIP_CONFIRM.name());
    }

    /** 回传可售库存，能力 INVENTORY_PUSH */
    default void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        throw new BizException("ecom.error.connectorNoCapability", platform().name(), ConnectorCapability.INVENTORY_PUSH.name());
    }

    /** 拉取在线商品，能力 LISTINGS */
    default List<ListingDraft> fetchListings(ConnectorContext ctx) {
        throw new BizException("ecom.error.connectorNoCapability", platform().name(), ConnectorCapability.LISTINGS.name());
    }

    default boolean supports(ConnectorCapability capability) {
        return info().supports(capability);
    }
}
