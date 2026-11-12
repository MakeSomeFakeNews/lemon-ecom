package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.fin.dto.AccountOpeningRequest;
import com.lemon.erp.modules.fin.dto.AccountOpeningRow;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillStatus;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinIoCategoryService;
import com.lemon.erp.modules.fin.service.FinIoService;
import com.lemon.erp.modules.fin.service.FinOpeningService;
import com.lemon.erp.modules.fin.service.FinSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 财务凭证（order 95，在基础业务数据之后）：让总账、账簿、财务报表、经营分析有合理的数据（不结账）。
 * <ul>
 *   <li>科目期初（租户还没有期初时）：银行存款 / 实收资本，作为开业资金</li>
 *   <li>支付供应商货款：账期已到的采购应付按供应商逐批付款（银行转账）</li>
 *   <li>每月的日常费用：房租、工资、水电、办公费（费用支出单，确认时生成凭证）</li>
 *   <li>用财务模块「按单据生成凭证」的能力给演示期间的应收应付（采购入库、销售出库 / 退货、运费等）和收付款（含平台结算收款）生成凭证</li>
 *   <li>成本核算与审核记账放在最后（{@link DemoFinanceCloseContributor}）：成本核算后的期间不能再办理出入库，其他开发线的组成部分还要写历史单据</li>
 * </ul>
 * 凭证号由财务模块按期间顺序分配，汇率沿用各单据的记账汇率；每一批在独立事务中执行，一批失败不影响其他
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoFinanceContributor implements DemoContributor {

    private static final String BANK_ACCOUNT = "100201";
    private static final String CAPITAL_ACCOUNT = "4001";
    private static final BigDecimal CAPITAL = BigDecimal.valueOf(3_000_000);
    private static final String TRANSFER = "TRANSFER";
    private static final int CHUNK = 200;
    /** 每月日常费用：收支类别名称、金额（元）、备注 */
    private static final Object[][] MONTHLY_EXPENSES = {{"房租", 12000, "深圳办公室与仓库租金"}, {"工资", 56000, "运营、仓库、客服人员工资"}, // i18n-ignore 演示数据存库
            {"水电费", 1500, "办公室与仓库水电"}, {"办公费", 1800, "办公用品、ERP 与选品工具订阅"}}; // i18n-ignore 演示数据存库

    private final FinAccountService accountService;
    private final FinOpeningService openingService;
    private final FinBillService billService;
    private final FinSettlementService settlementService;
    private final FinIoService ioService;
    private final FinIoCategoryService ioCategoryService;
    private final DemoTx tx;

    @Override
    public int order() {
        return 95;
    }

    @Override
    public String key() {
        return "finance";
    }

    @Override
    public void generate(DemoContext ctx) {
        FinAccount bank = accountService.leafByCode(BANK_ACCOUNT);
        LocalDate today = ctx.today();
        LocalDate begin = begin(ctx);
        tx.run("opening", () -> opening(bank));
        payables(ctx, bank.getId());
        expenses(today.minusDays(ctx.days()), today, bank.getId());
        long t0 = System.currentTimeMillis();
        int bills = generateVouchers(billService, settlementService, tx, begin, today);
        log.info("Demo finance: {} vouchers generated in {} ms", bills, System.currentTimeMillis() - t0);
    }

    /** 演示期间的开始（业务单据最早的日期所在月的月初） */
    static LocalDate begin(DemoContext ctx) {
        return ctx.today().minusDays(ctx.days() + 60L).withDayOfMonth(1);
    }

    /** 科目期初：租户还没有任何期初余额时，银行存款 / 实收资本 */
    private void opening(FinAccount bank) {
        List<AccountOpeningRow> rows = openingService.accountOpenings();
        if (rows.stream().anyMatch(r -> r.getDebit().signum() != 0 || r.getCredit().signum() != 0)) {
            return;
        }
        FinAccount capital = accountService.leafByCode(CAPITAL_ACCOUNT);
        openingService.saveAccountOpenings(new AccountOpeningRequest(List.of(new AccountOpeningRequest.Item(bank.getId(), CAPITAL, null),
                new AccountOpeningRequest.Item(capital.getId(), null, CAPITAL))));
    }

    /** 支付供应商货款：入库后按账期（30 / 45 天）付款，到期日不晚于今天的应付按供应商、到期日逐批付清 */
    private void payables(DemoContext ctx, Long bankId) {
        Map<String, Long> suppliers = ctx.get(DemoContributors.SUPPLIER_BY_CODE);
        if (suppliers == null) {
            return;
        }
        for (Long supplierId : suppliers.values()) {
            List<FinBill> due = billService.unsettled(BillKind.AP, supplierId).stream()
                    .filter(b -> b.getStatus() != BillStatus.SETTLED && b.getAmount().signum() > 0 && b.getDueDate() != null
                            && !b.getDueDate().isAfter(ctx.today()))
                    .toList();
            for (FinBill b : due) {
                BigDecimal amount = b.getAmount().subtract(b.getSettledAmount() == null ? BigDecimal.ZERO : b.getSettledAmount());
                if (amount.signum() <= 0) {
                    continue;
                }
                tx.run("payment", () -> settlementService.create(new SettlementRequest(SettleKind.PAYMENT, supplierId, b.getDueDate(), bankId, TRANSFER,
                        "支付货款 " + b.getSourceNo(), List.of(new SettlementItemRequest(b.getId(), amount)), null, amount, false, null, true))); // i18n-ignore 演示数据存库
            }
        }
    }

    /** 每月日常费用（开始有订单的月份起，当月按已过天数折算），确认时生成凭证 */
    private void expenses(LocalDate salesStart, LocalDate today, Long bankId) {
        Map<String, Long> categories = new java.util.HashMap<>();
        for (FinIoCategory c : ioCategoryService.list(FundDirection.OUT, EnableStatus.ENABLED)) {
            categories.putIfAbsent(c.getName(), c.getId());
        }
        for (YearMonth m = YearMonth.from(salesStart); !m.isAfter(YearMonth.from(today)); m = m.plusMonths(1)) {
            LocalDate date = m.equals(YearMonth.from(today)) ? today : m.atEndOfMonth();
            List<IoRequests.Item> items = new ArrayList<>();
            for (Object[] e : MONTHLY_EXPENSES) {
                Long categoryId = categories.get((String) e[0]);
                if (categoryId != null) {
                    double ratio = m.equals(YearMonth.from(today)) ? today.getDayOfMonth() / (double) m.lengthOfMonth() : 1;
                    items.add(new IoRequests.Item(categoryId, BigDecimal.valueOf(Math.round((Integer) e[1] * ratio)), (String) e[2]));
                }
            }
            if (items.isEmpty()) {
                continue;
            }
            String remark = m + " 日常费用"; // i18n-ignore 演示数据存库
            tx.run("expense", () -> {
                Long id = ioService.create(new IoRequests.Save(FundDirection.OUT, date, null, null, null, null, TRANSFER, bankId, null, remark, items));
                ioService.confirm(id);
            });
        }
    }

    /** 给日期范围内还没有凭证的应收应付、收付款按单据生成凭证（每批一个事务），返回生成的凭证数 */
    static int generateVouchers(FinBillService billService, FinSettlementService settlementService, DemoTx tx, LocalDate begin, LocalDate end) {
        int[] count = {0};
        List<Long> bills = tx.read(() -> billService.unvoucheredIds(begin, end));
        for (int i = 0; i < bills.size(); i += CHUNK) {
            List<Long> chunk = bills.subList(i, Math.min(bills.size(), i + CHUNK));
            tx.run("billVouchers", () -> count[0] += billService.generateVouchers(chunk));
        }
        List<Long> settles = tx.read(() -> settlementService.unvoucheredIds(begin, end));
        for (int i = 0; i < settles.size(); i += CHUNK) {
            List<Long> chunk = settles.subList(i, Math.min(settles.size(), i + CHUNK));
            tx.run("settlementVouchers", () -> count[0] += settlementService.generateVouchers(chunk));
        }
        return count[0];
    }
}
