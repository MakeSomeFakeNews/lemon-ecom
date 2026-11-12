package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.service.EcomReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 演示数据 · 退货退款（order 60）：约 3% 的已发货订单发生售后——退货退款（退回原仓库，质量问题 / 运输破损退到不良品仓）和仅退款，
 * 较早的已入库并退款，近期的有待处理、已入库待退款，少量已关闭；退货入库生成销售退货（库存增加、红字应收、冲减订单成本），
 * 退款回写订单重算利润。每张退货单在独立事务中处理
 */
@Component
@RequiredArgsConstructor
public class DemoReturnContributor implements DemoContributor {

    /** 原因（业务字典 ecom_return_reason）：值、名称、权重 */
    private static final String[][] REASONS = {{"QUALITY", "质量问题", "5"}, {"DAMAGED", "运输破损", "3"}, {"NOT_AS_DESCRIBED", "与描述不符", "3"}, // i18n-ignore 演示数据存库
            {"NOT_NEEDED", "买家不想要", "6"}, {"WRONG_ITEM", "发错货", "1"}, {"LATE", "送达太迟", "1"}, {"SIZE", "尺寸 / 规格不合适", "3"}, // i18n-ignore 演示数据存库
            {"NOT_RECEIVED", "未收到货", "2"}}; // i18n-ignore 演示数据存库

    /** 只能仅退款的原因（货没收到 / 太迟） */
    private static final Set<String> REFUND_ONLY_REASONS = Set.of("NOT_RECEIVED", "LATE");
    /** 退到不良品仓的原因 */
    private static final Set<String> DEFECT_REASONS = Set.of("QUALITY", "DAMAGED");

    private final EcomReturnService returnService;
    private final EcomOrderItemMapper itemMapper;
    private final DictLookup dictLookup;
    private final DemoTx tx;

    @Override
    public int order() {
        return 60;
    }

    @Override
    public String key() {
        return "returns";
    }

    @Override
    public void generate(DemoContext ctx) {
        DemoOrderPlan plan = ctx.get(DemoContributors.ORDER_PLAN);
        Long defectWh = ctx.get(DemoKeys.DEFECT_WAREHOUSE_ID);
        tx.run("returnReasons", () -> {
            for (String[] r : REASONS) {
                dictLookup.ensureBusinessValue(EcomConstants.RETURN_REASON_DICT, r[0], r[1]);
            }
        });
        int weightSum = 0;
        for (String[] r : REASONS) {
            weightSum += Integer.parseInt(r[2]);
        }
        LocalDate today = ctx.today();
        for (DemoOrderPlan.Order o : plan.orders) {
            if (o.id == null || o.stage != DemoOrderPlan.Stage.SHIPPED || !ctx.chance(0.03)) {
                continue;
            }
            String reason = pickReason(ctx, weightSum);
            boolean refundOnly = REFUND_ONLY_REASONS.contains(reason) || ctx.chance(0.3);
            long since = today.toEpochDay() - o.shipDate.toEpochDay();
            List<EcomOrderItem> items = itemMapper.selectByOrderId(o.id);
            if (items.isEmpty()) {
                continue;
            }
            EcomOrderItem it = items.get(0);
            BigDecimal qty = BigDecimal.ONE;
            BigDecimal lineTotal = it.getAmount().subtract(it.getDiscount() == null ? BigDecimal.ZERO : it.getDiscount());
            BigDecimal unit = lineTotal.divide(it.getQty(), DemoCatalog.scale(o.store.currency()), RoundingMode.HALF_UP);
            BigDecimal refund = refundOnly && ctx.chance(0.5) ? unit.multiply(BigDecimal.valueOf(0.5)).setScale(unit.scale(), RoundingMode.HALF_UP) : unit;
            // 平台仓订单退回平台仓，自发货退回国内仓；质量问题 / 破损退到不良品仓
            Long wh = DEFECT_REASONS.contains(reason) ? defectWh : o.warehouseId;
            String remark = refundOnly ? "买家申请仅退款，客服核实后同意" : "买家寄回商品，签收后验货"; // i18n-ignore 演示数据存库
            ReturnRequest req = new ReturnRequest(o.id, refundOnly ? ReturnType.REFUND_ONLY : ReturnType.RETURN_REFUND, reason, refund,
                    refundOnly ? null : wh, remark, List.of(new ReturnRequest.Item(it.getId(), qty, refund)));
            // 处理进度：10 天前发货的全部处理完；近期的停在待处理 / 已入库
            int progress = since > 12 ? (ctx.chance(0.06) ? -1 : 2) : ctx.between(0, 2);
            LocalDate receive = o.shipDate.plusDays(Math.min(since, ctx.between(6, 14)));
            tx.run("return", () -> {
                Long id = returnService.create(req);
                if (progress < 0) {
                    returnService.close(id);
                    return;
                }
                if (!refundOnly && progress >= 1) {
                    returnService.receive(id, receive);
                }
                if (progress >= 2 || (refundOnly && progress >= 1)) {
                    returnService.refund(id);
                }
            });
        }
    }

    private static String pickReason(DemoContext ctx, int weightSum) {
        int r = ctx.random().nextInt(weightSum);
        for (String[] x : REASONS) {
            r -= Integer.parseInt(x[2]);
            if (r < 0) {
                return x[0];
            }
        }
        return REASONS[0][0];
    }
}
