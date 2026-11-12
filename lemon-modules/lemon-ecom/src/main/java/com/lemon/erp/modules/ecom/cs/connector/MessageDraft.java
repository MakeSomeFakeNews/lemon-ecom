package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.modules.ecom.cs.model.CsDirection;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台返回的一条消息
 *
 * @param messageId       平台消息 ID（同一会话内唯一，用于去重）
 * @param type            消息类型（平台原值，如 text、image、order）
 * @param content         文字内容（图片等为说明或空）
 * @param attachments     附件地址（图片、视频等）
 * @param platformOrderNo 订单卡片等消息中带的平台订单号，没有为 null
 */
public record MessageDraft(String messageId, CsDirection direction, String type, String content, List<String> attachments,
                           LocalDateTime sentTime, String platformOrderNo) {
}
