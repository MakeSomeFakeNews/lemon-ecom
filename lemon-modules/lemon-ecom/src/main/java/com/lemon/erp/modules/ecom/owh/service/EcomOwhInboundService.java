package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomInboundBox;
import com.lemon.erp.modules.ecom.entity.EcomInboundBoxItem;
import com.lemon.erp.modules.ecom.entity.EcomInboundItem;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.BoxRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundUpdateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.LineRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInboundBox;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInboundItem;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundBoxMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundItemMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhStockMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * 海外仓入库单（头程到海外仓的 ASN）：从调入仓是海外仓的头程货件或在途调拨单生成（装箱取货件的装箱信息，没有时全部放在第 1 箱），
 * 草稿可修改头程信息与装箱 → 推送到服务商 → 定时 / 手工同步收货数量与状态 → 收货完成后 ERP 签收来源单据（货件签收或调拨收货，
 * 在途转为海外仓可用）。实收少于发货数量时 ERP 仍按整单签收，差异在「海外仓库存」中按海外仓数量调整。
 * 服务商接口在事务之外调用
 */
@Service
@RequiredArgsConstructor
public class EcomOwhInboundService {

    /** 需要同步的状态 */
    private static final Set<OwhInboundStatus> SYNCING = EnumSet.of(OwhInboundStatus.PUSHED, OwhInboundStatus.RECEIVING);
    private static final Set<InboundStatus> SOURCE_OPEN = EnumSet.of(InboundStatus.DRAFT, InboundStatus.APPROVING, InboundStatus.SHIPPED);
    private static final String TRANSFER_KIND = "TRANSFER";
    private static final String TRANSIT = "TRANSIT";
    private static final String DONE = "DONE";

    private final EcomOwhInboundMapper inboundMapper;
    private final EcomOwhInboundBoxMapper boxMapper;
    private final EcomOwhInboundItemMapper itemMapper;
    private final EcomOwhStockMapper stockMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhSupport support;
    private final EcomOwhSkuService skuService;
    private final EcomOwhDiffService diffService;
    private final com.lemon.erp.modules.ecom.mapper.EcomInboundItemMapper ecomInboundItemMapper;
    private final EcomInboundService ecomInboundService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /** 入库单 ERP 已签收（调拨单据此更新为已收货） */
    public record InboundReceivedEvent(Long inboundId) {
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomOwhInbound> page(long current, long size, Long accountId, Long warehouseId, OwhInboundStatus status, String keyword) {
        Page<EcomOwhInbound> page = Page.of(current, size);
        inboundMapper.selectPageList(page, accountId, warehouseId, status, keyword);
        return PageResult.of(page);
    }

