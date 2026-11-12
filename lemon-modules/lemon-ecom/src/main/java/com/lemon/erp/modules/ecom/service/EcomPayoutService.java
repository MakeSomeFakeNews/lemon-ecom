package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dto.PayoutImportRequest;
import com.lemon.erp.modules.ecom.dto.PayoutImportResult;
import com.lemon.erp.modules.ecom.dto.PayoutImportRow;
import com.lemon.erp.modules.ecom.dto.PayoutReceiveRequest;
import com.lemon.erp.modules.ecom.entity.EcomPayout;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomPayoutMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementMapper;
import com.lemon.erp.modules.ecom.model.PayoutSource;
import com.lemon.erp.modules.ecom.model.PayoutStatus;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.service.FinAccountService;
import com.lemon.erp.modules.fin.service.FinSettlementService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 回款对账：平台结算单的净额是应到账金额（导入结算单时生成「未到账」记录），银行 / 收款平台到账后登记或导入流水自动匹配。
 * <p>
 * <b>自动匹配</b>：流水币种与结算币种相同时比较原币金额，流水为本位币（收款平台已结汇）时比较本位币应到账；候选为未到账记录中
 * 店铺相符（流水带店铺编码或导入时选了店铺）、到账日期与结算周期结束日相差不超过日期窗口、金额差不超过容差（应到账 × %）的，
 * 备注 / 流水号中含结算编号的优先，其次金额差最小、日期差最小；一条记录只匹配一条流水。金额相同为已到账，否则为有差异；
 * 没有匹配到的流水单独记为有差异，可手工匹配。
 * <p>
 * <b>汇兑处理</b>：结算确认时按结算汇率（结算周期结束日所在月）生成收款单，借 收款账户 = 本位币净额；实际到账按到账汇率折算，
 * 差额生成一张凭证（来源 期末调汇 FX，摘要含结算编号，未审核）：借 到账账户（到账本位币）/ 贷 收款账户（结算本位币净额），
 * 差额计入财务费用（汇兑损益 = 应到账原币 × (到账汇率 − 结算汇率)，其余为金额差异，如收款平台手续费）；到账账户与收款账户相同时只记差额。
 * 结算单没有生成收款单（确认时没有选择收款账户）时不生成凭证，只记录差额
 */
@Service
@RequiredArgsConstructor
public class EcomPayoutService {

    /** 收款平台业务字典 */
    public static final String CHANNEL_DICT = "ecom_payout_channel";

    private final EcomPayoutMapper payoutMapper;
    private final EcomSettlementMapper settlementMapper;
    private final EcomStoreService storeService;
    private final ExchangeRateProvider rateProvider;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinSettlementService finSettlementService;
    private final FinVoucherService voucherService;
    private final DictLookup dictLookup;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomPayout> page(long current, long size, PayoutStatus status, Long storeId, LocalDate begin, LocalDate end, String keyword) {
        Page<EcomPayout> page = Page.of(current, size);
        payoutMapper.selectPageList(page, status, storeId, begin, end, keyword);
        return PageResult.of(page);
    }

    /** 各状态的记录数和本位币金额（标签页上的数字） */
    public List<Map<String, Object>> summary(Long storeId) {
        return payoutMapper.selectStatusSummary(storeId);
    }

