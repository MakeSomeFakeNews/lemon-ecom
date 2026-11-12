package com.lemon.erp.modules.ecom.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoTodo;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoTodoMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 自动化待办提醒：规则动作创建，处理人在「待办提醒」页面查看、标为完成（也可重新打开）
 */
@Service
@RequiredArgsConstructor
public class AutoTodoService {

    private final EcomAutoTodoMapper todoMapper;

    /** 给每个处理人建一条（同一规则同一对象该处理人已有未完成的不重复），返回新建条数 */
    @Transactional
    public int create(Long ruleId, Collection<Long> assignees, AutoEvent e, String title, String content, LocalDate dueDate) {
        int n = 0;
        for (Long userId : assignees) {
            boolean exists = todoMapper.exists(Wrappers.<EcomAutoTodo>lambdaQuery().eq(EcomAutoTodo::getRuleId, ruleId)
                    .eq(EcomAutoTodo::getObjectKey, e.objectKey()).eq(EcomAutoTodo::getAssigneeId, userId)
                    .eq(EcomAutoTodo::getStatus, AutoTodoStatus.OPEN));
            if (exists) {
                continue;
            }
            EcomAutoTodo t = new EcomAutoTodo();
            t.setRuleId(ruleId);
            t.setAssigneeId(userId);
            t.setStoreId(e.storeId());
            t.setTitle(AutoText.cut(title, 128));
            t.setContent(AutoText.cut(content, 500));
            t.setLink(AutoText.cut(e.link(), 255));
            t.setObjectKey(AutoText.cut(e.objectKey(), 64));
            t.setDueDate(dueDate);
            t.setStatus(AutoTodoStatus.OPEN);
            todoMapper.insert(t);
            n++;
        }
        return n;
    }

    /** @param mine 只看自己的 */
    public PageResult<EcomAutoTodo> page(long current, long size, boolean mine, AutoTodoStatus status, String keyword) {
        Page<EcomAutoTodo> page = Page.of(current, size);
        todoMapper.selectPageList(page, mine ? SecurityUtils.getUserId() : null, status, keyword);
        return PageResult.of(page);
    }

    /** 标为完成 / 重新打开 */
    public int changeStatus(List<Long> ids, AutoTodoStatus status) {
        boolean done = status == AutoTodoStatus.DONE;
        return todoMapper.update(Wrappers.<EcomAutoTodo>lambdaUpdate().set(EcomAutoTodo::getStatus, status)
                .set(EcomAutoTodo::getDoneBy, done ? SecurityUtils.getUserId() : null)
                .set(EcomAutoTodo::getDoneTime, done ? LocalDateTime.now() : null)
                .in(EcomAutoTodo::getId, ids).ne(EcomAutoTodo::getStatus, status));
    }

    public long countOpen(Long userId) {
        return todoMapper.selectCount(Wrappers.<EcomAutoTodo>lambdaQuery().eq(EcomAutoTodo::getAssigneeId, userId)
                .eq(EcomAutoTodo::getStatus, AutoTodoStatus.OPEN));
    }
}
