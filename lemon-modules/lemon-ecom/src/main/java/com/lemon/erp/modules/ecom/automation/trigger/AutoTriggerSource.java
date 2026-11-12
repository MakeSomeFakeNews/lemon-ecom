package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;

import java.util.List;
import java.util.Map;

/**
 * 一组触发器的事件来源（Spring Bean）：声明触发器的字段，按需产生事件。
 * <ul>
 *   <li>实时触发器：在本类中用 {@code @TransactionalEventListener(AFTER_COMMIT)} 监听业务事件，转为 {@link AutoEvent} 后交给
 *       {@code AutoDispatcher.dispatch}（异步执行，不拖慢业务）；</li>
 *   <li>扫描类触发器：实现 {@link #scan}，由定时任务（{@code EcomAutomationScanJob}）按产生方式调用；</li>
 *   <li>{@link #recent}：试运行用的最近真实事件。</li>
 * </ul>
 * 新增触发器见 {@link AutoTrigger} 的说明
 */
public interface AutoTriggerSource {

    /** 本来源负责的触发器及其字段（字段顺序即编辑页显示顺序） */
    Map<AutoTrigger, List<AutoField>> fields();

    /** 最近的真实事件（试运行），没有返回空 */
    List<AutoEvent> recent(AutoTrigger trigger, int limit);

    /** 扫描类触发器：当前满足基本条件的对象（如可售天数有值的在线商品），具体阈值由规则条件判断 */
    default List<AutoEvent> scan(AutoTrigger trigger) {
        return List.of();
    }
}
