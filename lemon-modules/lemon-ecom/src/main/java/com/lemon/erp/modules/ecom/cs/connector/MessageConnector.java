package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 客服连接器（SPI）：买家消息、商品评价、请求评价。与订单的 {@code ChannelConnector} 分开，按平台各实现一个并注册为 Bean，
 * 由 {@link MessageConnectorRegistry} 按 {@link EcomPlatform} 查找。
 * <p>
 * 复用店铺的渠道授权：调用方用 {@code EcomStoreService.context(store)} 组装 {@link ConnectorContext}（解密的授权、环境、自定义地址），
 * 调用前先由该平台的 {@code ChannelConnector.ensureToken} 刷新令牌（刷新后的令牌由框架加密写回店铺），连接器只读取 ctx 中的令牌，不自己刷新。
 * 地址规则与订单连接器相同：店铺填写了自定义地址时全部指向它（测试用的模拟服务器），否则按沙箱开关取默认地址。
 * <p>
 * 方法按 {@link #capabilities()} 声明实现，未实现的默认抛出 {@code ecom.error.cs.capabilityUnsupported}。
 * 接口调用不放在数据库事务中；平台错误统一转为 BizException（消息键 ecom.error.connector*）
 */
public interface MessageConnector {

    EcomPlatform platform();

    Set<CsCapability> capabilities();

    default boolean supports(CsCapability capability) {
        return capabilities().contains(capability);
    }

    /** 平台要求的回复时限（小时）：最早一条未回复的买家消息超过它算超时 */
    default int replyHours() {
        return com.lemon.erp.modules.ecom.cs.model.CsConstants.DEFAULT_REPLY_HOURS;
    }

    /**
     * 拉取 since 之后有新消息的会话，每个会话带上 since 之后的消息（按时间升序或任意顺序，框架按消息 ID 去重）
     */
    default List<ThreadDraft> fetchThreads(ConnectorContext ctx, LocalDateTime since) {
        throw unsupported(CsCapability.MESSAGES);
    }

    /** 发送文字回复，返回平台消息 ID */
    default SentMessage sendMessage(ConnectorContext ctx, ThreadRef thread, String content) {
        throw unsupported(CsCapability.SEND_MESSAGE);
    }

    /** 把会话标记为已读（可选，失败不影响本地） */
    default void markRead(ConnectorContext ctx, ThreadRef thread) {
    }

    /** 拉取 since 之后新增或有变化的商品评价 */
    default List<ReviewDraft> fetchReviews(ConnectorContext ctx, LocalDateTime since) {
        throw unsupported(CsCapability.REVIEWS);
    }

    /** 回复商品评价 */
    default void replyReview(ConnectorContext ctx, String reviewId, String content) {
        throw unsupported(CsCapability.REVIEW_REPLY);
    }

    /** 请求买家对订单留评 */
    default SolicitationResult requestReview(ConnectorContext ctx, String platformOrderNo) {
        throw unsupported(CsCapability.REVIEW_REQUEST);
    }

    private BizException unsupported(CsCapability capability) {
        return new BizException("ecom.error.cs.capabilityUnsupported", platform().name(), capability.name());
    }
}
