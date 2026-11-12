package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.FromReplenishRequest;
import com.lemon.erp.modules.ecom.dto.GenerateInboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipmentPlanRequest;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlan;
import com.lemon.erp.modules.ecom.model.ShipmentPlanStatus;
import com.lemon.erp.modules.ecom.service.EcomShipmentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发货计划 /ecom/shipment-plan
 */
@RestController
@RequestMapping("/ecom/shipment-plan")
@RequiredArgsConstructor
public class EcomShipmentPlanController {

    private final EcomShipmentPlanService planService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:list')")
    public R<PageResult<EcomShipmentPlan>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) Long storeId, @RequestParam(required = false) ShipmentPlanStatus status,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(planService.page(current, size, storeId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:list')")
    public R<EcomShipmentPlan> get(@PathVariable Long id) {
        return R.ok(planService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:add')")
    @OperLog(title = "发货计划", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody ShipmentPlanRequest req) {
        return R.ok(planService.create(req));
    }

    /** 由补货建议勾选生成发货计划（草稿）：body { storeId, toWarehouseId?, fromWarehouseId?, remark?, items: [{ listingId, qty }] }，返回计划 ID */
    @PostMapping("/from-replenish")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:add')")
    @OperLog(title = "发货计划", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> fromReplenish(@Validated @RequestBody FromReplenishRequest req) {
        return R.ok(planService.fromReplenish(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:edit')")
    @OperLog(title = "发货计划", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody ShipmentPlanRequest req) {
        planService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:remove')")
    @OperLog(title = "发货计划", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        planService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:confirm')")
    @OperLog(title = "发货计划", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> confirm(@PathVariable Long id) {
        planService.confirm(id);
        return R.ok();
    }

    /** 退回草稿（已确认、还没有生成货件） */
    @PostMapping("/{id}/unconfirm")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:confirm')")
    @OperLog(title = "发货计划", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> unconfirm(@PathVariable Long id) {
        planService.unconfirm(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:confirm')")
    @OperLog(title = "发货计划", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        planService.cancel(id);
        return R.ok();
    }

    /** 生成头程货件（草稿，可拆成多个货件），返回货件 ID */
    @PostMapping("/{id}/generate")
    @PreAuthorize("@ss.hasPerm('ecom:shipmentPlan:generate')")
    @OperLog(title = "发货计划", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<List<Long>> generate(@PathVariable Long id, @Validated @RequestBody(required = false) GenerateInboundRequest req) {
        return R.ok(planService.generate(id, req));
    }
}
