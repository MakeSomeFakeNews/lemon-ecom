package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.entity.EcomReturnItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomReturnItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 退货退款：仅退款直接登记退款；退货退款在收到货物时生成销售退货（引用订单出库的隐式订单，库存增加、红字应收），再登记退款。
 * 退款金额回写订单重算利润，退货入库冲减订单的商品成本
 */
@Service
@RequiredArgsConstructor
public class EcomReturnService {

    private final EcomReturnMapper returnMapper;
    private final EcomReturnItemMapper itemMapper;
    private final EcomOrderService orderService;
    private final EcomStoreService storeService;
    private final ScmTradeDocService tradeDocService;
    private final StockCostService stockCostService;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;

    @DataPermission(table = "ecom_return", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<EcomReturn> page(long current, long size, Long storeId, EcomReturnStatus status, String keyword) {
        Page<EcomReturn> page = Page.of(current, size);
        returnMapper.selectPageList(page, storeId, status, keyword);
        return PageResult.of(page);
    }

    public EcomReturn detail(Long id) {
        EcomReturn r = DataScopes.require(EcomConstants.RETURN_RULE, () -> returnMapper.selectDetail(id), "ecom.error.returnNotFound");
        r.setItems(itemMapper.selectByReturnId(id));
        return r;
    }

    private EcomReturn require(Long id) {
        return DataScopes.require(EcomConstants.RETURN_RULE, () -> returnMapper.selectById(id), "ecom.error.returnNotFound");
    }

    @Transactional
    public Long create(ReturnRequest req) {
        EcomOrder order = orderService.getForReturn(req.orderId());
        if (order.getLocalStatus() == EcomLocalStatus.CLOSED) {
            throw new BizException("ecom.error.orderClosed", order.getPlatformOrderNo());
        }
        if (req.returnType() == ReturnType.RETURN_REFUND && order.getLocalStatus() != EcomLocalStatus.OUTBOUND
                && order.getLocalStatus() != EcomLocalStatus.SHIPPED) {
            throw new BizException("ecom.error.orderNotOutbound", order.getPlatformOrderNo());
        }
        EcomStore store = storeService.get(order.getStoreId());
        dictLookup.check(EcomConstants.RETURN_REASON_DICT, req.reason(), "ecom.label.returnReason");
        Map<Long, EcomOrderItem> orderItems = order.getItems().stream().collect(Collectors.toMap(EcomOrderItem::getId, Function.identity()));
        Map<Long, BigDecimal> returned = new HashMap<>();
        for (Map<String, Object> row : itemMapper.selectReturnedQty(order.getId())) {
            returned.put(((Number) row.get("orderItemId")).longValue(), new BigDecimal(row.get("qty").toString()));
        }
        List<EcomReturnItem> items = new ArrayList<>();
        BigDecimal refund = BigDecimal.ZERO;
        for (ReturnRequest.Item ri : req.items()) {
            EcomOrderItem oi = orderItems.get(ri.orderItemId());
            if (oi == null) {
                throw new BizException("ecom.error.orderLineNotFound");
            }
            BigDecimal available = Decimals.nz(oi.getQty()).subtract(returned.getOrDefault(oi.getId(), BigDecimal.ZERO));
            if (ri.qty().compareTo(available) > 0) {
                throw new BizException("ecom.error.returnQtyExceeds", oi.getSellerSku(), available);
            }
            EcomReturnItem it = new EcomReturnItem();
            it.setOrderItemId(oi.getId());
            it.setMaterialId(oi.getMaterialId());
            it.setSellerSku(oi.getSellerSku());
            it.setQty(Decimals.qty(ri.qty()));
            it.setRefundAmount(Decimals.money(Decimals.nz(ri.refundAmount())));
            items.add(it);
            refund = refund.add(it.getRefundAmount());
        }
        EcomReturn r = new EcomReturn();
        r.setDocNo(docNoService.next(EcomDocNos.RETURN));
        r.setStoreId(order.getStoreId());
        r.setOrderId(order.getId());
        r.setPlatformOrderNo(order.getPlatformOrderNo());
        r.setReturnType(req.returnType());
        r.setReason(req.reason());
        r.setCurrency(order.getCurrency());
        r.setExchangeRate(order.getExchangeRate());
        r.setRefundAmount(req.refundAmount() != null ? Decimals.money(req.refundAmount()) : refund);
        if (req.returnType() == ReturnType.RETURN_REFUND) {
            r.setWarehouseId(req.warehouseId() != null ? req.warehouseId() : store.getWarehouseId());
        }
        r.setStatus(EcomReturnStatus.PENDING);
        r.setRemark(req.remark());
        LoginUser user = SecurityUtils.getLoginUser();
        r.setDeptId(user.deptId());
        returnMapper.insert(r);
        items.forEach(i -> i.setReturnId(r.getId()));
        itemMapper.insertBatch(items);
        return r.getId();
    }

    /**
     * 退货入库：生成销售退货单（引用订单出库时的隐式订单明细，数量按多件装折为物料数量），库存增加、红字应收，冲减订单成本
     */
    @Transactional
    public void receive(Long id, LocalDate date) {
        EcomReturn r = require(id);
        if (r.getReturnType() != ReturnType.RETURN_REFUND || r.getStatus() != EcomReturnStatus.PENDING) {
            throw new BizException("ecom.error.returnStatusNotAllow", r.getDocNo());
        }
        EcomOrder order = orderService.getForReturn(r.getOrderId());
        if (order.getTradeDocId() == null) {
            throw new BizException("ecom.error.orderNotOutbound", order.getPlatformOrderNo());
        }
        ScmTradeDoc out = tradeDocService.detail(TradeKind.SALES_OUT, order.getTradeDocId());
        Map<Long, List<ScmTradeDocItem>> outLines = new HashMap<>();
        for (ScmTradeDocItem line : out.getItems()) {
            outLines.computeIfAbsent(line.getMaterialId(), k -> new ArrayList<>()).add(line);
        }
        Map<Long, EcomOrderItem> orderItems = order.getItems().stream().collect(Collectors.toMap(EcomOrderItem::getId, Function.identity()));
        // 退回的物料数量：单品 = 件数 × 多件装；组合商品按组成比例退回各组件 = 件数 × 用量
        Map<Long, BigDecimal> need = new java.util.LinkedHashMap<>();
        Map<Long, String> skuOf = new HashMap<>();
        for (EcomReturnItem it : itemMapper.selectByReturnId(id)) {
            EcomOrderItem oi = orderItems.get(it.getOrderItemId());
            if (oi == null || !EcomOutboundSupport.mapped(oi)) {
                throw new BizException("ecom.error.returnLineNotInOutbound", it.getSellerSku());
            }
            if (EcomOutboundSupport.isBundle(oi)) {
                for (var part : oi.getParts()) {
                    need.merge(part.getMaterialId(), Decimals.qty(it.getQty().multiply(part.getUnitQty())), BigDecimal::add);
                    skuOf.putIfAbsent(part.getMaterialId(), it.getSellerSku());
                }
            } else {
                BigDecimal pack = oi.getQty().signum() > 0 ? oi.getMaterialQty().divide(oi.getQty(), 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ONE;
                need.merge(oi.getMaterialId(), Decimals.qty(it.getQty().multiply(pack)), BigDecimal::add);
                skuOf.putIfAbsent(oi.getMaterialId(), it.getSellerSku());
            }
        }
        // 同一物料在出库单上有多行（单品与组合的组件）时依次分到各行，最后一行取余数
        List<TradeDocItemRequest> lines = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : need.entrySet()) {
            List<ScmTradeDocItem> cands = outLines.get(e.getKey());
            if (cands == null || cands.isEmpty()) {
                throw new BizException("ecom.error.returnLineNotInOutbound", skuOf.get(e.getKey()));
            }
            BigDecimal rest = e.getValue();
            for (int i = 0; i < cands.size() && rest.signum() > 0; i++) {
                ScmTradeDocItem line = cands.get(i);
                BigDecimal q = i == cands.size() - 1 ? rest : rest.min(Decimals.nz(line.getQty()));
                if (q.signum() > 0) {
                    lines.add(new TradeDocItemRequest(line.getOrderItemId(), null, null, null, null, q, null, skuOf.get(e.getKey())));
                    rest = rest.subtract(q);
                }
            }
        }
        LocalDate bizDate = date == null ? LocalDate.now() : date;
        TradeDocRequest req = new TradeDocRequest(out.getOrderId(), r.getWarehouseId(), bizDate,
                Messages.get("ecom.text.returnRemark", r.getDocNo(), order.getPlatformOrderNo()), Boolean.FALSE, lines);
        Long docId = tradeDocService.create(TradeKind.SALES_RETURN, req);
        ScmTradeDoc doc = tradeDocService.detail(TradeKind.SALES_RETURN, docId);
        if (doc.getWmsDocId() != null) {
            BigDecimal cost = Decimals.sum(stockCostService.docCosts(StockBizType.IN, List.of(doc.getWmsDocId())).values(), c -> Decimals.nz(c.amount()).abs());
            orderService.subtractCost(order.getId(), cost);
        }
        StatusTransitions.change(returnMapper, EcomReturn::getId, id, EcomReturn::getStatus, EcomReturnStatus.PENDING, EcomReturnStatus.RECEIVED,
                w -> w.set(EcomReturn::getTradeDocId, docId).set(EcomReturn::getTradeDocNo, doc.getDocNo()), "ecom.error.returnStatusChanged");
    }

    /** 登记退款：仅退款的待处理单、退货退款已入库的单 */
    @Transactional
    public void refund(Long id) {
        EcomReturn r = require(id);
        EcomReturnStatus from = r.getReturnType() == ReturnType.REFUND_ONLY ? EcomReturnStatus.PENDING : EcomReturnStatus.RECEIVED;
        if (r.getStatus() != from) {
            throw new BizException("ecom.error.returnStatusNotAllow", r.getDocNo());
        }
        StatusTransitions.change(returnMapper, EcomReturn::getId, id, EcomReturn::getStatus, from, EcomReturnStatus.REFUNDED,
                w -> w.set(EcomReturn::getRefundTime, LocalDateTime.now()), "ecom.error.returnStatusChanged");
        orderService.addRefund(r.getOrderId(), r.getRefundAmount());
    }

    /** 关闭：待处理的单取消 */
    public void close(Long id) {
        EcomReturn r = require(id);
        StatusTransitions.change(returnMapper, EcomReturn::getId, id, EcomReturn::getStatus, EcomReturnStatus.PENDING, EcomReturnStatus.CLOSED,
                "ecom.error.returnStatusNotAllow", r.getDocNo());
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomReturn r = require(id);
            if (r.getStatus() != EcomReturnStatus.PENDING && r.getStatus() != EcomReturnStatus.CLOSED) {
                throw new BizException("ecom.error.returnStatusNotAllow", r.getDocNo());
            }
            itemMapper.delete(Wrappers.<EcomReturnItem>lambdaQuery().eq(EcomReturnItem::getReturnId, id));
        }
        returnMapper.deleteByIds(ids);
    }
}
