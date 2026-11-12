package com.lemon.erp.modules.ecom.assistant.model;

import com.lemon.erp.framework.message.ImChannel;

/** 运营日报订阅的推送渠道（字典 ecom_ops_push_channel）：站内消息，或系统「第三方集成」中接入的平台 */
public enum OpsPushChannel {
    SITE(null),
    DINGTALK(ImChannel.DINGTALK),
    WECOM(ImChannel.WECOM),
    FEISHU(ImChannel.FEISHU);

    private final ImChannel im;

    OpsPushChannel(ImChannel im) {
        this.im = im;
    }

    /** 对应的第三方平台，站内消息为 null */
    public ImChannel im() {
        return im;
    }
}
