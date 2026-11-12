package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.dto.InboundBoxRequest;
import com.lemon.erp.modules.ecom.dto.InboundRequest;
import com.lemon.erp.modules.ecom.dto.InboundSkuLine;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomInboundBox;
import com.lemon.erp.modules.ecom.entity.EcomInboundBoxItem;
import com.lemon.erp.modules.ecom.entity.EcomInboundItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomInboundBoxItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomInboundBoxMapper;
import com.lemon.erp.modules.ecom.mapper.EcomInboundItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomInboundMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 头程货件：草稿 → 提交（可审批）→ 发运：生成在途调拨单（本地仓 → 平台仓）、头程成本按数量分摊到每件 → 签收：调拨收货，平台仓库存增加
 */
@Service
@RequiredArgsConstructor
public class EcomInboundService {

    static final ApprovalFlow FLOW = new ApprovalFlow("ecom_inbound", "ecom_inbound:");
    private static final int LANDED_SCALE = 6;

    private final EcomInboundMapper inboundMapper;
    private final EcomInboundItemMapper itemMapper;
    private final EcomStoreService storeService;
    private final WmsWarehouseService warehouseService;
    private final WmsMaterialService materialService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;
    private final EcomInboundBoxMapper boxMapper;
    private final EcomInboundBoxItemMapper boxItemMapper;
    private final EcomListingService listingService;
    private final EcomBundleService bundleService;
    private final ApplicationEventPublisher eventPublisher;

    /** 可以修改装箱的状态 */
    private static final Set<InboundStatus> BOX_EDITABLE = EnumSet.of(InboundStatus.DRAFT, InboundStatus.APPROVING, InboundStatus.SHIPPED);
    private static final int SKU_QTY_SCALE = 4;
    private static final BigDecimal CM3_PER_M3 = new BigDecimal("1000000");

    // 数据范围按店铺授权（EcomStoreScope，接口执行期间自动过滤），不再按部门 / 制单人
    public PageResult<EcomInbound> page(long current, long size, Long storeId, InboundStatus status, String keyword) {
        Page<EcomInbound> page = Page.of(current, size);
        inboundMapper.selectPageList(page, storeId, status, keyword);
        return PageResult.of(page);
    }

    public EcomInbound detail(Long id) {
        EcomInbound i = EcomStoreScope.require(() -> inboundMapper.selectDetail(id), "ecom.error.inboundNotFound");
        i.setItems(itemMapper.selectByInboundId(id));
        i.setBoxes(boxes(id));
        i.setSkuLines(skuLines(i.getItems(), i.getBoxes()));
        return i;
    }

    /** 发货计划生成的货件（未取消的在前） */
    public List<EcomInbound> listByPlan(Long planId) {
        return inboundMapper.selectList(Wrappers.<EcomInbound>lambdaQuery().eq(EcomInbound::getPlanId, planId).orderByAsc(EcomInbound::getId));
    }

    private EcomInbound require(Long id) {
        return EcomStoreScope.require(() -> inboundMapper.selectById(id), "ecom.error.inboundNotFound");
    }

    @Transactional
    public Long create(InboundRequest req) {
        return create(req, null);
    }

    /** 新增货件草稿（发货计划生成时带上计划 ID，调用方的事务中执行） */
    public Long create(InboundRequest req, Long planId) {
        EcomInbound i = new EcomInbound();
        i.setDocNo(docNoService.next(EcomDocNos.INBOUND));
        i.setStatus(InboundStatus.DRAFT);
        i.setPlanId(planId);
        LoginUser user = SecurityUtils.getLoginUser();
        i.setDeptId(user.deptId());
        apply(i, req);
        inboundMapper.insert(i);
        saveItems(i, req);
        return i.getId();
    }

