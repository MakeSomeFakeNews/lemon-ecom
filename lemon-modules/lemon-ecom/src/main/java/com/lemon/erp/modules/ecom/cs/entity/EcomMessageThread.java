package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsThreadStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 买家消息会话：店铺 × 平台会话 ID 唯一。状态按消息计算：最近有未回复的买家消息为待回复，
 * 回复截止 replyDeadline = 最早一条未回复的买家消息时间 + 平台回复时限，超过为超时
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_message_thread")
public class EcomMessageThread extends BaseEntity {

    private Long storeId;
    private EcomPlatform platform;
    private String conversationId;
    private String buyerId;
    private String buyerName;
    private Long orderId;
    private String platformOrderNo;
    private String subject;
    private String lastMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;
    private Integer unread;
    private CsThreadStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyDeadline;
    private Long ownerId;
    private String tags;
    private String remark;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String ownerName;
    /** 待回复且已过回复截止 */
    @TableField(exist = false)
    private Boolean overdue;
}
