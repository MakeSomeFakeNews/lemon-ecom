package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.entity.EcomReturnItem;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ReturnCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhReturn;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhReturnItem;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhReturnItemMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhReturnMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomOutboundSupport;
import com.lemon.erp.modules.ecom.service.EcomReturnService;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 海外仓退件：买家退货寄回海外仓。由退回仓库是海外仓的 ERP 退货退款单（退货退款、待处理）生成退件单（明细为物料数量：多件装折算、组合商品拆为组件），
 * 推送到服务商 → 定时 / 手工同步收货质检结果 → 收货完成后 ERP 退货入库（{@link EcomReturnService#receive}：销售退货、红字应收、冲减订单成本，
 * 全部进入海外仓），其中不良品再转入映射设置的不良品仓（没有设置时报废出库）。实收少于退件数量的差异在「海外仓库存」中调整。
 * 服务商接口在事务之外调用
 */
@Service
@RequiredArgsConstructor
public class EcomOwhReturnService {

    private final EcomOwhReturnMapper returnMapper;
    private final EcomOwhReturnItemMapper itemMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhSkuService skuService;
    private final EcomOwhDiffService diffService;
    private final com.lemon.erp.modules.ecom.mapper.EcomReturnItemMapper ecomReturnItemMapper;
    private final EcomOwhSupport support;
    private final EcomReturnService ecomReturnService;
    private final EcomOrderService orderService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomOwhReturn> page(long current, long size, Long accountId, Long warehouseId, OwhReturnStatus status, String keyword) {
        Page<EcomOwhReturn> page = Page.of(current, size);
        returnMapper.selectPageList(page, accountId, warehouseId, status, keyword);
        return PageResult.of(page);
    }

    public Map<String, Long> statusCounts(Long accountId, Long warehouseId, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : returnMapper.countByStatus(accountId, warehouseId, keyword)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomOwhReturn detail(Long id) {
        EcomOwhReturn r = id == null ? null : returnMapper.selectDetail(id);
        if (r == null) {
            throw new BizException("ecom.error.owh.returnNotFound");
        }
        r.setItems(itemMapper.selectByReturn(id));
        return r;
    }

    /** 可生成退件单的 ERP 退货退款单 */
    public List<Map<String, Object>> sources(String keyword) {
        return returnMapper.selectSources(StringUtils.hasText(keyword) ? keyword.trim() : null);
    }

    /** 从 ERP 退货退款单生成草稿退件单 */
    @Transactional
    public Long create(ReturnCreateRequest req) {
        EcomReturn er = ecomReturnService.detail(req.ecomReturnId());
        if (er.getReturnType() != ReturnType.RETURN_REFUND || er.getStatus() != EcomReturnStatus.PENDING) {
            throw new BizException("ecom.error.owh.returnSourceStatus", er.getDocNo());
        }
        if (returnMapper.exists(Wrappers.<EcomOwhReturn>lambdaQuery().eq(EcomOwhReturn::getEcomReturnId, er.getId())
                .ne(EcomOwhReturn::getStatus, OwhReturnStatus.CANCELLED))) {
            throw new BizException("ecom.error.owh.inboundSourceUsed");
        }
        EcomOwhWarehouse m = accountService.mappingOf(er.getWarehouseId());
        if (m == null) {
            throw new BizException("ecom.error.owh.notOverseasWarehouse");
        }
        Map<Long, BigDecimal> qty = materialQty(er);
        Map<Long, String> skus = skuService.skus(m.getAccountId(), qty.keySet());
        EcomOwhReturn r = new EcomOwhReturn();
        r.setReturnNo(docNoService.next(EcomOwhDocNos.RETURN));
        r.setAccountId(m.getAccountId());
        r.setMappingId(m.getId());
        r.setWarehouseId(m.getWarehouseId());
        r.setStoreId(er.getStoreId());
        r.setEcomReturnId(er.getId());
        r.setEcomReturnNo(er.getDocNo());
        r.setPlatformOrderNo(er.getPlatformOrderNo());
        r.setStatus(OwhReturnStatus.DRAFT);
        r.setCarrier(blank(req.carrier()));
        r.setTrackingNo(blank(req.trackingNo()));
        r.setRemark(blank(req.remark()));
        r.setTotalQty(Decimals.qty(qty.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        r.setGoodQty(BigDecimal.ZERO);
        r.setBadQty(BigDecimal.ZERO);
        r.setErpReceived(0);
        returnMapper.insert(r);
        List<EcomOwhReturnItem> items = new ArrayList<>();
        qty.forEach((mat, q) -> {
            EcomOwhReturnItem it = new EcomOwhReturnItem();
            it.setOwhReturnId(r.getId());
            it.setMaterialId(mat);
            it.setSku(skus.getOrDefault(mat, String.valueOf(mat)));
            it.setQty(Decimals.qty(q));
            it.setGoodQty(BigDecimal.ZERO);
            it.setBadQty(BigDecimal.ZERO);
            items.add(it);
        });
        itemMapper.insertBatch(items);
        return r.getId();
    }

    /** ERP 退货单各物料的退回数量：单品 = 件数 × 多件装，组合商品 = 件数 × 组件用量 */
    private Map<Long, BigDecimal> materialQty(EcomReturn er) {
        EcomOrder order = orderService.getForReturn(er.getOrderId());
        Map<Long, EcomOrderItem> orderItems = order.getItems().stream().collect(Collectors.toMap(EcomOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        for (EcomReturnItem it : er.getItems()) {
            EcomOrderItem oi = orderItems.get(it.getOrderItemId());
            if (oi == null || !EcomOutboundSupport.mapped(oi)) {
                throw new BizException("ecom.error.returnLineNotInOutbound", it.getSellerSku());
            }
            if (EcomOutboundSupport.isBundle(oi)) {
                for (EcomOrderItemPart part : oi.getParts()) {
                    out.merge(part.getMaterialId(), it.getQty().multiply(part.getUnitQty()), BigDecimal::add);
                }
            } else {
                BigDecimal pack = oi.getQty().signum() > 0 ? oi.getMaterialQty().divide(oi.getQty(), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;
                out.merge(oi.getMaterialId(), it.getQty().multiply(pack), BigDecimal::add);
            }
        }
        return out;
    }

    /**
     * 取消：草稿直接取消；已推送（服务商还没有收货）的调用服务商取消接口（事务之外），成功后取消；服务商没有取消接口时提示到服务商后台作废后「标记已作废」。
     * ERP 退货退款单仍为待处理，可以重新生成退件单或关闭
     */
    public void cancel(Long id) {
        EcomOwhReturn r = detail(id);
        if (r.getStatus() == OwhReturnStatus.DRAFT) {
            markCancelled(id, OwhReturnStatus.DRAFT, null);
            return;
        }
        checkCancellable(r);
        EcomOwhAccount a = accountService.get(r.getAccountId());
        support.connector(a.getProvider()).cancelReturn(support.context(a), r.getOwhOrderNo());
        markCancelled(id, OwhReturnStatus.PUSHED, null);
    }

    /** 标记已作废：服务商后台已作废（没有取消接口的服务商），只改本地状态 */
    public void markVoided(Long id, String reason) {
        checkCancellable(detail(id));
        markCancelled(id, OwhReturnStatus.PUSHED, StringUtils.hasText(reason) ? reason.trim() : Messages.get("ecom.text.owh.voidedRemark"));
    }

    private static void checkCancellable(EcomOwhReturn r) {
        if (r.getStatus() != OwhReturnStatus.PUSHED) {
            throw new BizException("ecom.error.owh.returnNotCancellable", r.getReturnNo());
        }
    }

    private void markCancelled(Long id, OwhReturnStatus from, String remark) {
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhReturn locked = returnMapper.selectForUpdate(id);
            if (locked == null) {
                throw new BizException("ecom.error.owh.returnNotFound");
            }
            if (locked.getStatus() != from) {
                throw new BizException(from == OwhReturnStatus.DRAFT ? "ecom.error.owh.returnNotDraft" : "ecom.error.owh.returnNotCancellable", locked.getReturnNo());
            }
            returnMapper.update(Wrappers.<EcomOwhReturn>lambdaUpdate().set(EcomOwhReturn::getStatus, OwhReturnStatus.CANCELLED)
                    .set(remark != null, EcomOwhReturn::getRemark, remark).set(EcomOwhReturn::getErrorMsg, null).eq(EcomOwhReturn::getId, id));
        });
    }

    /** 推送到服务商：成功后为已推送，失败时记录原因并抛出 */
    public void push(Long id) {
        EcomOwhReturn r = detail(id);
        if (r.getStatus() != OwhReturnStatus.DRAFT) {
            throw new BizException("ecom.error.owh.returnNotDraft", r.getReturnNo());
        }
        EcomOwhAccount a = accountService.get(r.getAccountId());
        EcomOwhWarehouse m = accountService.mapping(r.getMappingId());
        try {
            EcomReturn er = ecomReturnService.detail(r.getEcomReturnId());
            List<OwhData.Line> lines = r.getItems().stream().map(it -> new OwhData.Line(it.getSku(), it.getMaterialName(), it.getQty())).toList();
            String no = support.connector(a.getProvider()).createReturn(support.context(a), new OwhData.ReturnDraft(r.getReturnNo(), m.getOwhCode(),
                    r.getPlatformOrderNo(), r.getCarrier(), r.getTrackingNo(), er.getBuyerName(), m.getCountry(), lines, r.getRemark()));
            markPushed(id, no);
        } catch (BizException e) {
            recordError(id, e.getMessage());
            throw e;
        }
    }

    private void markPushed(Long id, String no) {
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhReturn locked = returnMapper.selectForUpdate(id);
            if (locked.getStatus() != OwhReturnStatus.DRAFT) {
                throw new BizException("ecom.error.owh.returnNotDraft", locked.getReturnNo());
            }
            returnMapper.update(Wrappers.<EcomOwhReturn>lambdaUpdate().set(EcomOwhReturn::getOwhOrderNo, no).set(EcomOwhReturn::getStatus, OwhReturnStatus.PUSHED)
                    .set(EcomOwhReturn::getPushTime, LocalDateTime.now()).set(EcomOwhReturn::getErrorMsg, null).eq(EcomOwhReturn::getId, id));
        });
    }

    /** 同步全部需要同步的退件单（定时任务，只同步开启自动同步的账号） */
    public List<SyncResult> syncAll() {
        List<SyncResult> out = new ArrayList<>();
        for (Long id : returnMapper.selectSyncingIds()) {
            EcomOwhReturn r = returnMapper.selectById(id);
            try {
                sync(id);
                out.add(new SyncResult(r.getReturnNo(), 1, null));
            } catch (BizException e) {
                recordError(id, e.getMessage());
                out.add(new SyncResult(r.getReturnNo(), 0, e.getMessage()));
            }
        }
        return out;
    }

    /** 同步一张：查询服务商的状态与良品 / 不良品数量；已收货后 ERP 退货入库（失败时记录原因，下次同步重试） */
    public OwhReturnStatus sync(Long id) {
        EcomOwhReturn r = detail(id);
        if (r.getStatus() == OwhReturnStatus.RECEIVED) {
            if (r.getErpReceived() == null || r.getErpReceived() == 0) {
                receiveErp(r);
            }
            return r.getStatus();
        }
        if (r.getStatus() != OwhReturnStatus.PUSHED || r.getOwhOrderNo() == null) {
            throw new BizException("ecom.error.owh.returnNotPushed", r.getReturnNo());
        }
        EcomOwhAccount a = accountService.get(r.getAccountId());
        OwhData.ReturnState state = support.connector(a.getProvider()).queryReturn(support.context(a), r.getOwhOrderNo());
        applyState(r, state);
        if (state.status() == OwhReturnStatus.RECEIVED) {
            receiveErp(detail(id));
        }
        return state.status();
    }

    /** 按服务商推送 / 查询到的结果更新退件单（服务商回调、补录使用）：草稿先记为已推送，收货完成后 ERP 退货入库 */
    public void applyRemote(Long id, String owhOrderNo, OwhData.ReturnState state) {
        EcomOwhReturn r = detail(id);
        if (r.getStatus() == OwhReturnStatus.DRAFT) {
            markPushed(id, owhOrderNo);
            r = detail(id);
        }
        applyState(r, state);
        if (state.status() == OwhReturnStatus.RECEIVED) {
            receiveErp(detail(id));
        }
    }

    /** 写入状态与各 SKU 的良品 / 不良品（同一 SKU 多行时记在第一行） */
    private void applyState(EcomOwhReturn r, OwhData.ReturnState state) {
        transactionTemplate.executeWithoutResult(s -> {
            Set<String> seen = new HashSet<>();
            BigDecimal good = BigDecimal.ZERO;
            BigDecimal bad = BigDecimal.ZERO;
            for (EcomOwhReturnItem it : r.getItems()) {
                boolean first = seen.add(it.getSku());
                BigDecimal g = first ? Decimals.qty(Decimals.nz(state.good().get(it.getSku()))) : BigDecimal.ZERO;
                BigDecimal b = first ? Decimals.qty(Decimals.nz(state.bad().get(it.getSku()))) : BigDecimal.ZERO;
                good = good.add(g);
                bad = bad.add(b);
                itemMapper.update(Wrappers.<EcomOwhReturnItem>lambdaUpdate().set(EcomOwhReturnItem::getGoodQty, g).set(EcomOwhReturnItem::getBadQty, b)
                        .eq(EcomOwhReturnItem::getId, it.getId()));
            }
            OwhReturnStatus st = state.status() == null ? r.getStatus() : state.status();
            LocalDateTime t = st == OwhReturnStatus.RECEIVED ? (state.receiveTime() == null ? LocalDateTime.now() : state.receiveTime()) : null;
            returnMapper.update(Wrappers.<EcomOwhReturn>lambdaUpdate().set(EcomOwhReturn::getStatus, st).set(EcomOwhReturn::getOwhStatus, state.rawStatus())
                    .set(EcomOwhReturn::getGoodQty, Decimals.qty(good)).set(EcomOwhReturn::getBadQty, Decimals.qty(bad))
                    .set(t != null, EcomOwhReturn::getReceiveTime, t).set(EcomOwhReturn::getSyncTime, LocalDateTime.now())
                    .set(EcomOwhReturn::getErrorMsg, null).eq(EcomOwhReturn::getId, r.getId()));
        });
    }

    /**
     * ERP 退货入库：退货单全部入海外仓（已入库的直接标记），不良品转入不良品仓（在途外的普通调拨，已确认）或报废出库
     */
    private void receiveErp(EcomOwhReturn r) {
        LocalDate date = r.getReceiveTime() == null ? LocalDate.now() : r.getReceiveTime().toLocalDate();
        EcomOwhSupport.asSystem(() -> transactionTemplate.executeWithoutResult(s -> {
            EcomReturn er = ecomReturnService.detail(r.getEcomReturnId());
            boolean received = true;
            if (er.getStatus() == EcomReturnStatus.PENDING) {
                // 按实收：退货单各行改为实收件数后再入库（销售退货、红字应收只按实收），差异逐物料记录
                received = adjustToActual(r, er, date);
                if (received) {
                    ecomReturnService.receive(er.getId(), date);
                }
            } else if (er.getStatus() == EcomReturnStatus.CLOSED) {
                throw new BizException("ecom.error.owh.returnSourceStatus", er.getDocNo());
            }
            String defectNo = null;
            List<StockDocItemRequest> bad = new ArrayList<>();
            for (EcomOwhReturnItem it : r.getItems()) {
                if (Decimals.nz(it.getBadQty()).signum() > 0) {
                    bad.add(new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, it.getBadQty(), null, null));
                }
            }
            if (received && !bad.isEmpty()) {
                EcomOwhWarehouse m = accountService.mapping(r.getMappingId());
                String remark = Messages.get("ecom.text.owh.returnDefect", r.getReturnNo());
                WmsStockDoc doc = m.getDefectWarehouseId() != null
                        ? stockDocService.createConfirmed(StockBizType.TRANSFER, new StockDocRequest((String) null, r.getWarehouseId(), m.getDefectWarehouseId(), date,
                        remark, bad), r.getReturnNo())
                        : stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.SCRAP, r.getWarehouseId(), null, date, remark, bad),
                        r.getReturnNo());
                defectNo = doc.getDocNo();
            }
            returnMapper.update(Wrappers.<EcomOwhReturn>lambdaUpdate().set(EcomOwhReturn::getErpReceived, 1).set(EcomOwhReturn::getDefectDocNo, defectNo)
                    .set(EcomOwhReturn::getErrorMsg, null).eq(EcomOwhReturn::getId, r.getId()));
        }));
    }

    /**
     * 退货单按实收调整（内部，事务中）：实收 = 良品 + 不良品。单品行的件数 = 该物料实收 ÷ 多件装（依次分到同一物料的各行），
     * 组合商品行的件数 = 各组件实收 ÷ 用量的最小值；件数为 0 的行删除；组合商品凑不成整套的零散组件其他入库。
     * 实收与退件数量不一致的物料记为收货差异（退件不需要差异出库）
     *
     * @return 是否还有需要入库的行（全部没有收到时不入库，退货单保持待处理）
     */
    private boolean adjustToActual(EcomOwhReturn r, EcomReturn er, LocalDate date) {
        Map<Long, BigDecimal> actual = new LinkedHashMap<>();
        Map<Long, BigDecimal> expected = new LinkedHashMap<>();
        Map<Long, String> sku = new LinkedHashMap<>();
        for (EcomOwhReturnItem it : r.getItems()) {
            actual.merge(it.getMaterialId(), Decimals.nz(it.getGoodQty()).add(Decimals.nz(it.getBadQty())), BigDecimal::add);
            expected.merge(it.getMaterialId(), it.getQty(), BigDecimal::add);
            sku.putIfAbsent(it.getMaterialId(), it.getSku());
        }
        boolean same = expected.entrySet().stream().allMatch(e -> e.getValue().compareTo(Decimals.nz(actual.get(e.getKey()))) == 0);
        if (same) {
            return true;
        }
        EcomOrder order = orderService.getForReturn(er.getOrderId());
        Map<Long, EcomOrderItem> orderItems = order.getItems().stream().collect(Collectors.toMap(EcomOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> left = new LinkedHashMap<>(actual);
        boolean any = false;
        for (EcomReturnItem it : er.getItems()) {
            EcomOrderItem oi = orderItems.get(it.getOrderItemId());
            BigDecimal units;
            if (EcomOutboundSupport.isBundle(oi)) {
                units = it.getQty();
                for (EcomOrderItemPart part : oi.getParts()) {
                    units = units.min(Decimals.nz(left.get(part.getMaterialId())).divide(part.getUnitQty(), 0, RoundingMode.DOWN));
                }
                for (EcomOrderItemPart part : oi.getParts()) {
                    left.merge(part.getMaterialId(), units.multiply(part.getUnitQty()).negate(), BigDecimal::add);
                }
            } else {
                BigDecimal pack = oi.getQty().signum() > 0 ? oi.getMaterialQty().divide(oi.getQty(), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;
                units = it.getQty().min(Decimals.nz(left.get(oi.getMaterialId())).divide(pack, 0, RoundingMode.DOWN));
                left.merge(oi.getMaterialId(), units.multiply(pack).negate(), BigDecimal::add);
            }
            if (units.signum() <= 0) {
                ecomReturnItemMapper.deleteById(it.getId());
            } else {
                any = true;
                if (units.compareTo(it.getQty()) != 0) {
                    ecomReturnItemMapper.update(Wrappers.<EcomReturnItem>lambdaUpdate().set(EcomReturnItem::getQty, Decimals.qty(units))
                            .eq(EcomReturnItem::getId, it.getId()));
                }
            }
        }
        List<StockDocItemRequest> loose = new ArrayList<>();
        if (any) {
            for (Map.Entry<Long, BigDecimal> e : left.entrySet()) {
                if (e.getValue().signum() > 0) {
                    loose.add(new StockDocItemRequest(e.getKey(), null, null, null, null, null, Decimals.qty(e.getValue()), null, null));
                }
            }
        }
        if (!loose.isEmpty()) {
            stockDocService.createConfirmed(StockBizType.IN, new StockDocRequest(com.lemon.erp.modules.wms.model.StockInType.OTHER.name(), r.getWarehouseId(), null,
                    date, Messages.get("ecom.text.owh.returnLoose", r.getReturnNo()), loose, null, "OTHER"), r.getReturnNo());
        }
        List<EcomOwhDiffService.Line> lines = new ArrayList<>();
        expected.forEach((mat, q) -> lines.add(new EcomOwhDiffService.Line(mat, sku.get(mat), q, Decimals.nz(actual.get(mat)))));
        diffService.record(new EcomOwhDiffService.Source(OwhDiffSource.RETURN, r.getId(), r.getReturnNo(), er.getDocNo(), null, r.getWarehouseId()), lines,
                date, false);
        return any;
    }

    private void recordError(Long id, String message) {
        returnMapper.update(Wrappers.<EcomOwhReturn>lambdaUpdate().set(EcomOwhReturn::getErrorMsg, EcomOwhSupport.abbreviate(message))
                .set(EcomOwhReturn::getSyncTime, LocalDateTime.now()).eq(EcomOwhReturn::getId, id));
    }

    private static String blank(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