    @Transactional
    public void update(Long id, InboundRequest req) {
        EcomInbound i = require(id);
        if (i.getStatus() != InboundStatus.DRAFT) {
            throw new BizException("ecom.error.inboundNotDraft", i.getDocNo());
        }
        apply(i, req);
        inboundMapper.updateById(i);
        saveItems(i, req);
    }

    private void apply(EcomInbound i, InboundRequest req) {
        EcomStore store = storeService.requireEnabled(req.storeId());
        if (req.fromWarehouseId().equals(req.toWarehouseId())) {
            throw new BizException("ecom.error.inboundSameWarehouse");
        }
        warehouseService.get(req.fromWarehouseId());
        warehouseService.get(req.toWarehouseId());
        Set<Long> materialIds = req.items().stream().map(InboundRequest.Item::materialId).collect(Collectors.toSet());
        Map<Long, WmsMaterial> materials = materialService.getEnabled(materialIds);
        for (Long mid : materialIds) {
            if (!materials.containsKey(mid)) {
                throw new BizException("ecom.error.materialNotFoundOrDisabled", mid);
            }
        }
        i.setStoreId(store.getId());
        i.setShipmentId(req.shipmentId());
        i.setFromWarehouseId(req.fromWarehouseId());
        i.setToWarehouseId(req.toWarehouseId());
        i.setChannelId(req.channelId());
        i.setCarrier(req.carrier());
        i.setTrackingNo(req.trackingNo());
        i.setShipDate(req.shipDate());
        i.setEtaDate(req.etaDate());
        i.setBoxCount(req.boxCount());
        i.setWeight(req.weight());
        i.setVolume(req.volume());
        i.setFreight(Decimals.money(Decimals.nz(req.freight())));
        i.setDuty(Decimals.money(Decimals.nz(req.duty())));
        i.setImportVat(Decimals.money(Decimals.nz(req.importVat())));
        i.setOtherCost(Decimals.money(Decimals.nz(req.otherCost())));
        i.setRemark(req.remark());
        i.setTotalQty(Decimals.qty(Decimals.sum(req.items(), InboundRequest.Item::qty)));
    }

