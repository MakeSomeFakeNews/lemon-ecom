package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dto.SettlementImportRow;
import com.lemon.erp.modules.ecom.entity.EcomFeeAllocation;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.entity.EcomSettlementItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSettlementMapper;
import com.lemon.erp.modules.ecom.model.FeeAllocMethod;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.model.SettlementStatus;
import com.lemon.erp.modules.fin.dto.SettlementItemRequest;
import com.lemon.erp.modules.fin.dto.SettlementRequest;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.SettleKind;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.fin.service.FinSettlementService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台结算单：导入结算报告（一行一笔收入 / 费用，类型可直接填写或按「费用映射」识别原始类型名）按 店铺 + 结算编号 合并，
 * 导入时分摊不挂订单的费用（{@link EcomFeeAllocationService}）并生成应到账记录（{@link EcomPayoutService}）；
 * 确认时把佣金、配送费、退款、退货处理费、其他费用回写到订单，并可按结算净额生成收款单核销店铺客户的应收（平台费用作为折让）
 */
@Service
@RequiredArgsConstructor
public class EcomSettlementService {

    private final EcomSettlementMapper settlementMapper;
    private final EcomSettlementItemMapper itemMapper;
    private final EcomStoreService storeService;
    private final EcomOrderService orderService;
    private final ExchangeRateProvider rateProvider;
    private final FinBillService billService;
    private final FinSettlementService finSettlementService;
    private final EcomFeeMappingService feeMappingService;
    private final EcomFeeAllocationService allocationService;
    private final EcomPayoutService payoutService;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomSettlement> page(long current, long size, Long storeId, SettlementStatus status, String keyword) {
        Page<EcomSettlement> page = Page.of(current, size);
        settlementMapper.selectPageList(page, storeId, status, keyword);
        return PageResult.of(page);
    }

    public EcomSettlement detail(Long id) {
        EcomSettlement s = settlementMapper.selectDetail(id);
        if (s == null) {
            throw new BizException("ecom.error.settlementNotFound");
        }
        s.setItems(itemMapper.selectBySettlementId(id));
        return s;
    }

    private EcomSettlement get(Long id) {
        EcomSettlement s = settlementMapper.selectById(id);
        if (s == null) {
            throw new BizException("ecom.error.settlementNotFound");
        }
        return s;
    }