    public EcomPayout detail(Long id) {
        EcomPayout p = payoutMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("ecom.error.payout.notFound");
        }
        return p;
    }

    private EcomPayout get(Long id) {
        EcomPayout p = payoutMapper.selectById(id);
        if (p == null) {
            throw new BizException("ecom.error.payout.notFound");
        }
        return p;
    }

    // ---------------------------------------------------------------- 结算单联动

    /** 结算单导入后：净额为正时生成未到账记录 */
    public void createPending(EcomSettlement s) {
        if (Decimals.nz(s.getNetAmount()).signum() <= 0) {
            return;
        }
        EcomPayout p = new EcomPayout();
        p.setStoreId(s.getStoreId());
        p.setSettlementId(s.getId());
        p.setSource(PayoutSource.SETTLEMENT);
        p.setCurrency(s.getCurrency());
        p.setExpectedAmount(s.getNetAmount());
        p.setExpectedBase(s.getNetBase());
        p.setExpectedDate(s.getPeriodEnd());
        p.setStatus(PayoutStatus.PENDING);
        payoutMapper.insert(p);
    }

    /** 删除结算单前：已匹配到账的不能删除，未到账记录一并删除 */
    public void deleteBySettlement(Long settlementId, String settlementNo) {
        List<EcomPayout> list = payoutMapper.selectList(Wrappers.<EcomPayout>lambdaQuery().eq(EcomPayout::getSettlementId, settlementId));
        if (list.stream().anyMatch(p -> p.getStatus() != PayoutStatus.PENDING)) {
            throw new BizException("ecom.error.payout.settlementMatched", settlementNo);
        }
        if (!list.isEmpty()) {
            payoutMapper.deleteByIds(list.stream().map(EcomPayout::getId).toList());
        }
    }

    /** 结算单确认（生成收款单）后：已经匹配到账的补生成差额凭证 */
    public void afterSettlementConfirmed(Long settlementId) {
        for (EcomPayout p : payoutMapper.selectList(Wrappers.<EcomPayout>lambdaQuery().eq(EcomPayout::getSettlementId, settlementId)
                .ne(EcomPayout::getStatus, PayoutStatus.PENDING).isNull(EcomPayout::getVoucherId))) {
            postVoucher(p);
        }
    }

    // ---------------------------------------------------------------- 登记与匹配

    /** 手工登记到账（未到账记录） */
    @Transactional
    public void receive(Long id, PayoutReceiveRequest req) {
        EcomPayout p = get(id);
        if (p.getStatus() != PayoutStatus.PENDING || p.getSettlementId() == null) {
            throw new BizException("ecom.error.payout.notPending");
        }
        accountService.requireCashAccount(req.accountId());
        dictLookup.check(CHANNEL_DICT, req.channel(), "ecom.label.payout.channel");
        String payCurrency = StringUtils.hasText(req.payCurrency()) ? req.payCurrency().trim().toUpperCase(Locale.ROOT) : p.getCurrency();
        if (!payCurrency.equals(p.getCurrency()) && !ExchangeRateProvider.BASE.equals(payCurrency)) {
            throw new BizException("ecom.error.payout.currencyMismatch", payCurrency, p.getCurrency());
        }
        fill(p, req.payoutDate(), req.amount(), payCurrency, req.exchangeRate());
        p.setAccountId(req.accountId());
        p.setChannel(req.channel());
        p.setBankRef(req.bankRef());
        p.setRemark(req.remark());
        p.setSource(PayoutSource.MANUAL);
        payoutMapper.updateById(p);
        postVoucher(p);
    }

    /** 手工匹配：把一条未匹配的流水并入一条未到账记录 */
    @Transactional
    public void match(Long flowId, Long pendingId) {
        EcomPayout flow = get(flowId);
        if (flow.getSettlementId() != null || flow.getAmount() == null) {
            throw new BizException("ecom.error.payout.notUnmatched");
        }
        EcomPayout p = get(pendingId);
        if (p.getStatus() != PayoutStatus.PENDING || p.getSettlementId() == null) {
            throw new BizException("ecom.error.payout.notPending");
        }
        String payCurrency = flow.getPayCurrency() == null ? flow.getCurrency() : flow.getPayCurrency();
        if (!payCurrency.equals(p.getCurrency()) && !ExchangeRateProvider.BASE.equals(payCurrency)) {
            throw new BizException("ecom.error.payout.currencyMismatch", payCurrency, p.getCurrency());
        }
        fill(p, flow.getPayoutDate(), flow.getAmount(), payCurrency, payCurrency.equals(p.getCurrency()) ? flow.getExchangeRate() : null);
        copyFlow(flow, p);
        payoutMapper.updateById(p);
        payoutMapper.deleteById(flowId);
        postVoucher(p);
    }

    /**
     * 撤销到账：删除差额凭证（只能是未审核的），记录回到未到账；导入的流水重新记为一条未匹配的流水
     */
    @Transactional
    public void unmatch(Long id) {
        EcomPayout p = get(id);
        if (p.getSettlementId() == null || p.getStatus() == PayoutStatus.PENDING) {
            throw new BizException("ecom.error.payout.notMatched");
        }
        if (p.getVoucherId() != null) {
            voucherService.deleteOwned(p.getVoucherId());
        }
        if (p.getSource() == PayoutSource.IMPORT) {
            EcomPayout flow = new EcomPayout();
            flow.setStoreId(p.getStoreId());
            flow.setSource(PayoutSource.IMPORT);
            flow.setCurrency(p.getPayCurrency() == null ? p.getCurrency() : p.getPayCurrency());
            flow.setPayCurrency(flow.getCurrency());
            flow.setPayoutDate(p.getPayoutDate());
            flow.setAmount(p.getAmount());
            flow.setAccountId(p.getAccountId());
            flow.setChannel(p.getChannel());
            flow.setBankRef(p.getBankRef());
            flow.setRemark(p.getRemark());
            flow.setExchangeRate(Objects.equals(flow.getCurrency(), p.getCurrency()) ? p.getExchangeRate() : null);
            flow.setStatus(PayoutStatus.DIFF);
            payoutMapper.insert(flow);
        }
        payoutMapper.update(Wrappers.<EcomPayout>lambdaUpdate().set(EcomPayout::getPayoutDate, null).set(EcomPayout::getAmount, null)
                .set(EcomPayout::getPayCurrency, null).set(EcomPayout::getAccountId, null).set(EcomPayout::getChannel, null)
                .set(EcomPayout::getBankRef, null).set(EcomPayout::getExchangeRate, null).set(EcomPayout::getAmountBase, null)
                .set(EcomPayout::getFxDiff, null).set(EcomPayout::getAmountDiff, null).set(EcomPayout::getVoucherId, null)
                .set(EcomPayout::getRemark, null).set(EcomPayout::getSource, PayoutSource.SETTLEMENT).set(EcomPayout::getStatus, PayoutStatus.PENDING)
                .eq(EcomPayout::getId, id));
    }

    /** 删除未匹配的流水 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomPayout p = get(id);
            if (p.getSettlementId() != null) {
                throw new BizException("ecom.error.payout.cannotDelete");
            }
        }
        payoutMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 导入流水

    /** 导入到账流水并自动匹配；任何一行出错整批不导入 */
    public PayoutImportResult importRows(PayoutImportRequest req) {
        List<ImportResult.RowError> errors = new ArrayList<>();
        accountService.requireCashAccount(req.accountId());
        dictLookup.check(CHANNEL_DICT, req.channel(), "ecom.label.payout.channel");
        Map<String, EcomStore> stores = new HashMap<>();
        List<ImportRow<PayoutImportRow>> rows = new ArrayList<>();
        for (ImportRow<PayoutImportRow> row : req.rows()) {
            if (row == null || row.data() == null) {
                continue;
            }
            var violations = validator.validate(row.data());
            if (!violations.isEmpty()) {
                errors.add(new ImportResult.RowError(row.rowNum(), violations.stream().map(ConstraintViolation::getMessage).distinct().sorted()
                        .collect(Collectors.joining("；"))));
                continue;
            }
            PayoutImportRow r = row.data();
            if (StringUtils.hasText(r.getStoreCode())) {
                EcomStore s = stores.computeIfAbsent(r.getStoreCode().trim(), storeService::findByCode);
                if (s == null) {
                    errors.add(new ImportResult.RowError(row.rowNum(), Messages.get("ecom.error.storeCodeNotFound", r.getStoreCode())));
                    continue;
                }
            }
            rows.add(row);
        }
        if (!errors.isEmpty()) {
            return new PayoutImportResult(req.rows().size(), 0, 0, 0, 0, 0, errors);
        }
        int[] counts = new int[4];
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Set<Long> used = new HashSet<>();
                Set<String> refs = new HashSet<>();
                for (ImportRow<PayoutImportRow> row : rows) {
                    try {
                        importOne(req, row.data(), stores, used, refs, counts);
                    } catch (BizException ex) {
                        errors.add(new ImportResult.RowError(row.rowNum(), ex.getMessage()));
                    }
                }
                if (!errors.isEmpty()) {
                    status.setRollbackOnly();
                }
            });
        } catch (RuntimeException ex) {
            if (errors.isEmpty()) {
                throw ex;
            }
        }
        if (!errors.isEmpty()) {
            return new PayoutImportResult(req.rows().size(), 0, 0, 0, 0, 0, errors);
        }
        int imported = counts[0] + counts[2];
        return new PayoutImportResult(req.rows().size(), imported, counts[0], counts[1], counts[2], counts[3], List.of());
    }

    /** counts：0 匹配、1 其中有差异、2 未匹配、3 跳过 */
    private void importOne(PayoutImportRequest req, PayoutImportRow r, Map<String, EcomStore> stores, Set<Long> used, Set<String> refs, int[] counts) {
        String ref = StringUtils.hasText(r.getBankRef()) ? r.getBankRef().trim() : null;
        if (ref != null && (!refs.add(ref) || payoutMapper.exists(Wrappers.<EcomPayout>lambdaQuery().eq(EcomPayout::getBankRef, ref)))) {
            counts[3]++;
            return;
        }
        String currency = r.getCurrency().trim().toUpperCase(Locale.ROOT);
        Long storeId = StringUtils.hasText(r.getStoreCode()) ? stores.get(r.getStoreCode().trim()).getId() : req.storeId();
        EcomPayout best = bestCandidate(r, currency, storeId, req, used);
        if (best == null) {
            EcomPayout flow = new EcomPayout();
            flow.setStoreId(storeId);
            flow.setSource(PayoutSource.IMPORT);
            flow.setCurrency(currency);
            flow.setPayCurrency(currency);
            flow.setPayoutDate(r.getPayoutDate());
            flow.setAmount(Decimals.money(r.getAmount()));
            flow.setAccountId(req.accountId());
            flow.setChannel(req.channel());
            flow.setBankRef(ref);
            flow.setRemark(r.getRemark());
            flow.setExchangeRate(r.getExchangeRate());
            flow.setStatus(PayoutStatus.DIFF);
            payoutMapper.insert(flow);
            counts[2]++;
            return;
        }
        used.add(best.getId());
        fill(best, r.getPayoutDate(), r.getAmount(), currency, currency.equals(best.getCurrency()) ? r.getExchangeRate() : null);
        best.setAccountId(req.accountId());
        best.setChannel(req.channel());
        best.setBankRef(ref);
        best.setRemark(r.getRemark());
        best.setSource(PayoutSource.IMPORT);
        payoutMapper.updateById(best);
        postVoucher(best);
        counts[0]++;
        if (best.getStatus() == PayoutStatus.DIFF) {
            counts[1]++;
        }
    }

    private EcomPayout bestCandidate(PayoutImportRow r, String currency, Long storeId, PayoutImportRequest req, Set<Long> used) {
        boolean base = ExchangeRateProvider.BASE.equals(currency);
        BigDecimal amount = Decimals.money(r.getAmount());
        String text = ((r.getRemark() == null ? "" : r.getRemark()) + " " + (r.getBankRef() == null ? "" : r.getBankRef())).toLowerCase(Locale.ROOT);
        List<EcomPayout> pending = payoutMapper.selectPending(base ? null : currency, storeId);
        return pending.stream()
                .filter(p -> !used.contains(p.getId()))
                .filter(p -> currency.equals(p.getCurrency()) || base)
                .filter(p -> p.getExpectedDate() == null || Math.abs(ChronoUnit.DAYS.between(p.getExpectedDate(), r.getPayoutDate())) <= req.window())
                .filter(p -> {
                    BigDecimal expected = expectedIn(p, currency);
                    BigDecimal tol = expected.abs().multiply(req.tolerancePct()).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP);
                    return amount.subtract(expected).abs().compareTo(tol) <= 0;
                })
                .min(Comparator.<EcomPayout>comparingInt(p -> p.getSettlementNo() != null && !p.getSettlementNo().isEmpty()
                                && text.contains(p.getSettlementNo().toLowerCase(Locale.ROOT)) ? 0 : 1)
                        .thenComparing(p -> amount.subtract(expectedIn(p, currency)).abs())
                        .thenComparingLong(p -> p.getExpectedDate() == null ? 0 : Math.abs(ChronoUnit.DAYS.between(p.getExpectedDate(), r.getPayoutDate())))
                        .thenComparing(EcomPayout::getId))
                .orElse(null);
    }

    /** 应到账金额：按流水币种比较（结算币种为原币，流水为本位币时为本位币应到账） */
    private static BigDecimal expectedIn(EcomPayout p, String currency) {
        return currency.equals(p.getCurrency()) ? Decimals.nz(p.getExpectedAmount()) : Decimals.nz(p.getExpectedBase());
    }

    /**
     * 填写到账金额并计算差额：到账币种与结算币种相同时，到账本位币 = 金额 × 到账汇率（为空取到账日期所在月的汇率），
     * 金额差 = (到账 − 应到账) × 到账汇率，汇兑差 = 到账本位币 − 结算本位币 − 金额差；到账为本位币时差额全部记为汇兑差，汇率为 到账 ÷ 应到账原币
     */
    void fill(EcomPayout p, LocalDate date, BigDecimal amount, String payCurrency, BigDecimal rateInput) {
        BigDecimal amt = Decimals.money(amount);
        BigDecimal expected = Decimals.nz(p.getExpectedAmount());
        BigDecimal expectedBase = Decimals.nz(p.getExpectedBase());
        p.setPayoutDate(date);
        p.setAmount(amt);
        p.setPayCurrency(payCurrency);
        BigDecimal amountBase;
        BigDecimal amountDiff;
        if (payCurrency.equals(p.getCurrency())) {
            BigDecimal rate = rateInput != null ? rateInput : rateProvider.rate(p.getCurrency(), date);
            if (rate == null) {
                throw new BizException("ecom.error.exchangeRateMissing", p.getCurrency(), date.toString().substring(0, 7));
            }
            p.setExchangeRate(rate);
            amountBase = Decimals.money(amt.multiply(rate));
            amountDiff = Decimals.money(amt.subtract(expected).multiply(rate));
            p.setStatus(amt.compareTo(expected) == 0 ? PayoutStatus.RECEIVED : PayoutStatus.DIFF);
        } else {
            amountBase = amt;
            amountDiff = BigDecimal.ZERO;
            p.setExchangeRate(expected.signum() == 0 ? null : amt.divide(expected, 6, RoundingMode.HALF_UP));
            p.setStatus(PayoutStatus.RECEIVED);
        }
        p.setAmountBase(amountBase);
        p.setAmountDiff(amountDiff);
        p.setFxDiff(amountBase.subtract(expectedBase).subtract(amountDiff));
    }

    private static void copyFlow(EcomPayout flow, EcomPayout p) {
        p.setAccountId(flow.getAccountId());
        p.setChannel(flow.getChannel());
        p.setBankRef(flow.getBankRef());
        p.setRemark(flow.getRemark());
        p.setSource(PayoutSource.IMPORT);
    }

    /** 生成差额凭证：结算单已生成收款单、差额不为 0（或到账账户与收款账户不同）时 */
    private void postVoucher(EcomPayout p) {
        if (p.getSettlementId() == null || p.getStatus() == PayoutStatus.PENDING || p.getVoucherId() != null) {
            return;
        }
        EcomSettlement s = settlementMapper.selectById(p.getSettlementId());
        if (s == null || s.getFinSettlementId() == null) {
            return;
        }
        FinSettlement receipt = finSettlementService.detail(s.getFinSettlementId());
        Long receiptAccount = receipt.getAccountId();
        Long toAccount = p.getAccountId() == null ? receiptAccount : p.getAccountId();
        BigDecimal expectedBase = Decimals.nz(p.getExpectedBase());
        BigDecimal amountBase = Decimals.nz(p.getAmountBase());
        BigDecimal fx = Decimals.nz(p.getFxDiff());
        BigDecimal diff = Decimals.nz(p.getAmountDiff());
        if (Objects.equals(toAccount, receiptAccount) && fx.signum() == 0 && diff.signum() == 0) {
            return;
        }
        EcomStore store = storeService.get(s.getStoreId());
        String summary = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.payout.voucherSummary", store.getName(), s.getSettlementNo());
        Long finance = accountRules.accountId(AccountRole.FINANCE_EXPENSE);
        List<VoucherLine> lines = new ArrayList<>();
        if (Objects.equals(toAccount, receiptAccount)) {
            lines.add(VoucherLine.debit(summary, toAccount, null, fx.add(diff)));
        } else {
            lines.add(VoucherLine.debit(summary, toAccount, null, amountBase));
            lines.add(VoucherLine.credit(summary, receiptAccount, null, expectedBase));
        }
        if (fx.signum() != 0) {
            lines.add(VoucherLine.credit(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.payout.fxSummary", summary), finance, null, fx));
        }
        if (diff.signum() != 0) {
            lines.add(VoucherLine.credit(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.payout.diffSummary", summary), finance, null, diff));
        }
        lines.removeIf(VoucherLine::isZero);
        if (lines.size() < 2) {
            return;
        }
        String sourceNo = s.getSettlementNo().length() > 32 ? s.getSettlementNo().substring(0, 32) : s.getSettlementNo();
        Long voucherId = voucherService.createGenerated(p.getPayoutDate(), VoucherSource.FX, sourceNo, lines);
        p.setVoucherId(voucherId);
        payoutMapper.update(Wrappers.<EcomPayout>lambdaUpdate().set(EcomPayout::getVoucherId, voucherId).eq(EcomPayout::getId, p.getId()));
    }
}
