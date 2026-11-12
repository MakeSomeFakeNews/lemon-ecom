package com.lemon.erp.modules.ecom.cs.dto;

import com.lemon.erp.modules.ecom.cs.model.CsThreadStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;

/**
 * 会话列表条件
 */
@Data
public class ThreadQuery {

    private Long storeId;
    private EcomPlatform platform;
    private CsThreadStatus status;
    /** 只看超时（待回复且已过回复截止） */
    private Boolean overdue;
    /** 只看未读 */
    private Boolean unread;
    /** 负责人 */
    private Long ownerId;
    /** 买家、订单号、最近消息、标签 */
    private String keyword;
    /** 超时判断的当前时间（服务端填写） */
    private java.time.LocalDateTime now;
}