    /**
     * 保存明细：带了在线商品（或按 店铺 + 平台 SKU 找到）且物料是它的组成时，按平台 SKU 计件数（单品 = 数量 ÷ 多件装，
     * 组合 = 数量 ÷ 组件用量，组合只有第一个组件行计入件数），装箱、FNSKU 标签按件数
     */
    private void saveItems(EcomInbound i, InboundRequest req) {
        Set<String> skus = req.items().stream().filter(r -> r.listingId() == null && StringUtils.hasText(r.sellerSku()))
                .map(r -> r.sellerSku().trim()).collect(Collectors.toSet());
        Map<String, EcomListing> bySku = listingService.byStoreSkus(i.getStoreId(), skus);
        Map<Long, EcomListing> byId = listingService.mapByIds(req.items().stream().map(InboundRequest.Item::listingId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Set<Long> listingIds = new HashSet<>(byId.keySet());
        bySku.values().forEach(l -> listingIds.add(l.getId()));
        Map<Long, List<BundleComponent>> comps = bundleService.componentsOf(listingIds);
        List<EcomInboundItem> items = new ArrayList<>();
        for (InboundRequest.Item r : req.items()) {
            EcomInboundItem it = new EcomInboundItem();
            it.setMaterialId(r.materialId());
            it.setSellerSku(StringUtils.hasText(r.sellerSku()) ? r.sellerSku().trim() : null);
            it.setQty(Decimals.qty(r.qty()));
            it.setReceivedQty(BigDecimal.ZERO);
            it.setRemark(r.remark());
            it.setPlanItemId(r.planItemId());
            it.setPartMain(1);
            EcomListing l = r.listingId() != null ? byId.get(r.listingId()) : it.getSellerSku() == null ? null : bySku.get(it.getSellerSku());
            if (l != null && !Objects.equals(l.getStoreId(), i.getStoreId())) {
                throw new BizException("ecom.error.catalog.listingStoreDiffers", l.getSellerSku());
            }
            List<BundleComponent> c = l == null ? null : comps.get(l.getId());
            BundleComponent comp = c == null ? null : c.stream().filter(x -> x.materialId().equals(r.materialId())).findFirst().orElse(null);
            if (comp != null) {
                it.setListingId(l.getId());
                it.setSellerSku(l.getSellerSku());
                it.setSkuQty(it.getQty().divide(comp.qty(), SKU_QTY_SCALE, RoundingMode.HALF_UP));
                it.setPartMain(c.get(0) == comp ? 1 : 0);
            } else {
                it.setSkuQty(it.getQty());
            }
            items.add(it);
        }
        DetailLines.replace(itemMapper, EcomInboundItem::getInboundId, i.getId(), items, EcomInboundItem::setInboundId, itemMapper::insertBatch);
    }

    // ---------------------------------------------------------------- 装箱

    private List<EcomInboundBox> boxes(Long inboundId) {
        List<EcomInboundBox> boxes = boxMapper.selectList(Wrappers.<EcomInboundBox>lambdaQuery().eq(EcomInboundBox::getInboundId, inboundId)
                .orderByAsc(EcomInboundBox::getBoxNo));
        Map<Integer, List<EcomInboundBoxItem>> items = boxItemMapper.selectList(Wrappers.<EcomInboundBoxItem>lambdaQuery()
                        .eq(EcomInboundBoxItem::getInboundId, inboundId).orderByAsc(EcomInboundBoxItem::getId)).stream()
                .collect(Collectors.groupingBy(EcomInboundBoxItem::getBoxNo));
        boxes.forEach(b -> b.setItems(items.getOrDefault(b.getBoxNo(), List.of())));
        return boxes;
    }

    /** 按平台 SKU 汇总件数（没有平台 SKU 的按物料编码）与已装箱件数 */
    static List<InboundSkuLine> skuLines(List<EcomInboundItem> items, List<EcomInboundBox> boxes) {
        Map<String, BigDecimal> boxed = new HashMap<>();
        for (EcomInboundBox b : boxes) {
            for (EcomInboundBoxItem x : b.getItems()) {
                boxed.merge(x.getSellerSku(), Decimals.nz(x.getQty()), BigDecimal::add);
            }
        }
        Map<String, InboundSkuLine> out = new LinkedHashMap<>();
        for (EcomInboundItem it : items) {
            String key = skuKey(it);
            BigDecimal qty = it.getPartMain() != null && it.getPartMain() == 0 ? BigDecimal.ZERO
                    : it.getSkuQty() != null ? it.getSkuQty() : Decimals.nz(it.getQty());
            InboundSkuLine old = out.get(key);
            out.put(key, new InboundSkuLine(key, old != null && old.listingId() != null ? old.listingId() : it.getListingId(),
                    old != null && old.fnsku() != null ? old.fnsku() : it.getFnsku(), old != null && old.title() != null ? old.title() : it.getTitle(),
                    old != null ? old.materialCode() : it.getMaterialCode(), old != null ? old.materialName() : it.getMaterialName(),
                    Decimals.qty((old == null ? BigDecimal.ZERO : old.qty()).add(qty)), Decimals.qty(boxed.getOrDefault(key, BigDecimal.ZERO))));
        }
        return new ArrayList<>(out.values());
    }

    private static String skuKey(EcomInboundItem it) {
        return StringUtils.hasText(it.getSellerSku()) ? it.getSellerSku() : it.getMaterialCode();
    }

    /**
     * 保存装箱（整体替换）：箱号按顺序从 1 编起，箱内平台 SKU 必须在货件中、各 SKU 装箱件数合计不超过货件件数；
     * 同时按装箱更新货件的箱数、毛重（各箱重量都填写时）、体积（各箱尺寸都填写时）。签收、取消后不能修改
     */
    @Transactional
    public void saveBoxes(Long id, InboundBoxRequest req) {
        EcomInbound i = require(id);
        if (!BOX_EDITABLE.contains(i.getStatus())) {
            throw new BizException("ecom.error.catalog.boxNotEditable", i.getDocNo());
        }
        Map<String, InboundSkuLine> lines = skuLines(itemMapper.selectByInboundId(id), List.of()).stream()
                .collect(Collectors.toMap(InboundSkuLine::sellerSku, l -> l));
        Map<String, BigDecimal> packed = new HashMap<>();
        List<EcomInboundBox> boxes = new ArrayList<>();
        List<EcomInboundBoxItem> boxItems = new ArrayList<>();
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;
        boolean weightKnown = true;
        boolean volumeKnown = true;
        int no = 0;
        for (InboundBoxRequest.Box b : req.boxes()) {
            no++;
            EcomInboundBox box = new EcomInboundBox();
            box.setInboundId(id);
            box.setBoxNo(no);
            box.setLength(b.length());
            box.setWidth(b.width());
            box.setHeight(b.height());
            box.setWeight(b.weight());
            box.setRemark(b.remark());
            boxes.add(box);
            if (b.weight() == null) weightKnown = false;
            else weight = weight.add(b.weight());
            if (b.length() == null || b.width() == null || b.height() == null) volumeKnown = false;
            else volume = volume.add(b.length().multiply(b.width()).multiply(b.height()).divide(CM3_PER_M3, 4, RoundingMode.HALF_UP));
            Map<String, BigDecimal> inBox = new LinkedHashMap<>();
            for (InboundBoxRequest.Item x : b.items()) {
                inBox.merge(x.sellerSku().trim(), x.qty(), BigDecimal::add);
            }
            for (Map.Entry<String, BigDecimal> e : inBox.entrySet()) {
                InboundSkuLine line = lines.get(e.getKey());
                if (line == null) {
                    throw new BizException("ecom.error.catalog.boxSkuNotInInbound", e.getKey());
                }
                BigDecimal total = packed.merge(e.getKey(), e.getValue(), BigDecimal::add);
                if (total.compareTo(line.qty()) > 0) {
                    throw new BizException("ecom.error.catalog.boxQtyExceeds", e.getKey(), line.qty().stripTrailingZeros().toPlainString());
                }
                EcomInboundBoxItem item = new EcomInboundBoxItem();
                item.setInboundId(id);
                item.setBoxNo(no);
                item.setSellerSku(e.getKey());
                item.setQty(Decimals.qty(e.getValue()));
                boxItems.add(item);
            }
        }
        boxMapper.delete(Wrappers.<EcomInboundBox>lambdaQuery().eq(EcomInboundBox::getInboundId, id));
        boxItemMapper.delete(Wrappers.<EcomInboundBoxItem>lambdaQuery().eq(EcomInboundBoxItem::getInboundId, id));
        if (!boxes.isEmpty()) {
            boxMapper.insertBatch(boxes);
        }
        if (!boxItems.isEmpty()) {
            boxItemMapper.insertBatch(boxItems);
        }
        if (!boxes.isEmpty()) {
            inboundMapper.update(Wrappers.<EcomInbound>lambdaUpdate().set(EcomInbound::getBoxCount, boxes.size())
                    .set(weightKnown, EcomInbound::getWeight, weight.setScale(3, RoundingMode.HALF_UP))
                    .set(volumeKnown, EcomInbound::getVolume, volume)
                    .eq(EcomInbound::getId, id));
        }
    }

    /** 货件取消 / 删除后通知发货计划重新计算状态 */
    public record InboundReleasedEvent(Long planId) {
    }

    @Transactional
    public void delete(List<Long> ids) {
        Set<Long> plans = new HashSet<>();
        for (Long id : ids) {
            EcomInbound i = require(id);
            if (i.getStatus() != InboundStatus.DRAFT && i.getStatus() != InboundStatus.CANCELLED) {
                throw new BizException("ecom.error.inboundNotDraft", i.getDocNo());
            }
            itemMapper.delete(Wrappers.<EcomInboundItem>lambdaQuery().eq(EcomInboundItem::getInboundId, id));
            boxMapper.delete(Wrappers.<EcomInboundBox>lambdaQuery().eq(EcomInboundBox::getInboundId, id));
            boxItemMapper.delete(Wrappers.<EcomInboundBoxItem>lambdaQuery().eq(EcomInboundBoxItem::getInboundId, id));
            if (i.getPlanId() != null) {
                plans.add(i.getPlanId());
            }
        }
        inboundMapper.deleteByIds(ids);
        plans.forEach(p -> eventPublisher.publishEvent(new InboundReleasedEvent(p)));
    }

    /**
     * 提交发运：流程「头程货件审批」已发布时进入审批，通过后发运；未发布时直接发运
     *
     * @return true 已发起审批，false 已直接发运
     */
    @Transactional
    public boolean submit(Long id) {
        EcomInbound i = require(id);
        if (i.getStatus() != InboundStatus.DRAFT) {
            throw new BizException("ecom.error.inboundNotDraft", i.getDocNo());
        }
        EcomStore store = storeService.get(i.getStoreId());
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", totalCost(i));
        vars.put("totalQty", i.getTotalQty());
        vars.put("storeName", store.getName());
        return approvalSupport.submit(FLOW, id, vars, () -> ship(i),
                () -> StatusTransitions.change(inboundMapper, EcomInbound::getId, id, EcomInbound::getStatus, InboundStatus.DRAFT,
                        InboundStatus.APPROVING, "ecom.error.inboundStatusChanged"),
                instanceId -> inboundMapper.update(Wrappers.<EcomInbound>lambdaUpdate().set(EcomInbound::getFlowInstanceId, instanceId)
                        .eq(EcomInbound::getId, id)));
    }

    @EventListener
    @Transactional
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> {
            EcomInbound i = inboundMapper.selectById(id);
            if (i == null || i.getStatus() != InboundStatus.APPROVING) {
                return;
            }
            if (approved) {
                ship(i);
            } else {
                inboundMapper.update(Wrappers.<EcomInbound>lambdaUpdate().set(EcomInbound::getStatus, InboundStatus.DRAFT).eq(EcomInbound::getId, id));
            }
        });
    }

