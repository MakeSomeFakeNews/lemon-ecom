package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.dto.FromReplenishRequest;
import com.lemon.erp.modules.ecom.dto.GenerateInboundRequest;
import com.lemon.erp.modules.ecom.dto.InboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipmentPlanRequest;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlan;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlanItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomShipmentPlanItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomShipmentPlanMapper;
import com.lemon.erp.modules.ecom.model.ShipmentPlanStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 发货计划（FBA / 平台仓补货计划）：草稿（手工新建，或由补货建议勾选生成）→ 确认 → 生成头程货件（可分多次、拆成多个货件，
 * 平台 SKU 按组成展开为物料：单品 = 件数 × 多件装，组合 = 件数 × 各组件用量）→ 计划件数全部生成后为已生成货件；
 * 生成的货件取消 / 删除后计划回到已确认，可以重新生成
 */
@Service
@RequiredArgsConstructor
public class EcomShipmentPlanService {

    /** 发货计划按制单人部门 + 制单人做数据权限 */
    public static final DataPermissionRule PLAN_RULE = new DataPermissionRule("ecom_shipment_plan", "dept_id", "create_by");

    private final EcomShipmentPlanMapper planMapper;
    private final EcomShipmentPlanItemMapper itemMapper;
    private final EcomStoreService storeService;
    private final EcomListingService listingService;
    private final EcomBundleService bundleService;
    private final EcomInboundService inboundService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;

