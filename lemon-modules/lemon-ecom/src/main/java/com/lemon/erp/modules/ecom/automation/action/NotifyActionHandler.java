package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.message.ChannelMessageSender;
import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 通知：发给指定用户、角色下的用户、店铺负责人；写入站内消息，并按选择推送到钉钉 / 企业微信 / 飞书的应用消息
 * （经系统模块的 {@link ChannelMessageSender}，只推送到已接入、开启了应用消息的平台中已绑定的成员）。标题、内容支持 ${字段} 变量
 */
@Component
@RequiredArgsConstructor
public class NotifyActionHandler implements AutoActionHandler {

    private static final int TITLE_MAX = 128;
    private static final int CONTENT_MAX = 500;

    private final AutoActionSupport support;
    private final ChannelMessageSender channelSender;

    @Override
    public AutoActionType type() {
        return AutoActionType.NOTIFY;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        AutoActionSupport.requireRecipients(a);
        if (Boolean.FALSE.equals(a.site()) && (a.channels() == null || a.channels().isEmpty())) {
            throw new BizException("ecom.error.auto.channelRequired");
        }
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return Messages.get("ecom.text.auto.desc.notify", support.recipientText(a, e), channelText(a), title(a, e, rule));
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        Set<Long> users = support.recipients(a, e);
        if (users.isEmpty()) {
            throw new BizException("ecom.error.auto.noRecipient");
        }
        Set<ImChannel> channels = a.channels() == null || a.channels().isEmpty() ? Set.of() : EnumSet.copyOf(a.channels());
        MessageSender.Message message = MessageSender.Message.of(MessageCategory.ALERT, AutoText.cut(title(a, e, rule), TITLE_MAX),
                AutoText.cut(content(a, e, rule), CONTENT_MAX), e.link());
        channelSender.send(users, message, !Boolean.FALSE.equals(a.site()), channels);
        return AutoOutcome.success(AutoText.t("ecom.text.auto.out.notified", users.size(), channelText(a)));
    }

    static String title(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return StringUtils.hasText(a.title()) ? AutoText.render(a.title(), e, rule) : AutoText.defaultTitle(e, rule);
    }

    static String content(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return StringUtils.hasText(a.content()) ? AutoText.render(a.content(), e, rule) : AutoText.defaultContent(e);
    }

    private static String channelText(AutoRuleParts.Action a) {
        List<String> names = new ArrayList<>();
        if (!Boolean.FALSE.equals(a.site())) {
            names.add(Messages.get("ecom.text.auto.channel.SITE"));
        }
        if (a.channels() != null) {
            a.channels().forEach(c -> names.add(Messages.get(AutoText.key("ecom.text.auto.channel", c.name()))));
        }
        return String.join(" + ", names);
    }
}
