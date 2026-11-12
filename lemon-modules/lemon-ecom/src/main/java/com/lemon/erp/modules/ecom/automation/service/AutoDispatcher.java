package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * 实时事件的分发：触发器来源在业务事务提交后调用 {@link #active} 判断有没有相关的启用规则（读本机缓存，没有时立即返回），
 * 有时调用 {@link #dispatchAsync} 在后台线程（租户由 TenantTaskDecorator 传递）中取数、构造事件并执行规则，业务主流程不等待
 */
@Slf4j
@Component
public class AutoDispatcher {

    private final AutoRuleCache ruleCache;
    private final AutoEngine engine;
    /** 引擎延迟注入（引擎 → 目录 → 触发器来源 → 分发器）；自身代理（@Async 需要经过代理调用） */
    private final AutoDispatcher self;

    public AutoDispatcher(AutoRuleCache ruleCache, @Lazy AutoEngine engine, @Lazy AutoDispatcher self) {
        this.ruleCache = ruleCache;
        this.engine = engine;
        this.self = self;
    }

    public boolean active(AutoTrigger... triggers) {
        try {
            return ruleCache.active(triggers);
        } catch (RuntimeException e) {
            log.warn("Automation rule cache unavailable", e);
            return false;
        }
    }

    /** 后台构造事件并执行（loader 在后台线程中调用，可以查询数据库） */
    public void dispatch(Supplier<List<AutoEvent>> loader) {
        self.dispatchAsync(loader);
    }

    @Async
    public void dispatchAsync(Supplier<List<AutoEvent>> loader) {
        run(loader);
    }

    /** 同步构造事件并执行，返回执行了动作的次数（扫描任务、测试使用） */
    public int run(Supplier<List<AutoEvent>> loader) {
        List<AutoEvent> events;
        try {
            events = loader.get();
        } catch (RuntimeException e) {
            log.warn("Automation event loading failed", e);
            return 0;
        }
        int executed = 0;
        for (AutoEvent e : events) {
            executed += engine.handle(e);
        }
        return executed;
    }
}
