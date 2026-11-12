package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.service.AutoRuleCodec;
import com.lemon.erp.modules.ecom.automation.service.AutoStoredEvent;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook：把事件 POST 到指定地址（JSON：规则、触发器、对象、店铺、字段值、关联对象 ID、页面地址、时间），签名与重试见 {@link AutoWebhookClient}。
 * 在规则执行的后台线程中调用（不在事务中），失败只记录在执行日志中，可在日志页重试
 */
@Component
@RequiredArgsConstructor
public class WebhookActionHandler implements AutoActionHandler {

    private final AutoWebhookClient client;
    private final AutoRuleCodec codec;

    @Override
    public AutoActionType type() {
        return AutoActionType.WEBHOOK;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        if (!StringUtils.hasText(a.webhook()) || !(a.webhook().trim().startsWith("https://") || a.webhook().trim().startsWith("http://"))) {
            throw new BizException("ecom.error.auto.urlInvalid");
        }
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return Messages.get("ecom.text.auto.desc.webhook", a.webhook(),
                Messages.get(StringUtils.hasText(a.secret()) ? "ecom.text.auto.signed" : "ecom.text.auto.unsigned"));
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        AutoWebhookClient.Result r = client.post(a.webhook().trim(), codec.secret(a), payload(e, rule));
        return AutoOutcome.success(AutoText.t("ecom.text.auto.out.webhook", r.status(), r.attempts()));
    }

    /** 请求体 */
    static String payload(AutoEvent e, EcomAutoRule rule) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenantId", TenantContext.get());
        body.put("ruleId", rule.getId());
        body.put("ruleName", rule.getName());
        body.put("trigger", e.trigger().name());
        body.put("triggerName", AutoText.t(e.trigger().label()));
        body.put("objectKey", e.objectKey());
        body.put("objectLabel", e.objectLabel());
        body.put("event", HttpJson.MAPPER.readValue(AutoStoredEvent.json(e), Map.class));
        return HttpJson.MAPPER.writeValueAsString(body);
    }
}