    /**
     * 导入：逐行校验，按 店铺编码 + 结算编号 合并为结算单；已导入过的结算编号整批报错，任何一行出错整批不导入
     */
    public ImportResult importRows(ImportRequest<SettlementImportRow> req) {
        List<ImportResult.RowError> errors = new ArrayList<>();
        Map<String, List<SettlementImportRow>> groups = new LinkedHashMap<>();
        Map<String, Integer> firstRow = new LinkedHashMap<>();
        Map<String, EcomStore> stores = new LinkedHashMap<>();
        EcomFeeMappingService.Resolver resolver = feeMappingService.resolver();
        for (ImportRow<SettlementImportRow> row : req.rows()) {
            if (row == null || row.data() == null) {
                continue;
            }
            var violations = validator.validate(row.data());
            if (!violations.isEmpty()) {
                errors.add(new ImportResult.RowError(row.rowNum(), violations.stream().map(ConstraintViolation::getMessage).distinct()
                        .sorted().collect(Collectors.joining("；"))));
                continue;
            }
            SettlementImportRow r = row.data();
            EcomStore store = stores.computeIfAbsent(r.getStoreCode().trim(), storeService::findByCode);
            if (store == null) {
                errors.add(new ImportResult.RowError(row.rowNum(), Messages.get("ecom.error.storeCodeNotFound", r.getStoreCode())));
                continue;
            }
            if (r.getItemType() == null) {
                // 没有填写类型：按店铺平台的费用映射识别原始类型名
                r.setItemType(resolver.resolve(store.getPlatform(), r.getRawType()));
                if (r.getItemType() == null) {
                    errors.add(new ImportResult.RowError(row.rowNum(), StringUtils.hasText(r.getRawType())
                            ? Messages.get("ecom.error.fee.rawTypeUnknown", r.getRawType().trim()) : Messages.get("ecom.valid.selectItemType")));
                    continue;
                }
            }
            String key = store.getId() + "|" + r.getSettlementNo().trim();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            firstRow.putIfAbsent(key, row.rowNum());
        }
        if (!errors.isEmpty()) {
            return new ImportResult(req.rows().size(), 0, errors);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<String, List<SettlementImportRow>> e : groups.entrySet()) {
                    try {
                        EcomStore store = stores.get(e.getValue().get(0).getStoreCode().trim());
                        save(store, e.getValue());
                    } catch (BizException ex) {
                        errors.add(new ImportResult.RowError(firstRow.get(e.getKey()), ex.getMessage()));
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
        return errors.isEmpty() ? new ImportResult(req.rows().size(), req.rows().size(), List.of()) : new ImportResult(req.rows().size(), 0, errors);
    }

    private void save(EcomStore store, List<SettlementImportRow> rows) {
        SettlementImportRow first = rows.get(0);
        String no = first.getSettlementNo().trim();
        if (settlementMapper.exists(Wrappers.<EcomSettlement>lambdaQuery().eq(EcomSettlement::getStoreId, store.getId()).eq(EcomSettlement::getSettlementNo, no))) {
            throw new BizException("ecom.error.settlementExists", no);
        }
        EcomSettlement s = new EcomSettlement();
        s.setStoreId(store.getId());
        s.setSettlementNo(no);
        s.setPeriodStart(rows.stream().map(SettlementImportRow::getPeriodStart).min(Comparator.naturalOrder()).orElse(first.getPeriodStart()));
        s.setPeriodEnd(rows.stream().map(SettlementImportRow::getPeriodEnd).max(Comparator.naturalOrder()).orElse(first.getPeriodEnd()));
        s.setCurrency(first.getCurrency() == null || first.getCurrency().isBlank() ? store.getCurrency() : first.getCurrency().trim().toUpperCase());
        BigDecimal sales = BigDecimal.ZERO, refund = BigDecimal.ZERO, commission = BigDecimal.ZERO, fulfillment = BigDecimal.ZERO,
                storage = BigDecimal.ZERO, ad = BigDecimal.ZERO, other = BigDecimal.ZERO, adjustment = BigDecimal.ZERO;
        List<EcomSettlementItem> items = new ArrayList<>();
        for (SettlementImportRow r : rows) {
            BigDecimal amount = Decimals.money(r.getAmount());
            // 汇总栏：长期仓储费并入仓储费，预留 / 代收代缴税并入调整，其余细分类型并入其他费用
            switch (r.getItemType()) {
                case ORDER -> sales = sales.add(amount);
                case REFUND -> refund = refund.add(amount);
                case COMMISSION -> commission = commission.add(amount);
                case FULFILLMENT -> fulfillment = fulfillment.add(amount);
                case STORAGE, LONG_STORAGE -> storage = storage.add(amount);
                case AD -> ad = ad.add(amount);
                case ADJUSTMENT, RESERVE, MARKETPLACE_TAX -> adjustment = adjustment.add(amount);
                default -> other = other.add(amount);
            }
            EcomSettlementItem it = new EcomSettlementItem();
            it.setPostedDate(r.getPostedDate());
            it.setItemType(r.getItemType());
            it.setPlatformOrderNo(StringUtils.hasText(r.getPlatformOrderNo()) ? r.getPlatformOrderNo().trim() : null);
            it.setSellerSku(StringUtils.hasText(r.getSellerSku()) ? r.getSellerSku().trim() : null);
            it.setAmount(amount);
            it.setDescription(r.getDescription());
            it.setRawType(StringUtils.hasText(r.getRawType()) ? r.getRawType().trim() : null);
            items.add(it);
        }
        s.setSalesAmount(sales);
        s.setRefundAmount(refund);
        s.setCommissionFee(commission);
        s.setFulfillmentFee(fulfillment);
        s.setStorageFee(storage);
        s.setAdFee(ad);
        s.setOtherFee(other);
        s.setAdjustment(adjustment);
        s.setNetAmount(sales.add(refund).add(commission).add(fulfillment).add(storage).add(ad).add(other).add(adjustment));
        BigDecimal rate = rateProvider.rate(s.getCurrency(), s.getPeriodEnd());
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", s.getCurrency(), s.getPeriodEnd().toString().substring(0, 7));
        }
        s.setExchangeRate(rate);
        s.setNetBase(Decimals.money(s.getNetAmount().multiply(rate)));
        s.setStatus(SettlementStatus.IMPORTED);
        settlementMapper.insert(s);
        items.forEach(i -> i.setSettlementId(s.getId()));
        for (int i = 0; i < items.size(); i += 500) {
            itemMapper.insertBatch(items.subList(i, Math.min(items.size(), i + 500)));
        }
        allocationService.allocate(s);
        payoutService.createPending(s);
    }

    /**
     * 确认：按平台订单号把佣金、配送费、退款回写到订单；传入收款账户时按结算净额生成收款单，
     * 先到期的应收先核销，平台费用（订单收入 − 退款 − 净额）作为折让；净额超过未核销应收的部分转预收
     *
     * @param accountId 收款账户（现金 / 银行末级科目），为空只回写费用不生成收款
     */
    @Transactional
    public void confirm(Long id, Long accountId) {
        EcomSettlement s = get(id);
        if (s.getStatus() != SettlementStatus.IMPORTED) {
            throw new BizException("ecom.error.settlementConfirmed", s.getSettlementNo());
        }
        // 订单可能在导入结算单之后才写入：按当前的订单重新分摊不挂订单的费用，避免与回写到订单的费用重复
        allocationService.allocate(s);
        Map<String, Map<SettlementItemType.OrderFee, BigDecimal>> fees = new LinkedHashMap<>();
        for (Map<String, Object> fee : itemMapper.selectOrderFees(id)) {
            SettlementItemType type = SettlementItemType.valueOf((String) fee.get("itemType"));
            SettlementItemType.OrderFee col = type.orderFeeColumn();
            if (col != null) {
                fees.computeIfAbsent((String) fee.get("platformOrderNo"), k -> new java.util.EnumMap<>(SettlementItemType.OrderFee.class))
                        .merge(col, dec(fee.get("amount")), BigDecimal::add);
            }
        }
        fees.forEach((no, f) -> orderService.applySettlementFees(s.getStoreId(), no, f.get(SettlementItemType.OrderFee.COMMISSION),
                f.get(SettlementItemType.OrderFee.FULFILLMENT), f.get(SettlementItemType.OrderFee.REFUND), f.get(SettlementItemType.OrderFee.RETURN_FEE),
                f.get(SettlementItemType.OrderFee.OTHER)));
        Long finId = accountId == null ? null : receipt(s, accountId);
        StatusTransitions.change(settlementMapper, EcomSettlement::getId, id, EcomSettlement::getStatus, SettlementStatus.IMPORTED, SettlementStatus.CONFIRMED,
                w -> w.set(EcomSettlement::getFinSettlementId, finId), "ecom.error.settlementConfirmed", s.getSettlementNo());
        if (finId != null) {
            payoutService.afterSettlementConfirmed(id);
        }
    }

    /**
     * 修改不挂订单费用的分摊方式并重新分摊（已确认的也可以）
     *
     * @param method QTY 按销量 / SALES 按销售额，为空按类型默认
     * @return 分摊结果行数
     */
    @Transactional
    public int reallocate(Long id, FeeAllocMethod method) {
        EcomSettlement s = get(id);
        if (method != null && method != FeeAllocMethod.QTY && method != FeeAllocMethod.SALES) {
            throw new BizException("ecom.error.fee.allocMethodInvalid");
        }
        s.setAllocMethod(method);
        settlementMapper.update(Wrappers.<EcomSettlement>lambdaUpdate().set(EcomSettlement::getAllocMethod, method).eq(EcomSettlement::getId, id));
        return allocationService.allocate(s);
    }

    /** 结算单的费用分摊结果 */
    public List<EcomFeeAllocation> allocations(Long id) {
        get(id);
        return allocationService.list(id);
    }

    private Long receipt(EcomSettlement s, Long accountId) {
        EcomStore store = storeService.get(s.getStoreId());
        if (store.getCustomerId() == null) {
            throw new BizException("ecom.error.storeCustomerMissing", store.getName());
        }
        if (s.getNetBase().signum() <= 0) {
            throw new BizException("ecom.error.settlementNetNotPositive", s.getSettlementNo());
        }
        BigDecimal rate = Decimals.nz(s.getExchangeRate());
        BigDecimal grossBase = Decimals.money(Decimals.nz(s.getSalesAmount()).add(Decimals.nz(s.getRefundAmount())).multiply(rate));
        BigDecimal discount = grossBase.subtract(s.getNetBase()).max(BigDecimal.ZERO);
        BigDecimal remaining = s.getNetBase();
        List<FinBill> bills = billService.unsettled(BillKind.AR, store.getCustomerId()).stream()
                .filter(b -> Decimals.nz(b.getAmount()).signum() > 0)
                .sorted(Comparator.comparing(FinBill::getBillDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(FinBill::getId))
                .toList();
        if (bills.isEmpty()) {
            throw new BizException("ecom.error.noOpenReceivable", store.getName());
        }
        List<SettlementItemRequest> items = new ArrayList<>();
        for (FinBill b : bills) {
            BigDecimal open = Decimals.nz(b.getAmount()).subtract(Decimals.nz(b.getSettledAmount()));
            if (open.signum() <= 0) {
                continue;
            }
            BigDecimal amount = remaining.min(open);
            BigDecimal disc = discount.min(open.subtract(amount));
            if (amount.signum() == 0 && disc.signum() == 0) {
                break;
            }
            items.add(new SettlementItemRequest(b.getId(), amount, disc.signum() > 0 ? disc : null));
            remaining = remaining.subtract(amount);
            discount = discount.subtract(disc);
            if (remaining.signum() == 0 && discount.signum() == 0) {
                break;
            }
        }
        if (items.isEmpty()) {
            throw new BizException("ecom.error.noOpenReceivable", store.getName());
        }
        boolean toAdvance = remaining.signum() > 0;
        SettlementRequest req = new SettlementRequest(SettleKind.RECEIPT, store.getCustomerId(), LocalDate.now(), accountId, null,
                Messages.get("ecom.text.settlementReceiptRemark", store.getName(), s.getSettlementNo()), items, null,
                toAdvance ? s.getNetBase() : null, toAdvance ? Boolean.TRUE : null, null, Boolean.TRUE);
        return finSettlementService.create(req);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(v.toString());
    }

    /** 只能删除未确认的结算单 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomSettlement s = get(id);
            if (s.getStatus() != SettlementStatus.IMPORTED) {
                throw new BizException("ecom.error.settlementConfirmed", s.getSettlementNo());
            }
            payoutService.deleteBySettlement(id, s.getSettlementNo());
            allocationService.deleteBySettlement(id);
            itemMapper.delete(Wrappers.<EcomSettlementItem>lambdaQuery().eq(EcomSettlementItem::getSettlementId, id));
        }
        settlementMapper.deleteByIds(ids);
    }

    /** 结算明细类型（导入模板的下拉） */
    public List<SettlementItemType> itemTypes() {
        return List.of(SettlementItemType.values());
    }
}
