package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.service.AutoTodoService;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/**
 * 待办提醒：给处理人（指定用户、角色、店铺负责人）各建一条待办，出现在首页「我的待办」、电商看板和「待办提醒」页面；
 * 同一规则同一对象已有未完成的待办时不重复创建。期限 = 今天 + dueDays（为空不设期限）
 */
@Component
@RequiredArgsConstructor
public class TodoActionHandler implements AutoActionHandler {

    private final AutoActionSupport support;
    private final AutoTodoService todoService;

    @Override
    public AutoActionType type() {
        return AutoActionType.TODO;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        AutoActionSupport.requireRecipients(a);
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return Messages.get("ecom.text.auto.desc.todo", support.recipientText(a, e), NotifyActionHandler.title(a, e, rule),
                a.dueDays() == null ? "-" : LocalDate.now().plusDays(a.dueDays()).toString());
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        Set<Long> users = support.recipients(a, e);
        if (users.isEmpty()) {
            throw new BizException("ecom.error.auto.noRecipient");
        }
        int n = todoService.create(rule.getId(), users, e, NotifyActionHandler.title(a, e, rule), NotifyActionHandler.content(a, e, rule),
                a.dueDays() == null ? null : LocalDate.now().plusDays(a.dueDays()));
        return n == 0 ? AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.todoExists"))
                : AutoOutcome.success(AutoText.t("ecom.text.auto.out.todoCreated", n));
    }
}
