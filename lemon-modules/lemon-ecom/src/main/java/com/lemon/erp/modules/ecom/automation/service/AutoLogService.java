package com.lemon.erp.modules.ecom.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.automation.dto.AutoLogQuery;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoLogMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoRuleMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoActionStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoRows;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化执行日志：查询、详情（事件字段、条件判断、每个动作的结果与耗时）、重试失败的动作、统计、清理
 */
@Service
@RequiredArgsConstructor
public class AutoLogService {

    private final EcomAutoLogMapper logMapper;
    private final EcomAutoRuleMapper ruleMapper;
    private final AutoRuleService ruleService;
    private final AutoEngine engine;

    public PageResult<EcomAutoLog> page(long current, long size, AutoLogQuery q) {
        Page<EcomAutoLog> page = Page.of(current, size);
        logMapper.selectPageList(page, q);
        page.getRecords().forEach(l -> {
            l.setActions(actions(l.getActionsJson()));
            l.setActionsJson(null);
        });
        return PageResult.of(page);
    }

    public EcomAutoLog detail(Long id) {
        EcomAutoLog l = require(id);
        l.setActions(actions(l.getActionsJson()));
        if (l.getEventJson() != null) {
            AutoEvent e = AutoStoredEvent.parse(l.getTriggerCode(), l.getObjectKey(), l.getEventJson());
            l.setFields(e.fields());
            l.setLink(e.link());
        }
        return l;
    }

    /**
     * 重试失败的动作（按规则当前的动作配置，以规则创建人的身份执行，不在事务中）：只重新执行失败的那几个动作，
     * 合并结果后重新计算日志的结果，重试次数 + 1
     */
    public EcomAutoLog retry(Long id) {
        EcomAutoLog l = require(id);
        if (l.getResult() != AutoResult.FAILED && l.getResult() != AutoResult.PARTIAL) {
            throw new BizException("ecom.error.auto.retryNotFailed");
        }
        EcomAutoRule rule = ruleService.require(l.getRuleId());
        List<AutoViews.ActionResult> results = new ArrayList<>(actions(l.getActionsJson()));
        List<Integer> failed = results.stream().filter(r -> r.status() == AutoActionStatus.FAILED && r.index() < rule.getActions().size())
                .map(AutoViews.ActionResult::index).toList();
        if (failed.isEmpty()) {
            throw new BizException("ecom.error.auto.retryNothing");
        }
        AutoEvent e = AutoStoredEvent.parse(l.getTriggerCode(), l.getObjectKey(), l.getEventJson());
        Map<Integer, AutoViews.ActionResult> retried = new HashMap<>();
        engine.runActions(rule, e, failed).forEach(r -> retried.put(r.index(), r));
        results.replaceAll(r -> retried.getOrDefault(r.index(), r));
        AutoResult result = AutoEngine.summarize(results);
        logMapper.update(Wrappers.<EcomAutoLog>lambdaUpdate().set(EcomAutoLog::getActionsJson, AutoRuleCodec.json(results))
                .set(EcomAutoLog::getResult, result).set(EcomAutoLog::getErrorMsg, AutoText.cut(AutoEngine.firstError(results), 500))
                .set(EcomAutoLog::getRetryCount, (l.getRetryCount() == null ? 0 : l.getRetryCount()) + 1).eq(EcomAutoLog::getId, id));
        return detail(id);
    }

    /** 近 N 天的统计：按结果计数、按天的执行与失败次数（没有记录的日期补 0）、规则数 */
    public AutoViews.LogStats stats(int days, Long ruleId) {
        LocalDate from = LocalDate.now().minusDays(Math.max(1, Math.min(days, 90)) - 1L);
        LocalDateTime since = from.atStartOfDay();
        Map<AutoResult, Long> results = new EnumMap<>(AutoResult.class);
        for (AutoResult r : AutoResult.values()) {
            results.put(r, 0L);
        }
        for (Map<String, Object> row : logMapper.countByResult(since, ruleId)) {
            results.put(AutoResult.valueOf(AutoRows.str(row, "result")), ((Number) row.get("cnt")).longValue());
        }
        Map<LocalDate, long[]> byDay = new HashMap<>();
        for (Map<String, Object> row : logMapper.countByDay(since, ruleId)) {
            byDay.put(AutoRows.date(row, "day"), new long[]{((Number) row.get("executed")).longValue(), ((Number) row.get("failed")).longValue()});
        }
        List<AutoViews.DayCount> list = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            long[] v = byDay.getOrDefault(d, new long[2]);
            list.add(new AutoViews.DayCount(d.toString(), v[0], v[1]));
        }
        long rules = ruleMapper.selectCount(null);
        long enabled = ruleMapper.selectCount(Wrappers.<EcomAutoRule>lambdaQuery().eq(EcomAutoRule::getStatus, EnableStatus.ENABLED));
        return new AutoViews.LogStats(results, list, rules, enabled);
    }

    /** 清理 N 天前的日志 */
    public int cleanup(int keepDays) {
        return logMapper.deleteBefore(LocalDate.now().minusDays(keepDays).atStartOfDay());
    }

    private EcomAutoLog require(Long id) {
        EcomAutoLog l = id == null ? null : logMapper.selectById(id);
        if (l == null) {
            throw new BizException("ecom.error.auto.logNotFound");
        }
        return l;
    }

    static List<AutoViews.ActionResult> actions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return HttpJson.MAPPER.readValue(json, new TypeReference<List<AutoViews.ActionResult>>() {
        });
    }
}
