package com.lemon.erp.modules.ecom.cs.connector;

/**
 * 发送回复、标记已读时传给连接器的会话信息
 *
 * @param lastMessageId 最近一条买家消息的平台 ID（标记已读时使用），没有为 null
 */
public record ThreadRef(String conversationId, String buyerId, String platformOrderNo, String lastMessageId) {
}
