package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.AmazonReportsClient;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaInboundReceipt;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaLedger;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaReimbursement;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaClaimMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaInboundReceiptMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaLedgerMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaReimbursementMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import com.lemon.erp.modules.ecom.fba.model.FbaDataSource;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * FBA 索赔：
 * <ul>
 *   <li><b>库存流水</b>：库存分类账明细报告（GET_LEDGER_DETAIL_VIEW_DATA）按行摘要去重写入 {@code ecom_fba_ledger}；</li>
 *   <li><b>自动识别</b>（{@link #detect}）：① 仓库丢失：调整原因 M / 5 的减少，先用同一 FNSKU 找到（F）的数量按时间从最早的丢失开始冲抵，
 *       有未核对数量时取未核对数量；② 仓库损坏：调整原因 E / 6 / 7 的减少；③ 入库短少：已签收货件的发货件数大于「货件对账」中亚马逊实收件数；
 *       ④ 退货未退回：平台配送订单退货退款已退款超过 45 天，库存流水中没有该订单的退货入库、也没有赔偿；
 *       同一来源（{@code source_key}）只生成一次，已过截止日期的不再生成。截止日期：丢失 / 损坏为发生后 60 天，入库短少为签收后 60 天，
 *       退货未退回为退款后 120 天；估算单价 = 在线商品售价 × (1 − 店铺佣金率)，退货按退款金额 ÷ 件数；</li>
 *   <li><b>赔偿匹配</b>：赔偿报告（GET_FBA_REIMBURSEMENTS_DATA）按 店铺 + 赔偿编号 + SKU 去重写入，未匹配的依次按 Case 编号、订单号（退货未退回）、
 *       原因对应的类型 + SKU / FNSKU（最早发生的待提交 / 已提交索赔）匹配，匹配后索赔记为已赔付并累加赔付金额（本位币按批准日期的汇率）；
 *       赔付金额已在平台结算中以「赔偿」类型计入收入，这里只做索赔跟踪，不重复生成收入凭证；</li>
 *   <li><b>货件对账</b>：头程货件明细的亚马逊实收件数，手工填写或调用 FBA Inbound API 同步。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FbaClaimService implements AmzReportHandler {

    static final Set<String> LOST_REASONS = Set.of("M", "5");
    static final Set<String> FOUND_REASONS = Set.of("F");
    static final Set<String> DAMAGED_REASONS = Set.of("E", "6", "7");
    /** 丢失 / 损坏的索赔期限（天） */
    static final int WAREHOUSE_CLAIM_DAYS = 60;
    /** 入库短少的索赔期限（签收后，天） */
    static final int INBOUND_CLAIM_DAYS = 60;
    /** 退款后多少天仍未退回才可索赔 */
    static final int RETURN_WAIT_DAYS = 45;
    /** 退货未退回的索赔期限（退款后，天） */
    static final int RETURN_CLAIM_DAYS = 120;
    /** 流水识别回看的月数 */
    static final int LEDGER_LOOKBACK_MONTHS = 18;
    /** 「即将到期」的天数 */
    static final int EXPIRING_DAYS = 7;
    private static final Set<FbaClaimStatus> OPEN = EnumSet.of(FbaClaimStatus.PENDING, FbaClaimStatus.SUBMITTED);
    private static final Set<FbaClaimStatus> REMOVABLE = EnumSet.of(FbaClaimStatus.PENDING, FbaClaimStatus.IGNORED, FbaClaimStatus.REJECTED);

    private final EcomFbaClaimMapper claimMapper;
    private final EcomFbaLedgerMapper ledgerMapper;
    private final EcomFbaReimbursementMapper reimbursementMapper;
    private final EcomFbaInboundReceiptMapper receiptMapper;
    private final DocNoService docNoService;
    private final FbaSupport support;
    private final AmazonReportsClient client;
    private final EcomSyncService syncService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomFbaClaim> page(long current, long size, Long storeId, FbaClaimType type, FbaClaimStatus status, String keyword,
                                         Integer expiringDays) {
        Page<EcomFbaClaim> page = Page.of(current, size);
        LocalDate before = expiringDays == null ? null : LocalDate.now().plusDays(expiringDays);
        claimMapper.selectPageList(page, storeId, type, status, keyword, before);
        LocalDate today = LocalDate.now();
        page.getRecords().forEach(c -> c.setDaysLeft(c.getDeadline() == null ? null : (int) java.time.temporal.ChronoUnit.DAYS.between(today, c.getDeadline())));
        return PageResult.of(page);
    }

    public EcomFbaClaim get(Long id) {
        EcomFbaClaim c = id == null ? null : claimMapper.selectById(id);
        if (c == null) {
            throw new BizException("ecom.error.fba.claimNotFound");
        }
        return c;
    }

    /** 按状态、类型汇总（本位币），可索赔金额 = 待提交 + 已提交的估算金额 */
    public FbaViews.ClaimSummary summary(Long storeId) {
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> rates = new HashMap<>();
        Set<String> missing = new TreeSet<>();
        Map<String, Map<String, Object>> byStatus = new LinkedHashMap<>();
        for (FbaClaimStatus s : FbaClaimStatus.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", s.name());
            m.put("cnt", 0L);
            m.put("amountBase", BigDecimal.ZERO);
            m.put("paidBase", BigDecimal.ZERO);
            byStatus.put(s.name(), m);
        }
        BigDecimal claimable = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (Map<String, Object> row : claimMapper.selectSummary(storeId)) {
            String status = String.valueOf(value(row, "status"));
            Map<String, Object> m = byStatus.get(status);
            if (m == null) {
                continue;
            }
            BigDecimal amountBase = base(decimal(value(row, "amount")), (String) value(row, "currency"), today, rates, missing);
            BigDecimal paidBase = decimal(value(row, "paid_base"));
            m.put("cnt", ((Number) m.get("cnt")).longValue() + ((Number) value(row, "cnt")).longValue());
            m.put("amountBase", ((BigDecimal) m.get("amountBase")).add(amountBase));
            m.put("paidBase", ((BigDecimal) m.get("paidBase")).add(paidBase));
            if (OPEN.contains(FbaClaimStatus.valueOf(status))) {
                claimable = claimable.add(amountBase);
            }
            paid = paid.add(paidBase);
        }
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (Map<String, Object> row : claimMapper.selectTypeSummary(storeId)) {
            String type = String.valueOf(value(row, "claim_type"));
            Map<String, Object> m = byType.computeIfAbsent(type, k -> {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("claimType", k);
                x.put("cnt", 0L);
                x.put("amountBase", BigDecimal.ZERO);
                return x;
            });
            m.put("cnt", ((Number) m.get("cnt")).longValue() + ((Number) value(row, "cnt")).longValue());
            m.put("amountBase", ((BigDecimal) m.get("amountBase")).add(base(decimal(value(row, "amount")), (String) value(row, "currency"), today, rates, missing)));
        }
        long expiring = claimMapper.selectCount(Wrappers.<EcomFbaClaim>lambdaQuery().eq(storeId != null, EcomFbaClaim::getStoreId, storeId)
                .in(EcomFbaClaim::getStatus, OPEN).le(EcomFbaClaim::getDeadline, today.plusDays(EXPIRING_DAYS)).ge(EcomFbaClaim::getDeadline, today));
        return new FbaViews.ClaimSummary(new ArrayList<>(byStatus.values()), new ArrayList<>(byType.values()), Decimals.money(claimable),
                Decimals.money(paid), (int) expiring, new ArrayList<>(missing));
    }

    private BigDecimal base(BigDecimal amount, String currency, LocalDate date, Map<String, BigDecimal> rates, Set<String> missing) {
        if (amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = currency == null ? null : rates.computeIfAbsent(currency, c -> support.rateOrNull(c, date));
        if (rate == null) {
            if (currency != null) {
                missing.add(currency);
            }
            return BigDecimal.ZERO;
        }
        return Decimals.money(amount.multiply(rate));
    }

    /** 汇总结果的列名在 MySQL / H2 中大小写可能不同，按不区分大小写取值 */
    private static Object value(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v != null) {
            return v;
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static BigDecimal decimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }

    // ---------------------------------------------------------------- 新增、处理、删除

    /** 手工登记索赔 */
    public Long create(FbaRequests.ClaimCreate req) {
        EcomStore store = support.amazonStore(req.storeId());
        EcomFbaClaim c = new EcomFbaClaim();
        c.setStoreId(store.getId());
        c.setClaimType(req.claimType());
        c.setSourceKey("MANUAL:" + java.util.UUID.randomUUID());
        c.setSourceRef(req.sourceRef());
        c.setSellerSku(req.sellerSku());
        c.setQty(Decimals.qty(req.qty()));
        c.setUnitValue(Decimals.money(Decimals.nz(req.unitValue())));
        c.setEstAmount(req.estAmount() != null ? Decimals.money(req.estAmount()) : Decimals.money(c.getQty().multiply(c.getUnitValue())));
        c.setCurrency(store.getCurrency());
        c.setEventDate(req.eventDate() == null ? LocalDate.now() : req.eventDate());
        c.setDeadline(req.deadline());
        c.setRemark(req.remark());
        insert(c);
        return c.getId();
    }

    /** 改状态、Case 编号、估算与备注；手工登记已赔付时填写赔付金额 */
    @Transactional
    public void update(Long id, FbaRequests.ClaimUpdate req) {
        EcomFbaClaim c = get(id);
        if (req.status() == FbaClaimStatus.SUBMITTED && (req.caseNo() == null || req.caseNo().isBlank()) && c.getCaseNo() == null) {
            throw new BizException("ecom.error.fba.caseNoRequired");
        }
        c.setStatus(req.status());
        c.setCaseNo(req.caseNo() == null || req.caseNo().isBlank() ? c.getCaseNo() : req.caseNo().trim());
        if (req.status() == FbaClaimStatus.SUBMITTED || c.getCaseNo() != null) {
            c.setSubmitDate(req.submitDate() != null ? req.submitDate() : c.getSubmitDate() != null ? c.getSubmitDate() : LocalDate.now());
        }
        if (req.qty() != null) {
            c.setQty(Decimals.qty(req.qty()));
        }
        if (req.unitValue() != null) {
            c.setUnitValue(Decimals.money(req.unitValue()));
        }
        if (req.qty() != null || req.unitValue() != null) {
            c.setEstAmount(Decimals.money(c.getQty().multiply(c.getUnitValue())));
        }
        if (req.deadline() != null) {
            c.setDeadline(req.deadline());
        }
        if (req.status() == FbaClaimStatus.PAID && req.paidAmount() != null) {
            c.setPaidAmount(Decimals.money(req.paidAmount()));
            c.setPaidDate(req.paidDate() == null ? LocalDate.now() : req.paidDate());
            c.setPaidBase(support.toBase(c.getPaidAmount(), c.getCurrency(), c.getPaidDate()));
            if (Decimals.nz(c.getPaidQty()).signum() == 0) {
                c.setPaidQty(c.getQty());
            }
        } else if (req.status() == FbaClaimStatus.PAID && Decimals.nz(c.getPaidAmount()).signum() == 0) {
            throw new BizException("ecom.error.fba.paidAmountRequired");
        }
        c.setRemark(req.remark());
        claimMapper.updateById(c);
    }

    public void delete(List<Long> ids) {
        for (EcomFbaClaim c : claimMapper.selectByIds(ids)) {
            if (!REMOVABLE.contains(c.getStatus()) || Decimals.nz(c.getPaidAmount()).signum() != 0) {
                throw new BizException("ecom.error.fba.claimNotRemovable", c.getClaimNo());
            }
        }
        claimMapper.deleteByIds(ids);
    }

    private void insert(EcomFbaClaim c) {
        c.setClaimNo(docNoService.next(FbaDocNos.CLAIM));
        if (c.getStatus() == null) {
            c.setStatus(FbaClaimStatus.PENDING);
        }
        c.setPaidAmount(Decimals.nz(c.getPaidAmount()));
        c.setPaidQty(Decimals.nz(c.getPaidQty()));
        c.setPaidBase(Decimals.nz(c.getPaidBase()));
        claimMapper.insert(c);
    }

    /**
     * 写入识别出的索赔（来源键已存在的跳过），返回新增的索赔；费用核查、演示数据也通过它生成
     */
    public List<EcomFbaClaim> createDetected(Long storeId, List<EcomFbaClaim> drafts) {
        if (drafts.isEmpty()) {
            return List.of();
        }
        Set<String> keys = drafts.stream().map(EcomFbaClaim::getSourceKey).collect(Collectors.toSet());
        Set<String> existing = new HashSet<>(claimMapper.selectExistingKeys(storeId, keys));
        List<EcomFbaClaim> created = new ArrayList<>();
        for (EcomFbaClaim c : drafts) {
            if (existing.add(c.getSourceKey())) {
                c.setStoreId(storeId);
                insert(c);
                created.add(c);
            }
        }
        return created;
    }

    // ---------------------------------------------------------------- 自动识别

    /** 识别可索赔项：店铺为空时识别全部亚马逊店铺（每个店铺一个事务） */
    public FbaViews.DetectResult detect(Long storeId) {
        List<EcomStore> stores = storeId == null ? support.amazonStores() : List.of(support.amazonStore(storeId));
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FbaClaimType t : FbaClaimType.values()) {
            counts.put(t.name(), 0);
        }
        for (EcomStore s : stores) {
            List<EcomFbaClaim> created = transactionTemplate.execute(st -> detectStore(s, LocalDate.now()));
            if (created != null) {
                created.forEach(c -> counts.merge(c.getClaimType().name(), 1, Integer::sum));
            }
        }
        return new FbaViews.DetectResult(counts, counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    List<EcomFbaClaim> detectStore(EcomStore store, LocalDate today) {
        Map<String, FbaViews.ListingValue> values = new HashMap<>();
        Map<String, FbaViews.ListingValue> byFnsku = new HashMap<>();
        for (FbaViews.ListingValue v : claimMapper.selectListingValues(store.getId())) {
            values.put(v.getSellerSku(), v);
            if (v.getFnsku() != null) {
                byFnsku.put(v.getFnsku(), v);
            }
        }
        List<EcomFbaClaim> drafts = new ArrayList<>();
        drafts.addAll(warehouseClaims(store, today, values, byFnsku));
        drafts.addAll(shortageClaims(store, today));
        drafts.addAll(returnClaims(store, today));
        return createDetected(store.getId(), drafts);
    }

    /** 丢失（扣除后来找到的）与损坏 */
    private List<EcomFbaClaim> warehouseClaims(EcomStore store, LocalDate today, Map<String, FbaViews.ListingValue> values,
                                               Map<String, FbaViews.ListingValue> byFnsku) {
        Set<String> reasons = new HashSet<>(LOST_REASONS);
        reasons.addAll(FOUND_REASONS);
        reasons.addAll(DAMAGED_REASONS);
        List<EcomFbaLedger> rows = ledgerMapper.selectAdjustments(store.getId(), today.minusMonths(LEDGER_LOOKBACK_MONTHS), reasons);
        Map<String, BigDecimal> foundPool = new HashMap<>();
        for (EcomFbaLedger g : rows) {
            if (FOUND_REASONS.contains(g.getReason()) && Decimals.positive(g.getQuantity())) {
                foundPool.merge(skuKey(g), g.getQuantity(), BigDecimal::add);
            }
        }
        List<EcomFbaClaim> drafts = new ArrayList<>();
        for (EcomFbaLedger g : rows) {
            if (g.getQuantity() == null || g.getQuantity().signum() >= 0) {
                continue;
            }
            boolean lost = LOST_REASONS.contains(g.getReason());
            boolean damaged = DAMAGED_REASONS.contains(g.getReason());
            if (!lost && !damaged) {
                continue;
            }
            BigDecimal qty = g.getQuantity().abs();
            if (lost && g.getUnreconciledQty() != null) {
                qty = qty.min(g.getUnreconciledQty().abs());
            }
            if (lost) {
                BigDecimal pool = foundPool.getOrDefault(skuKey(g), BigDecimal.ZERO);
                BigDecimal offset = pool.min(qty);
                foundPool.put(skuKey(g), pool.subtract(offset));
                qty = qty.subtract(offset);
            }
            LocalDate deadline = g.getEventDate().plusDays(WAREHOUSE_CLAIM_DAYS);
            if (qty.signum() <= 0 || deadline.isBefore(today)) {
                continue;
            }
            FbaViews.ListingValue v = g.getSellerSku() != null && values.containsKey(g.getSellerSku()) ? values.get(g.getSellerSku()) : byFnsku.get(g.getFnsku());
            EcomFbaClaim c = draft(store, lost ? FbaClaimType.LOST : FbaClaimType.DAMAGED, "LEDGER:" + g.getRowKey(), g.getReferenceId(),
                    g.getSellerSku() != null ? g.getSellerSku() : v == null ? null : v.getSellerSku(), g.getFnsku(), qty, unitValue(v),
                    g.getEventDate(), deadline);
            c.setDetail(abbreviate(g.getEventType() + " " + g.getReason() + " " + g.getQuantity().stripTrailingZeros().toPlainString()
                    + (g.getFulfillmentCenter() == null ? "" : " @" + g.getFulfillmentCenter())));
            drafts.add(c);
        }
        return drafts;
    }

    private static String skuKey(EcomFbaLedger g) {
        return g.getFnsku() != null ? g.getFnsku() : String.valueOf(g.getSellerSku());
    }

    /** 入库短少：已签收、有实收记录且发货件数大于实收件数 */
    private List<EcomFbaClaim> shortageClaims(EcomStore store, LocalDate today) {
        List<EcomFbaClaim> drafts = new ArrayList<>();
        for (FbaViews.ReceiptRow r : claimMapper.selectShortages(store.getId())) {
            LocalDate base = r.getReceiveDate() != null ? r.getReceiveDate() : r.getShipDate() != null ? r.getShipDate() : today;
            LocalDate deadline = base.plusDays(INBOUND_CLAIM_DAYS);
            if (deadline.isBefore(today) || !Decimals.positive(r.getShortQty())) {
                continue;
            }
            BigDecimal unit = estimate(r.getPrice(), r.getCommissionRate());
            EcomFbaClaim c = draft(store, FbaClaimType.INBOUND_SHORTAGE, "INBOUND:" + r.getInboundItemId(),
                    r.getShipmentId() != null ? r.getShipmentId() : r.getDocNo(), r.getSellerSku(), null,
                    r.getShortQty().setScale(0, RoundingMode.FLOOR).max(BigDecimal.ONE), unit, base, deadline);
            c.setDetail(abbreviate(r.getDocNo() + " " + Decimals.qty(r.getShippedQty()).stripTrailingZeros().toPlainString() + " → "
                    + Decimals.qty(r.getReceivedQty()).stripTrailingZeros().toPlainString()));
            drafts.add(c);
        }
        return drafts;
    }

    /** 退货未退回：退款超过 45 天、120 天内，流水中没有该订单的退货入库，也没有赔偿 */
    private List<EcomFbaClaim> returnClaims(EcomStore store, LocalDate today) {
        LocalDateTime before = today.minusDays(RETURN_WAIT_DAYS).atTime(23, 59, 59);
        LocalDateTime after = today.minusDays(RETURN_CLAIM_DAYS).atStartOfDay();
        List<EcomFbaClaim> drafts = new ArrayList<>();
        for (FbaViews.UnreturnedRefund r : claimMapper.selectUnreturnedRefunds(store.getId(), before, after)) {
            BigDecimal qty = Decimals.nz(r.getQty());
            if (qty.signum() <= 0) {
                continue;
            }
            BigDecimal unit = Decimals.nz(r.getRefundAmount()).divide(qty, 2, RoundingMode.HALF_UP);
            EcomFbaClaim c = draft(store, FbaClaimType.RETURN_NOT_RECEIVED, "RETURN:" + r.getReturnId() + ":" + r.getSellerSku(),
                    r.getPlatformOrderNo(), r.getSellerSku(), null, qty, unit, r.getRefundDate(), r.getRefundDate().plusDays(RETURN_CLAIM_DAYS));
            c.setCurrency(r.getCurrency() != null ? r.getCurrency() : store.getCurrency());
            c.setDetail(abbreviate(r.getDocNo()));
            drafts.add(c);
        }
        return drafts;
    }

    static EcomFbaClaim draft(EcomStore store, FbaClaimType type, String sourceKey, String sourceRef, String sku, String fnsku, BigDecimal qty,
                              BigDecimal unit, LocalDate eventDate, LocalDate deadline) {
        EcomFbaClaim c = new EcomFbaClaim();
        c.setStoreId(store.getId());
        c.setClaimType(type);
        c.setSourceKey(sourceKey.length() <= 128 ? sourceKey : sourceKey.substring(0, 128));
        c.setSourceRef(sourceRef == null || sourceRef.length() <= 64 ? sourceRef : sourceRef.substring(0, 64));
        c.setSellerSku(sku);
        c.setFnsku(fnsku);
        c.setQty(Decimals.qty(qty));
        c.setUnitValue(Decimals.money(Decimals.nz(unit)));
        c.setEstAmount(Decimals.money(c.getQty().multiply(c.getUnitValue())));
        c.setCurrency(store.getCurrency());
        c.setEventDate(eventDate);
        c.setDeadline(deadline);
        c.setStatus(FbaClaimStatus.PENDING);
        return c;
    }

    private static BigDecimal unitValue(FbaViews.ListingValue v) {
        return v == null ? BigDecimal.ZERO : estimate(v.getPrice(), v.getCommissionRate());
    }

    /** 估算单价 = 售价 × (1 − 佣金率) */
    static BigDecimal estimate(BigDecimal price, BigDecimal commissionRate) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = Decimals.nz(commissionRate);
        return price.multiply(Decimals.HUNDRED.subtract(rate)).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }

    // ---------------------------------------------------------------- 报告：库存流水、赔偿

    @Override
    public Set<AmzReportType> types() {
        return Set.of(AmzReportType.LEDGER_DETAIL, AmzReportType.REIMBURSEMENTS);
    }

    @Override
    public int apply(EcomStore store, AmzReportType type, TsvReport report, LocalDate snapshotDate) {
        Integer n = transactionTemplate.execute(st -> type == AmzReportType.LEDGER_DETAIL ? applyLedger(store, report) : applyReimbursements(store, report));
        if (type == AmzReportType.LEDGER_DETAIL) {
            transactionTemplate.execute(st -> detectStore(store, LocalDate.now()));
        } else {
            transactionTemplate.execute(st -> matchReimbursements(store.getId()));
        }
        return n == null ? 0 : n;
    }

    /** 库存流水按行摘要去重，单条多值 INSERT 写入，返回新增条数 */
    int applyLedger(EcomStore store, TsvReport report) {
        Map<String, EcomFbaLedger> rows = new LinkedHashMap<>();
        for (TsvReport.Row row : report.rows()) {
            LocalDate date = row.date("date", "date-and-time");
            String eventType = row.text("event-type");
            if (date == null || eventType == null) {
                continue;
            }
            EcomFbaLedger g = new EcomFbaLedger();
            g.setStoreId(store.getId());
            g.setEventDate(date);
            g.setFnsku(row.text("fnsku"));
            g.setAsin(row.text("asin"));
            g.setSellerSku(row.text("msku", "sku"));
            g.setEventType(cut(eventType, 32));
            g.setReferenceId(cut(row.text("reference-id"), 64));
            g.setQuantity(row.qty("quantity"));
            g.setFulfillmentCenter(cut(row.text("fulfillment-center"), 16));
            g.setDisposition(cut(row.text("disposition"), 32));
            g.setReason(cut(row.text("reason"), 16));
            g.setUnreconciledQty(row.decimal("unreconciled-quantity"));
            g.setRowKey(rowKey(store.getId(), row));
            rows.putIfAbsent(g.getRowKey(), g);
        }
        if (rows.isEmpty()) {
            return 0;
        }
        List<EcomFbaLedger> fresh = new ArrayList<>();
        List<String> keys = new ArrayList<>(rows.keySet());
        for (int i = 0; i < keys.size(); i += 500) {
            List<String> part = keys.subList(i, Math.min(keys.size(), i + 500));
            Set<String> existing = new HashSet<>(ledgerMapper.selectExistingKeys(store.getId(), part));
            part.stream().filter(k -> !existing.contains(k)).forEach(k -> fresh.add(rows.get(k)));
        }
        for (int i = 0; i < fresh.size(); i += 500) {
            ledgerMapper.insertBatch(fresh.subList(i, Math.min(fresh.size(), i + 500)));
        }
        return fresh.size();
    }

    /** 行摘要：店铺 + 规范化后的全部列（按列名排序），SHA-256 前 40 位 */
    static String rowKey(Long storeId, TsvReport.Row row) {
        StringBuilder sb = new StringBuilder(String.valueOf(storeId));
        new TreeSet<>(row.values().keySet()).forEach(k -> sb.append('|').append(k).append('=').append(row.values().get(k)));
        return ConnectorSigns.sha256Hex(sb.toString()).substring(0, 40);
    }

    /** 赔偿记录按 店铺 + 赔偿编号 + SKU 去重写入，返回新增条数 */
    int applyReimbursements(EcomStore store, TsvReport report) {
        int count = 0;
        for (TsvReport.Row row : report.rows()) {
            String id = row.text("reimbursement-id");
            if (id == null) {
                continue;
            }
            String sku = row.text("sku", "msku");
            String skuKey = sku == null ? "" : cut(sku, 64);
            EcomFbaReimbursement old = reimbursementMapper.selectOne(Wrappers.<EcomFbaReimbursement>lambdaQuery()
                    .eq(EcomFbaReimbursement::getStoreId, store.getId()).eq(EcomFbaReimbursement::getReimbursementId, id)
                    .eq(EcomFbaReimbursement::getSellerSku, skuKey));
            if (old != null) {
                continue;
            }
            EcomFbaReimbursement r = new EcomFbaReimbursement();
            r.setStoreId(store.getId());
            r.setReimbursementId(cut(id, 64));
            r.setApprovalDate(row.date("approval-date"));
            r.setCaseId(cut(row.text("case-id"), 64));
            r.setAmazonOrderId(cut(row.text("amazon-order-id"), 64));
            r.setReason(cut(row.text("reason"), 64));
            r.setSellerSku(skuKey);
            r.setFnsku(cut(row.text("fnsku"), 32));
            r.setAsin(cut(row.text("asin"), 32));
            r.setCurrency(cut(row.text("currency-unit", "currency"), 8));
            if (r.getCurrency() == null) {
                r.setCurrency(store.getCurrency());
            }
            r.setAmountPerUnit(row.decimal("amount-per-unit"));
            r.setAmountTotal(Decimals.money(row.qty("amount-total")));
            r.setQtyCash(row.qty("quantity-reimbursed-cash"));
            r.setQtyInventory(row.qty("quantity-reimbursed-inventory"));
            r.setAmountBase(support.toBase(r.getAmountTotal(), r.getCurrency(), r.getApprovalDate()));
            reimbursementMapper.insert(r);
            count++;
        }
        return count;
    }

    /** 未匹配的赔偿依次按 Case 编号、订单号、原因 + SKU 匹配索赔，返回匹配条数 */
    public int matchReimbursements(Long storeId) {
        List<EcomFbaReimbursement> list = reimbursementMapper.selectList(Wrappers.<EcomFbaReimbursement>lambdaQuery()
                .eq(storeId != null, EcomFbaReimbursement::getStoreId, storeId).isNull(EcomFbaReimbursement::getClaimId)
                .orderByAsc(EcomFbaReimbursement::getApprovalDate).orderByAsc(EcomFbaReimbursement::getId));
        int matched = 0;
        for (EcomFbaReimbursement r : list) {
            EcomFbaClaim c = findClaim(r);
            if (c == null) {
                continue;
            }
            c.setPaidAmount(Decimals.nz(c.getPaidAmount()).add(Decimals.nz(r.getAmountTotal())));
            c.setPaidQty(Decimals.nz(c.getPaidQty()).add(Decimals.nz(r.getQtyCash())).add(Decimals.nz(r.getQtyInventory())));
            c.setPaidBase(Decimals.nz(c.getPaidBase()).add(Decimals.nz(r.getAmountBase())));
            c.setPaidDate(r.getApprovalDate() == null ? LocalDate.now() : r.getApprovalDate());
            c.setStatus(FbaClaimStatus.PAID);
            if (c.getCaseNo() == null && r.getCaseId() != null) {
                c.setCaseNo(r.getCaseId());
            }
            claimMapper.updateById(c);
            r.setClaimId(c.getId());
            reimbursementMapper.updateById(r);
            matched++;
        }
        return matched;
    }

    private EcomFbaClaim findClaim(EcomFbaReimbursement r) {
        if (r.getCaseId() != null) {
            List<EcomFbaClaim> byCase = claimMapper.selectList(Wrappers.<EcomFbaClaim>lambdaQuery().eq(EcomFbaClaim::getStoreId, r.getStoreId())
                    .eq(EcomFbaClaim::getCaseNo, r.getCaseId()).ne(EcomFbaClaim::getStatus, FbaClaimStatus.IGNORED).orderByAsc(EcomFbaClaim::getId));
            if (!byCase.isEmpty()) {
                return byCase.get(0);
            }
        }
        if (r.getAmazonOrderId() != null) {
            List<EcomFbaClaim> byOrder = claimMapper.selectList(Wrappers.<EcomFbaClaim>lambdaQuery().eq(EcomFbaClaim::getStoreId, r.getStoreId())
                    .eq(EcomFbaClaim::getSourceRef, r.getAmazonOrderId()).in(EcomFbaClaim::getStatus, OPEN).orderByAsc(EcomFbaClaim::getId));
            if (!byOrder.isEmpty()) {
                return byOrder.get(0);
            }
        }
        FbaClaimType type = typeOf(r.getReason());
        if (type == null || (isBlank(r.getSellerSku()) && isBlank(r.getFnsku()))) {
            return null;
        }
        return claimMapper.selectList(Wrappers.<EcomFbaClaim>lambdaQuery().eq(EcomFbaClaim::getStoreId, r.getStoreId())
                        .eq(EcomFbaClaim::getClaimType, type).in(EcomFbaClaim::getStatus, OPEN)
                        .and(w -> w.eq(!isBlank(r.getSellerSku()), EcomFbaClaim::getSellerSku, r.getSellerSku()).or(!isBlank(r.getFnsku()))
                                .eq(!isBlank(r.getFnsku()), EcomFbaClaim::getFnsku, r.getFnsku())))
                .stream().min(Comparator.comparing((EcomFbaClaim c) -> c.getEventDate() == null ? LocalDate.MAX : c.getEventDate())
                        .thenComparing(EcomFbaClaim::getId))
                .orElse(null);
    }

    /** 赔偿原因 → 索赔类型（Lost_Warehouse、Damaged_Warehouse、Lost_Inbound、CustomerReturn、CustomerServiceIssue、Fee_Correction） */
    static FbaClaimType typeOf(String reason) {
        if (reason == null) {
            return null;
        }
        String r = reason.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (r.startsWith("lostinbound") || r.startsWith("damagedinbound") || r.contains("inbound")) {
            return FbaClaimType.INBOUND_SHORTAGE;
        }
        if (r.startsWith("lost")) {
            return FbaClaimType.LOST;
        }
        if (r.startsWith("damaged")) {
            return FbaClaimType.DAMAGED;
        }
        if (r.startsWith("customerreturn") || r.startsWith("customerserviceissue") || r.startsWith("refund")) {
            return FbaClaimType.RETURN_NOT_RECEIVED;
        }
        if (r.startsWith("fee")) {
            return FbaClaimType.FEE_OVERCHARGE;
        }
        return null;
    }

    public PageResult<EcomFbaLedger> ledger(long current, long size, Long storeId, String eventType, String keyword, LocalDate begin, LocalDate end) {
        Page<EcomFbaLedger> page = Page.of(current, size);
        ledgerMapper.selectPageList(page, storeId, eventType, keyword, begin, end);
        return PageResult.of(page);
    }

    public PageResult<EcomFbaReimbursement> reimbursements(long current, long size, Long storeId, Boolean matched, String keyword) {
        Page<EcomFbaReimbursement> page = Page.of(current, size);
        reimbursementMapper.selectPageList(page, storeId, matched, keyword);
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 货件对账

    public PageResult<FbaViews.ReceiptRow> receipts(long current, long size, Long storeId, String keyword, Boolean shortOnly) {
        Page<FbaViews.ReceiptRow> page = Page.of(current, size);
        claimMapper.selectReceiptPage(page, storeId, keyword, shortOnly);
        return PageResult.of(page);
    }

    /** 填写亚马逊实收件数 */
    public void saveReceipt(FbaRequests.ReceiptSave req) {
        FbaViews.ReceiptRow item = receiptItem(req.inboundItemId());
        upsertReceipt(item, req.receivedQty(), FbaDataSource.MANUAL);
    }

    private FbaViews.ReceiptRow receiptItem(Long inboundItemId) {
        FbaViews.ReceiptRow item = receiptMapper.selectInboundItem(inboundItemId);
        if (item == null) {
            throw new BizException("ecom.error.fba.inboundItemNotFound");
        }
        return item;
    }

    private void upsertReceipt(FbaViews.ReceiptRow item, BigDecimal receivedQty, FbaDataSource source) {
        EcomFbaInboundReceipt r = receiptMapper.selectOne(Wrappers.<EcomFbaInboundReceipt>lambdaQuery()
                .eq(EcomFbaInboundReceipt::getInboundItemId, item.getInboundItemId()));
        boolean isNew = r == null;
        if (isNew) {
            r = new EcomFbaInboundReceipt();
            r.setInboundItemId(item.getInboundItemId());
            r.setInboundId(item.getInboundId());
        }
        r.setShippedQty(Decimals.qty(Decimals.nz(item.getShippedQty())));
        r.setReceivedQty(Decimals.qty(receivedQty));
        r.setSource(source);
        if (isNew) {
            receiptMapper.insert(r);
        } else {
            receiptMapper.updateById(r);
        }
    }

    /** 调用 FBA Inbound API 同步货件的实收件数（按平台 SKU 对应），返回更新的明细数 */
    public int syncReceipts(Long inboundId) {
        List<FbaViews.ReceiptRow> items = receiptMapper.selectInboundItems(inboundId);
        if (items.isEmpty()) {
            throw new BizException("ecom.error.fba.inboundItemNotFound");
        }
        FbaViews.ReceiptRow head = items.get(0);
        if (isBlank(head.getShipmentId())) {
            throw new BizException("ecom.error.fba.shipmentIdMissing", head.getDocNo());
        }
        EcomStore store = support.amazonStore(head.getStoreId());
        ConnectorContext ctx = syncService.requireContext(store);
        Map<String, BigDecimal> received = new HashMap<>();
        for (AmazonReportsClient.ShipmentItem it : client.shipmentItems(ctx, head.getShipmentId())) {
            if (it.sellerSku() != null) {
                received.merge(it.sellerSku(), it.receivedQty(), BigDecimal::add);
            }
        }
        Integer n = transactionTemplate.execute(st -> {
            int count = 0;
            for (FbaViews.ReceiptRow item : items) {
                BigDecimal qty = item.getSellerSku() == null ? null : received.get(item.getSellerSku());
                if (qty != null) {
                    upsertReceipt(item, qty, FbaDataSource.API);
                    count++;
                }
            }
            return count;
        });
        return n == null ? 0 : n;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
