package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.ecom.dto.PurchasePlanFromReplenish;
import com.lemon.erp.modules.ecom.dto.PurchasePlanRequest;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.entity.EcomPurchasePlan;
import com.lemon.erp.modules.ecom.entity.EcomPurchasePlanItem;
import com.lemon.erp.modules.ecom.mapper.EcomPurchasePlanItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPurchasePlanMapper;
import com.lemon.erp.modules.ecom.model.PurchasePlanStatus;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmPricingService;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购计划：补货建议（本地仓采购）勾选生成 → 草稿（调整数量、供应商、单价、到货日期）→ 审核 → 生成采购订单（按供应商拆单，
 * 调用供应链的采购订单创建，订单为草稿、来源「采购计划」，回写订单号）→ 采购订单入库后明细显示已到货数量。
 * 数据权限按制单人部门 + 制单人
 */
@Service
@RequiredArgsConstructor
public class EcomPurchasePlanService {

    static final DataPermissionRule PLAN_RULE = new DataPermissionRule("ecom_purchase_plan", "dept_id", "create_by");
    /** 需求构成 JSON 的最大长度（与表字段一致） */
    private static final int DEMAND_MAX = 2000;

    private final EcomPurchasePlanMapper planMapper;
    private final EcomPurchasePlanItemMapper itemMapper;
    private final EcomReplenishService replenishService;
    private final DocNoService docNoService;
    private final WmsMaterialService materialService;
    private final WmsWarehouseService warehouseService;
    private final ScmSupplierService supplierService;
    private final ScmOrderService orderService;
    private final ScmPricingService pricingService;
    private final ObjectMapper objectMapper;

    @DataPermission(table = "ecom_purchase_plan", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<EcomPurchasePlan> page(long current, long size, PurchasePlanStatus status, String keyword) {
        Page<EcomPurchasePlan> page = Page.of(current, size);
        planMapper.selectPageList(page, status, keyword);
        return PageResult.of(page);
    }

    public EcomPurchasePlan detail(Long id) {
        EcomPurchasePlan p = DataScopes.require(PLAN_RULE, () -> planMapper.selectDetail(id), "ecom.error.purchase.planNotFound");
        p.setItems(itemMapper.selectByPlanId(id));
        return p;
    }

    private EcomPurchasePlan require(Long id) {
        return DataScopes.require(PLAN_RULE, () -> planMapper.selectById(id), "ecom.error.purchase.planNotFound");
    }

    /**
     * 由补货建议生成：按物料汇总多店铺的需求（后端按同样的加权参数重新计算），带出默认供应商、单价、税率、到货日期（今天 + 采购周期）
     *
     * @return 采购计划 ID
     */
    @Transactional
    public Long createFromReplenish(PurchasePlanFromReplenish req) {
        ReplenishWeights weights = ReplenishWeights.of(req.w7(), req.w14(), req.w30(), req.w60()).withForecast(req.forecast());
        Map<Long, BigDecimal> wanted = new LinkedHashMap<>();
        req.items().forEach(i -> wanted.merge(i.materialId(), i.qty() == null ? BigDecimal.ZERO : i.qty(), BigDecimal::add));
        Map<Long, PurchaseSuggestRow> suggestions = replenishService.purchaseRowsOf(wanted.keySet(), weights);
        List<EcomPurchasePlanItem> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : wanted.entrySet()) {
            PurchaseSuggestRow s = suggestions.get(e.getKey());
            if (s == null) {
                throw new BizException("ecom.error.purchase.notInSuggestion", e.getKey());
            }
            BigDecimal qty = e.getValue().signum() > 0 ? e.getValue() : s.getSuggestQty();
            if (qty.signum() <= 0) {
                throw new BizException("ecom.error.purchase.noSuggestQty", s.getMaterialCode());
            }
            EcomPurchasePlanItem it = new EcomPurchasePlanItem();
            it.setMaterialId(s.getMaterialId());
            it.setSuggestQty(s.getSuggestQty());
            it.setQty(Decimals.qty(qty));
            it.setSupplierId(s.getSupplierId());
            it.setTaxRate(s.getTaxRate());
            it.setArrivalDate(s.getArrivalDate());
            it.setDemand(demandJson(s.getDemands()));
            items.add(it);
        }
        fillDefaultPrices(items);
        EcomPurchasePlan plan = new EcomPurchasePlan();
        plan.setDocNo(docNoService.next(EcomPurchaseDocNos.PURCHASE_PLAN));
        plan.setPlanDate(LocalDate.now());
        plan.setStatus(PurchasePlanStatus.DRAFT);
        plan.setWarehouseId(req.warehouseId() != null ? checkWarehouse(req.warehouseId()) : defaultWarehouse(suggestions.values()));
        plan.setRemark(req.remark());
        plan.setDeptId(SecurityUtils.getLoginUser().deptId());
        plan.setTotalQty(Decimals.qty(Decimals.sum(items, EcomPurchasePlanItem::getQty)));
        planMapper.insert(plan);
        items.forEach(i -> i.setPlanId(plan.getId()));
        itemMapper.insertBatch(items);
        return plan.getId();
    }

