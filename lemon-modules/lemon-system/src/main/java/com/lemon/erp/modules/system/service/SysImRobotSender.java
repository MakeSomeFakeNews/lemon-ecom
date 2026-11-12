package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.framework.message.ImRobotSender;
import com.lemon.erp.modules.system.entity.SysImConfig;
import com.lemon.erp.modules.system.im.ImCard;
import com.lemon.erp.modules.system.im.ImClient;
import com.lemon.erp.modules.system.im.ImPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 群机器人推送（{@link ImRobotSender} 的实现）：指定了 Webhook 时直接发送（地址必须是该平台的群机器人地址），
 * 否则使用「第三方集成」中该平台已启用的配置里的群机器人
 */
@Service
@RequiredArgsConstructor
public class SysImRobotSender implements ImRobotSender {

    private final SysImConfigService configService;

    @Override
    public void send(ImChannel platform, String webhook, String secret, String title, String content, String route) {
        if (platform == null) {
            throw new BizException("system.error.robotPlatformInvalid", "-");
        }
        ImPlatform p = ImPlatform.valueOf(platform.name());
        ImClient client = configService.client(p);
        SysImConfig config = configService.find(p);
        String url = webhook;
        String key = secret;
        if (!StringUtils.hasText(url)) {
            config = configService.requireEnabled(p);
            if (!StringUtils.hasText(config.getRobotWebhook())) {
                throw new BizException("system.error.enterSaveGroupBotWebhookUrl");
            }
            url = config.getRobotWebhook();
            key = config.getRobotSecret();
        } else if (!url.trim().startsWith(client.robotPrefix())) {
            throw new BizException("system.error.groupBotWebhookUrlMustStart", p.text(), client.robotPrefix());
        }
        String link = config != null && Boolean.TRUE.equals(config.getEnabled()) ? SysImPushService.link(config, route) : null;
        client.robot(url.trim(), key, new ImCard(title, content, link));
    }
}
