package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderLog;
import com.lemon.erp.modules.ecom.mapper.EcomOrderLogMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.service.EcomOrderLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 演示数据 · 订单操作日志（由 {@link DemoOrderOpsContributor} 在订单作业之后调用）：
 * <ul>
 *   <li>近期订单走真实流程时各 Service 已记录日志，但时间都是生成时刻：按下单 / 付款时间重新排列（系统动作不带操作人，人工动作随机分给运营）</li>
 *   <li>历史批量写入的订单没有日志：为最近 {@value #HISTORY_DAYS} 天已发货的订单补写入、审核、发货、回传等日志</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DemoOrderLogs {

    static final int HISTORY_DAYS = 10;
    private static final int BATCH = 500;
    /** 系统自动执行的动作（同步、规则、回传），没有操作人 */
    private static final Set<EcomOrderLogAction> SYSTEM = EnumSet.of(EcomOrderLogAction.CREATE, EcomOrderLogAction.RULE, EcomOrderLogAction.PLATFORM_STATUS,
            EcomOrderLogAction.SHIP_CONFIRM, EcomOrderLogAction.EXCEPTION);

    private final EcomOrderLogMapper logMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderLogService logService;

    public void generate(DemoContext ctx, List<Long> recentIds) {
        List<Long> operators = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        retime(ctx, recentIds, operators);
        history(ctx, Set.copyOf(recentIds), operators);
    }

    /** 近期订单：第一条（写入）在付款后几分钟，之后每条间隔 10–90 分钟，不超过当前时间 */
    private void retime(DemoContext ctx, List<Long> orderIds, List<Long> operators) {
        if (orderIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        for (EcomOrder o : orderMapper.selectByIds(orderIds)) {
            List<EcomOrderLog> logs = logMapper.selectList(Wrappers.<EcomOrderLog>lambdaQuery().eq(EcomOrderLog::getOrderId, o.getId())
                    .orderByAsc(EcomOrderLog::getId));
            LocalDateTime t = (o.getPaidTime() != null ? o.getPaidTime() : o.getOrderTime()).plusMinutes(ctx.between(1, 5));
            Long operator = pick(ctx, operators);
            for (EcomOrderLog l : logs) {
                if (l.getAction() != EcomOrderLogAction.CREATE && l.getAction() != EcomOrderLogAction.RULE) {
                    t = t.plusMinutes(ctx.between(10, 90));
                }
                EcomOrderLog u = new EcomOrderLog();
                u.setId(l.getId());
                u.setCreateTime(t.isAfter(now) ? now : t);
                u.setCreateBy(SYSTEM.contains(l.getAction()) ? null : operator);
                logMapper.updateById(u);
                if (SYSTEM.contains(l.getAction())) {
                    logMapper.update(Wrappers.<EcomOrderLog>lambdaUpdate().set(EcomOrderLog::getCreateBy, null).eq(EcomOrderLog::getId, l.getId()));
                }
            }
        }
    }

    /** 最近已发货的历史订单：写入 → 审核 → 发货 → 回传（平台配送的只有写入和平台状态） */
    private void history(DemoContext ctx, Set<Long> skip, List<Long> operators) {
        LocalDateTime from = ctx.today().minusDays(HISTORY_DAYS).atStartOfDay();
        List<EcomOrder> orders = orderMapper.selectList(Wrappers.<EcomOrder>lambdaQuery()
                .select(EcomOrder::getId, EcomOrder::getOrderTime, EcomOrder::getPaidTime, EcomOrder::getShipTime, EcomOrder::getFulfillment,
                        EcomOrder::getCarrier, EcomOrder::getTrackingNo, EcomOrder::getTradeDocNo)
                .ge(EcomOrder::getOrderTime, from).eq(EcomOrder::getLocalStatus, EcomLocalStatus.SHIPPED));
        List<EcomOrderLog> rows = new ArrayList<>();
        for (EcomOrder o : orders) {
            if (skip.contains(o.getId())) {
                continue;
            }
            LocalDateTime paid = o.getPaidTime() != null ? o.getPaidTime() : o.getOrderTime();
            LocalDateTime created = paid.plusMinutes(ctx.between(1, 10));
            rows.add(row(o.getId(), EcomOrderLogAction.CREATE, EcomOrderLogService.text("ecom.log.createSync"), created, null));
            LocalDateTime shipped = o.getShipTime() != null && o.getShipTime().isAfter(created) ? o.getShipTime() : created.plusHours(ctx.between(4, 20));
            if (o.getFulfillment() == FulfillmentType.PLATFORM) {
                rows.add(row(o.getId(), EcomOrderLogAction.PLATFORM_STATUS, EcomOrderLogService.text("ecom.log.platformStatus", "已发货"), // i18n-ignore 演示数据存库
                        shipped, null));
                continue;
            }
            Long operator = pick(ctx, operators);
            LocalDateTime audited = created.plusMinutes(ctx.between(20, 180));
            if (audited.isAfter(shipped)) {
                audited = created.plusMinutes(5);
            }
            rows.add(row(o.getId(), EcomOrderLogAction.AUDIT, null, audited, operator));
            if (o.getTradeDocNo() != null) {
                rows.add(row(o.getId(), EcomOrderLogAction.OUTBOUND, EcomOrderLogService.text("ecom.log.outbound", o.getTradeDocNo()), shipped.minusMinutes(30),
                        operator));
            }
            rows.add(row(o.getId(), EcomOrderLogAction.SHIP, Objects.toString(o.getCarrier(), "") + " " + Objects.toString(o.getTrackingNo(), ""), shipped,
                    operator));
            rows.add(row(o.getId(), EcomOrderLogAction.SHIP_CONFIRM, o.getTrackingNo(), shipped.plusMinutes(ctx.between(2, 15)), null));
        }
        for (int i = 0; i < rows.size(); i += BATCH) {
            logService.saveAll(rows.subList(i, Math.min(rows.size(), i + BATCH)));
        }
    }

    /** 时间不晚于当前（个别演示订单的发货时间在当前之后） */
    private static EcomOrderLog row(Long orderId, EcomOrderLogAction action, String content, LocalDateTime time, Long by) {
        EcomOrderLog l = new EcomOrderLog();
        l.setOrderId(orderId);
        l.setAction(action);
        l.setContent(content);
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        l.setCreateTime(time.isAfter(now) ? now : time);
        l.setCreateBy(by);
        return l;
    }

    private static Long pick(DemoContext ctx, List<Long> operators) {
        return operators == null || operators.isEmpty() ? null : operators.get(ctx.between(0, operators.size() - 1));
    }
}
