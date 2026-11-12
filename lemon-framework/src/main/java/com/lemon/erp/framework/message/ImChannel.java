package com.lemon.erp.framework.message;

/**
 * 第三方即时通讯平台（与系统模块「第三方集成」的平台一一对应，名称相同）：业务模块按渠道推送消息时使用
 */
public enum ImChannel {
    /** 钉钉 */
    DINGTALK,
    /** 企业微信 */
    WECOM,
    /** 飞书 */
    FEISHU
}