    /** 手工新建（草稿） */
    @Transactional
    public Long create(PurchasePlanRequest req) {
        EcomPurchasePlan plan = new EcomPurchasePlan();
        plan.setDocNo(docNoService.next(EcomPurchaseDocNos.PURCHASE_PLAN));
        plan.setStatus(PurchasePlanStatus.DRAFT);
        plan.setDeptId(SecurityUtils.getLoginUser().deptId());
        List<EcomPurchasePlanItem> items = toItems(req);
        apply(plan, req, items);
        planMapper.insert(plan);
        items.forEach(i -> i.setPlanId(plan.getId()));
        itemMapper.insertBatch(items);
        return plan.getId();
    }

    /** 修改草稿：数量、供应商、单价、税率、到货日期、收货仓库 */
    @Transactional
    public void update(Long id, PurchasePlanRequest req) {
        EcomPurchasePlan plan = requireStatus(id, PurchasePlanStatus.DRAFT);
        List<EcomPurchasePlanItem> items = toItems(req);
        apply(plan, req, items);
        planMapper.update(Wrappers.<EcomPurchasePlan>lambdaUpdate()
                .set(EcomPurchasePlan::getPlanDate, plan.getPlanDate())
                .set(EcomPurchasePlan::getWarehouseId, plan.getWarehouseId())
                .set(EcomPurchasePlan::getRemark, plan.getRemark())
                .set(EcomPurchasePlan::getTotalQty, plan.getTotalQty())
                .set(EcomPurchasePlan::getUpdateTime, LocalDateTime.now())
                .eq(EcomPurchasePlan::getId, id)
                .eq(EcomPurchasePlan::getStatus, PurchasePlanStatus.DRAFT));
        DetailLines.replace(itemMapper, EcomPurchasePlanItem::getPlanId, id, items, EcomPurchasePlanItem::setPlanId, itemMapper::insertBatch);
    }

    private void apply(EcomPurchasePlan plan, PurchasePlanRequest req, List<EcomPurchasePlanItem> items) {
        plan.setPlanDate(req.planDate());
        plan.setWarehouseId(req.warehouseId() == null ? null : checkWarehouse(req.warehouseId()));
        plan.setRemark(req.remark());
        plan.setTotalQty(Decimals.qty(Decimals.sum(items, EcomPurchasePlanItem::getQty)));
    }

    private List<EcomPurchasePlanItem> toItems(PurchasePlanRequest req) {
        Set<Long> materialIds = req.items().stream().map(PurchasePlanRequest.Item::materialId).collect(Collectors.toSet());
        if (materialIds.size() < req.items().size()) {
            throw new BizException("ecom.error.purchase.duplicateMaterial");
        }
        Map<Long, WmsMaterial> materials = materialService.getEnabled(materialIds);
        for (Long mid : materialIds) {
            if (!materials.containsKey(mid)) {
                throw new BizException("ecom.error.materialNotFoundOrDisabled", mid);
            }
        }
        req.items().stream().map(PurchasePlanRequest.Item::supplierId).filter(Objects::nonNull).distinct().forEach(supplierService::get);
        return req.items().stream().map(r -> {
            EcomPurchasePlanItem it = new EcomPurchasePlanItem();
            it.setMaterialId(r.materialId());
            it.setQty(Decimals.qty(r.qty()));
            it.setSuggestQty(r.suggestQty());
            it.setSupplierId(r.supplierId());
            it.setPrice(r.price());
            it.setTaxRate(r.taxRate());
            it.setArrivalDate(r.arrivalDate());
            it.setDemand(r.demand());
            it.setRemark(r.remark());
            return it;
        }).toList();
    }

    /** 删除草稿 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            requireStatus(id, PurchasePlanStatus.DRAFT);
        }
        itemMapper.delete(Wrappers.<EcomPurchasePlanItem>lambdaQuery().in(EcomPurchasePlanItem::getPlanId, ids));
        planMapper.deleteByIds(ids);
    }

    /** 审核：草稿 → 已审核 */
    public void approve(Long id) {
        requireStatus(id, PurchasePlanStatus.DRAFT);
        StatusTransitions.change(planMapper, EcomPurchasePlan::getId, id, EcomPurchasePlan::getStatus, PurchasePlanStatus.DRAFT,
                PurchasePlanStatus.APPROVED,
                w -> w.set(EcomPurchasePlan::getApproveBy, SecurityUtils.getUserId()).set(EcomPurchasePlan::getApproveTime, LocalDateTime.now()),
                "ecom.error.purchase.planStatusChanged");
    }

