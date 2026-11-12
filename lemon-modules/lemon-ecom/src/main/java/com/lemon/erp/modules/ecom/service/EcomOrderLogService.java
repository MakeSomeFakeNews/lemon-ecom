package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomOrderLog;
import com.lemon.erp.modules.ecom.mapper.EcomOrderLogMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 订单操作日志：各 Service 在订单的关键动作处调用 {@link #log}，与业务写操作在同一个事务中（业务回滚时日志一起回滚）；
 * 操作人取当前登录用户（同步、定时任务等系统执行时为空）。内容按默认语言存库（{@link #text}）
 */
@Service
@RequiredArgsConstructor
public class EcomOrderLogService {

    static final int CONTENT_MAX = 500;

    private final EcomOrderLogMapper logMapper;

    /** 订单的操作日志（新的在前） */
    public List<EcomOrderLog> list(Long orderId) {
        return logMapper.selectByOrderId(orderId);
    }

    /** 记录一张订单的动作，content 可为空 */
    public void log(Long orderId, EcomOrderLogAction action, String content) {
        log(List.of(orderId), action, content);
    }

    /** 多张订单记录同一个动作（单条多值 INSERT） */
    public void log(Collection<Long> orderIds, EcomOrderLogAction action, String content) {
        Long userId = SecurityUtils.getUserIdOrNull();
        String c = abbreviate(content);
        List<EcomOrderLog> rows = orderIds.stream().filter(Objects::nonNull).distinct().map(id -> {
            EcomOrderLog l = new EcomOrderLog();
            l.setOrderId(id);
            l.setAction(action);
            l.setContent(c);
            l.setCreateBy(userId);
            return l;
        }).toList();
        if (!rows.isEmpty()) {
            logMapper.insertBatch(rows);
        }
    }

    /** 写入已组装好的日志（演示数据带时间和操作人） */
    public void saveAll(List<EcomOrderLog> rows) {
        if (!rows.isEmpty()) {
            rows.forEach(r -> r.setContent(abbreviate(r.getContent())));
            logMapper.insertBatch(rows);
        }
    }

    /** 日志内容：按默认语言取消息（存库内容不随操作人的界面语言变化） */
    public static String text(String code, Object... args) {
        return Messages.get(Messages.DEFAULT_LOCALE, code, args);
    }

    static String abbreviate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return t.length() <= CONTENT_MAX ? t : t.substring(0, CONTENT_MAX - 3) + "...";
    }
}
