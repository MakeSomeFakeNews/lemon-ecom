package com.lemon.erp.modules.ecom.fba.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.demo.DemoTx;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomAmzReport;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemoval;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemovalItem;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.mapper.EcomAmzReportMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportStatus;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import com.lemon.erp.modules.ecom.fba.model.FbaDisposition;
import com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import com.lemon.erp.modules.ecom.fba.service.FbaClaimService;
import com.lemon.erp.modules.ecom.fba.service.FbaFeeAuditService;
import com.lemon.erp.modules.ecom.fba.service.FbaFeeCalculator;
import com.lemon.erp.modules.ecom.fba.service.FbaHealthService;
import com.lemon.erp.modules.ecom.fba.service.FbaRateService;
import com.lemon.erp.modules.ecom.fba.service.FbaRemovalService;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · FBA 运营（order 120）：基于演示店铺中的亚马逊店铺（美国 / 德国 / 日本站），经各 FBA 服务写入——
 * <ul>
 *   <li>费率用内置的美国、德国、日本站标准；平台仓配送 SKU 的尺寸重量按结算实扣配送费反推（约三分之一有多收）；</li>
 *   <li>库存健康：按头程推算库龄后，美国站上传一份库存规划报告（含超龄、冗余、滞销 SKU），德 / 日站调整部分 SKU 的库龄分段；</li>
 *   <li>移除订单：计划（草稿）、已提交、处理中、亚马逊已完成待收货、已收货（含不良品）、弃置已出库、已取消，通过移除订单报告同步；</li>
 *   <li>索赔：上传库存分类账（丢失、找到冲抵、仓库损坏）自动识别，货件对账的入库短少，退货未退回，费用核查的多收配送费；
 *       部分已提交（Case）、已驳回、已忽略，再上传赔偿报告自动匹配为已赔付；</li>
 *   <li>报告任务：以上上传记录，另加失败、已取消的任务各一条。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FbaDemoContributor implements DemoContributor {

    static final String[] STORES = {"DEMO-AMZ-US", "DEMO-AMZ-DE", "DEMO-AMZ-JP"};
    /** 各店铺的运营中心 */
    static final String[][] FCS = {{"ONT8", "LGB8", "SBD1"}, {"DTM2", "LEJ1"}, {"NRT5", "HND3"}};
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final EcomStoreService storeService;
    private final FbaHealthService healthService;
    private final FbaRemovalService removalService;
    private final FbaClaimService claimService;
    private final FbaFeeAuditService auditService;
    private final FbaRateService rateService;
    private final AmzReportService reportService;
    private final EcomAmzReportMapper reportMapper;
    private final EcomOrderMapper orderMapper;
    private final DemoTx tx;

    @Override
    public int order() {
        return 120;
    }

    @Override
    public String key() {
        return "fba";
    }

    @Override
    public void generate(DemoContext ctx) {
        Long defectWh = ctx.get(DemoKeys.DEFECT_WAREHOUSE_ID);
        for (int idx = 0; idx < STORES.length; idx++) {
            int storeIndex = idx;
            EcomStore store = storeService.findByCode(STORES[idx]);
            if (store == null) {
                continue;
            }
            List<FbaViews.DimRow> skus = auditService.dims(1, 500, store.getId(), null, null).records();
            if (skus.isEmpty()) {
                continue;
            }
            tx.run("fba-dims", () -> dims(ctx, store, skus));
            tx.run("fba-health", () -> healthService.rebuildFromInbound(store.getId()));
            health(ctx, store);
            if (storeIndex < 2) {
                removals(ctx, store, storeIndex == 0 ? "US" : "DE", defectWh);
            }
            ledger(ctx, store, skus, FCS[storeIndex]);
            receipts(ctx, store);
            tx.run("fba-returns", () -> returnClaims(ctx, store));
            tx.run("fba-fee-claims", () -> feeClaims(store));
            claimFlow(ctx, store);
        }
        tx.run("fba-report-tasks", this::extraTasks);
    }

    // ---------------------------------------------------------------- 费率与尺寸

    /**
     * 尺寸重量：先取结算实扣的每件配送费，选一组按站点费率算出来接近（约三分之一明显偏低 = 多收）的尺寸；没有结算的随机标准件尺寸
     */
    private void dims(DemoContext ctx, EcomStore store, List<FbaViews.DimRow> skus) {
        Map<String, BigDecimal> actual = new java.util.HashMap<>();
        for (FbaViews.AuditRow r : auditService.audit(store.getId(), LocalDate.now().minusDays(120), LocalDate.now(), false).rows()) {
            actual.put(r.getSellerSku(), r.getActualUnit());
        }
        List<EcomFbaSizeTier> tiers = rateService.tiers(store.getMarketplace());
        List<double[]> candidates = new ArrayList<>();
        for (double w = 0.05; w <= 6; w += 0.05) {
            double[][] boxes = {{20, 15, 1.5}, {24, 17, 1.8}, {25, 18, 6}, {30, 22, 10}, {40, 30, 15}};
            for (double[] b : boxes) {
                FbaViews.FeeQuote q = FbaFeeCalculator.quote(dims(b, w), tiers);
                if (q != null) {
                    candidates.add(new double[]{b[0], b[1], b[2], w, q.fee().doubleValue()});
                }
            }
        }
        int i = 0;
        for (FbaViews.DimRow r : skus) {
            i++;
            double[] pick;
            BigDecimal a = actual.get(r.getSellerSku());
            if (a != null && a.signum() > 0 && !candidates.isEmpty()) {
                double target = a.doubleValue() * (i % 3 == 0 ? 0.72 + ctx.random().nextDouble() * 0.12 : 1);
                pick = candidates.stream().filter(c -> c[4] <= target * 1.001).max(Comparator.comparingDouble(c -> c[4]))
                        .orElse(candidates.get(0));
            } else {
                pick = new double[]{ctx.between(15, 35), ctx.between(10, 25), ctx.between(3, 12), 0.1 + ctx.random().nextDouble() * 1.8, 0};
            }
            auditService.saveDim(new FbaRequests.DimSave(store.getId(), r.getSellerSku(), bd(pick[0], 1), bd(pick[1], 1), bd(pick[2], 1), bd(pick[3], 3)));
        }
    }

    private static FbaFeeCalculator.Dims dims(double[] box, double w) {
        return new FbaFeeCalculator.Dims(BigDecimal.valueOf(box[0]), BigDecimal.valueOf(box[1]), BigDecimal.valueOf(box[2]),
                BigDecimal.valueOf(w).setScale(3, RoundingMode.HALF_UP));
    }

    private static BigDecimal bd(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 库存健康

    /** 美国站上传库存规划报告（REPORT），其他站按头程推算后再调整部分 SKU 的库龄分段 */
    private void health(DemoContext ctx, EcomStore store) {
        // 在新事务中读：库存健康由前面的 tx.run 重建，外层事务在 MySQL 可重复读下看不到（H2 读已提交没有这个问题）
        List<FbaViews.HealthRow> rows = tx.read(() -> healthService.rows(new FbaHealthService.Query(store.getId(), null, null, null, null, null)));
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder tsv = new StringBuilder("snapshot-date\tsku\tfnsku\tasin\tproduct-name\tavailable\tinv-age-0-to-90-days\tinv-age-91-to-180-days\t"
                + "inv-age-181-to-270-days\tinv-age-271-to-365-days\tinv-age-365-plus-days\tunits-shipped-t30\testimated-storage-cost-next-month\t"
                + "currency\tinbound-quantity\ttotal-reserved-quantity\tunfulfillable-quantity\n");
        StringBuilder myi = new StringBuilder("sku\tfnsku\tasin\tproduct-name\tafn-fulfillable-quantity\tafn-inbound-working-quantity\t"
                + "afn-inbound-shipped-quantity\tafn-inbound-receiving-quantity\tafn-reserved-quantity\tafn-unsellable-quantity\tafn-researching-quantity\n");
        int i = 0;
        for (FbaViews.HealthRow r : rows) {
            i++;
            int pattern = i % 6;
            long sold = Decimals.nz(r.getSold30()).longValue();
            long available = Math.max(Decimals.nz(r.getAvailableQty()).longValue(), 20 + ctx.between(0, 60));
            // 冗余：可售远超 90 天销量；滞销：没有销量还有超龄库存
            if (pattern == 1 || pattern == 4) {
                available = Math.max(available, sold * ctx.between(5, 9) + ctx.between(80, 200));
            }
            long[] age = new long[5];
            switch (pattern) {
                case 1 -> split(age, available, 0.45, 0.25, 0.20, 0.10, 0);
                case 2 -> split(age, available, 0.40, 0.20, 0.10, 0.15, 0.15);
                case 4 -> split(age, available, 0.30, 0.30, 0.25, 0.15, 0);
                case 5 -> split(age, available, 0.70, 0.30, 0, 0, 0);
                default -> split(age, available, 1, 0, 0, 0, 0);
            }
            long reserved = ctx.between(0, 6);
            long unfulfillable = pattern == 3 ? ctx.between(2, 8) : ctx.chance(0.2) ? 1 : 0;
            long researching = pattern == 3 ? ctx.between(1, 4) : 0;
            long inbound = ctx.chance(0.35) ? ctx.between(20, 120) : 0;
            long shipped30 = pattern == 2 && sold == 0 ? 0 : Math.max(sold, ctx.between(0, 30));
            tsv.append(LocalDate.now()).append('\t').append(r.getSellerSku()).append('\t').append(nn(r.getFnsku())).append('\t').append(nn(r.getAsin()))
                    .append('\t').append(nn(r.getTitle())).append('\t').append(available);
            for (long a : age) {
                tsv.append('\t').append(a);
            }
            tsv.append('\t').append(shipped30).append('\t').append(bd(available * 0.031, 2)).append('\t').append(store.getCurrency())
                    .append('\t').append(inbound).append('\t').append(reserved).append('\t').append(unfulfillable).append('\n');
            myi.append(r.getSellerSku()).append('\t').append(nn(r.getFnsku())).append('\t').append(nn(r.getAsin())).append('\t').append(nn(r.getTitle()))
                    .append('\t').append(available).append('\t').append(inbound / 3).append('\t').append(inbound - inbound / 3 - inbound / 4).append('\t')
                    .append(inbound / 4).append('\t').append(reserved).append('\t').append(unfulfillable).append('\t').append(researching).append('\n');
        }
        String planning = tsv.toString();
        String inventory = myi.toString();
        tx.run("fba-planning", () -> reportService.upload(new FbaRequests.ReportUpload(store.getId(), AmzReportType.FBA_INVENTORY_PLANNING, planning, LocalDate.now())));
        tx.run("fba-myi", () -> reportService.upload(new FbaRequests.ReportUpload(store.getId(), AmzReportType.FBA_MYI_INVENTORY, inventory, LocalDate.now())));
    }

    private static void split(long[] age, long total, double... shares) {
        long rest = total;
        for (int i = 0; i < shares.length; i++) {
            long v = i == shares.length - 1 ? rest : Math.round(total * shares[i]);
            v = Math.min(v, rest);
            age[i] = v;
            rest -= v;
        }
        age[0] += rest;
    }

    private static String nn(String s) {
        return s == null ? "" : s.replace('\t', ' ');
    }

    // ---------------------------------------------------------------- 移除订单

    private void removals(DemoContext ctx, EcomStore store, String prefix, Long defectWh) {
        // 在新事务中读：库存健康由前面的 tx.run 重建，外层事务在 MySQL 可重复读下看不到（H2 读已提交没有这个问题）
        List<FbaViews.HealthRow> rows = tx.read(() -> healthService.rows(new FbaHealthService.Query(store.getId(), null, null, null, null, null)));
        if (rows.size() < 4) {
            return;
        }
        // 计划（草稿）：按建议移除的 SKU
        List<FbaRequests.RemovalLine> plan = rows.stream().filter(r -> r.getAdvice() == FbaHealthAdvice.REMOVE).limit(3)
                .map(r -> new FbaRequests.RemovalLine(r.getSellerSku(), FbaDisposition.SELLABLE, BigDecimal.valueOf(Math.max(5, Decimals.nz(r.getAgedQty()).longValue()))))
                .toList();
        if (!plan.isEmpty()) {
            tx.run("fba-removal-plan", () -> removalService.create(new FbaRequests.RemovalRequest(store.getId(), FbaRemovalType.RETURN, null,
                    null, null, null, LocalDate.now(), "超龄库存清理计划，待在卖家平台创建", plan))); // i18n-ignore 演示数据存库
        }
        // 已在系统中建计划并提交的移除订单（之后由报告同步发货数量）
        String submittedId = prefix + "-RMV-" + ctx.between(100000, 999999);
        String sku0 = rows.get(0).getSellerSku();
        tx.run("fba-removal-submitted", () -> removalService.create(new FbaRequests.RemovalRequest(store.getId(), FbaRemovalType.RETURN, submittedId,
                null, null, "Shenzhen Pinghu Logistics Park B3", LocalDate.now().minusDays(9), "退回国内仓重新贴标", // i18n-ignore 演示数据存库
                List.of(new FbaRequests.RemovalLine(sku0, FbaDisposition.SELLABLE, BigDecimal.valueOf(8)),
                        new FbaRequests.RemovalLine(sku0, FbaDisposition.UNSELLABLE, BigDecimal.valueOf(2))))));
        // 移除订单报告：已完成（待收货）、处理中、弃置已完成、已取消、待处理
        LocalDate t = LocalDate.now();
        String sku1 = rows.get(1).getSellerSku();
        String sku2 = rows.get(2).getSellerSku();
        String sku3 = rows.get(3).getSellerSku();
        String doneId = prefix + "-RMV-" + ctx.between(100000, 999999);
        String receivedId = prefix + "-RMV-" + ctx.between(100000, 999999);
        String disposalId = prefix + "-DSP-" + ctx.between(100000, 999999);
        String cancelledId = prefix + "-RMV-" + ctx.between(100000, 999999);
        String pendingId = prefix + "-RMV-" + ctx.between(100000, 999999);
        String fee = "0.97";
        StringBuilder tsv = new StringBuilder("request-date\torder-id\torder-type\torder-status\tlast-updated-date\tsku\tfnsku\tdisposition\t"
                + "requested-quantity\tcancelled-quantity\tdisposed-quantity\tshipped-quantity\tin-process-quantity\tremoval-fee\tcurrency\n");
        line(tsv, t.minusDays(30), doneId, "Return", "Completed", t.minusDays(6), sku1, "Sellable", 6, 0, 0, 6, 0, mul(fee, 6), store.getCurrency());
        line(tsv, t.minusDays(30), doneId, "Return", "Completed", t.minusDays(6), sku1, "Unsellable", 2, 0, 0, 2, 0, mul(fee, 2), store.getCurrency());
        line(tsv, t.minusDays(45), receivedId, "Return", "Completed", t.minusDays(20), sku2, "Sellable", 5, 0, 0, 5, 0, mul(fee, 5), store.getCurrency());
        line(tsv, t.minusDays(45), receivedId, "Return", "Completed", t.minusDays(20), sku2, "Unsellable", 1, 0, 0, 1, 0, mul(fee, 1), store.getCurrency());
        line(tsv, t.minusDays(12), submittedId, "Return", "Processing", t.minusDays(1), sku0, "Sellable", 8, 0, 0, 5, 3, mul(fee, 5), store.getCurrency());
        line(tsv, t.minusDays(12), submittedId, "Return", "Processing", t.minusDays(1), sku0, "Unsellable", 2, 0, 0, 0, 2, "0", store.getCurrency());
        line(tsv, t.minusDays(25), disposalId, "Disposal", "Completed", t.minusDays(10), sku3, "Unsellable", 4, 0, 4, 0, 0, mul(fee, 4), store.getCurrency());
        line(tsv, t.minusDays(18), cancelledId, "Return", "Cancelled", t.minusDays(16), sku2, "Sellable", 10, 10, 0, 0, 0, "0", store.getCurrency());
        line(tsv, t.minusDays(2), pendingId, "Return", "Pending", t.minusDays(2), sku3, "Sellable", 12, 0, 0, 0, 12, "0", store.getCurrency());
        String text = tsv.toString();
        tx.run("fba-removal-report", () -> reportService.upload(new FbaRequests.ReportUpload(store.getId(), AmzReportType.REMOVAL_ORDERS, text, t)));
        // 已收货：全部收货（含 1 件不良品）；处理中：先收到一部分；弃置：做报废出库
        receive(store, receivedId, true, defectWh);
        receive(store, submittedId, false, defectWh);
        tx.run("fba-removal-dispose", () -> {
            EcomFbaRemoval r = find(store, disposalId);
            if (r != null) {
                removalService.dispose(r.getId(), t.minusDays(9));
            }
        });
    }

    private void receive(EcomStore store, String orderId, boolean all, Long defectWh) {
        tx.run("fba-removal-receive", () -> {
            EcomFbaRemoval r = find(store, orderId);
            if (r == null) {
                return;
            }
            EcomFbaRemoval detail = removalService.detail(r.getId());
            List<FbaRequests.ReceiveLine> lines = new ArrayList<>();
            for (EcomFbaRemovalItem it : detail.getItems()) {
                BigDecimal shipped = Decimals.nz(it.getShippedQty());
                if (shipped.signum() <= 0) {
                    continue;
                }
                BigDecimal qty = all ? shipped : shipped.subtract(BigDecimal.ONE).max(BigDecimal.ONE);
                boolean bad = it.getDisposition() == FbaDisposition.UNSELLABLE;
                lines.add(new FbaRequests.ReceiveLine(it.getId(), bad ? BigDecimal.ZERO : qty, bad ? qty : BigDecimal.ZERO));
            }
            if (lines.isEmpty()) {
                return;
            }
            Long bad = defectWh != null ? defectWh : store.getWarehouseId();
            removalService.receive(r.getId(), new FbaRequests.RemovalReceive(LocalDate.now().minusDays(all ? 14 : 0), store.getWarehouseId(), bad,
                    "UPS", "1Z" + orderId.hashCode(), lines));
        });
    }

    private EcomFbaRemoval find(EcomStore store, String orderId) {
        return removalService.page(1, 50, store.getId(), null, null, orderId).records().stream()
                .filter(r -> orderId.equals(r.getPlatformOrderId())).findFirst().orElse(null);
    }

    private static void line(StringBuilder sb, LocalDate request, String orderId, String type, String status, LocalDate updated, String sku,
                             String disposition, int requested, int cancelled, int disposed, int shipped, int inProcess, String fee, String currency) {
        sb.append(request).append('\t').append(orderId).append('\t').append(type).append('\t').append(status).append('\t').append(updated)
                .append('\t').append(sku).append("\t\t").append(disposition).append('\t').append(requested).append('\t').append(cancelled)
                .append('\t').append(disposed).append('\t').append(shipped).append('\t').append(inProcess).append('\t').append(fee)
                .append('\t').append(currency).append('\n');
    }

    private static String mul(String unit, int qty) {
        return new BigDecimal(unit).multiply(BigDecimal.valueOf(qty)).toPlainString();
    }

    // ---------------------------------------------------------------- 库存流水与索赔

    /** 上传库存分类账：签收、发货、退货入库，以及丢失（部分后来找到）、仓库损坏；上传后自动识别丢失 / 损坏索赔 */
    private void ledger(DemoContext ctx, EcomStore store, List<FbaViews.DimRow> skus, String[] fcs) {
        StringBuilder tsv = new StringBuilder("Date\tFNSKU\tASIN\tMSKU\tTitle\tEvent Type\tReference ID\tQuantity\tFulfillment Center\tDisposition\t"
                + "Reason\tCountry\tReconciled Quantity\tUnreconciled Quantity\n");
        LocalDate t = LocalDate.now();
        int i = 0;
        for (FbaViews.DimRow r : skus) {
            i++;
            String fnsku = "X00" + Integer.toString(Math.abs(r.getSellerSku().hashCode()) % 100000000, 36).toUpperCase();
            String fc = fcs[i % fcs.length];
            ledgerLine(tsv, t.minusDays(ctx.between(40, 80)), fnsku, r.getSellerSku(), "Receipts", "FBA" + ctx.between(10000000, 99999999), ctx.between(40, 120), fc, "SELLABLE", "", null);
            for (int k = 0; k < 4; k++) {
                ledgerLine(tsv, t.minusDays(ctx.between(1, 60)), fnsku, r.getSellerSku(), "Shipments", "11" + ctx.between(1000000, 9999999) + "-" + ctx.between(1000000, 9999999),
                        -ctx.between(1, 3), fc, "SELLABLE", "", null);
            }
            if (i % 4 == 1) {
                // 丢失 2 件，后来找到 1 件 → 可索赔 1 件
                ledgerLine(tsv, t.minusDays(ctx.between(10, 40)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), -2, fc, "SELLABLE", "M", 2);
                ledgerLine(tsv, t.minusDays(ctx.between(2, 8)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), 1, fc, "SELLABLE", "F", null);
            } else if (i % 4 == 2) {
                ledgerLine(tsv, t.minusDays(ctx.between(5, 50)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), -1, fc, "SELLABLE", "E", null);
                ledgerLine(tsv, t.minusDays(ctx.between(5, 50)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), 1, fc, "DEFECTIVE", "E", null);
            } else if (i % 4 == 3) {
                ledgerLine(tsv, t.minusDays(ctx.between(5, 55)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), -ctx.between(1, 3), fc, "SELLABLE", "M", null);
                // 超过 60 天的丢失（已过索赔期，不再识别）
                ledgerLine(tsv, t.minusDays(ctx.between(70, 100)), fnsku, r.getSellerSku(), "Adjustments", "ADJ" + ctx.between(100000, 999999), -1, fc, "SELLABLE", "M", null);
            }
            if (ctx.chance(0.4)) {
                ledgerLine(tsv, t.minusDays(ctx.between(3, 30)), fnsku, r.getSellerSku(), "CustomerReturns", "11" + ctx.between(1000000, 9999999) + "-" + ctx.between(1000000, 9999999),
                        1, fc, ctx.chance(0.7) ? "SELLABLE" : "CUSTOMER_DAMAGED", "", null);
            }
        }
        String text = tsv.toString();
        tx.run("fba-ledger", () -> reportService.upload(new FbaRequests.ReportUpload(store.getId(), AmzReportType.LEDGER_DETAIL, text, t)));
    }

    private static void ledgerLine(StringBuilder sb, LocalDate date, String fnsku, String sku, String event, String ref, int qty, String fc,
                                   String disposition, String reason, Integer unreconciled) {
        sb.append(date.format(US_DATE)).append('\t').append(fnsku).append('\t').append('\t').append(sku).append('\t').append('\t').append(event)
                .append('\t').append(ref).append('\t').append(qty).append('\t').append(fc).append('\t').append(disposition).append('\t').append(reason)
                .append('\t').append('\t').append('\t').append(unreconciled == null ? "" : unreconciled).append('\n');
    }

    /** 货件对账：已签收货件按发货数登记实收，最近的两行少收 1–3 件（入库短少） */
    private void receipts(DemoContext ctx, EcomStore store) {
        PageResult<FbaViews.ReceiptRow> page = tx.read(() -> claimService.receipts(1, 200, store.getId(), null, null));
        int n = 0;
        for (FbaViews.ReceiptRow r : page.records()) {
            if (r.getStatus() != InboundStatus.RECEIVED || r.getShippedQty() == null || r.getShippedQty().signum() <= 0) {
                continue;
            }
            BigDecimal shipped = r.getShippedQty().setScale(0, RoundingMode.FLOOR);
            BigDecimal received = n < 2 ? shipped.subtract(BigDecimal.valueOf(ctx.between(1, 3))).max(BigDecimal.ZERO) : shipped;
            n++;
            tx.run("fba-receipt", () -> claimService.saveReceipt(new FbaRequests.ReceiptSave(r.getInboundItemId(), received)));
        }
        tx.run("fba-detect", () -> claimService.detect(store.getId()));
    }

    /** 退货未退回：取近期已发货的平台仓订单，按退款超过 45 天未退回登记 */
    private void returnClaims(DemoContext ctx, EcomStore store) {
        List<EcomOrder> orders = Queries.limit(orderMapper, Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, store.getId())
                .eq(EcomOrder::getFulfillment, FulfillmentType.PLATFORM).orderByAsc(EcomOrder::getOrderTime), 3);
        List<EcomFbaClaim> drafts = new ArrayList<>();
        for (EcomOrder o : orders) {
            LocalDate refund = LocalDate.now().minusDays(ctx.between(50, 90));
            BigDecimal unit = o.getTotalAmount() == null ? BigDecimal.TEN : o.getTotalAmount();
            EcomFbaClaim c = new EcomFbaClaim();
            c.setClaimType(FbaClaimType.RETURN_NOT_RECEIVED);
            c.setSourceKey("DEMO-RETURN:" + o.getId());
            c.setSourceRef(o.getPlatformOrderNo());
            c.setQty(BigDecimal.ONE);
            c.setUnitValue(Decimals.money(unit));
            c.setEstAmount(Decimals.money(unit));
            c.setCurrency(o.getCurrency());
            c.setEventDate(refund);
            c.setDeadline(refund.plusDays(120));
            c.setDetail("买家已退款，超过 45 天未退回 FBA 仓库"); // i18n-ignore 演示数据存库
            drafts.add(c);
        }
        claimService.createDetected(store.getId(), drafts);
    }

    /** 费用核查：多收最多的 3 个 SKU 生成多收配送费索赔 */
    private void feeClaims(EcomStore store) {
        FbaViews.AuditResult audit = auditService.audit(store.getId(), LocalDate.now().minusDays(120), LocalDate.now(), true);
        List<FbaRequests.AuditKey> keys = audit.rows().stream().limit(3).map(r -> new FbaRequests.AuditKey(r.getStoreId(), r.getSellerSku())).toList();
        if (!keys.isEmpty()) {
            auditService.generateClaims(new FbaRequests.AuditClaims(store.getId(), LocalDate.now().minusDays(120), LocalDate.now(), keys));
        }
    }

    /** 索赔处理：一部分已提交（Case），少量驳回、忽略；再上传赔偿报告，按 Case 编号 / SKU 自动匹配为已赔付 */
    private void claimFlow(DemoContext ctx, EcomStore store) {
        // 在新事务中读：索赔由前面的 tx.run（fba-detect 等）提交
        List<EcomFbaClaim> claims = tx.read(() -> claimService.page(1, 500, store.getId(), null, FbaClaimStatus.PENDING, null, null).records());
        List<EcomFbaClaim> submitted = new ArrayList<>();
        int i = 0;
        for (EcomFbaClaim c : claims) {
            i++;
            int p = i % 10;
            FbaClaimStatus status = p < 5 ? FbaClaimStatus.SUBMITTED : p == 5 ? FbaClaimStatus.REJECTED : p == 6 ? FbaClaimStatus.IGNORED : null;
            if (status == null) {
                continue;
            }
            String caseNo = status == FbaClaimStatus.IGNORED ? null : String.valueOf(ctx.between(1000000000, 1999999999)) + ctx.between(0, 9);
            String remark = status == FbaClaimStatus.REJECTED ? "亚马逊答复：已在运营中心找到，库存已恢复" // i18n-ignore 演示数据存库
                    : status == FbaClaimStatus.IGNORED ? "数量很小，暂不提交" : "已在卖家平台开 Case"; // i18n-ignore 演示数据存库
            boolean ok = tx.run("fba-claim-status", () -> claimService.update(c.getId(), new FbaRequests.ClaimUpdate(status, caseNo,
                    LocalDate.now().minusDays(ctx.between(3, 20)), null, null, null, null, null, remark)));
            if (ok && status == FbaClaimStatus.SUBMITTED) {
                c.setCaseNo(caseNo);
                submitted.add(c);
            }
        }
        if (submitted.isEmpty()) {
            return;
        }
        StringBuilder tsv = new StringBuilder("approval-date\treimbursement-id\tcase-id\tamazon-order-id\treason\tsku\tfnsku\tasin\tproduct-name\t"
                + "condition\tcurrency-unit\tamount-per-unit\tamount-total\tquantity-reimbursed-cash\tquantity-reimbursed-inventory\t"
                + "quantity-reimbursed-total\toriginal-reimbursement-id\toriginal-reimbursement-type\n");
        int k = 0;
        for (EcomFbaClaim c : submitted) {
            if (k++ % 3 == 2) {
                continue;
            }
            BigDecimal qty = Decimals.nz(c.getQty()).max(BigDecimal.ONE);
            BigDecimal per = Decimals.nz(c.getUnitValue()).multiply(BigDecimal.valueOf(0.85 + ctx.random().nextDouble() * 0.2)).setScale(2, RoundingMode.HALF_UP);
            tsv.append(LocalDate.now().minusDays(ctx.between(1, 6))).append('\t').append(ctx.between(100000000, 999999999)).append('\t')
                    .append(c.getCaseNo()).append('\t').append(c.getClaimType() == FbaClaimType.RETURN_NOT_RECEIVED ? nn(c.getSourceRef()) : "").append('\t')
                    .append(reason(c.getClaimType())).append('\t').append(nn(c.getSellerSku())).append('\t').append(nn(c.getFnsku())).append("\t\t\t")
                    .append("NewItem\t").append(store.getCurrency()).append('\t').append(per).append('\t').append(per.multiply(qty).setScale(2, RoundingMode.HALF_UP))
                    .append('\t').append(qty.intValue()).append("\t0\t").append(qty.intValue()).append("\t\t\n");
        }
        String text = tsv.toString();
        tx.run("fba-reimbursements", () -> reportService.upload(new FbaRequests.ReportUpload(store.getId(), AmzReportType.REIMBURSEMENTS, text, LocalDate.now())));
    }

    private static String reason(FbaClaimType type) {
        return switch (type) {
            case LOST -> "Lost_Warehouse";
            case DAMAGED -> "Damaged_Warehouse";
            case INBOUND_SHORTAGE -> "Lost_Inbound";
            case RETURN_NOT_RECEIVED -> "CustomerReturn";
            case FEE_OVERCHARGE -> "Fee_Correction";
            default -> "General_Adjustment";
        };
    }

    /** 报告任务列表中的失败、已取消示例（接口方式创建的任务） */
    private void extraTasks() {
        EcomStore us = storeService.findByCode(STORES[0]);
        if (us == null) {
            return;
        }
        EcomAmzReport failed = task(us, AmzReportType.REIMBURSEMENTS, AmzReportStatus.FAILED, "店铺「" + us.getName() + "」没有配置渠道授权，只能手工导入订单"); // i18n-ignore 演示数据存库
        reportMapper.insert(failed);
        EcomAmzReport cancelled = task(us, AmzReportType.REMOVAL_ORDERS, AmzReportStatus.CANCELLED, "亚马逊取消了报告（通常是该范围没有数据）"); // i18n-ignore 演示数据存库
        cancelled.setReportId("5" + System.identityHashCode(cancelled));
        reportMapper.insert(cancelled);
    }

    private static EcomAmzReport task(EcomStore store, AmzReportType type, AmzReportStatus status, String message) {
        EcomAmzReport r = new EcomAmzReport();
        r.setStoreId(store.getId());
        r.setReportType(type);
        r.setDataStart(LocalDate.now().minusDays(30));
        r.setDataEnd(LocalDate.now());
        r.setStatus(status);
        r.setUploaded(0);
        r.setRowCount(0);
        r.setAppliedCount(0);
        r.setPollCount(status == AmzReportStatus.CANCELLED ? 3 : 0);
        r.setMessage(message);
        r.setFinishTime(java.time.LocalDateTime.now().minusHours(2));
        return r;
    }
}
