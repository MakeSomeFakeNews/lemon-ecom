package com.lemon.erp.modules.ecom.publish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 后台执行发布（@Async，租户由 TenantTaskDecorator 传递）：逐条处理，一条失败不影响其他
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcomPublishExecutor {

    private final EcomPublishWorker worker;

    @Async
    public void runAsync(List<Long> itemIds) {
        for (Long id : itemIds) {
            try {
                worker.process(id);
            } catch (RuntimeException e) {
                log.warn("Publish item {} failed", id, e);
            }
        }
    }
}
