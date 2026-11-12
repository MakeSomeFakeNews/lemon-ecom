package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.message.ImRobotSender;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.service.AutoRuleCodec;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 群机器人：发送到钉钉 / 企业微信 / 飞书群（经系统模块的 {@link ImRobotSender}）。没有填写 Webhook 时使用「第三方集成」中该平台配置的群机器人；
 * 填写时地址必须是该平台的群机器人地址，加签密钥加密保存
 */
@Component
@RequiredArgsConstructor
public class ImRobotActionHandler implements AutoActionHandler {

    private final ImRobotSender robotSender;
    private final AutoRuleCodec codec;

    @Override
    public AutoActionType type() {
        return AutoActionType.IM_ROBOT;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        if (a.imPlatform() == null) {
            throw new BizException("ecom.error.auto.robotPlatformRequired");
        }
        if (StringUtils.hasText(a.webhook()) && !a.webhook().trim().startsWith("https://")) {
            throw new BizException("ecom.error.auto.urlInvalid");
        }
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return Messages.get("ecom.text.auto.desc.robot", Messages.get(AutoText.key("ecom.text.auto.channel", a.imPlatform())),
                StringUtils.hasText(a.webhook()) ? Messages.get("ecom.text.auto.robotCustom") : Messages.get("ecom.text.auto.robotDefault"),
                NotifyActionHandler.title(a, e, rule));
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        robotSender.send(a.imPlatform(), StringUtils.hasText(a.webhook()) ? a.webhook().trim() : null, codec.secret(a),
                AutoText.cut(NotifyActionHandler.title(a, e, rule), 128), AutoText.cut(NotifyActionHandler.content(a, e, rule), 1000), e.link());
        return AutoOutcome.success(AutoText.t("ecom.text.auto.out.robotSent", AutoText.t(AutoText.key("ecom.text.auto.channel", a.imPlatform()))));
    }
}
