package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.util.List;
import java.util.Set;

/**
 * 刊登与定价连接器（SPI）：查询类目与属性、发布商品、查询异步发布结果、下架、推送价格、拉取竞品价格。
 * 与订单的 {@code ChannelConnector} 分开，按平台各实现一个并注册为 Bean，由 {@link PublishConnectorRegistry} 按 {@link EcomPlatform} 查找。
 * <p>
 * 复用店铺的渠道授权：调用方组装 {@link ConnectorContext}（解密的授权、环境、自定义地址），并先由该平台的 {@code ChannelConnector.ensureToken}
 * 刷新令牌（刷新后的令牌由框架加密写回店铺），连接器只读取 ctx 中的令牌。地址规则与订单连接器相同：店铺填写了自定义地址时全部指向它
 * （测试用的模拟服务器），否则按沙箱开关取默认地址。
 * <p>
 * 方法按 {@link #capabilities()} 声明实现，未实现的默认抛出 {@code ecom.error.pub.capabilityUnsupported}。
 * 接口调用不放在数据库事务中；平台错误统一转为 BizException（消息键 ecom.error.connector*），或在 {@link PublishResult#failed} / {@link PriceResult#fail} 中返回。
 * 库存的推送沿用订单连接器的 {@code pushInventory}（能力 INVENTORY_PUSH）
 */
public interface PublishConnector {

    EcomPlatform platform();

    Set<PublishCapability> capabilities();

    default boolean supports(PublishCapability capability) {
        return capabilities().contains(capability);
    }

    /** 发布时必须选择类目（默认：能查询类目的平台都要求） */
    default boolean categoryRequired() {
        return supports(PublishCapability.CATEGORIES);
    }

    /** 至少需要的图片数（默认 1） */
    default int minImages() {
        return 1;
    }

    /** 平台刊登的说明（消息键，如亚马逊 ASIN 要等平台生成），没有为 null */
    default String note() {
        return null;
    }

    /**
     * 类目：keyword 不为空时按关键字搜索，否则返回 parentId 的下级（parentId 为空时为顶级）
     */
    default List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        throw unsupported(PublishCapability.CATEGORIES);
    }

    /** 类目的属性（必填项在前） */
    default List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        throw unsupported(PublishCapability.ATTRIBUTES);
    }

    /** 发布：成功、平台异步处理中（返回 ref）或失败（返回平台错误） */
    default PublishResult publish(ConnectorContext ctx, PublishPayload payload) {
        throw unsupported(PublishCapability.PUBLISH);
    }

    /** 查询异步发布的结果（publish 返回 PENDING 的平台实现） */
    default PublishResult checkPublish(ConnectorContext ctx, PublishPayload payload, String ref) {
        return PublishResult.pending(ref, null);
    }

    /** 下架：refs 为刊登记录生成的在线商品 */
    default void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        throw unsupported(PublishCapability.OFFLINE);
    }

    /** 推送价格，逐条返回结果（不抛出单个 SKU 的错误） */
    default List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        throw unsupported(PublishCapability.PRICE);
    }

    /** 拉取竞品价格（没有数据的商品不在结果中） */
    default List<CompetitivePrice> competitivePrices(ConnectorContext ctx, List<ListingRef> refs) {
        throw unsupported(PublishCapability.COMPETITIVE_PRICE);
    }

    private BizException unsupported(PublishCapability capability) {
        return new BizException("ecom.error.pub.capabilityUnsupported", platform().name(), capability.name());
    }
}