    /** 反审核：已审核且还没有生成采购订单 → 草稿 */
    public void unapprove(Long id) {
        EcomPurchasePlan plan = requireStatus(id, PurchasePlanStatus.APPROVED);
        long total = itemMapper.selectCount(Wrappers.<EcomPurchasePlanItem>lambdaQuery().eq(EcomPurchasePlanItem::getPlanId, id));
        if (openItems(id).size() < total) {
            throw new BizException("ecom.error.purchase.planHasOrders", plan.getDocNo());
        }
        StatusTransitions.change(planMapper, EcomPurchasePlan::getId, id, EcomPurchasePlan::getStatus, PurchasePlanStatus.APPROVED,
                PurchasePlanStatus.DRAFT,
                w -> w.set(EcomPurchasePlan::getApproveBy, null).set(EcomPurchasePlan::getApproveTime, null),
                "ecom.error.purchase.planStatusChanged");
    }

    /** 关闭：已审核、已下单的计划不再生成采购订单（已生成的采购订单不受影响） */
    public void close(Long id) {
        EcomPurchasePlan plan = require(id);
        if (plan.getStatus() != PurchasePlanStatus.APPROVED && plan.getStatus() != PurchasePlanStatus.ORDERED) {
            throw new BizException("ecom.error.purchase.planCannotClose", plan.getDocNo());
        }
        StatusTransitions.change(planMapper, EcomPurchasePlan::getId, id, EcomPurchasePlan::getStatus, plan.getStatus(),
                PurchasePlanStatus.CLOSED, "ecom.error.purchase.planStatusChanged");
    }

