package com.lemon.erp.modules.ecom.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoRuleMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 各租户启用的规则（按触发器分组），本机内存缓存 60 秒：业务事件提交后先据此判断有没有相关规则，没有时直接返回，
 * 不给业务主流程增加数据库查询。本机修改规则时立即失效；多实例部署时其他实例最多延迟 60 秒生效
 */
@Component
@RequiredArgsConstructor
public class AutoRuleCache {

    private static final long TTL_MILLIS = 60_000;
    private static final Long NO_TENANT = 0L;

    private record Entry(long loadedAt, Map<AutoTrigger, List<EcomAutoRule>> rules) {
    }

    private final EcomAutoRuleMapper ruleMapper;
    private final AutoRuleCodec codec;
    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    /** 启用的规则（已解析范围、条件、动作） */
    public List<EcomAutoRule> rules(AutoTrigger trigger) {
        return entry().rules().getOrDefault(trigger, List.of());
    }

    /** 是否有任一触发器的启用规则 */
    public boolean active(AutoTrigger... triggers) {
        Map<AutoTrigger, List<EcomAutoRule>> map = entry().rules();
        for (AutoTrigger t : triggers) {
            if (!map.getOrDefault(t, List.of()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void invalidate() {
        cache.remove(key());
    }

    private Entry entry() {
        Long key = key();
        Entry e = cache.get(key);
        long now = System.currentTimeMillis();
        if (e == null || now - e.loadedAt() > TTL_MILLIS) {
            Map<AutoTrigger, List<EcomAutoRule>> map = new EnumMap<>(AutoTrigger.class);
            for (EcomAutoRule r : ruleMapper.selectList(Wrappers.<EcomAutoRule>lambdaQuery().eq(EcomAutoRule::getStatus, EnableStatus.ENABLED)
                    .orderByAsc(EcomAutoRule::getId))) {
                codec.read(r);
                map.computeIfAbsent(r.getTriggerCode(), k -> new ArrayList<>()).add(r);
            }
            e = new Entry(now, map);
            cache.put(key, e);
        }
        return e;
    }

    private static Long key() {
        Long t = TenantContext.get();
        return t == null ? NO_TENANT : t;
    }
}
