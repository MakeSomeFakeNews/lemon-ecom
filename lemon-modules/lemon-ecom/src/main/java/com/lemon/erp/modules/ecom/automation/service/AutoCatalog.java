package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.action.AutoActionHandler;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoFieldType;
import com.lemon.erp.modules.ecom.automation.model.AutoOperator;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.trigger.AutoField;
import com.lemon.erp.modules.ecom.automation.trigger.AutoTriggerSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 触发器与动作的注册表：启动时收集全部 {@link AutoTriggerSource} 和 {@link AutoActionHandler}，
 * 每个触发器必须有且只有一个来源声明字段、每种动作必须有处理器（缺少时启动失败，避免规则保存后无法执行）
 */
@Component
public class AutoCatalog {

    private final Map<AutoTrigger, AutoTriggerSource> sources = new EnumMap<>(AutoTrigger.class);
    private final Map<AutoTrigger, Map<String, AutoField>> fields = new EnumMap<>(AutoTrigger.class);
    private final Map<AutoActionType, AutoActionHandler> handlers = new EnumMap<>(AutoActionType.class);

    public AutoCatalog(List<AutoTriggerSource> triggerSources, List<AutoActionHandler> actionHandlers) {
        for (AutoTriggerSource s : triggerSources) {
            s.fields().forEach((trigger, list) -> {
                if (sources.put(trigger, s) != null) {
                    throw new IllegalStateException("Duplicate automation trigger source: " + trigger);
                }
                Map<String, AutoField> map = new LinkedHashMap<>();
                list.forEach(f -> map.put(f.key(), f));
                fields.put(trigger, map);
            });
        }
        for (AutoActionHandler h : actionHandlers) {
            if (handlers.put(h.type(), h) != null) {
                throw new IllegalStateException("Duplicate automation action handler: " + h.type());
            }
        }
        for (AutoTrigger t : AutoTrigger.values()) {
            if (!sources.containsKey(t)) {
                throw new IllegalStateException("Automation trigger has no source: " + t);
            }
        }
        for (AutoActionType t : AutoActionType.values()) {
            if (!handlers.containsKey(t)) {
                throw new IllegalStateException("Automation action has no handler: " + t);
            }
        }
    }

    public AutoTriggerSource source(AutoTrigger trigger) {
        return sources.get(trigger);
    }

    public Map<String, AutoField> fields(AutoTrigger trigger) {
        return fields.getOrDefault(trigger, Map.of());
    }

    public AutoActionHandler handler(AutoActionType type) {
        return handlers.get(type);
    }

    /** 编辑页目录（按当前语言） */
    public AutoViews.Catalog view() {
        List<AutoViews.TriggerView> triggers = new ArrayList<>();
        for (AutoTrigger t : AutoTrigger.values()) {
            List<AutoViews.FieldView> fv = fields(t).values().stream()
                    .map(f -> new AutoViews.FieldView(f.key(), f.type(), Messages.get(f.label()), f.dict())).toList();
            List<AutoActionType> actions = Arrays.stream(AutoActionType.values()).filter(a -> a.supports(t)).toList();
            triggers.add(new AutoViews.TriggerView(t, t.category(), t.mode(), Messages.get(t.label()), Messages.get(t.label() + ".desc"), fv,
                    List.copyOf(t.refs()), actions));
        }
        List<AutoViews.ActionTypeView> actions = Arrays.stream(AutoActionType.values())
                .map(a -> new AutoViews.ActionTypeView(a, Messages.get(a.label()), Messages.get(a.label() + ".desc"), a.required())).toList();
        Map<AutoFieldType, List<AutoOperator>> ops = new EnumMap<>(AutoFieldType.class);
        for (AutoFieldType ft : AutoFieldType.values()) {
            ops.put(ft, Arrays.stream(AutoOperator.values()).filter(o -> o.supports(ft)).toList());
        }
        List<AutoViews.VariableView> vars = Arrays.stream(AutoText.COMMON_VARIABLES)
                .map(v -> new AutoViews.VariableView(v, Messages.get(AutoText.key("ecom.label.auto.var", v)))).toList();
        return new AutoViews.Catalog(triggers, actions, ops, vars);
    }
}