    /**
     * 生成采购订单：还没有生成（或生成的订单已被删除）的明细按供应商拆单，每个供应商一张采购订单（草稿，来源「采购计划」）。
     * 单价取明细单价 → 供应商价格（阶梯价）→ 最近进价 → 参考成本，税率取明细税率 → 供应商价格的税率 → 默认税率；
     * 交货日期取明细的到货日期，订单交货日期为其中最早的一个
     *
     * @return 生成的采购订单号
     */
    @Transactional
    public List<String> generateOrders(Long id) {
        EcomPurchasePlan plan = require(id);
        if (plan.getStatus() != PurchasePlanStatus.APPROVED && plan.getStatus() != PurchasePlanStatus.ORDERED) {
            throw new BizException("ecom.error.purchase.planNotApproved", plan.getDocNo());
        }
        List<EcomPurchasePlanItem> open = openItems(id);
        if (open.isEmpty()) {
            throw new BizException("ecom.error.purchase.planAllOrdered", plan.getDocNo());
        }
        for (EcomPurchasePlanItem it : open) {
            if (it.getSupplierId() == null) {
                WmsMaterial m = materialService.get(it.getMaterialId());
                throw new BizException("ecom.error.purchase.supplierMissing", m.getCode());
            }
        }
        Map<Long, List<EcomPurchasePlanItem>> bySupplier = open.stream()
                .collect(Collectors.groupingBy(EcomPurchasePlanItem::getSupplierId, LinkedHashMap::new, Collectors.toList()));
        String remark = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.purchase.orderRemark", plan.getDocNo());
        LocalDate today = LocalDate.now();
        List<String> orderNos = new ArrayList<>();
        for (Map.Entry<Long, List<EcomPurchasePlanItem>> e : bySupplier.entrySet()) {
            List<EcomPurchasePlanItem> lines = e.getValue();
            List<PricingViews.DefaultPrice> defaults = pricingService.defaults(OrderKind.PURCHASE, e.getKey(),
                    lines.stream().map(EcomPurchasePlanItem::getMaterialId).toList(), lines.stream().map(EcomPurchasePlanItem::getQty).toList(), today);
            List<OrderItemRequest> orderItems = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                EcomPurchasePlanItem it = lines.get(i);
                PricingViews.DefaultPrice d = defaults.get(i);
                BigDecimal price = it.getPrice() != null ? it.getPrice() : d.price();
                if (price == null) {
                    throw new BizException("ecom.error.purchase.priceMissing", materialService.get(it.getMaterialId()).getCode());
                }
                BigDecimal taxRate = it.getTaxRate() != null ? it.getTaxRate() : d.taxRate();
                orderItems.add(new OrderItemRequest(it.getMaterialId(), it.getQty(), price, taxRate, it.getArrivalDate(), it.getRemark()));
            }
            LocalDate delivery = lines.stream().map(EcomPurchasePlanItem::getArrivalDate).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            OrderRequest req = new OrderRequest(e.getKey(), today, delivery, plan.getWarehouseId(), remark, orderItems);
            Long orderId = orderService.createDraft(OrderKind.PURCHASE, req, OrderSource.PURCHASE_PLAN);
            ScmOrder order = orderService.find(orderId);
            orderNos.add(order.getOrderNo());
            itemMapper.update(Wrappers.<EcomPurchasePlanItem>lambdaUpdate()
                    .set(EcomPurchasePlanItem::getOrderId, orderId)
                    .set(EcomPurchasePlanItem::getOrderNo, order.getOrderNo())
                    .set(EcomPurchasePlanItem::getUpdateTime, LocalDateTime.now())
                    .in(EcomPurchasePlanItem::getId, lines.stream().map(EcomPurchasePlanItem::getId).toList()));
        }
        if (plan.getStatus() == PurchasePlanStatus.APPROVED) {
            StatusTransitions.change(planMapper, EcomPurchasePlan::getId, id, EcomPurchasePlan::getStatus, PurchasePlanStatus.APPROVED,
                    PurchasePlanStatus.ORDERED, "ecom.error.purchase.planStatusChanged");
        }
        return orderNos;
    }

    // ---------------------------------------------------------------- 内部

    private EcomPurchasePlan requireStatus(Long id, PurchasePlanStatus status) {
        EcomPurchasePlan plan = require(id);
        if (plan.getStatus() != status) {
            throw new BizException(status == PurchasePlanStatus.DRAFT ? "ecom.error.purchase.planNotDraft" : "ecom.error.purchase.planNotApproved",
                    plan.getDocNo());
        }
        return plan;
    }

    /** 还没有生成采购订单（或生成的订单已被删除）的明细 */
    private List<EcomPurchasePlanItem> openItems(Long planId) {
        List<EcomPurchasePlanItem> items = itemMapper.selectList(Wrappers.<EcomPurchasePlanItem>lambdaQuery()
                .eq(EcomPurchasePlanItem::getPlanId, planId).orderByAsc(EcomPurchasePlanItem::getId));
        return items.stream().filter(i -> i.getOrderId() == null || orderService.find(i.getOrderId()) == null).toList();
    }

    /** 没有填写单价的明细按默认供应商取默认采购价（供应商价格 → 最近进价 → 参考成本） */
    private void fillDefaultPrices(List<EcomPurchasePlanItem> items) {
        Map<Long, List<EcomPurchasePlanItem>> bySupplier = new HashMap<>();
        for (EcomPurchasePlanItem it : items) {
            if (it.getSupplierId() != null) {
                bySupplier.computeIfAbsent(it.getSupplierId(), k -> new ArrayList<>()).add(it);
            }
        }
        LocalDate today = LocalDate.now();
        bySupplier.forEach((supplierId, lines) -> {
            List<PricingViews.DefaultPrice> defaults = pricingService.defaults(OrderKind.PURCHASE, supplierId,
                    lines.stream().map(EcomPurchasePlanItem::getMaterialId).toList(), lines.stream().map(EcomPurchasePlanItem::getQty).toList(), today);
            for (int i = 0; i < lines.size(); i++) {
                EcomPurchasePlanItem it = lines.get(i);
                it.setPrice(defaults.get(i).price());
                if (it.getTaxRate() == null) {
                    it.setTaxRate(defaults.get(i).taxRate());
                }
            }
        });
    }

    /** 需求店铺中最多的默认发货仓（按在线商品计数） */
    private static Long defaultWarehouse(Collection<PurchaseSuggestRow> rows) {
        return rows.stream().flatMap(r -> r.getDemands().stream()).map(PurchaseSuggestRow.Demand::warehouseId).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private Long checkWarehouse(Long warehouseId) {
        warehouseService.get(warehouseId);
        return warehouseId;
    }

    /** 需求构成 JSON，超过字段长度时只保留需求量大的店铺 */
    private String demandJson(List<PurchaseSuggestRow.Demand> demands) {
        if (demands.isEmpty()) {
            return null;
        }
        List<PurchaseSuggestRow.Demand> list = new ArrayList<>(demands);
        list.sort(Comparator.comparing(PurchaseSuggestRow.Demand::qty).reversed());
        String json = objectMapper.writeValueAsString(list);
        while (json.length() > DEMAND_MAX && list.size() > 1) {
            list.remove(list.size() - 1);
            json = objectMapper.writeValueAsString(list);
        }
        return json.length() > DEMAND_MAX ? null : json;
    }
}
