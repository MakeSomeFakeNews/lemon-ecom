package com.lemon.erp.modules.ecom.automation.dto;

import com.lemon.erp.modules.ecom.automation.model.AutoActionStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoCategory;
import com.lemon.erp.modules.ecom.automation.model.AutoFieldType;
import com.lemon.erp.modules.ecom.automation.model.AutoOperator;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoScanMode;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;

import java.util.List;
import java.util.Map;

/**
 * 自动化规则中心的返回结构
 */
public final class AutoViews {

    private AutoViews() {
    }

    /** 一个动作的执行结果 */
    public record ActionResult(int index, AutoActionType type, AutoActionStatus status, String message, long costMs) {
    }

    /**
     * 触发器字段
     *
     * @param label 名称（按当前语言）
     * @param dict  ENUM 字段的字典类型
     */
    public record FieldView(String key, AutoFieldType type, String label, String dict) {
    }

    /** 触发器 */
    public record TriggerView(AutoTrigger code, AutoCategory category, AutoScanMode mode, String name, String description, List<FieldView> fields,
                              List<AutoRef> refs, List<AutoActionType> actions) {
    }

    /** 动作类型 */
    public record ActionTypeView(AutoActionType type, String name, String description, AutoRef required) {
    }

    /** 公共变量（所有触发器都可用） */
    public record VariableView(String key, String label) {
    }

    /** 规则编辑页需要的目录：触发器（含字段）、动作、各字段类型可用的运算符、公共变量 */
    public record Catalog(List<TriggerView> triggers, List<ActionTypeView> actions, Map<AutoFieldType, List<AutoOperator>> operators,
                          List<VariableView> variables) {
    }

    /** 规则模板 */
    public record TemplateView(String code, String name, String description, AutoTrigger trigger, AutoCategory category, String triggerName,
                               List<AutoActionType> actionTypes, Object rule) {
    }

    /** 试运行中一个动作将执行的内容 */
    public record TestAction(int index, AutoActionType type, String description, boolean available) {
    }

    /**
     * 试运行的一个样本
     *
     * @param source      LOG 最近的执行日志中的事件 / REAL 当前的真实数据 / SAMPLE 样例数据
     * @param inScope     是否在对象范围内
     * @param matched     条件是否满足
     * @param matchDetail 条件判断说明
     * @param debounced   按防抖设置正式执行时会跳过
     */
    public record TestSample(String source, String objectKey, String objectLabel, String storeName, Map<String, Object> fields, boolean inScope,
                             boolean matched, String matchDetail, boolean debounced, List<TestAction> actions) {
    }

    public record TestResult(List<TestSample> samples, String summary) {
    }

    /** 规则执行统计（日志页顶部）：按结果计数、按天执行次数 */
    public record LogStats(Map<AutoResult, Long> results, List<DayCount> days, long rules, long enabledRules) {
    }

    public record DayCount(String date, long executed, long failed) {
    }

    /** 角色选项（通知接收人） */
    public record RoleOption(Long id, String name) {
    }

    /** 重试 / 立即扫描的结果 */
    public record RunResult(int events, int executed, String message) {
    }
}
