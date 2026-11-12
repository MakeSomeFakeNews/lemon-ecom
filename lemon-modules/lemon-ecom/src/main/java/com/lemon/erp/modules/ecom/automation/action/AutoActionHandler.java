package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;

/**
 * 一种自动化动作的处理器（Spring Bean，每种 {@link AutoActionType} 一个）。
 * <ul>
 *   <li>{@link #validate}：保存规则时校验配置，不合法抛出 BizException；</li>
 *   <li>{@link #describe}：试运行时说明将执行的内容（不产生任何副作用）；</li>
 *   <li>{@link #execute}：执行。在规则创建人的身份下、不在事务中调用；内部写操作经过所属 Service（各自的事务），
 *       外部调用（Webhook、群机器人、平台接口）直接在这里进行。失败抛出异常（BizException 的消息记入日志），
 *       不会影响同一规则的其他动作。</li>
 * </ul>
 */
public interface AutoActionHandler {

    AutoActionType type();

    void validate(AutoRuleParts.Action action);

    String describe(AutoRuleParts.Action action, AutoEvent event, EcomAutoRule rule);

    AutoOutcome execute(AutoRuleParts.Action action, AutoEvent event, EcomAutoRule rule);
}
