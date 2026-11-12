package com.lemon.erp.modules.ecom.cs.connector;

import java.time.LocalDateTime;

/**
 * 回复发送成功后平台返回的消息 ID 与时间（平台没有返回时间时为 null，取发送时间）
 */
public record SentMessage(String messageId, LocalDateTime sentTime) {
}
