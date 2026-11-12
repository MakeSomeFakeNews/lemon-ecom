package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.model.AutoScanMode;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 扫描类触发器的执行（定时任务调用，按租户逐个执行）：只扫描有启用规则的触发器，一个触发器出错不影响其他
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScanService {

    private final AutoCatalog catalog;
    private final AutoRuleCache ruleCache;
    private final AutoEngine engine;

    public AutoViews.RunResult scan(AutoScanMode mode) {
        ruleCache.invalidate();
        int events = 0;
        int executed = 0;
        StringBuilder errors = new StringBuilder();
        for (AutoTrigger t : AutoTrigger.values()) {
            if (t.mode() != mode || !ruleCache.active(t)) {
                continue;
            }
            try {
                List<AutoEvent> list = catalog.source(t).scan(t);
                events += list.size();
                for (AutoEvent e : list) {
                    executed += engine.handle(e);
                }
            } catch (RuntimeException e) {
                log.warn("Automation scan {} failed", t, e);
                errors.append(t.name()).append(": ").append(e.getMessage()).append("; ");
            }
        }
        return new AutoViews.RunResult(events, executed, errors.isEmpty() ? null : errors.toString());
    }
}