    public Map<String, Long> statusCounts(Long accountId, Long warehouseId, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : inboundMapper.countByStatus(accountId, warehouseId, keyword)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomOwhInbound detail(Long id) {
        EcomOwhInbound i = id == null ? null : inboundMapper.selectDetail(id);
        if (i == null) {
            throw new BizException("ecom.error.owh.inboundNotFound");
        }
        i.setBoxes(boxMapper.selectList(Wrappers.<EcomOwhInboundBox>lambdaQuery().eq(EcomOwhInboundBox::getInboundId, id)
                .orderByAsc(EcomOwhInboundBox::getBoxNo)));
        i.setItems(itemMapper.selectByInbound(id));
        return i;
    }

    /** 可生成入库单的来源（调入仓是海外仓的头程货件、在途调拨单） */
    public List<Map<String, Object>> sources(String keyword) {
        return inboundMapper.selectSources(StringUtils.hasText(keyword) ? keyword.trim() : null);
    }

    // ---------------------------------------------------------------- 生成与修改

    /** 从头程货件 / 在途调拨单生成草稿入库单 */
    @Transactional
    public Long create(InboundCreateRequest req) {
        return createFrom(req, false);
    }

    /**
     * 生成草稿入库单（内部）
     *
     * @param allowReceived 允许已签收的头程货件（补录历史入库单，如迁移、演示数据）
     */
    public Long createFrom(InboundCreateRequest req, boolean allowReceived) {
        if (inboundMapper.exists(Wrappers.<EcomOwhInbound>lambdaQuery().eq(EcomOwhInbound::getSourceType, req.sourceType())
                .eq(EcomOwhInbound::getSourceId, req.sourceId()).ne(EcomOwhInbound::getStatus, OwhInboundStatus.CANCELLED))) {
            throw new BizException("ecom.error.owh.inboundSourceUsed");
        }
        EcomOwhInbound i = new EcomOwhInbound();
        i.setSourceType(req.sourceType());
        i.setSourceId(req.sourceId());
        // 箱号 → 物料 → 数量
        TreeMap<Integer, Map<Long, BigDecimal>> lines = new TreeMap<>();
        List<EcomOwhInboundBox> boxes = new ArrayList<>();
        Long warehouseId;
        if (req.sourceType() == OwhInboundSource.INBOUND) {
            EcomInbound ib = ecomInboundService.detail(req.sourceId());
            if (!SOURCE_OPEN.contains(ib.getStatus()) && !(allowReceived && ib.getStatus() == InboundStatus.RECEIVED)) {
                throw new BizException("ecom.error.owh.inboundSourceStatus", ib.getDocNo());
            }
            warehouseId = ib.getToWarehouseId();
            i.setSourceNo(ib.getDocNo());
            i.setCarrier(ib.getCarrier());
            i.setTrackingNo(ib.getTrackingNo());
            i.setEtaDate(ib.getEtaDate());
            fromInbound(ib, lines, boxes);
        } else {
            Map<String, Object> doc = inboundMapper.selectTransfer(req.sourceId());
            if (doc == null || !TRANSFER_KIND.equals(doc.get("kind")) || !(TRANSIT.equals(doc.get("status")) || allowReceived && DONE.equals(doc.get("status")))) {
                throw new BizException("ecom.error.owh.transferNotTransit", doc == null ? "-" : doc.get("docNo"));
            }
            warehouseId = ((Number) doc.get("toWarehouseId")).longValue();
            i.setSourceNo(String.valueOf(doc.get("docNo")));
            Map<Long, BigDecimal> all = new LinkedHashMap<>();
            for (Map<String, Object> row : inboundMapper.selectTransferItems(req.sourceId())) {
                all.merge(((Number) row.get("materialId")).longValue(), dec(row.get("qty")), BigDecimal::add);
            }
            lines.put(1, all);
        }
        EcomOwhWarehouse m = accountService.mappingOf(warehouseId);
        if (m == null) {
            throw new BizException("ecom.error.owh.notOverseasWarehouse");
        }
        i.setAccountId(m.getAccountId());
        i.setMappingId(m.getId());
        i.setWarehouseId(warehouseId);
        i.setInboundNo(docNoService.next(EcomOwhDocNos.INBOUND));
        i.setStatus(OwhInboundStatus.DRAFT);
        i.setErpReceived(0);
        i.setReceivedQty(BigDecimal.ZERO);
        inboundMapper.insert(i);
        saveLines(i, lines, boxes);
        return i.getId();
    }

    /** 头程货件的装箱：按箱内平台 SKU 件数折算为物料数量（多件装、组合商品按货件明细的 数量 ÷ SKU 件数）；没有装箱或折算不一致时全部放在第 1 箱 */
    private static void fromInbound(EcomInbound ib, TreeMap<Integer, Map<Long, BigDecimal>> lines, List<EcomOwhInboundBox> boxes) {
        Map<Long, BigDecimal> total = new LinkedHashMap<>();
        for (EcomInboundItem it : ib.getItems()) {
            total.merge(it.getMaterialId(), it.getQty(), BigDecimal::add);
        }
        if (ib.getBoxes() != null && !ib.getBoxes().isEmpty()) {
            TreeMap<Integer, Map<Long, BigDecimal>> boxed = new TreeMap<>();
            Map<Long, BigDecimal> check = new HashMap<>();
            for (EcomInboundBox b : ib.getBoxes()) {
                Map<Long, BigDecimal> inBox = boxed.computeIfAbsent(b.getBoxNo(), k -> new LinkedHashMap<>());
                for (EcomInboundBoxItem bi : b.getItems() == null ? List.<EcomInboundBoxItem>of() : b.getItems()) {
                    for (EcomInboundItem it : ib.getItems()) {
                        String sku = StringUtils.hasText(it.getSellerSku()) ? it.getSellerSku() : it.getMaterialCode();
                        if (!Objects.equals(sku, bi.getSellerSku())) {
                            continue;
                        }
                        BigDecimal skuQty = it.getSkuQty() == null || it.getSkuQty().signum() == 0 ? it.getQty() : it.getSkuQty();
                        BigDecimal q = Decimals.qty(bi.getQty().multiply(it.getQty()).divide(skuQty, 6, RoundingMode.HALF_UP));
                        inBox.merge(it.getMaterialId(), q, BigDecimal::add);
                        check.merge(it.getMaterialId(), q, BigDecimal::add);
                    }
                }
            }
            boolean same = check.size() == total.size() && total.entrySet().stream().allMatch(e -> e.getValue().compareTo(Decimals.nz(check.get(e.getKey()))) == 0);
            if (same) {
                boxed.values().removeIf(Map::isEmpty);
                lines.putAll(boxed);
                for (EcomInboundBox b : ib.getBoxes()) {
                    if (boxed.containsKey(b.getBoxNo())) {
                        EcomOwhInboundBox x = new EcomOwhInboundBox();
                        x.setBoxNo(b.getBoxNo());
                        x.setLength(b.getLength());
                        x.setWidth(b.getWidth());
                        x.setHeight(b.getHeight());
                        x.setWeight(b.getWeight());
                        boxes.add(x);
                    }
                }
                return;
            }
        }
        lines.put(1, total);
    }

    /** 写入箱子与明细（先删后插），更新箱数、数量合计 */
    private void saveLines(EcomOwhInbound i, TreeMap<Integer, Map<Long, BigDecimal>> lines, List<EcomOwhInboundBox> boxSpecs) {
        boxMapper.delete(Wrappers.<EcomOwhInboundBox>lambdaQuery().eq(EcomOwhInboundBox::getInboundId, i.getId()));
        itemMapper.delete(Wrappers.<EcomOwhInboundItem>lambdaQuery().eq(EcomOwhInboundItem::getInboundId, i.getId()));
        Set<Long> materialIds = new HashSet<>();
        lines.values().forEach(m -> materialIds.addAll(m.keySet()));
        Map<Long, String> codes = skuService.skus(i.getAccountId(), materialIds);
        Map<Integer, EcomOwhInboundBox> specs = new HashMap<>();
        boxSpecs.forEach(b -> specs.put(b.getBoxNo(), b));
        List<EcomOwhInboundItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Map<Long, BigDecimal>> e : lines.entrySet()) {
            EcomOwhInboundBox spec = specs.get(e.getKey());
            EcomOwhInboundBox b = new EcomOwhInboundBox();
            b.setInboundId(i.getId());
            b.setBoxNo(e.getKey());
            b.setBoxMark(i.getInboundNo() + "-" + String.format("%03d", e.getKey()));
            if (spec != null) {
                b.setLength(spec.getLength());
                b.setWidth(spec.getWidth());
                b.setHeight(spec.getHeight());
                b.setWeight(spec.getWeight());
            }
            boxMapper.insert(b);
            for (Map.Entry<Long, BigDecimal> l : e.getValue().entrySet()) {
                EcomOwhInboundItem it = new EcomOwhInboundItem();
                it.setInboundId(i.getId());
                it.setBoxNo(e.getKey());
                it.setMaterialId(l.getKey());
                it.setSku(codes.getOrDefault(l.getKey(), String.valueOf(l.getKey())));
                it.setQty(Decimals.qty(l.getValue()));
                it.setReceivedQty(BigDecimal.ZERO);
                items.add(it);
                total = total.add(l.getValue());
            }
        }
        if (!items.isEmpty()) {
            itemMapper.insertBatch(items);
        }
        inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getBoxCount, lines.size())
                .set(EcomOwhInbound::getTotalQty, Decimals.qty(total)).eq(EcomOwhInbound::getId, i.getId()));
    }

    /** 修改草稿：头程信息与装箱；各物料的数量合计须与原来一致（即与来源单据一致） */
    @Transactional
    public void update(Long id, InboundUpdateRequest req) {
        EcomOwhInbound i = inboundMapper.selectForUpdate(id);
        if (i == null) {
            throw new BizException("ecom.error.owh.inboundNotFound");
        }
        if (i.getStatus() != OwhInboundStatus.DRAFT) {
            throw new BizException("ecom.error.owh.inboundNotDraft", i.getInboundNo());
        }
        Map<Long, BigDecimal> before = new HashMap<>();
        itemMapper.selectByInbound(id).forEach(it -> before.merge(it.getMaterialId(), it.getQty(), BigDecimal::add));
        Set<Integer> boxNos = new HashSet<>();
        List<EcomOwhInboundBox> specs = new ArrayList<>();
        for (BoxRequest b : req.boxes()) {
            if (!boxNos.add(b.boxNo())) {
                throw new BizException("ecom.error.owh.boxDuplicate", b.boxNo());
            }
            EcomOwhInboundBox x = new EcomOwhInboundBox();
            x.setBoxNo(b.boxNo());
            x.setLength(b.length());
            x.setWidth(b.width());
            x.setHeight(b.height());
            x.setWeight(b.weight());
            specs.add(x);
        }
        TreeMap<Integer, Map<Long, BigDecimal>> lines = new TreeMap<>();
        Map<Long, BigDecimal> after = new HashMap<>();
        for (LineRequest l : req.lines()) {
            if (!boxNos.contains(l.boxNo())) {
                throw new BizException("ecom.error.owh.boxMissing", l.boxNo());
            }
            lines.computeIfAbsent(l.boxNo(), k -> new LinkedHashMap<>()).merge(l.materialId(), l.qty(), BigDecimal::add);
            after.merge(l.materialId(), l.qty(), BigDecimal::add);
        }
        boolean same = before.size() == after.size()
                && before.entrySet().stream().allMatch(e -> e.getValue().compareTo(Decimals.nz(after.get(e.getKey()))) == 0);
        if (!same) {
            throw new BizException("ecom.error.owh.boxQtyMismatch");
        }
        inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getTransportMode, blank(req.transportMode()))
                .set(EcomOwhInbound::getCarrier, blank(req.carrier())).set(EcomOwhInbound::getTrackingNo, blank(req.trackingNo()))
                .set(EcomOwhInbound::getEtaDate, req.etaDate()).set(EcomOwhInbound::getRemark, blank(req.remark()))
                .eq(EcomOwhInbound::getId, id));
        saveLines(i, lines, specs);
    }

    /** 取消：只有草稿可以取消 */
    @Transactional
    public void cancelDraft(Long id) {
        EcomOwhInbound i = inboundMapper.selectForUpdate(id);
        if (i == null) {
            throw new BizException("ecom.error.owh.inboundNotFound");
        }
        if (i.getStatus() != OwhInboundStatus.DRAFT) {
            throw new BizException("ecom.error.owh.inboundNotDraft", i.getInboundNo());
        }
        inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getStatus, OwhInboundStatus.CANCELLED).eq(EcomOwhInbound::getId, id));
    }

    /**
     * 取消：草稿直接取消；已推送（服务商还没有收货）的调用服务商取消接口（事务之外），成功后取消。服务商没有取消接口时提示到服务商后台作废后「标记已作废」。
     * 来源单据在 ERP 中仍是在途，可以重新生成入库单
     */
    public void cancel(Long id) {
        EcomOwhInbound i = detail(id);
        if (i.getStatus() == OwhInboundStatus.DRAFT) {
            cancelDraft(id);
            return;
        }
        checkCancellable(i);
        EcomOwhAccount a = accountService.get(i.getAccountId());
        support.connector(a.getProvider()).cancelInbound(support.context(a), i.getOwhOrderNo());
        markCancelled(id, null);
    }

    /** 标记已作废：服务商后台已作废（没有取消接口的服务商），只改本地状态 */
    public void markVoided(Long id, String reason) {
        checkCancellable(detail(id));
        markCancelled(id, StringUtils.hasText(reason) ? reason.trim() : Messages.get("ecom.text.owh.voidedRemark"));
    }

    /** 已推送且服务商还没有收货的入库单才能取消 / 作废 */
    private static void checkCancellable(EcomOwhInbound i) {
        if (i.getStatus() != OwhInboundStatus.PUSHED || Decimals.nz(i.getReceivedQty()).signum() > 0) {
            throw new BizException("ecom.error.owh.inboundNotCancellable", i.getInboundNo());
        }
    }

    private void markCancelled(Long id, String remark) {
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhInbound locked = inboundMapper.selectForUpdate(id);
            checkCancellable(locked);
            inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getStatus, OwhInboundStatus.CANCELLED)
                    .set(remark != null, EcomOwhInbound::getRemark, remark).set(EcomOwhInbound::getErrorMsg, null).eq(EcomOwhInbound::getId, id));
        });
    }

    // ---------------------------------------------------------------- 推送与同步

    /** 推送到服务商：成功后为已推送，失败时记录原因并抛出 */
    public void push(Long id) {
        EcomOwhInbound i = detail(id);
        if (i.getStatus() != OwhInboundStatus.DRAFT) {
            throw new BizException("ecom.error.owh.inboundNotDraft", i.getInboundNo());
        }
        EcomOwhAccount a = accountService.get(i.getAccountId());
        try {
            Map<Integer, List<OwhData.Line>> byBox = new TreeMap<>();
            for (EcomOwhInboundItem it : i.getItems()) {
                byBox.computeIfAbsent(it.getBoxNo(), k -> new ArrayList<>()).add(new OwhData.Line(it.getSku(), it.getMaterialName(), it.getQty()));
            }
            List<OwhData.Box> boxes = new ArrayList<>();
            for (EcomOwhInboundBox b : i.getBoxes()) {
                boxes.add(new OwhData.Box(b.getBoxNo(), b.getBoxMark(), b.getLength(), b.getWidth(), b.getHeight(), b.getWeight(),
                        byBox.getOrDefault(b.getBoxNo(), List.of())));
            }
            OwhData.InboundDraft draft = new OwhData.InboundDraft(i.getInboundNo(), i.getOwhCode(), i.getTransportMode(), i.getCarrier(), i.getTrackingNo(),
                    i.getEtaDate(), boxes, i.getRemark());
            String no = support.connector(a.getProvider()).createInbound(support.context(a), draft);
            transactionTemplate.executeWithoutResult(s -> {
                EcomOwhInbound locked = inboundMapper.selectForUpdate(id);
                if (locked.getStatus() != OwhInboundStatus.DRAFT) {
                    throw new BizException("ecom.error.owh.inboundNotDraft", locked.getInboundNo());
                }
                inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getOwhOrderNo, no)
                        .set(EcomOwhInbound::getStatus, OwhInboundStatus.PUSHED).set(EcomOwhInbound::getPushTime, LocalDateTime.now())
                        .set(EcomOwhInbound::getErrorMsg, null).eq(EcomOwhInbound::getId, id));
            });
        } catch (BizException e) {
            recordError(id, e.getMessage());
            throw e;
        }
    }

    /** 同步全部需要同步的入库单（定时任务，只同步开启自动同步的账号），返回每张的结果 */
    public List<SyncResult> syncAll() {
        List<SyncResult> out = new ArrayList<>();
        Set<Long> auto = accountService.autoSyncAccountIds();
        List<EcomOwhInbound> list = inboundMapper.selectList(Wrappers.<EcomOwhInbound>lambdaQuery()
                .and(q -> q.in(EcomOwhInbound::getStatus, SYNCING).or(w -> w.eq(EcomOwhInbound::getStatus, OwhInboundStatus.RECEIVED).eq(EcomOwhInbound::getErpReceived, 0))));
        for (EcomOwhInbound i : list) {
            if (!auto.contains(i.getAccountId())) {
                continue;
            }
            out.add(syncQuietly(i.getId(), i.getInboundNo()));
        }
        return out;
    }

    private SyncResult syncQuietly(Long id, String no) {
        try {
            sync(id);
            return new SyncResult(no, 1, null);
        } catch (BizException e) {
            recordError(id, e.getMessage());
            return new SyncResult(no, 0, e.getMessage());
        }
    }

    /**
     * 同步一张：查询服务商的状态与收货数量；收货完成后 ERP 签收来源单据（失败时记录原因，下次同步重试）
     *
     * @return 同步后的状态
     */
    public OwhInboundStatus sync(Long id) {
        EcomOwhInbound i = detail(id);
        if (i.getStatus() == OwhInboundStatus.RECEIVED) {
            if (i.getErpReceived() == null || i.getErpReceived() == 0) {
                receiveErp(i);
            }
            return i.getStatus();
        }
        if (!SYNCING.contains(i.getStatus()) || i.getOwhOrderNo() == null) {
            throw new BizException("ecom.error.owh.inboundNotPushed", i.getInboundNo());
        }
        EcomOwhAccount a = accountService.get(i.getAccountId());
        OwhData.InboundState state = support.connector(a.getProvider()).queryInbound(support.context(a), i.getOwhOrderNo());
        applyState(i, state);
        if (state.status() == OwhInboundStatus.RECEIVED) {
            receiveErp(detail(id));
        }
        return state.status();
    }

    /**
     * 按服务商推送 / 查询到的状态更新已推送的入库单（服务商回调、补录使用）：写入服务商单号（还没有时）、状态与收货数量，收货完成后 ERP 签收
     */
    public void applyRemote(Long id, String owhOrderNo, OwhData.InboundState state) {
        EcomOwhInbound i = detail(id);
        if (i.getStatus() == OwhInboundStatus.DRAFT) {
            inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getOwhOrderNo, owhOrderNo)
                    .set(EcomOwhInbound::getStatus, OwhInboundStatus.PUSHED).set(EcomOwhInbound::getPushTime, LocalDateTime.now())
                    .eq(EcomOwhInbound::getId, id));
            i = detail(id);
        }
        applyState(i, state);
        if (state.status() == OwhInboundStatus.RECEIVED) {
            receiveErp(detail(id));
        }
    }

    /** 写入服务商返回的状态与各 SKU 收货数量（同一 SKU 多箱时记在第一行） */
    void applyState(EcomOwhInbound i, OwhData.InboundState state) {
        transactionTemplate.executeWithoutResult(s -> {
            Set<String> seen = new HashSet<>();
            BigDecimal total = BigDecimal.ZERO;
            for (EcomOwhInboundItem it : i.getItems()) {
                BigDecimal q = seen.add(it.getSku()) ? Decimals.qty(Decimals.nz(state.received().get(it.getSku()))) : BigDecimal.ZERO;
                total = total.add(q);
                if (q.compareTo(Decimals.nz(it.getReceivedQty())) != 0) {
                    itemMapper.update(Wrappers.<EcomOwhInboundItem>lambdaUpdate().set(EcomOwhInboundItem::getReceivedQty, q).eq(EcomOwhInboundItem::getId, it.getId()));
                }
            }
            OwhInboundStatus st = state.status() == null ? i.getStatus() : state.status();
            if (st == OwhInboundStatus.PUSHED && total.signum() > 0) {
                st = OwhInboundStatus.RECEIVING;
            }
            LocalDateTime receiveTime = st == OwhInboundStatus.RECEIVED ? (state.receiveTime() == null ? LocalDateTime.now() : state.receiveTime()) : null;
            inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getStatus, st).set(EcomOwhInbound::getOwhStatus, state.rawStatus())
                    .set(EcomOwhInbound::getReceivedQty, Decimals.qty(total)).set(receiveTime != null, EcomOwhInbound::getReceiveTime, receiveTime)
                    .set(EcomOwhInbound::getSyncTime, LocalDateTime.now()).set(EcomOwhInbound::getErrorMsg, null)
                    .eq(EcomOwhInbound::getId, i.getId()));
        });
    }

    /**
     * ERP 签收来源单据（在途转海外仓可用）：货件已发运的签收、调拨单在途的收货；已经签收 / 收货的直接标记；
     * 货件还没有发运时记录原因（发运后下次同步再签收）。收货日期为服务商的完成日期，早于来源单据日期时取来源单据日期
     */
    void receiveErp(EcomOwhInbound i) {
        LocalDate date = i.getReceiveTime() == null ? LocalDate.now() : i.getReceiveTime().toLocalDate();
        EcomOwhSupport.asSystem(() -> transactionTemplate.executeWithoutResult(s -> {
            LocalDate d;
            String refNo;
            Long ecomInboundId = null;
            if (i.getSourceType() == OwhInboundSource.INBOUND) {
                EcomInbound ib = ecomInboundService.detail(i.getSourceId());
                d = ib.getShipDate() != null && date.isBefore(ib.getShipDate()) ? ib.getShipDate() : date;
                if (ib.getStatus() == InboundStatus.SHIPPED) {
                    ecomInboundService.receive(ib.getId(), d);
                } else if (ib.getStatus() != InboundStatus.RECEIVED) {
                    throw new BizException("ecom.error.owh.inboundSourceNotShipped", ib.getDocNo());
                }
                refNo = ib.getDocNo();
                ecomInboundId = ib.getId();
                updateShipmentReceived(i, ib);
            } else {
                Map<String, Object> doc = inboundMapper.selectTransfer(i.getSourceId());
                LocalDate biz = doc == null || doc.get("bizDate") == null ? date : LocalDate.parse(String.valueOf(doc.get("bizDate")).substring(0, 10));
                d = date.isBefore(biz) ? biz : date;
                if (doc != null && TRANSIT.equals(doc.get("status"))) {
                    stockDocService.receive(i.getSourceId(), d);
                } else if (doc == null || !DONE.equals(doc.get("status"))) {
                    throw new BizException("ecom.error.owh.transferNotTransit", doc == null ? "-" : doc.get("docNo"));
                }
                refNo = doc == null ? null : String.valueOf(doc.get("docNo"));
            }
            // 按实收：在途调拨整单收货后，短少的差异出库、溢收的其他入库，海外仓库存等于实收，差异逐物料记录待处理
            diffService.record(new EcomOwhDiffService.Source(OwhDiffSource.INBOUND, i.getId(), i.getInboundNo(), refNo, ecomInboundId, i.getWarehouseId()),
                    diffLines(i), d, true);
            inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getErpReceived, 1).set(EcomOwhInbound::getErrorMsg, null)
                    .eq(EcomOwhInbound::getId, i.getId()));
            eventPublisher.publishEvent(new InboundReceivedEvent(i.getId()));
        }));
    }

    /** 各物料的单据数量与实收数量（实收为服务商返回的收货数量，同一 SKU 多箱时记在第一行） */
    private static List<EcomOwhDiffService.Line> diffLines(EcomOwhInbound i) {
        Map<Long, BigDecimal[]> m = new LinkedHashMap<>();
        Map<Long, String> sku = new HashMap<>();
        for (EcomOwhInboundItem it : i.getItems()) {
            BigDecimal[] v = m.computeIfAbsent(it.getMaterialId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            v[0] = v[0].add(it.getQty());
            v[1] = v[1].add(Decimals.nz(it.getReceivedQty()));
            sku.putIfAbsent(it.getMaterialId(), it.getSku());
        }
        List<EcomOwhDiffService.Line> out = new ArrayList<>();
        m.forEach((mat, v) -> out.add(new EcomOwhDiffService.Line(mat, sku.get(mat), v[0], v[1])));
        return out;
    }

    /** 头程货件明细的已收数量按海外仓实收写回（同一物料多行时依次分配，最后一行取余数） */
    private void updateShipmentReceived(EcomOwhInbound i, EcomInbound ib) {
        Map<Long, BigDecimal> actual = new HashMap<>();
        for (EcomOwhInboundItem it : i.getItems()) {
            actual.merge(it.getMaterialId(), Decimals.nz(it.getReceivedQty()), BigDecimal::add);
        }
        Map<Long, List<EcomInboundItem>> byMaterial = new LinkedHashMap<>();
        ib.getItems().forEach(it -> byMaterial.computeIfAbsent(it.getMaterialId(), k -> new ArrayList<>()).add(it));
        byMaterial.forEach((mat, items) -> {
            BigDecimal rest = Decimals.nz(actual.get(mat));
            for (int k = 0; k < items.size(); k++) {
                EcomInboundItem it = items.get(k);
                BigDecimal q = k == items.size() - 1 ? rest.max(BigDecimal.ZERO) : rest.min(it.getQty()).max(BigDecimal.ZERO);
                rest = rest.subtract(q);
                ecomInboundItemMapper.update(Wrappers.<EcomInboundItem>lambdaUpdate().set(EcomInboundItem::getReceivedQty, Decimals.qty(q))
                        .eq(EcomInboundItem::getId, it.getId()));
            }
        });
    }

    private void recordError(Long id, String message) {
        inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getErrorMsg, EcomOwhSupport.abbreviate(message))
                .eq(EcomOwhInbound::getId, id));
    }

    private static String blank(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }
}
