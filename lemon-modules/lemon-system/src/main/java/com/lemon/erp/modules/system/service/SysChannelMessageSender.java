package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.message.ChannelMessageSender;
import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.entity.SysImUser;
import com.lemon.erp.modules.system.im.ImCard;
import com.lemon.erp.modules.system.im.ImPlatform;
import com.lemon.erp.modules.system.mapper.SysImUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@link ChannelMessageSender} 的实现：站内消息只写入（不触发 {@link SysImPushService} 的全局推送），
 * 事务提交后异步推送到调用方指定、且已接入并开启应用消息的平台（发给已绑定的成员，链接经平台客户端内的免登入口打开）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysChannelMessageSender implements ChannelMessageSender {

    /** 提交后推送的事件 */
    public record ChannelPushEvent(List<Long> userIds, MessageSender.Message message, Set<ImChannel> channels) {
    }

    private final SysMessageService messageService;
    private final SysImConfigService configService;
    private final SysImUserMapper imUserMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    public List<ImChannel> enabledChannels() {
        return configService.enabledList().stream().filter(c -> Boolean.TRUE.equals(c.getMessageEnabled()))
                .map(c -> ImChannel.valueOf(c.getPlatform().name())).distinct().sorted().toList();
    }

    @Override
    public void send(Collection<Long> userIds, MessageSender.Message message, boolean site, Set<ImChannel> channels) {
        List<Long> targets;
        if (site) {
            targets = messageService.write(userIds, message);
        } else {
            Set<Long> set = new LinkedHashSet<>();
            userIds.stream().filter(Objects::nonNull).forEach(set::add);
            targets = List.copyOf(set);
        }
        if (!targets.isEmpty() && channels != null && !channels.isEmpty()) {
            publisher.publishEvent(new ChannelPushEvent(targets, message, EnumSet.copyOf(channels)));
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPush(ChannelPushEvent e) {
        for (SysImConfig c : configService.enabledList()) {
            ImPlatform p = c.getPlatform();
            if (!Boolean.TRUE.equals(c.getMessageEnabled()) || !e.channels().contains(ImChannel.valueOf(p.name()))) {
                continue;
            }
            List<String> members = imUserMapper.selectList(Wrappers.<SysImUser>lambdaQuery()
                    .eq(SysImUser::getPlatform, p).in(SysImUser::getUserId, e.userIds())).stream().map(SysImUser::getImUserId).toList();
            if (members.isEmpty()) {
                continue;
            }
            try {
                configService.client(p).send(c, members, new ImCard(e.message().title(), e.message().content(), SysImPushService.link(c, e.message().link())));
            } catch (RuntimeException ex) {
                log.warn("推送{}消息失败：{}", p.defaultText(), ex.getMessage()); // i18n-ignore 日志
            }
        }
    }
}
