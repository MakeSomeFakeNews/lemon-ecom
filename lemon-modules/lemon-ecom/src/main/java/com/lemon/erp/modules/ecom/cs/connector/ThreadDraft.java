package com.lemon.erp.modules.ecom.cs.connector;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台返回的一个会话（含本次拉取到的消息）
 *
 * @param conversationId  平台会话 ID（店铺内唯一）
 * @param buyerId         买家 ID（发送消息时作为收件人，部分平台需要）
 * @param unreadCount     平台记录的未读数，没有为 null（按新收到的买家消息累加）
 * @param platformOrderNo 会话关联的订单号（平台直接给出时），没有为 null
 */
public record ThreadDraft(String conversationId, String buyerId, String buyerName, String subject, Integer unreadCount,
                          LocalDateTime lastMessageTime, String platformOrderNo, List<MessageDraft> messages) {

    public ThreadDraft {
        messages = messages == null ? new ArrayList<>() : messages;
    }
}
