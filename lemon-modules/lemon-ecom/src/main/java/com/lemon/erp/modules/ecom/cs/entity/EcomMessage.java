package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsDirection;
import com.lemon.erp.modules.ecom.cs.model.CsSendStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话中的一条消息：买家消息由同步写入（按 会话 + 平台消息 ID 去重）；卖家回复先保存为发送中，调用平台接口后为已发送 / 发送失败
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_message")
public class EcomMessage extends BaseEntity {

    private Long threadId;
    private String platformMessageId;
    private CsDirection direction;
    private String msgType;
    private String content;
    /** 附件地址（JSON 数组） */
    private String attachments;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentTime;
    private CsSendStatus sendStatus;
    private String errorMessage;
    private Long templateId;
    private Long senderId;

    @TableField(exist = false)
    private String senderName;
}
