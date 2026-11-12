package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.entity.EcomSettlementItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementItemMapper;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台结算的费用确认（财务凭证侧）：结算单中订单收入、退款以外的明细（佣金、配送费、仓储费、广告费、退货处理费、订阅费、赔偿冲减等）
 * 按凭证科目设置「电商平台费用」（{@link AccountRole#PLATFORM_FEE}，按结算明细类型分别设置，默认销售费用）取科目，
 * 每个科目生成一张红字应收（来源 PLATFORM_FEE，凭证 借 费用科目 / 贷 应收账款-店铺客户），日期为结算周期结束日（该期间已结账时为今天）。
 * 金额合计 = 本位币的（订单收入 + 退款）− 结算净额，各科目按原币费用的比例分摊，尾差计入最后一个科目；
 * 收款单只按结算净额收款、用这些红字应收冲抵应收，平台费用不再作为收款折让计入财务费用。电商利润分析的口径不受影响
 */
@Component
@RequiredArgsConstructor
public class EcomSettlementFees {

    private final EcomSettlementItemMapper itemMapper;
    private final FinAccountRuleService accountRules;
    private final FinBillService billService;
    private final FinPeriodService periodService;

    /**
     * @param feeBase 平台费用合计（本位币，正数），为 0 时不生成
     * @return 生成的红字应收（金额为负数），合计等于 −feeBase
     */
    public List<FinBill> createFeeBills(EcomSettlement s, EcomStore store, BigDecimal feeBase) {
        if (feeBase == null || feeBase.signum() <= 0) {
            return List.of();
        }
        Map<Long, BigDecimal> byAccount = new LinkedHashMap<>();
        for (EcomSettlementItem it : itemMapper.selectList(Wrappers.<EcomSettlementItem>lambdaQuery().eq(EcomSettlementItem::getSettlementId, s.getId()))) {
            SettlementItemType type = it.getItemType();
            if (type == null || type == SettlementItemType.ORDER || type == SettlementItemType.REFUND) {
                continue;
            }
            byAccount.merge(accountRules.accountId(AccountRole.PLATFORM_FEE, type.name()), Decimals.nz(it.getAmount()).negate(), BigDecimal::add);
        }
        byAccount.values().removeIf(v -> v.signum() <= 0);
        if (byAccount.isEmpty()) {
            byAccount.put(accountRules.accountId(AccountRole.PLATFORM_FEE), BigDecimal.ONE);
        }
        BigDecimal total = byAccount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate date = s.getPeriodEnd() == null || periodService.isClosed(s.getPeriodEnd().getYear() * 100 + s.getPeriodEnd().getMonthValue())
                ? LocalDate.now() : s.getPeriodEnd();
        String remark = Messages.get("ecom.text.settlementFeeRemark", store.getName(), s.getSettlementNo());
        List<FinBill> bills = new ArrayList<>();
        BigDecimal allotted = BigDecimal.ZERO;
        List<Map.Entry<Long, BigDecimal>> entries = new ArrayList<>(byAccount.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            BigDecimal amount = i == entries.size() - 1 ? feeBase.subtract(allotted)
                    : feeBase.multiply(entries.get(i).getValue()).divide(total, 2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                continue;
            }
            allotted = allotted.add(amount);
            bills.add(billService.createPlatformFeeBill(store.getCustomerId(), s.getId(), s.getSettlementNo(), date, amount, entries.get(i).getKey(), remark));
        }
        return bills;
    }
}
