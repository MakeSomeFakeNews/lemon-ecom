package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则的范围、条件、动作与 JSON 列互转；动作中的密钥（群机器人加签密钥、Webhook 签名密钥）加密存储，
 * 返回前端时为掩码，保存时传回掩码表示不修改（按动作位置和类型取原来的密钥）
 */
@Component
@RequiredArgsConstructor
public class AutoRuleCodec {

    public static final String MASK = "******";

    private final CredentialCipher cipher;

    /** JSON 列 → 范围、条件、动作（密钥保持密文） */
    public EcomAutoRule read(EcomAutoRule r) {
        r.setScope(r.getScopeJson() == null ? AutoRuleParts.Scope.empty() : HttpJson.MAPPER.readValue(r.getScopeJson(), AutoRuleParts.Scope.class));
        r.setCondition(r.getConditionJson() == null ? AutoRuleParts.Group.empty()
                : HttpJson.MAPPER.readValue(r.getConditionJson(), AutoRuleParts.Group.class));
        r.setActions(r.getActionsJson() == null ? new ArrayList<>()
                : HttpJson.MAPPER.readValue(r.getActionsJson(), new TypeReference<List<AutoRuleParts.Action>>() {
        }));
        return r;
    }

    /**
     * 范围、条件、动作 → JSON 列；密钥为掩码时取 previous 中同一位置、同一类型动作的密钥，否则加密
     *
     * @param previous 修改前的动作（新建为空）
     */
    public void write(EcomAutoRule r, List<AutoRuleParts.Action> previous) {
        List<AutoRuleParts.Action> actions = new ArrayList<>();
        for (int i = 0; i < r.getActions().size(); i++) {
            AutoRuleParts.Action a = r.getActions().get(i);
            String secret = a.secret();
            if (MASK.equals(secret)) {
                AutoRuleParts.Action old = previous != null && i < previous.size() && previous.get(i).type() == a.type() ? previous.get(i) : null;
                secret = old == null ? null : old.secret();
            } else if (StringUtils.hasText(secret)) {
                secret = cipher.encrypt(secret.trim());
            } else {
                secret = null;
            }
            actions.add(a.withSecret(secret));
        }
        r.setActions(actions);
        r.setScopeJson(HttpJson.MAPPER.writeValueAsString(r.getScope() == null ? AutoRuleParts.Scope.empty() : r.getScope()));
        r.setConditionJson(HttpJson.MAPPER.writeValueAsString(r.getCondition() == null ? AutoRuleParts.Group.empty() : r.getCondition()));
        r.setActionsJson(HttpJson.MAPPER.writeValueAsString(actions));
    }

    /** 返回前端：密钥替换为掩码 */
    public EcomAutoRule masked(EcomAutoRule r) {
        if (r.getActions() != null) {
            r.setActions(r.getActions().stream().map(a -> StringUtils.hasText(a.secret()) ? a.withSecret(MASK) : a).toList());
        }
        return r;
    }

    /** 动作的明文密钥（执行时使用） */
    public String secret(AutoRuleParts.Action a) {
        return MASK.equals(a.secret()) ? null : cipher.decrypt(a.secret());
    }

    public static String json(Object v) {
        return HttpJson.MAPPER.writeValueAsString(v);
    }
}
