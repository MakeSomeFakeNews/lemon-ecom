package com.lemon.erp.modules.ecom.oper.demo;

import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType;
import com.lemon.erp.modules.ecom.oper.service.EcomOpsTargetService;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 演示数据 · 运营目标（order 153）：最近 3 个月、本月和下月的店铺目标与运营人员目标。过去的月份按当月实际 × 90%–120% 倒推（达成率有高有低），
 * 本月、下月按上月实际 × 105%–125%；运营人员目标为其负责店铺目标的合计，利润目标约为销售额目标 × 当月利润率
 */
@Component
@RequiredArgsConstructor
public class OperTargetDemoContributor implements DemoContributor {

    private final EcomOpsTargetService targetService;

    @Override
    public int order() {
        return 153;
    }

    @Override
    public String key() {
        return "oper-target";
    }

    @Override
    public void generate(DemoContext ctx) {
        Random rnd = ctx.random();
        YearMonth current = YearMonth.from(ctx.today());
        for (int offset = -3; offset <= 1; offset++) {
            YearMonth month = current.plusMonths(offset);
            // editRows(下个月) 的「上月实际」= 本月实际；本月及以后用上月实际
            List<OperViews.TargetEditRow> base = targetService.editRows((offset < 0 ? month.plusMonths(1) : current).toString(), EcomOpsTargetType.STORE);
            List<OperRequests.TargetRow> storeRows = new ArrayList<>();
            Map<Long, BigDecimal[]> byOwner = new HashMap<>();
            for (OperViews.TargetEditRow r : base) {
                if (r.getLastSales().signum() <= 0) {
                    continue;
                }
                double factor = offset < 0 ? 1 / (0.9 + rnd.nextDouble() * 0.3) : 1.05 + rnd.nextDouble() * 0.2;
                BigDecimal sales = round(r.getLastSales().multiply(BigDecimal.valueOf(factor)), 100);
                BigDecimal margin = r.getLastSales().signum() == 0 ? BigDecimal.valueOf(0.15)
                        : r.getLastProfit().divide(r.getLastSales(), 4, RoundingMode.HALF_UP).max(BigDecimal.valueOf(0.05));
                BigDecimal profit = round(sales.multiply(margin), 50);
                int orders = (int) Math.max(1, Math.round(r.getLastOrders() * factor));
                storeRows.add(new OperRequests.TargetRow(r.getRefId(), sales, profit, orders, null));
                if (r.getOwnerId() != null) {
                    BigDecimal[] sum = byOwner.computeIfAbsent(r.getOwnerId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    sum[0] = sum[0].add(sales);
                    sum[1] = sum[1].add(profit);
                    sum[2] = sum[2].add(BigDecimal.valueOf(orders));
                }
            }
            if (storeRows.isEmpty()) {
                continue;
            }
            targetService.save(new OperRequests.TargetSaveRequest(month.toString(), EcomOpsTargetType.STORE, storeRows));
            List<OperRequests.TargetRow> userRows = byOwner.entrySet().stream()
                    .map(e -> new OperRequests.TargetRow(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2].intValue(), null)).toList();
            targetService.save(new OperRequests.TargetSaveRequest(month.toString(), EcomOpsTargetType.USER, userRows));
        }
    }

    /** 取整到 step（如 100 元） */
    private static BigDecimal round(BigDecimal v, int step) {
        BigDecimal s = BigDecimal.valueOf(step);
        return v.divide(s, 0, RoundingMode.HALF_UP).multiply(s);
    }
}