    /**
     * 发运：生成在途调拨单（调出仓即时减少库存，调入仓待签收），单件头程成本 = (运费 + 关税 + 其他) ÷ 数量合计
     */
    private void ship(EcomInbound i) {
        List<EcomInboundItem> items = itemMapper.selectByInboundId(i.getId());
        if (items.isEmpty()) {
            throw new BizException("ecom.error.inboundNoItems", i.getDocNo());
        }
        LocalDate date = i.getShipDate() == null ? LocalDate.now() : i.getShipDate();
        List<StockDocItemRequest> lines = items.stream()
                .map(it -> new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, it.getQty(), null, it.getSellerSku()))
                .toList();
        StockDocRequest req = new StockDocRequest(null, i.getFromWarehouseId(), i.getToWarehouseId(), date,
                Messages.get("ecom.text.inboundTransferRemark", i.getDocNo()), lines, true, null);
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.TRANSFER, req, i.getDocNo());
        BigDecimal unit = unitLanded(i);
        for (EcomInboundItem it : items) {
            it.setUnitLanded(unit);
            itemMapper.updateById(it);
        }
        boolean changed = StatusTransitions.tryChangeFromAny(inboundMapper, EcomInbound::getId, i.getId(), EcomInbound::getStatus,
                Set.of(InboundStatus.DRAFT, InboundStatus.APPROVING), InboundStatus.SHIPPED,
                w -> w.set(EcomInbound::getTransferDocId, doc.getId()).set(EcomInbound::getTransferDocNo, doc.getDocNo()).set(EcomInbound::getShipDate, date));
        if (!changed) {
            throw new BizException("ecom.error.inboundStatusChanged");
        }
    }

    private static BigDecimal totalCost(EcomInbound i) {
        return Decimals.nz(i.getFreight()).add(Decimals.nz(i.getDuty())).add(Decimals.nz(i.getOtherCost()));
    }

    private static BigDecimal unitLanded(EcomInbound i) {
        BigDecimal total = totalCost(i);
        if (total.signum() == 0 || i.getTotalQty() == null || i.getTotalQty().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(i.getTotalQty(), LANDED_SCALE, RoundingMode.HALF_UP);
    }

    /** 签收：在途调拨收货，平台仓库存增加 */
    @Transactional
    public void receive(Long id, LocalDate receiveDate) {
        EcomInbound i = require(id);
        if (i.getStatus() != InboundStatus.SHIPPED) {
            throw new BizException("ecom.error.inboundNotShipped", i.getDocNo());
        }
        LocalDate date = receiveDate == null ? LocalDate.now() : receiveDate;
        stockDocService.receive(i.getTransferDocId(), date);
        StatusTransitions.change(inboundMapper, EcomInbound::getId, id, EcomInbound::getStatus, InboundStatus.SHIPPED, InboundStatus.RECEIVED,
                w -> w.set(EcomInbound::getReceiveDate, date), "ecom.error.inboundStatusChanged");
        for (EcomInboundItem it : itemMapper.selectByInboundId(id)) {
            it.setReceivedQty(it.getQty());
            itemMapper.updateById(it);
        }
    }

    /** 取消：草稿直接取消，审批中的撤回流程 */
    @Transactional
    public void cancel(Long id) {
        EcomInbound i = require(id);
        if (i.getStatus() == InboundStatus.APPROVING) {
            approvalSupport.cancelIfRunning(i.getFlowInstanceId(), Messages.get("ecom.text.inboundCancelled"));
        } else if (i.getStatus() != InboundStatus.DRAFT) {
            throw new BizException("ecom.error.inboundNotDraft", i.getDocNo());
        }
        StatusTransitions.changeFromAny(inboundMapper, EcomInbound::getId, id, EcomInbound::getStatus,
                Set.of(InboundStatus.DRAFT, InboundStatus.APPROVING), InboundStatus.CANCELLED, "ecom.error.inboundStatusChanged");
        if (i.getPlanId() != null) {
            eventPublisher.publishEvent(new InboundReleasedEvent(i.getPlanId()));
        }
    }

    /**
     * 已发运 / 已签收后补录物流信息与头程费用（运费单往往后到），重新分摊单件头程成本；之后出库的平台配送订单按新成本分摊
     */
    @Transactional
    public void updateCosts(Long id, InboundRequest req) {
        EcomInbound i = require(id);
        if (i.getStatus() != InboundStatus.SHIPPED && i.getStatus() != InboundStatus.RECEIVED) {
            throw new BizException("ecom.error.inboundNotShipped", i.getDocNo());
        }
        i.setCarrier(req.carrier());
        i.setTrackingNo(req.trackingNo());
        i.setChannelId(req.channelId());
        i.setEtaDate(req.etaDate());
        i.setBoxCount(req.boxCount());
        i.setWeight(req.weight());
        i.setVolume(req.volume());
        i.setFreight(Decimals.money(Decimals.nz(req.freight())));
        i.setDuty(Decimals.money(Decimals.nz(req.duty())));
        i.setImportVat(Decimals.money(Decimals.nz(req.importVat())));
        i.setOtherCost(Decimals.money(Decimals.nz(req.otherCost())));
        i.setRemark(req.remark());
        inboundMapper.updateById(i);
        BigDecimal unit = unitLanded(i);
        for (EcomInboundItem it : itemMapper.selectByInboundId(id)) {
            it.setUnitLanded(unit);
            itemMapper.updateById(it);
        }
    }
}
