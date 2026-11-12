package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemoval;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemovalItem;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaClaimMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaRemovalItemMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaRemovalMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaDisposition;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * FBA 移除订单：
 * <ul>
 *   <li>计划：选择 SKU、库存状态（可售 / 不可售）与数量，退回类型指定退回的本地仓（良品）和不良品仓；亚马逊没有开放创建移除订单的接口，
 *       在卖家平台按计划创建后「提交」填写移除订单号；</li>
 *   <li>同步：移除订单详情报告（GET_FBA_FULFILLMENT_REMOVAL_ORDER_DETAIL_DATA）按 店铺 + 移除订单号 更新各 SKU 的取消、已发、已弃置数量、
 *       移除费与状态（在卖家平台直接创建、系统中没有的移除订单自动建立）；</li>
 *   <li>本地仓收货（退回）：按良品 / 不良品件数从店铺平台仓调拨到本地仓 / 不良品仓（物料数量 = 件数 × 多件装），可分多次收货，
 *       收完且亚马逊已完成时为「已收货」；</li>
 *   <li>弃置出库（弃置）：按已弃置数量从平台仓做报废出库，平台仓库存与亚马逊一致；</li>
 *   <li>费用：移除 / 弃置费按报告记在移除订单上（店铺币种，按完成日期折算本位币），实际扣费在平台结算中以「移除 / 弃置费」类型计入，
 *       并由结算的费用分摊计入 SKU 成本（利润分析），这里不重复生成费用。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FbaRemovalService implements AmzReportHandler {

    private static final Set<FbaRemovalStatus> EDITABLE = EnumSet.of(FbaRemovalStatus.DRAFT, FbaRemovalStatus.SUBMITTED);
    private static final Set<FbaRemovalStatus> RECEIVABLE = EnumSet.of(FbaRemovalStatus.SUBMITTED, FbaRemovalStatus.PROCESSING, FbaRemovalStatus.COMPLETED);

    private final EcomFbaRemovalMapper removalMapper;
    private final EcomFbaRemovalItemMapper itemMapper;
    private final EcomFbaClaimMapper claimMapper;
    private final DocNoService docNoService;
    private final WmsStockDocService stockDocService;
    private final FbaSupport support;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomFbaRemoval> page(long current, long size, Long storeId, FbaRemovalType type, FbaRemovalStatus status, String keyword) {
        Page<EcomFbaRemoval> page = Page.of(current, size);
        removalMapper.selectPageList(page, storeId, type, status, keyword);
        return PageResult.of(page);
    }

    /** 各状态的移除订单数（状态标签页）：条件与列表相同、不含状态；店铺数据权限由 EcomStoreScope 自动过滤 */
    public Map<String, Long> statusCounts(Long storeId, FbaRemovalType type, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : removalMapper.countByStatus(storeId, type, null, keyword)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomFbaRemoval detail(Long id) {
        EcomFbaRemoval r = removalMapper.selectDetail(id);
        if (r == null) {
            throw new BizException("ecom.error.fba.removalNotFound");
        }
        r.setItems(itemMapper.selectByRemovalId(id));
        return r;
    }

    private EcomFbaRemoval require(Long id) {
        EcomFbaRemoval r = id == null ? null : removalMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.fba.removalNotFound");
        }
        return r;
    }

    // ---------------------------------------------------------------- 计划

    @Transactional
    public Long create(FbaRequests.RemovalRequest req) {
        EcomStore store = support.amazonStore(req.storeId());
        EcomFbaRemoval r = new EcomFbaRemoval();
        r.setDocNo(docNoService.next(FbaDocNos.REMOVAL));
        r.setStoreId(store.getId());
        fill(r, req, store);
        boolean submitted = req.platformOrderId() != null && !req.platformOrderId().isBlank();
        r.setStatus(submitted ? FbaRemovalStatus.SUBMITTED : FbaRemovalStatus.DRAFT);
        r.setRemovalFee(BigDecimal.ZERO);
        r.setFeeBase(BigDecimal.ZERO);
        r.setCurrency(store.getCurrency());
        LoginUser user = SecurityUtils.getLoginUser();
        r.setDeptId(user == null ? null : user.deptId());
        List<EcomFbaRemovalItem> items = items(store, req.items());
        r.setTotalQty(Decimals.sum(items, EcomFbaRemovalItem::getRequestedQty));
        removalMapper.insert(r);
        items.forEach(i -> {
            i.setRemovalId(r.getId());
            itemMapper.insert(i);
        });
        return r.getId();
    }

    @Transactional
    public void update(Long id, FbaRequests.RemovalRequest req) {
        EcomFbaRemoval r = require(id);
        if (!EDITABLE.contains(r.getStatus())) {
            throw new BizException("ecom.error.fba.removalNotEditable", r.getDocNo());
        }
        EcomStore store = support.amazonStore(req.storeId());
        r.setStoreId(store.getId());
        fill(r, req, store);
        if (req.platformOrderId() != null && !req.platformOrderId().isBlank()) {
            r.setStatus(FbaRemovalStatus.SUBMITTED);
        }
        List<EcomFbaRemovalItem> items = items(store, req.items());
        r.setTotalQty(Decimals.sum(items, EcomFbaRemovalItem::getRequestedQty));
        removalMapper.updateById(r);
        itemMapper.delete(Wrappers.<EcomFbaRemovalItem>lambdaQuery().eq(EcomFbaRemovalItem::getRemovalId, id));
        items.forEach(i -> {
            i.setRemovalId(id);
            itemMapper.insert(i);
        });
    }

    private void fill(EcomFbaRemoval r, FbaRequests.RemovalRequest req, EcomStore store) {
        r.setRemovalType(req.removalType());
        String orderId = req.platformOrderId() == null || req.platformOrderId().isBlank() ? null : req.platformOrderId().trim();
        if (orderId != null) {
            checkOrderId(store.getId(), orderId, r.getId());
        }
        r.setPlatformOrderId(orderId);
        if (req.removalType() == FbaRemovalType.RETURN) {
            r.setWarehouseId(req.warehouseId() != null ? req.warehouseId() : store.getWarehouseId());
            r.setDefectiveWarehouseId(req.defectiveWarehouseId());
            if (r.getWarehouseId() != null && r.getWarehouseId().equals(store.getPlatformWarehouseId())) {
                throw new BizException("ecom.error.fba.removalWarehouseInvalid");
            }
        } else {
            r.setWarehouseId(null);
            r.setDefectiveWarehouseId(null);
        }
        r.setShipAddress(req.shipAddress());
        r.setRequestDate(req.requestDate() == null ? LocalDate.now() : req.requestDate());
        r.setRemark(req.remark());
    }

    private void checkOrderId(Long storeId, String orderId, Long selfId) {
        EcomFbaRemoval dup = removalMapper.selectOne(Wrappers.<EcomFbaRemoval>lambdaQuery().eq(EcomFbaRemoval::getStoreId, storeId)
                .eq(EcomFbaRemoval::getPlatformOrderId, orderId).ne(selfId != null, EcomFbaRemoval::getId, selfId));
        if (dup != null) {
            throw new BizException("ecom.error.fba.removalOrderExists", orderId, dup.getDocNo());
        }
    }

    /** 明细：同一 SKU + 库存状态合并，按在线商品带出物料、多件装和 FNSKU */
    private List<EcomFbaRemovalItem> items(EcomStore store, List<FbaRequests.RemovalLine> lines) {
        Map<String, FbaViews.ListingValue> listings = listings(store.getId());
        Map<String, EcomFbaRemovalItem> map = new LinkedHashMap<>();
        for (FbaRequests.RemovalLine l : lines) {
            String sku = l.sellerSku().trim();
            FbaDisposition disposition = l.disposition() == null ? FbaDisposition.SELLABLE : l.disposition();
            EcomFbaRemovalItem it = map.computeIfAbsent(sku + "|" + disposition, k -> newItem(sku, disposition, listings.get(sku)));
            it.setRequestedQty(it.getRequestedQty().add(Decimals.qty(l.qty())));
        }
        return new ArrayList<>(map.values());
    }

    private Map<String, FbaViews.ListingValue> listings(Long storeId) {
        Map<String, FbaViews.ListingValue> map = new HashMap<>();
        claimMapper.selectListingValues(storeId).forEach(v -> map.put(v.getSellerSku(), v));
        return map;
    }

    private static EcomFbaRemovalItem newItem(String sku, FbaDisposition disposition, FbaViews.ListingValue v) {
        EcomFbaRemovalItem it = new EcomFbaRemovalItem();
        it.setSellerSku(sku);
        it.setDisposition(disposition);
        it.setFnsku(v == null ? null : v.getFnsku());
        it.setMaterialId(v == null ? null : v.getMaterialId());
        it.setPackQty(v == null || v.getPackQty() == null ? BigDecimal.ONE : v.getPackQty());
        it.setRequestedQty(BigDecimal.ZERO);
        it.setCancelledQty(BigDecimal.ZERO);
        it.setShippedQty(BigDecimal.ZERO);
        it.setDisposedQty(BigDecimal.ZERO);
        it.setReceivedQty(BigDecimal.ZERO);
        it.setDefectiveQty(BigDecimal.ZERO);
        it.setScrappedQty(BigDecimal.ZERO);
        it.setRemovalFee(BigDecimal.ZERO);
        return it;
    }

    /** 提交：在卖家平台创建后填写亚马逊移除订单号 */
    public void submit(Long id, FbaRequests.RemovalSubmit req) {
        EcomFbaRemoval r = require(id);
        if (!EDITABLE.contains(r.getStatus())) {
            throw new BizException("ecom.error.fba.removalNotEditable", r.getDocNo());
        }
        String orderId = req.platformOrderId().trim();
        checkOrderId(r.getStoreId(), orderId, id);
        r.setPlatformOrderId(orderId);
        r.setStatus(FbaRemovalStatus.SUBMITTED);
        removalMapper.updateById(r);
    }

    /** 取消：还没有收货 / 弃置出库的可以取消 */
    public void cancel(Long id) {
        EcomFbaRemoval r = require(id);
        if (r.getStatus() == FbaRemovalStatus.CANCELLED || r.getStatus() == FbaRemovalStatus.RECEIVED || r.getStockDocNos() != null) {
            throw new BizException("ecom.error.fba.removalNotCancellable", r.getDocNo());
        }
        r.setStatus(FbaRemovalStatus.CANCELLED);
        removalMapper.updateById(r);
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (EcomFbaRemoval r : removalMapper.selectByIds(ids)) {
            if ((r.getStatus() != FbaRemovalStatus.DRAFT && r.getStatus() != FbaRemovalStatus.CANCELLED) || r.getStockDocNos() != null) {
                throw new BizException("ecom.error.fba.removalNotRemovable", r.getDocNo());
            }
            itemMapper.delete(Wrappers.<EcomFbaRemovalItem>lambdaQuery().eq(EcomFbaRemovalItem::getRemovalId, r.getId()));
            removalMapper.deleteById(r.getId());
        }
    }

    // ---------------------------------------------------------------- 本地仓收货、弃置出库

    /** 每行可收的件数上限：亚马逊有已发数量时按已发，否则按 申请 − 取消 */
    static BigDecimal expected(EcomFbaRemovalItem it) {
        BigDecimal shipped = Decimals.nz(it.getShippedQty());
        return shipped.signum() > 0 ? shipped : Decimals.nz(it.getRequestedQty()).subtract(Decimals.nz(it.getCancelledQty())).max(BigDecimal.ZERO);
    }

    /**
     * 收货：良品从平台仓调拨到本地仓，不良品调拨到不良品仓（各生成一张已确认的调拨单），返回生成的单据号
     */
    @Transactional
    public List<String> receive(Long id, FbaRequests.RemovalReceive req) {
        EcomFbaRemoval r = removalMapper.selectForUpdate(id);
        if (r == null) {
            throw new BizException("ecom.error.fba.removalNotFound");
        }
        if (r.getRemovalType() != FbaRemovalType.RETURN) {
            throw new BizException("ecom.error.fba.removalNotReturn", r.getDocNo());
        }
        if (!RECEIVABLE.contains(r.getStatus())) {
            throw new BizException("ecom.error.fba.removalNotReceivable", r.getDocNo());
        }
        EcomStore store = support.store(r.getStoreId());
        if (store.getPlatformWarehouseId() == null) {
            throw new BizException("ecom.error.fba.platformWarehouseMissing", store.getName());
        }
        Long goodWh = req.warehouseId() != null ? req.warehouseId() : r.getWarehouseId();
        Long badWh = req.defectiveWarehouseId() != null ? req.defectiveWarehouseId() : r.getDefectiveWarehouseId();
        Map<Long, EcomFbaRemovalItem> items = new HashMap<>();
        itemMapper.selectByRemovalId(id).forEach(i -> items.put(i.getId(), i));
        List<StockDocItemRequest> good = new ArrayList<>();
        List<StockDocItemRequest> bad = new ArrayList<>();
        for (FbaRequests.ReceiveLine line : req.lines()) {
            EcomFbaRemovalItem it = items.get(line.itemId());
            if (it == null) {
                throw new BizException("ecom.error.fba.removalItemNotFound");
            }
            BigDecimal g = Decimals.qty(Decimals.nz(line.goodQty()));
            BigDecimal d = Decimals.qty(Decimals.nz(line.defectiveQty()));
            if (g.signum() < 0 || d.signum() < 0) {
                throw new BizException("ecom.error.fba.qtyNegative");
            }
            if (g.signum() == 0 && d.signum() == 0) {
                continue;
            }
            BigDecimal left = expected(it).subtract(it.getReceivedQty()).subtract(it.getDefectiveQty());
            if (g.add(d).compareTo(left) > 0) {
                throw new BizException("ecom.error.fba.receiveExceeds", it.getSellerSku(), left.stripTrailingZeros().toPlainString());
            }
            if (it.getMaterialId() == null) {
                throw new BizException("ecom.error.fba.skuUnmapped", it.getSellerSku());
            }
            String remark = it.getSellerSku();
            if (g.signum() > 0) {
                good.add(new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, g.multiply(it.getPackQty()), null, remark, null));
            }
            if (d.signum() > 0) {
                bad.add(new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, d.multiply(it.getPackQty()), null, remark, null));
            }
            it.setReceivedQty(it.getReceivedQty().add(g));
            it.setDefectiveQty(it.getDefectiveQty().add(d));
            itemMapper.updateById(it);
        }
        if (good.isEmpty() && bad.isEmpty()) {
            throw new BizException("ecom.error.fba.receiveEmpty");
        }
        if (!good.isEmpty() && goodWh == null) {
            throw new BizException("ecom.error.fba.receiveWarehouseRequired");
        }
        if (!bad.isEmpty() && badWh == null) {
            throw new BizException("ecom.error.fba.defectiveWarehouseRequired");
        }
        LocalDate date = req.receiveDate() == null ? LocalDate.now() : req.receiveDate();
        String remark = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.fba.removalReceiveRemark", r.getDocNo(),
                r.getPlatformOrderId() == null ? "" : r.getPlatformOrderId());
        List<String> docNos = new ArrayList<>();
        if (!good.isEmpty()) {
            docNos.add(transfer(store.getPlatformWarehouseId(), goodWh, date, remark, good, r.getDocNo()));
        }
        if (!bad.isEmpty()) {
            docNos.add(transfer(store.getPlatformWarehouseId(), badWh, date, remark, bad, r.getDocNo()));
        }
        boolean all = items.values().stream().allMatch(it -> it.getReceivedQty().add(it.getDefectiveQty()).compareTo(expected(it)) >= 0);
        if (all && r.getStatus() == FbaRemovalStatus.COMPLETED) {
            r.setStatus(FbaRemovalStatus.RECEIVED);
        } else if (r.getStatus() == FbaRemovalStatus.SUBMITTED) {
            r.setStatus(FbaRemovalStatus.PROCESSING);
        }
        if (req.carrier() != null && !req.carrier().isBlank()) {
            r.setCarrier(req.carrier().trim());
        }
        if (req.trackingNo() != null && !req.trackingNo().isBlank()) {
            r.setTrackingNo(req.trackingNo().trim());
        }
        r.setWarehouseId(goodWh != null ? goodWh : r.getWarehouseId());
        r.setDefectiveWarehouseId(badWh != null ? badWh : r.getDefectiveWarehouseId());
        r.setStockDocNos(appendNos(r.getStockDocNos(), docNos));
        removalMapper.updateById(r);
        return docNos;
    }

    private String transfer(Long from, Long to, LocalDate date, String remark, List<StockDocItemRequest> lines, String sourceNo) {
        if (from.equals(to)) {
            throw new BizException("ecom.error.fba.removalWarehouseInvalid");
        }
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.TRANSFER, new StockDocRequest((String) null, from, to, date, remark, lines), sourceNo);
        return doc.getDocNo();
    }

    /** 弃置出库：已弃置（亚马逊没有返回已弃置数量但已完成时按 申请 − 取消）减去已出库的部分，从平台仓报废出库 */
    @Transactional
    public String dispose(Long id, LocalDate date) {
        EcomFbaRemoval r = removalMapper.selectForUpdate(id);
        if (r == null) {
            throw new BizException("ecom.error.fba.removalNotFound");
        }
        if (r.getRemovalType() != FbaRemovalType.DISPOSAL) {
            throw new BizException("ecom.error.fba.removalNotDisposal", r.getDocNo());
        }
        if (!RECEIVABLE.contains(r.getStatus())) {
            throw new BizException("ecom.error.fba.removalNotReceivable", r.getDocNo());
        }
        EcomStore store = support.store(r.getStoreId());
        if (store.getPlatformWarehouseId() == null) {
            throw new BizException("ecom.error.fba.platformWarehouseMissing", store.getName());
        }
        List<StockDocItemRequest> lines = new ArrayList<>();
        List<EcomFbaRemovalItem> changed = new ArrayList<>();
        for (EcomFbaRemovalItem it : itemMapper.selectByRemovalId(id)) {
            BigDecimal disposed = Decimals.nz(it.getDisposedQty());
            if (disposed.signum() == 0 && r.getStatus() == FbaRemovalStatus.COMPLETED) {
                disposed = Decimals.nz(it.getRequestedQty()).subtract(Decimals.nz(it.getCancelledQty())).max(BigDecimal.ZERO);
            }
            BigDecimal qty = disposed.subtract(Decimals.nz(it.getScrappedQty()));
            if (qty.signum() <= 0) {
                continue;
            }
            if (it.getMaterialId() == null) {
                throw new BizException("ecom.error.fba.skuUnmapped", it.getSellerSku());
            }
            lines.add(new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, qty.multiply(it.getPackQty()), null, it.getSellerSku(), null));
            it.setScrappedQty(Decimals.nz(it.getScrappedQty()).add(qty));
            changed.add(it);
        }
        if (lines.isEmpty()) {
            throw new BizException("ecom.error.fba.disposeEmpty");
        }
        String remark = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.fba.removalDisposeRemark", r.getDocNo(),
                r.getPlatformOrderId() == null ? "" : r.getPlatformOrderId());
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.SCRAP, store.getPlatformWarehouseId(), null,
                date == null ? LocalDate.now() : date, remark, lines), r.getDocNo());
        changed.forEach(itemMapper::updateById);
        r.setStockDocNos(appendNos(r.getStockDocNos(), List.of(doc.getDocNo())));
        if (r.getStatus() == FbaRemovalStatus.SUBMITTED) {
            r.setStatus(FbaRemovalStatus.PROCESSING);
        }
        removalMapper.updateById(r);
        return doc.getDocNo();
    }

    private static String appendNos(String old, List<String> nos) {
        List<String> all = new ArrayList<>();
        if (old != null && !old.isBlank()) {
            all.add(old);
        }
        all.addAll(nos);
        String s = String.join(",", all);
        return s.length() <= 500 ? s : s.substring(s.length() - 500);
    }

    // ---------------------------------------------------------------- 报告同步

    @Override
    public Set<AmzReportType> types() {
        return Set.of(AmzReportType.REMOVAL_ORDERS);
    }

    @Override
    public int apply(EcomStore store, AmzReportType type, TsvReport report, LocalDate snapshotDate) {
        Map<String, List<TsvReport.Row>> orders = new LinkedHashMap<>();
        for (TsvReport.Row row : report.rows()) {
            String orderId = row.text("order-id");
            if (orderId != null && row.text("sku") != null) {
                orders.computeIfAbsent(orderId, k -> new ArrayList<>()).add(row);
            }
        }
        int count = 0;
        for (Map.Entry<String, List<TsvReport.Row>> e : orders.entrySet()) {
            Boolean ok = transactionTemplate.execute(st -> applyOrder(store, e.getKey(), e.getValue()));
            if (Boolean.TRUE.equals(ok)) {
                count++;
            }
        }
        return count;
    }

    /** 一个移除订单：没有时建立（报告同步的没有制单人），按 SKU + 库存状态更新数量与费用，再推导状态 */
    boolean applyOrder(EcomStore store, String orderId, List<TsvReport.Row> rows) {
        EcomFbaRemoval r = removalMapper.selectOne(Wrappers.<EcomFbaRemoval>lambdaQuery().eq(EcomFbaRemoval::getStoreId, store.getId())
                .eq(EcomFbaRemoval::getPlatformOrderId, orderId));
        TsvReport.Row first = rows.get(0);
        boolean isNew = r == null;
        if (isNew) {
            r = new EcomFbaRemoval();
            r.setDocNo(docNoService.next(FbaDocNos.REMOVAL));
            r.setStoreId(store.getId());
            r.setPlatformOrderId(orderId.length() <= 64 ? orderId : orderId.substring(0, 64));
            r.setRemovalType(removalType(first.text("order-type")));
            r.setWarehouseId(r.getRemovalType() == FbaRemovalType.RETURN ? store.getWarehouseId() : null);
            r.setRequestDate(first.date("request-date"));
            r.setStatus(FbaRemovalStatus.SUBMITTED);
            r.setTotalQty(BigDecimal.ZERO);
            r.setRemovalFee(BigDecimal.ZERO);
            r.setFeeBase(BigDecimal.ZERO);
            r.setCurrency(store.getCurrency());
            r.setCreateBy(null);
            removalMapper.insert(r);
        }
        Long removalId = r.getId();
        Map<String, FbaViews.ListingValue> listings = listings(store.getId());
        List<EcomFbaRemovalItem> items = itemMapper.selectByRemovalId(removalId);
        String currency = null;
        LocalDate updated = null;
        String status = null;
        for (TsvReport.Row row : rows) {
            String sku = row.text("sku");
            FbaDisposition disposition = "unsellable".equalsIgnoreCase(row.text("disposition")) ? FbaDisposition.UNSELLABLE : FbaDisposition.SELLABLE;
            EcomFbaRemovalItem it = items.stream().filter(x -> x.getSellerSku().equals(sku) && x.getDisposition() == disposition).findFirst()
                    .orElseGet(() -> items.stream().filter(x -> x.getSellerSku().equals(sku) && Decimals.nz(x.getShippedQty()).signum() == 0
                            && Decimals.nz(x.getDisposedQty()).signum() == 0).findFirst().orElse(null));
            boolean newItem = it == null;
            if (newItem) {
                it = newItem(sku, disposition, listings.get(sku));
                it.setRemovalId(removalId);
                items.add(it);
            }
            it.setDisposition(disposition);
            it.setFnsku(row.text("fnsku") != null ? row.text("fnsku") : it.getFnsku());
            BigDecimal requested = row.decimal("requested-quantity");
            if (requested != null) {
                it.setRequestedQty(requested);
            }
            it.setCancelledQty(row.qty("cancelled-quantity"));
            it.setShippedQty(row.qty("shipped-quantity"));
            it.setDisposedQty(row.qty("disposed-quantity"));
            it.setRemovalFee(Decimals.money(row.qty("removal-fee")));
            if (newItem) {
                itemMapper.insert(it);
            } else {
                itemMapper.updateById(it);
            }
            currency = row.text("currency") != null ? row.text("currency") : currency;
            LocalDate d = row.date("last-updated-date");
            updated = d != null && (updated == null || d.isAfter(updated)) ? d : updated;
            status = row.text("order-status") != null ? row.text("order-status") : status;
        }
        r.setTotalQty(Decimals.sum(items, EcomFbaRemovalItem::getRequestedQty));
        r.setRemovalFee(Decimals.money(Decimals.sum(items, EcomFbaRemovalItem::getRemovalFee)));
        if (currency != null) {
            r.setCurrency(currency.length() <= 8 ? currency : currency.substring(0, 8));
        }
        r.setStatus(status(r, status, items));
        if (r.getStatus() == FbaRemovalStatus.COMPLETED || r.getStatus() == FbaRemovalStatus.RECEIVED) {
            r.setCompleteDate(updated != null ? updated : r.getCompleteDate() != null ? r.getCompleteDate() : LocalDate.now());
        }
        r.setFeeBase(support.toBase(r.getRemovalFee(), r.getCurrency(), r.getCompleteDate() != null ? r.getCompleteDate() : updated));
        r.setLastSyncTime(LocalDateTime.now());
        removalMapper.updateById(r);
        return true;
    }

    static FbaRemovalType removalType(String orderType) {
        String t = orderType == null ? "" : orderType.toLowerCase(Locale.ROOT);
        return t.startsWith("return") ? FbaRemovalType.RETURN : FbaRemovalType.DISPOSAL;
    }

    /** 报告状态 Completed / Cancelled / Pending / Processing 等 → 系统状态；本地已收完的退回保持已收货 */
    static FbaRemovalStatus status(EcomFbaRemoval r, String reportStatus, List<EcomFbaRemovalItem> items) {
        String s = reportStatus == null ? "" : reportStatus.toLowerCase(Locale.ROOT);
        boolean receivedAll = r.getRemovalType() == FbaRemovalType.RETURN && !items.isEmpty()
                && items.stream().allMatch(it -> it.getReceivedQty().add(it.getDefectiveQty()).compareTo(expected(it)) >= 0
                && expected(it).signum() > 0);
        boolean anyLocal = items.stream().anyMatch(it -> it.getReceivedQty().add(it.getDefectiveQty()).add(Decimals.nz(it.getScrappedQty())).signum() > 0);
        if (s.startsWith("cancel")) {
            return anyLocal ? r.getStatus() : FbaRemovalStatus.CANCELLED;
        }
        if (s.startsWith("complete")) {
            return receivedAll ? FbaRemovalStatus.RECEIVED : FbaRemovalStatus.COMPLETED;
        }
        boolean moving = items.stream().anyMatch(it -> Decimals.nz(it.getShippedQty()).signum() > 0 || Decimals.nz(it.getDisposedQty()).signum() > 0);
        return moving || anyLocal ? FbaRemovalStatus.PROCESSING : FbaRemovalStatus.SUBMITTED;
    }
}
