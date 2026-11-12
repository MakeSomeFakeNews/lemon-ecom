package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.modules.ecom.dto.PayoutReceiveRequest;
import com.lemon.erp.modules.ecom.dto.SettlementImportRow;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPayout;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPayoutMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.PayoutStatus;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.service.EcomPayoutService;
import com.lemon.erp.modules.ecom.service.EcomSettlementService;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 平台结算与回款（order 65）：亚马逊、TikTok、Temu、Shopify、eBay 双周结算，虾皮、速卖通月结，经结算报告导入
 * （订单收入、佣金、FBA 配送费、退款与退货处理费，以及仓储费、订阅费、入库配置费、优惠券、赔偿等不挂订单的费用，按费用分摊规则分摊）。
 * 除最近一期外都已确认（费用回写订单、生成收款单核销店铺应收）；已确认的较早几期登记了回款到账（少量手续费差异、到账汇率差额生成凭证），
 * 最近确认的一期待到账
 */
@Component
@RequiredArgsConstructor
public class DemoSettlementContributor implements DemoContributor {

    /** 结算计划：店铺编码、周期天数、第一期开始于几天前 */
    private static final Object[][] SCHEDULES = {{"DEMO-AMZ-US", 14, 114}, {"DEMO-AMZ-DE", 14, 114}, {"DEMO-AMZ-JP", 14, 114},
            {"DEMO-TT-US", 14, 114}, {"DEMO-SHP-MY", 30, 116}, {"DEMO-SHP-SG", 30, 116}, {"DEMO-TEMU-US", 14, 114}, {"DEMO-AE", 30, 116},
            {"DEMO-SHOPIFY", 14, 114}, {"DEMO-EBAY-US", 14, 114}};
    private static final String BANK_ACCOUNT = "100201";
    private static final String[] PAYOUT_CHANNELS = {"PAYONEER", "LIANLIAN", "PINGPONG", "WORLDFIRST"};

    private final EcomSettlementService settlementService;
    private final EcomSettlementMapper settlementMapper;
    private final EcomPayoutService payoutService;
    private final EcomPayoutMapper payoutMapper;
    private final EcomOrderMapper orderMapper;
    private final FinAccountService accountService;
    private final ExchangeRateProvider rateProvider;
    private final DemoTx tx;

    @Override
    public int order() {
        return 65;
    }

    @Override
    public String key() {
        return "settlements";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        FinAccount bank = accountService.leafByCode(BANK_ACCOUNT);
        LocalDate today = ctx.today();
        for (Object[] sch : SCHEDULES) {
            String code = (String) sch[0];
            DemoCatalog.Store d = DemoCatalog.store(code);
            Long storeId = stores.get(code);
            int days = (Integer) sch[1];
            List<Long> settlementIds = new ArrayList<>();
            for (LocalDate start = today.minusDays((Integer) sch[2]); !start.plusDays(days - 1L).isAfter(today.minusDays(2)); start = start.plusDays(days)) {
                LocalDate end = start.plusDays(days - 1L);
                String no = DemoCatalog.STORES.indexOf(d) + 1 + String.valueOf(ctx.between(100000000, 999999999)) + (settlementIds.size() + 1);
                List<ImportRow<SettlementImportRow>> rows = rows(ctx, d, storeId, no, start, end);
                if (rows.isEmpty()) {
                    continue;
                }
                tx.run("settlementImport", () -> settlementService.importRows(new ImportRequest<>(rows)));
                EcomSettlement s = settlementMapper.selectOne(Wrappers.<EcomSettlement>lambdaQuery().eq(EcomSettlement::getStoreId, storeId)
                        .eq(EcomSettlement::getSettlementNo, no));
                if (s != null) {
                    settlementIds.add(s.getId());
                }
            }
            // 最近一期待确认，其余确认并生成收款；确认的除最近一期外都已到账
            for (int i = 0; i < settlementIds.size() - 1; i++) {
                Long id = settlementIds.get(i);
                if (bank == null || !tx.run("settlementConfirm", () -> settlementService.confirm(id, bank.getId()))) {
                    continue;
                }
                if (i < settlementIds.size() - 2) {
                    receivePayout(ctx, id, d, bank.getId());
                }
            }
        }
    }

