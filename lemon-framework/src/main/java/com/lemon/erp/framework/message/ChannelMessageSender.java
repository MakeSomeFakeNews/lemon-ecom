package com.lemon.erp.framework.message;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 按渠道发送消息：写入站内消息，并且<b>只</b>推送到调用方指定的第三方平台（钉钉、企业微信、飞书的应用消息，发给已绑定的成员），
 * 不按系统的全局设置推送到全部平台。用于用户可以自选推送渠道的订阅类通知（如运营日报）。由系统模块实现。
 * <p>
 * 推送在事务提交后异步执行，失败只记日志、不影响站内消息；平台没有接入或没有开启应用消息时跳过该平台。
 */
public interface ChannelMessageSender {

    /** 已接入且开启了应用消息推送的平台（页面据此列出可选渠道；为空时只能站内消息） */
    List<ImChannel> enabledChannels();

    /**
     * 发送
     *
     * @param userIds  接收人（忽略空 ID、重复 ID）
     * @param message  消息（类别、标题、内容、站内链接、去重键）
     * @param site     是否写入站内消息
     * @param channels 同时推送的第三方平台，可为空
     */
    void send(Collection<Long> userIds, MessageSender.Message message, boolean site, Set<ImChannel> channels);
}
