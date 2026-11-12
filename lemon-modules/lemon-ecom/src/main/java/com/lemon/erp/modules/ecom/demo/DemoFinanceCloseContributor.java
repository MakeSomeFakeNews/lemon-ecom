package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.LoginUserLoader;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.VoucherQuery;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinCostService;
import com.lemon.erp.modules.fin.service.FinSettlementService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.mapper.EcomDemoMapper;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoPartnerType;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.fin.service.FinIoCategoryService;
import com.lemon.erp.modules.fin.service.FinIoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 演示数据 · 财务收尾（order 9900，在所有组成部分之后）：每月的跨境物流费与平台广告费（费用支出单，销售费用），
 * 给其他组成部分后来写入的应收应付、收付款补生成凭证，
 * 逐月成本核算（生成成本结转凭证，核算后的期间不能再办理出入库，所以放在最后），再以会计演示账号的身份
 * 把演示期间未审核的凭证审核并记账（制单人不能审核自己的凭证；没有会计账号时以当前用户审核），不结账
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoFinanceCloseContributor implements DemoContributor {

    private static final int PAGE = 500;
    private static final String BANK_ACCOUNT = "100201";
    private static final String TRANSFER = "TRANSFER";
    private static final String FREIGHT_CATEGORY = "跨境物流费"; // i18n-ignore 演示数据存库
    private static final String AD_CATEGORY = "平台广告费"; // i18n-ignore 演示数据存库

    private final FinBillService billService;
    private final FinSettlementService settlementService;
    private final FinCostService costService;
    private final FinVoucherService voucherService;
    private final LoginUserLoader loginUserLoader;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinIoService ioService;
    private final FinIoCategoryService ioCategoryService;
    private final EcomDemoMapper demoMapper;
    private final ExchangeRateProvider rateProvider;
    private final DemoTx tx;

    @Override
    public int order() {
        return 9900;
    }

    @Override
    public String key() {
        return "finance-close";
    }

    @Override
    public void generate(DemoContext ctx) {
        if (ctx.get(DemoKeys.STORE_IDS) == null) {
            return;
        }
        LocalDate begin = DemoFinanceContributor.begin(ctx);
        LocalDate today = ctx.today();
        long t0 = System.currentTimeMillis();
        sellingExpenses(ctx, today);
        int vouchers = DemoFinanceContributor.generateVouchers(billService, settlementService, tx, begin, today);
        List<Integer> periods = new ArrayList<>();
        // 只核算到上月：当月核算成本后系统不再允许当月的出入库单据，演示环境当天就无法出库发货、采购入库
        for (YearMonth m = YearMonth.from(begin); m.isBefore(YearMonth.from(today)); m = m.plusMonths(1)) {
            periods.add(m.getYear() * 100 + m.getMonthValue());
        }
        for (Integer p : periods) {
            tx.run("costRun", () -> costService.run(p));
        }
        Long accountantId = ctx.get(DemoContributors.ACCOUNTANT_ID);
        LoginUser accountant = accountantId == null ? null : loginUserLoader.loadLoginUser(accountantId);
        int[] posted = {0};
        Runnable post = () -> periods.forEach(p -> posted[0] += auditAndPost(p));
        if (accountant != null) {
            DemoDataJob.runAs(accountant, post);
        } else {
            post.run();
        }
        log.info("Demo finance close: {} more vouchers, cost runs for {} periods, {} vouchers posted by {} in {} ms", vouchers, periods.size(),
                posted[0], accountant == null ? SecurityUtils.getUsernameOrNull() : accountant.username(), System.currentTimeMillis() - t0);
    }

    /**
     * 每月的跨境物流费（头程货件的运费、关税与杂费按发运月 + 自发货订单的尾程运费按发货月）和平台广告费（按广告日报折本位币），
     * 付给货代 / 物流商、平台的费用支出单（销售费用），确认时生成凭证；放在最后以包含各组成部分写入的货件、订单和广告
     */
    private void sellingExpenses(DemoContext ctx, LocalDate today) {
        List<Long> stores = ctx.get(DemoKeys.STORE_IDS);
        Long bankId = accountService.leafByCode(BANK_ACCOUNT).getId();
        Long freightCat = category(FREIGHT_CATEGORY, accountRules.accountId(AccountRole.SALES_FREIGHT));
        Long adCat = category(AD_CATEGORY, accountRules.accountId(AccountRole.SELLING_EXPENSE));
        Map<Integer, BigDecimal> freight = new TreeMap<>();
        demoMapper.selectMonthlySellerShipping(stores).forEach(r -> freight.merge(ym(r), dec(r), BigDecimal::add));
        demoMapper.selectMonthlyInboundCost(stores).forEach(r -> freight.merge(ym(r), dec(r), BigDecimal::add));
        Map<Integer, BigDecimal> ads = new TreeMap<>();
        for (Map<String, Object> r : demoMapper.selectMonthlyAdSpend(stores)) {
            int ym = ym(r);
            BigDecimal rate = rateProvider.rate((String) r.get("currency"), LocalDate.of(ym / 100, ym % 100, 1));
            if (rate != null) {
                ads.merge(ym, dec(r).multiply(rate).setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
            }
        }
        Long forwarder = ctx.get(DemoContributors.FORWARDER_ID);
        freight.forEach((ym, amount) -> expense(ym, today, bankId, freightCat, amount, forwarder, "跨境物流费（头程货代与尾程物流商）")); // i18n-ignore 演示数据存库
        ads.forEach((ym, amount) -> expense(ym, today, bankId, adCat, amount, null, "平台站内广告费")); // i18n-ignore 演示数据存库
    }

    private void expense(int ym, LocalDate today, Long bankId, Long categoryId, BigDecimal amount, Long supplierId, String remark) {
        if (categoryId == null || amount.signum() <= 0) {
            return;
        }
        YearMonth m = YearMonth.of(ym / 100, ym % 100);
        LocalDate date = m.equals(YearMonth.from(today)) ? today : m.atEndOfMonth();
        if (date.isAfter(today)) {
            return;
        }
        tx.run("sellingExpense", () -> {
            Long id = ioService.create(new IoRequests.Save(FundDirection.OUT, date, null, supplierId == null ? null : IoPartnerType.SUPPLIER, supplierId,
                    null, TRANSFER, bankId, null, m + " " + remark, List.of(new IoRequests.Item(categoryId, amount, remark))));
            ioService.confirm(id);
        });
    }

    /** 费用支出类别（按名称复用，没有时新建） */
    private Long category(String name, Long accountId) {
        return ioCategoryService.list(FundDirection.OUT, null).stream().filter(c -> name.equals(c.getName())).map(FinIoCategory::getId).findFirst()
                .orElseGet(() -> tx.read(() -> ioCategoryService.create(new IoRequests.CategorySave(name, FundDirection.OUT, accountId, 20, EnableStatus.ENABLED,
                        "演示数据：跨境电商销售费用")))); // i18n-ignore 演示数据存库
    }

    private static int ym(Map<String, Object> r) {
        return ((Number) r.get("ym")).intValue();
    }

    private static BigDecimal dec(Map<String, Object> r) {
        Object v = r.get("amount");
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 期间内未审核的凭证审核并记账（每批一个事务） */
    private int auditAndPost(int period) {
        int total = 0;
        while (true) {
            List<Long> ids = tx.read(() -> voucherService.page(1, PAGE, new VoucherQuery(period, VoucherStatus.DRAFT, null, null, null)).records()
                    .stream().map(FinVoucher::getId).toList());
            if (ids.isEmpty()) {
                return total;
            }
            if (!tx.run("post", () -> {
                voucherService.audit(ids);
                voucherService.post(ids);
            })) {
                // 有一张不能审核时逐张处理，处理不了的留在未审核
                for (Long id : ids) {
                    if (tx.run("postOne", () -> {
                        voucherService.audit(List.of(id));
                        voucherService.post(List.of(id));
                    })) {
                        total++;
                    }
                }
                return total;
            }
            total += ids.size();
        }
    }
}