    @DataPermission(table = "ecom_shipment_plan", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<EcomShipmentPlan> page(long current, long size, Long storeId, ShipmentPlanStatus status, String keyword) {
        Page<EcomShipmentPlan> page = Page.of(current, size);
        planMapper.selectPageList(page, storeId, status, keyword);
        return PageResult.of(page);
    }

    public EcomShipmentPlan detail(Long id) {
        EcomShipmentPlan p = DataScopes.require(PLAN_RULE, () -> planMapper.selectDetail(id), "ecom.error.catalog.planNotFound");
        p.setItems(itemMapper.selectByPlanId(id));
        p.setInbounds(inboundService.listByPlan(id));
        return p;
    }

    private EcomShipmentPlan require(Long id) {
        return DataScopes.require(PLAN_RULE, () -> planMapper.selectById(id), "ecom.error.catalog.planNotFound");
    }

    @Transactional
    public Long create(ShipmentPlanRequest req) {
        EcomShipmentPlan p = new EcomShipmentPlan();
        p.setPlanNo(docNoService.next(EcomCatalogDocNos.SHIPMENT_PLAN));
        p.setStatus(ShipmentPlanStatus.DRAFT);
        LoginUser user = SecurityUtils.getLoginUser();
        p.setDeptId(user == null ? null : user.deptId());
        List<EcomShipmentPlanItem> items = apply(p, req);
        planMapper.insert(p);
        DetailLines.replace(itemMapper, EcomShipmentPlanItem::getPlanId, p.getId(), items, EcomShipmentPlanItem::setPlanId, itemMapper::insertBatch);
        return p.getId();
    }

    /**
     * 由补货建议勾选生成发货计划（草稿）：补货建议页「生成发货计划」调用
     */
    @Transactional
    public Long fromReplenish(FromReplenishRequest req) {
        List<ShipmentPlanRequest.Item> items = req.items().stream().map(l -> new ShipmentPlanRequest.Item(l.listingId(), l.qty(), null)).toList();
        return create(new ShipmentPlanRequest(req.storeId(), req.fromWarehouseId(), req.toWarehouseId(), null, req.remark(), items));
    }

    @Transactional
    public void update(Long id, ShipmentPlanRequest req) {
        EcomShipmentPlan p = require(id);
        if (p.getStatus() != ShipmentPlanStatus.DRAFT) {
            throw new BizException("ecom.error.catalog.planNotDraft", p.getPlanNo());
        }
        List<EcomShipmentPlanItem> items = apply(p, req);
        planMapper.updateById(p);
        planMapper.update(Wrappers.<EcomShipmentPlan>lambdaUpdate().set(EcomShipmentPlan::getPlanDate, p.getPlanDate())
                .set(EcomShipmentPlan::getRemark, p.getRemark()).eq(EcomShipmentPlan::getId, id));
        DetailLines.replace(itemMapper, EcomShipmentPlanItem::getPlanId, id, items, EcomShipmentPlanItem::setPlanId, itemMapper::insertBatch);
    }

    /** 校验并填写表头，返回明细（同一平台 SKU 多行合并）：目标仓默认店铺的平台仓，发货仓默认店铺的默认发货仓 */
    private List<EcomShipmentPlanItem> apply(EcomShipmentPlan p, ShipmentPlanRequest req) {
        EcomStore store = storeService.requireEnabled(req.storeId());
        Long to = req.toWarehouseId() != null ? req.toWarehouseId() : store.getPlatformWarehouseId();
        Long from = req.fromWarehouseId() != null ? req.fromWarehouseId() : store.getWarehouseId();
        if (to == null) {
            throw new BizException("ecom.error.storePlatformWarehouseMissing", store.getName());
        }
        if (from == null) {
            throw new BizException("ecom.error.catalog.fromWarehouseRequired");
        }
        if (from.equals(to)) {
            throw new BizException("ecom.error.inboundSameWarehouse");
        }
        warehouseService.get(from);
        warehouseService.get(to);
        Map<Long, EcomListing> listings = listingService.mapByIds(req.items().stream().map(ShipmentPlanRequest.Item::listingId).collect(Collectors.toSet()));
        Map<Long, EcomShipmentPlanItem> merged = new LinkedHashMap<>();
        for (ShipmentPlanRequest.Item r : req.items()) {
            EcomListing l = listings.get(r.listingId());
            if (l == null) {
                throw new BizException("ecom.error.listingNotFound");
            }
            if (!l.getStoreId().equals(store.getId())) {
                throw new BizException("ecom.error.catalog.listingStoreDiffers", l.getSellerSku());
            }
            EcomShipmentPlanItem it = merged.get(l.getId());
            if (it == null) {
                it = new EcomShipmentPlanItem();
                it.setListingId(l.getId());
                it.setSellerSku(l.getSellerSku());
                it.setQty(BigDecimal.ZERO);
                it.setRemark(r.remark());
                merged.put(l.getId(), it);
            }
            it.setQty(Decimals.qty(it.getQty().add(r.qty())));
        }
        p.setStoreId(store.getId());
        p.setFromWarehouseId(from);
        p.setToWarehouseId(to);
        p.setPlanDate(req.planDate());
        p.setRemark(req.remark());
        p.setTotalQty(Decimals.qty(merged.values().stream().map(EcomShipmentPlanItem::getQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
        return new ArrayList<>(merged.values());
    }

    /** 确认：各平台 SKU 都要已映射物料（单品）或有组成（组合） */
    @Transactional
    public void confirm(Long id) {
        EcomShipmentPlan p = require(id);
        if (p.getStatus() != ShipmentPlanStatus.DRAFT) {
            throw new BizException("ecom.error.catalog.planNotDraft", p.getPlanNo());
        }
        List<EcomShipmentPlanItem> items = itemMapper.selectByPlanId(id);
        Map<Long, List<BundleComponent>> comps = bundleService.componentsOf(items.stream().map(EcomShipmentPlanItem::getListingId).toList());
        for (EcomShipmentPlanItem it : items) {
            if (!comps.containsKey(it.getListingId())) {
                throw new BizException("ecom.error.catalog.planSkuUnmapped", it.getSellerSku());
            }
        }
        StatusTransitions.change(planMapper, EcomShipmentPlan::getId, id, EcomShipmentPlan::getStatus, ShipmentPlanStatus.DRAFT,
                ShipmentPlanStatus.CONFIRMED, "ecom.error.catalog.planStatusChanged");
    }

    /** 退回草稿：已确认且还没有生成货件 */
    @Transactional
    public void unconfirm(Long id) {
        EcomShipmentPlan p = requireDetail(id);
        if (p.getStatus() != ShipmentPlanStatus.CONFIRMED || Decimals.nz(p.getShippedQty()).signum() > 0) {
            throw new BizException("ecom.error.catalog.planStatusNotAllow", p.getPlanNo());
        }
        StatusTransitions.change(planMapper, EcomShipmentPlan::getId, id, EcomShipmentPlan::getStatus, ShipmentPlanStatus.CONFIRMED,
                ShipmentPlanStatus.DRAFT, "ecom.error.catalog.planStatusChanged");
    }

    /** 取消：草稿、已确认且没有未取消的货件 */
    @Transactional
    public void cancel(Long id) {
        EcomShipmentPlan p = requireDetail(id);
        if ((p.getStatus() != ShipmentPlanStatus.DRAFT && p.getStatus() != ShipmentPlanStatus.CONFIRMED) || Decimals.nz(p.getShippedQty()).signum() > 0) {
            throw new BizException("ecom.error.catalog.planStatusNotAllow", p.getPlanNo());
        }
        StatusTransitions.changeFromAny(planMapper, EcomShipmentPlan::getId, id, EcomShipmentPlan::getStatus,
                Set.of(ShipmentPlanStatus.DRAFT, ShipmentPlanStatus.CONFIRMED), ShipmentPlanStatus.CANCELLED, "ecom.error.catalog.planStatusChanged");
    }

    /** 删除：草稿、已取消 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomShipmentPlan p = require(id);
            if (p.getStatus() != ShipmentPlanStatus.DRAFT && p.getStatus() != ShipmentPlanStatus.CANCELLED) {
                throw new BizException("ecom.error.catalog.planStatusNotAllow", p.getPlanNo());
            }
            if (!inboundService.listByPlan(id).isEmpty()) {
                throw new BizException("ecom.error.catalog.planHasInbound", p.getPlanNo());
            }
            itemMapper.delete(Wrappers.<EcomShipmentPlanItem>lambdaQuery().eq(EcomShipmentPlanItem::getPlanId, id));
        }
        planMapper.deleteByIds(ids);
    }

    private EcomShipmentPlan requireDetail(Long id) {
        require(id);
        return planMapper.selectDetail(id);
    }

    /**
     * 生成头程货件（草稿）：每个 shipment 一张货件，平台 SKU 按组成展开为物料行（带平台 SKU、在线商品、计划明细）；
     * 各平台 SKU 生成的件数合计不能超过计划未生成的件数。shipments 为空时把全部未生成的件数生成一张货件
     *
     * @return 生成的货件 ID
     */
    @Transactional
    public List<Long> generate(Long id, GenerateInboundRequest req) {
        EcomShipmentPlan p = requireDetail(id);
        if (p.getStatus() != ShipmentPlanStatus.CONFIRMED) {
            throw new BizException("ecom.error.catalog.planNotConfirmed", p.getPlanNo());
        }
        List<EcomShipmentPlanItem> items = itemMapper.selectByPlanId(id);
        Map<Long, EcomShipmentPlanItem> byId = items.stream().collect(Collectors.toMap(EcomShipmentPlanItem::getId, i -> i));
        Map<Long, BigDecimal> remaining = new HashMap<>();
        items.forEach(i -> remaining.put(i.getId(), Decimals.nz(i.getQty()).subtract(Decimals.nz(i.getShippedQty()))));
        List<GenerateInboundRequest.Shipment> shipments = req == null || req.shipments() == null || req.shipments().isEmpty()
                ? List.of(new GenerateInboundRequest.Shipment(null, null, items.stream().filter(i -> remaining.get(i.getId()).signum() > 0)
                .map(i -> new GenerateInboundRequest.Line(i.getId(), remaining.get(i.getId()))).toList()))
                : req.shipments();
        Map<Long, List<BundleComponent>> comps = bundleService.componentsOf(items.stream().map(EcomShipmentPlanItem::getListingId).toList());
        List<Long> ids = new ArrayList<>();
        for (GenerateInboundRequest.Shipment sh : shipments) {
            if (sh.lines().isEmpty()) {
                throw new BizException("ecom.error.catalog.planNothingToGenerate", p.getPlanNo());
            }
            List<InboundRequest.Item> lines = new ArrayList<>();
            for (GenerateInboundRequest.Line line : sh.lines()) {
                EcomShipmentPlanItem it = byId.get(line.planItemId());
                if (it == null) {
                    throw new BizException("ecom.error.catalog.planItemNotFound");
                }
                BigDecimal rest = remaining.get(it.getId());
                if (line.qty().compareTo(rest) > 0) {
                    throw new BizException("ecom.error.catalog.planQtyExceeds", it.getSellerSku(), rest.stripTrailingZeros().toPlainString());
                }
                remaining.put(it.getId(), rest.subtract(line.qty()));
                List<BundleComponent> c = comps.get(it.getListingId());
                if (c == null || c.isEmpty()) {
                    throw new BizException("ecom.error.catalog.planSkuUnmapped", it.getSellerSku());
                }
                for (BundleComponent bc : c) {
                    lines.add(new InboundRequest.Item(bc.materialId(), it.getSellerSku(), Decimals.qty(line.qty().multiply(bc.qty())), it.getRemark(),
                            it.getListingId(), it.getId()));
                }
            }
            InboundRequest ir = new InboundRequest(p.getStoreId(), sh.shipmentId(), p.getFromWarehouseId(), p.getToWarehouseId(), sh.channelId(), null, null,
                    p.getPlanDate() == null || p.getPlanDate().isBefore(LocalDate.now()) ? null : p.getPlanDate(), null, null, null, null, null, null, null,
                    p.getRemark(), lines);
            ids.add(inboundService.create(ir, id));
        }
        refreshStatus(id);
        return ids;
    }

    /** 按已生成货件的件数更新状态：全部生成为已生成货件，否则（货件取消 / 删除后）回到已确认 */
    private void refreshStatus(Long id) {
        EcomShipmentPlan p = planMapper.selectDetail(id);
        if (p == null || (p.getStatus() != ShipmentPlanStatus.CONFIRMED && p.getStatus() != ShipmentPlanStatus.GENERATED)) {
            return;
        }
        boolean all = itemMapper.selectByPlanId(id).stream().allMatch(i -> Decimals.nz(i.getShippedQty()).compareTo(Decimals.nz(i.getQty())) >= 0);
        ShipmentPlanStatus st = all ? ShipmentPlanStatus.GENERATED : ShipmentPlanStatus.CONFIRMED;
        if (st != p.getStatus()) {
            planMapper.update(Wrappers.<EcomShipmentPlan>lambdaUpdate().set(EcomShipmentPlan::getStatus, st).eq(EcomShipmentPlan::getId, id));
        }
    }

    /** 生成的货件取消 / 删除后重新计算计划状态（同一事务中） */
    @EventListener
    public void onInboundReleased(EcomInboundService.InboundReleasedEvent e) {
        refreshStatus(e.planId());
    }
}
