package com.lemon.erp.framework.message;

/**
 * 发送到钉钉 / 企业微信 / 飞书群机器人，由系统模块实现（{@code SysImRobotSender}）。
 * 业务模块（如电商自动化规则）按自己的配置推送群消息时使用；调用会访问外网，不要在事务中调用
 */
public interface ImRobotSender {

    /**
     * @param platform 平台
     * @param webhook  群机器人 Webhook 地址；为空时使用「第三方集成」中该平台配置的群机器人
     * @param secret   加签密钥（钉钉 / 飞书），为空不加签；webhook 为空时使用集成中配置的密钥
     * @param route    点击跳转的前端路由（集成配置了系统访问地址时转为免登链接），可为空
     * @throws com.lemon.erp.common.exception.BizException 平台未启用、没有配置群机器人、地址不是该平台的群机器人地址、平台返回错误
     */
    void send(ImChannel platform, String webhook, String secret, String title, String content, String route);
}
