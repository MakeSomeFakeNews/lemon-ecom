package com.lemon.erp.modules.ecom.service;

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
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.ecom.dto.InboundRequest;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomInboundItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @DataPermission(table = "ecom_inbound", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<EcomInbound> page(long current, long size, Long storeId, InboundStatus status, String keyword) {
        Page<EcomInbound> page = Page.of(current, size);
        inboundMapper.selectPageList(page, storeId, status, keyword);
        return PageResult.of(page);
    }

    public EcomInbound detail(Long id) {
        EcomInbound i = DataScopes.require(EcomConstants.INBOUND_RULE, () -> inboundMapper.selectDetail(id), "ecom.error.inboundNotFound");
        i.setItems(itemMapper.selectByInboundId(id));
        return i;
    }

    private EcomInbound require(Long id) {
        return DataScopes.require(EcomConstants.INBOUND_RULE, () -> inboundMapper.selectById(id), "ecom.error.inboundNotFound");
    }

    @Transactional
    public Long create(InboundRequest req) {
        EcomInbound i = new EcomInbound();
        i.setDocNo(docNoService.next(EcomDocNos.INBOUND));
        i.setStatus(InboundStatus.DRAFT);
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
        i.setOtherCost(Decimals.money(Decimals.nz(req.otherCost())));
        i.setRemark(req.remark());
        i.setTotalQty(Decimals.qty(Decimals.sum(req.items(), InboundRequest.Item::qty)));
    }

    private void saveItems(EcomInbound i, InboundRequest req) {
        List<EcomInboundItem> items = req.items().stream().map(r -> {
            EcomInboundItem it = new EcomInboundItem();
            it.setMaterialId(r.materialId());
            it.setSellerSku(r.sellerSku());
            it.setQty(Decimals.qty(r.qty()));
            it.setReceivedQty(BigDecimal.ZERO);
            it.setRemark(r.remark());
            return it;
        }).toList();
        DetailLines.replace(itemMapper, EcomInboundItem::getInboundId, i.getId(), items, EcomInboundItem::setInboundId, itemMapper::insertBatch);
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomInbound i = require(id);
            if (i.getStatus() != InboundStatus.DRAFT && i.getStatus() != InboundStatus.CANCELLED) {
                throw new BizException("ecom.error.inboundNotDraft", i.getDocNo());
            }
            itemMapper.delete(Wrappers.<EcomInboundItem>lambdaQuery().eq(EcomInboundItem::getInboundId, id));
        }
        inboundMapper.deleteByIds(ids);
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
