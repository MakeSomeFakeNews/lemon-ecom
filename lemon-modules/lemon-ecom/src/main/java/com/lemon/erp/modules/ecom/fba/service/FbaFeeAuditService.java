package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaItemDim;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaClaimMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaItemDimMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import com.lemon.erp.modules.ecom.fba.model.FbaDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * FBA 费用核查：按商品尺寸重量与配送费费率表计算每件应收的 FBA 配送费，与平台结算中实际扣的配送费（结算明细类型「配送费」，
 * 按 店铺 + 订单 + SKU 汇总后除以订单行件数；结算行没有 SKU 时按订单各 SKU 件数占比分摊）对比，每件多收超过 {@value #TOLERANCE} 的列为多收，可生成「多收配送费」索赔
 * （来源键 FEE:SKU:核查截止日期所在月，同一 SKU 每月只生成一次，截止日期为核查截止日 + {@value #CLAIM_DAYS} 天）。
 * 商品尺寸手工维护，或由预估 FBA 费用报告（GET_FBA_ESTIMATED_FBA_FEES_TXT_DATA）带入（按报告中的单位换算为 cm / kg），
 * 报告中亚马逊给出的尺寸分段与预估配送费一并保存作参考
 */
@Service
@RequiredArgsConstructor
public class FbaFeeAuditService implements AmzReportHandler {

    /** 每件差额容差（店铺币种） */
    static final String TOLERANCE = "0.05";
    static final int CLAIM_DAYS = 90;
    private static final BigDecimal TOLERANCE_VALUE = new BigDecimal(TOLERANCE);
    static final String FLAG_MISSING_DIMS = "MISSING_DIMS";
    static final String FLAG_NO_RATE = "NO_RATE";
    static final String FLAG_OVER = "OVER";
    static final String FLAG_UNDER = "UNDER";
    static final String FLAG_OK = "OK";

    private final EcomFbaItemDimMapper dimMapper;
    private final EcomFbaClaimMapper claimMapper;
    private final FbaRateService rateService;
    private final FbaClaimService claimService;
    private final FbaSupport support;
    private final TransactionTemplate transactionTemplate;

    /** 费用核查：期间默认最近 90 天 */
    public FbaViews.AuditResult audit(Long storeId, LocalDate begin, LocalDate end, Boolean overOnly) {
        LocalDate e = end == null ? LocalDate.now() : end;
        LocalDate b = begin == null ? e.minusDays(90) : begin;
        Map<String, FbaViews.AuditRow> base = new HashMap<>();
        for (FbaViews.AuditRow r : dimMapper.selectAuditBase(storeId)) {
            base.put(r.getStoreId() + "|" + r.getSellerSku(), r);
        }
        Map<String, List<EcomFbaSizeTier>> tiers = rateService.tiersByMarketplace();
        Map<String, String> claims = existingClaims(storeId, e);
        List<FbaViews.AuditRow> rows = new ArrayList<>();
        for (FbaViews.FeeActual a : dimMapper.selectFeeActuals(storeId, b, e)) {
            FbaViews.AuditRow r = base.get(a.getStoreId() + "|" + a.getSellerSku());
            if (r == null) {
                continue;
            }
            r.setOrderCount(a.getOrderCount());
            r.setQty(Decimals.qty(a.getQty()));
            r.setActualFee(Decimals.money(a.getActualFee()));
            r.setActualUnit(r.getQty().signum() > 0 ? r.getActualFee().divide(r.getQty(), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            compare(r, tiers.getOrDefault(r.getMarketplace(), List.of()));
            r.setClaimNo(claims.get(r.getStoreId() + "|" + r.getSellerSku()));
            rows.add(r);
        }
        rows.sort(Comparator.comparing((FbaViews.AuditRow r) -> Decimals.nz(r.getOvercharge())).reversed()
                .thenComparing(FbaViews.AuditRow::getStoreId).thenComparing(FbaViews.AuditRow::getSellerSku));
        int over = 0;
        int missing = 0;
        BigDecimal overBase = BigDecimal.ZERO;
        Set<String> missingRates = new TreeSet<>();
        Map<String, BigDecimal> rates = new HashMap<>();
        for (FbaViews.AuditRow r : rows) {
            if (FLAG_MISSING_DIMS.equals(r.getFlag())) {
                missing++;
            }
            if (FLAG_OVER.equals(r.getFlag())) {
                over++;
                BigDecimal rate = rates.computeIfAbsent(r.getCurrency(), c -> support.rateOrNull(c, e));
                if (rate == null) {
                    missingRates.add(r.getCurrency());
                } else {
                    overBase = overBase.add(r.getOvercharge().multiply(rate));
                }
            }
        }
        List<FbaViews.AuditRow> result = overOnly != null && overOnly ? rows.stream().filter(r -> FLAG_OVER.equals(r.getFlag())).toList() : rows;
        return new FbaViews.AuditResult(result, rows.size(), over, missing, Decimals.money(overBase), new ArrayList<>(missingRates));
    }

    /** 计算应收配送费并比较 */
    static void compare(FbaViews.AuditRow r, List<EcomFbaSizeTier> tiers) {
        FbaFeeCalculator.Dims dims = new FbaFeeCalculator.Dims(r.getLengthCm(), r.getWidthCm(), r.getHeightCm(), r.getWeightKg());
        if (!dims.complete()) {
            r.setFlag(FLAG_MISSING_DIMS);
            return;
        }
        FbaViews.FeeQuote q = FbaFeeCalculator.quote(dims, tiers);
        if (q == null) {
            r.setFlag(FLAG_NO_RATE);
            return;
        }
        r.setTierName(q.tierName());
        r.setShippingWeight(q.shippingWeight());
        r.setWeightUnit(q.weightUnit());
        r.setExpectedUnit(q.fee());
        r.setExpectedFee(Decimals.money(q.fee().multiply(r.getQty())));
        r.setDiffUnit(r.getActualUnit().subtract(q.fee()));
        if (r.getDiffUnit().compareTo(TOLERANCE_VALUE) > 0) {
            r.setFlag(FLAG_OVER);
            r.setOvercharge(Decimals.money(r.getActualFee().subtract(r.getExpectedFee()).max(BigDecimal.ZERO)));
        } else if (r.getDiffUnit().compareTo(TOLERANCE_VALUE.negate()) < 0) {
            r.setFlag(FLAG_UNDER);
            r.setOvercharge(BigDecimal.ZERO);
        } else {
            r.setFlag(FLAG_OK);
            r.setOvercharge(BigDecimal.ZERO);
        }
    }

    private Map<String, String> existingClaims(Long storeId, LocalDate end) {
        String suffix = ":" + month(end);
        Map<String, String> map = new HashMap<>();
        claimMapper.selectList(Wrappers.<EcomFbaClaim>lambdaQuery().eq(storeId != null, EcomFbaClaim::getStoreId, storeId)
                        .eq(EcomFbaClaim::getClaimType, FbaClaimType.FEE_OVERCHARGE).likeLeft(EcomFbaClaim::getSourceKey, suffix))
                .forEach(c -> map.put(c.getStoreId() + "|" + c.getSellerSku(), c.getClaimNo()));
        return map;
    }

    private static String month(LocalDate d) {
        return d.getYear() + String.format(Locale.ROOT, "%02d", d.getMonthValue());
    }

    /** 选中的多收 SKU 生成「多收配送费」索赔，返回新增条数 */
    @Transactional
    public int generateClaims(FbaRequests.AuditClaims req) {
        LocalDate e = req.endDate() == null ? LocalDate.now() : req.endDate();
        FbaViews.AuditResult audit = audit(req.storeId(), req.beginDate(), e, true);
        Map<String, FbaViews.AuditRow> over = new HashMap<>();
        audit.rows().forEach(r -> over.put(r.getStoreId() + "|" + r.getSellerSku(), r));
        Map<Long, List<EcomFbaClaim>> drafts = new HashMap<>();
        Map<Long, EcomStore> stores = support.stores(req.items().stream().map(FbaRequests.AuditKey::storeId).toList());
        for (FbaRequests.AuditKey k : req.items()) {
            FbaViews.AuditRow r = over.get(k.storeId() + "|" + k.sellerSku());
            EcomStore store = stores.get(k.storeId());
            if (r == null || store == null) {
                continue;
            }
            EcomFbaClaim c = FbaClaimService.draft(store, FbaClaimType.FEE_OVERCHARGE, "FEE:" + r.getSellerSku() + ":" + month(e), null,
                    r.getSellerSku(), null, r.getQty(), r.getDiffUnit(), e, e.plusDays(CLAIM_DAYS));
            c.setEstAmount(r.getOvercharge());
            c.setCurrency(r.getCurrency());
            c.setDetail(r.getTierName() + " " + r.getExpectedUnit().toPlainString() + " / " + r.getActualUnit().toPlainString());
            drafts.computeIfAbsent(k.storeId(), x -> new ArrayList<>()).add(c);
        }
        int n = 0;
        for (Map.Entry<Long, List<EcomFbaClaim>> d : drafts.entrySet()) {
            n += claimService.createDetected(d.getKey(), d.getValue()).size();
        }
        return n;
    }

    // ---------------------------------------------------------------- 商品尺寸

    public PageResult<FbaViews.DimRow> dims(long current, long size, Long storeId, String keyword, Boolean missingOnly) {
        Page<FbaViews.DimRow> page = Page.of(current, size);
        dimMapper.selectDimPage(page, storeId, keyword, missingOnly);
        Map<String, List<EcomFbaSizeTier>> tiers = rateService.tiersByMarketplace();
        for (FbaViews.DimRow r : page.getRecords()) {
            FbaViews.FeeQuote q = FbaFeeCalculator.quote(new FbaFeeCalculator.Dims(r.getLengthCm(), r.getWidthCm(), r.getHeightCm(), r.getWeightKg()),
                    tiers.getOrDefault(r.getMarketplace(), List.of()));
            if (q != null) {
                r.setTierName(q.tierName());
                r.setExpectedFee(q.fee());
            }
        }
        return PageResult.of(page);
    }

    /** 保存尺寸（店铺 + SKU 已有时覆盖），三边按从大到小保存 */
    public void saveDim(FbaRequests.DimSave req) {
        support.amazonStore(req.storeId());
        saveDim(req.storeId(), req.sellerSku().trim(), req.lengthCm(), req.widthCm(), req.heightCm(), req.weightKg(), FbaDataSource.MANUAL, null, null);
    }

    void saveDim(Long storeId, String sku, BigDecimal a, BigDecimal b, BigDecimal c, BigDecimal weightKg, FbaDataSource source, String tier, BigDecimal fee) {
        BigDecimal[] sides = {a, b, c};
        java.util.Arrays.sort(sides, Comparator.reverseOrder());
        EcomFbaItemDim d = dimMapper.selectOne(Wrappers.<EcomFbaItemDim>lambdaQuery().eq(EcomFbaItemDim::getStoreId, storeId).eq(EcomFbaItemDim::getSellerSku, sku));
        boolean isNew = d == null;
        if (isNew) {
            d = new EcomFbaItemDim();
            d.setStoreId(storeId);
            d.setSellerSku(sku);
        }
        d.setLengthCm(sides[0].setScale(2, RoundingMode.HALF_UP));
        d.setWidthCm(sides[1].setScale(2, RoundingMode.HALF_UP));
        d.setHeightCm(sides[2].setScale(2, RoundingMode.HALF_UP));
        d.setWeightKg(weightKg.setScale(3, RoundingMode.HALF_UP));
        d.setSource(source);
        if (source == FbaDataSource.REPORT) {
            d.setPlatformTier(tier);
            d.setPlatformFee(fee);
        }
        if (isNew) {
            dimMapper.insert(d);
        } else {
            dimMapper.updateById(d);
        }
    }

    public void deleteDims(List<Long> ids) {
        dimMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 预估 FBA 费用报告

    @Override
    public Set<AmzReportType> types() {
        return Set.of(AmzReportType.ESTIMATED_FEES);
    }

    @Override
    public int apply(EcomStore store, AmzReportType type, TsvReport report, LocalDate snapshotDate) {
        Integer n = transactionTemplate.execute(st -> {
            int count = 0;
            for (TsvReport.Row row : report.rows()) {
                String sku = row.text("sku", "seller-sku");
                BigDecimal l = row.decimal("longest-side");
                BigDecimal m = row.decimal("median-side");
                BigDecimal s = row.decimal("shortest-side");
                BigDecimal w = row.decimal("item-package-weight");
                if (sku == null || l == null || m == null || s == null || w == null || l.signum() <= 0 || w.signum() <= 0) {
                    continue;
                }
                boolean inches = !String.valueOf(row.text("unit-of-dimension")).toLowerCase(Locale.ROOT).startsWith("c");
                boolean pounds = !String.valueOf(row.text("unit-of-weight")).toLowerCase(Locale.ROOT).startsWith("k");
                BigDecimal factor = inches ? FbaFeeCalculator.CM_PER_INCH : BigDecimal.ONE;
                BigDecimal kg = pounds ? w.divide(FbaFeeCalculator.LB_PER_KG, 4, RoundingMode.HALF_UP) : w;
                String tier = row.text("product-size-tier");
                saveDim(store.getId(), sku, l.multiply(factor), m.multiply(factor), s.multiply(factor), kg, FbaDataSource.REPORT,
                        tier == null || tier.length() <= 64 ? tier : tier.substring(0, 64),
                        row.decimal("expected-fulfillment-fee-per-unit", "expected-domestic-fulfilment-fee-per-unit"));
                count++;
            }
            return count;
        });
        return n == null ? 0 : n;
    }
}