    private List<ImportRow<SettlementImportRow>> rows(DemoContext ctx, DemoCatalog.Store d, Long storeId, String no, LocalDate start, LocalDate end) {
        List<EcomOrder> orders = orderMapper.selectList(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, storeId)
                .eq(EcomOrder::getLocalStatus, EcomLocalStatus.SHIPPED)
                .ge(EcomOrder::getShipTime, start.atStartOfDay()).lt(EcomOrder::getShipTime, end.plusDays(1).atStartOfDay()));
        int scale = DemoCatalog.scale(d.currency());
        List<ImportRow<SettlementImportRow>> rows = new ArrayList<>();
        BigDecimal sales = BigDecimal.ZERO;
        for (EcomOrder o : orders) {
            LocalDate posted = o.getShipTime().toLocalDate();
            BigDecimal income = o.getTotalAmount().subtract(nz(o.getTaxAmount()));
            sales = sales.add(income);
            add(rows, d, no, start, end, posted, SettlementItemType.ORDER, "Principal", o.getPlatformOrderNo(), income, null);
            BigDecimal commission = income.multiply(BigDecimal.valueOf(d.commission() / 100.0 * (0.96 + ctx.random().nextDouble() * 0.08)))
                    .setScale(scale, RoundingMode.HALF_UP);
            add(rows, d, no, start, end, posted, SettlementItemType.COMMISSION, "Commission", o.getPlatformOrderNo(), commission.negate(), null);
            if (o.getFulfillment() == FulfillmentType.PLATFORM && nz(o.getFulfillmentFee()).signum() > 0) {
                BigDecimal fee = o.getFulfillmentFee().multiply(BigDecimal.valueOf(0.97 + ctx.random().nextDouble() * 0.08)).setScale(scale, RoundingMode.HALF_UP);
                add(rows, d, no, start, end, posted, SettlementItemType.FULFILLMENT, "FBAPerUnitFulfillmentFee", o.getPlatformOrderNo(), fee.negate(), null);
            }
            if (nz(o.getRefundAmount()).signum() > 0) {
                add(rows, d, no, start, end, posted.plusDays(5).isAfter(end) ? end : posted.plusDays(5), SettlementItemType.REFUND, "Refund",
                        o.getPlatformOrderNo(), o.getRefundAmount().negate(), null);
                if (o.getFulfillment() == FulfillmentType.PLATFORM) {
                    add(rows, d, no, start, end, end, SettlementItemType.RETURN_FEE, "FBACustomerReturnPerUnitFee", o.getPlatformOrderNo(),
                            BigDecimal.valueOf(-2.9 * DemoCatalog.usdFactor(d.currency())).setScale(scale, RoundingMode.HALF_UP), null);
                }
            }
        }
        if (rows.isEmpty()) {
            return rows;
        }
        // 不挂订单的费用（店铺币种）
        double f = DemoCatalog.usdFactor(d.currency());
        add(rows, d, no, start, end, end, SettlementItemType.STORAGE, "Storage Fee", null, sales.multiply(BigDecimal.valueOf(-0.008)).setScale(scale, RoundingMode.HALF_UP),
                "月度仓储费"); // i18n-ignore 演示数据存库
        if (d.platformWh() != null && d.platform() == com.lemon.erp.modules.ecom.model.EcomPlatform.AMAZON) {
            add(rows, d, no, start, end, start, SettlementItemType.SUBSCRIPTION, "Subscription Fee", null, BigDecimal.valueOf(-39.99 * f).setScale(scale, RoundingMode.HALF_UP),
                    "专业销售计划月费"); // i18n-ignore 演示数据存库
            add(rows, d, no, start, end, start.plusDays(3), SettlementItemType.INBOUND_FEE, "FBAInboundTransportationFee", null,
                    BigDecimal.valueOf(-(60 + ctx.between(0, 90)) * f).setScale(scale, RoundingMode.HALF_UP), "入库配置费"); // i18n-ignore 演示数据存库
            if (ctx.chance(0.5)) {
                add(rows, d, no, start, end, end.minusDays(2), SettlementItemType.REIMBURSEMENT, "REVERSAL_REIMBURSEMENT", null,
                        BigDecimal.valueOf(ctx.between(12, 80) * f).setScale(scale, RoundingMode.HALF_UP), "FBA 仓库丢失赔偿"); // i18n-ignore 演示数据存库
            }
        }
        add(rows, d, no, start, end, start.plusDays(6), SettlementItemType.COUPON, "CouponRedemptionFee", null,
                BigDecimal.valueOf(-(10 + ctx.between(0, 40)) * f).setScale(scale, RoundingMode.HALF_UP), "优惠券兑换费"); // i18n-ignore 演示数据存库
        return rows;
    }

    private static void add(List<ImportRow<SettlementImportRow>> rows, DemoCatalog.Store d, String no, LocalDate start, LocalDate end, LocalDate posted,
                            SettlementItemType type, String rawType, String orderNo, BigDecimal amount, String description) {
        SettlementImportRow r = new SettlementImportRow();
        r.setStoreCode(d.code());
        r.setSettlementNo(no);
        r.setPeriodStart(start);
        r.setPeriodEnd(end);
        r.setCurrency(d.currency());
        r.setPostedDate(posted);
        r.setItemType(type);
        r.setRawType(rawType);
        r.setPlatformOrderNo(orderNo);
        r.setAmount(amount);
        r.setDescription(description);
        rows.add(new ImportRow<>(rows.size() + 2, r));
    }

    /** 登记到账：结算周期结束后 3–5 天到账，少量收款手续费，到账汇率与结算汇率略有差异 */
    private void receivePayout(DemoContext ctx, Long settlementId, DemoCatalog.Store d, Long accountId) {
        EcomPayout p = payoutMapper.selectOne(Wrappers.<EcomPayout>lambdaQuery().eq(EcomPayout::getSettlementId, settlementId)
                .eq(EcomPayout::getStatus, PayoutStatus.PENDING));
        if (p == null) {
            return;
        }
        LocalDate date = p.getExpectedDate().plusDays(ctx.between(3, 5));
        if (date.isAfter(ctx.today())) {
            return;
        }
        BigDecimal amount = ctx.chance(0.7) ? p.getExpectedAmount()
                : p.getExpectedAmount().multiply(BigDecimal.valueOf(0.997)).setScale(DemoCatalog.scale(d.currency()), RoundingMode.HALF_UP);
        BigDecimal rate = rateProvider.rate(d.currency(), date);
        BigDecimal payRate = rate == null ? null : rate.multiply(BigDecimal.valueOf(0.995 + ctx.random().nextDouble() * 0.01)).setScale(6, RoundingMode.HALF_UP);
        String channel = PAYOUT_CHANNELS[ctx.random().nextInt(PAYOUT_CHANNELS.length)];
        tx.run("payout", () -> payoutService.receive(p.getId(), new PayoutReceiveRequest(date, amount, d.currency(), accountId, channel,
                "PO" + DemoListingContributor.code(ctx, 10), payRate, null)));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
