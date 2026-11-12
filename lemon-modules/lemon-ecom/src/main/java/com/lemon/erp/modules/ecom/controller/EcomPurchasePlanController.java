package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.PurchasePlanFromReplenish;
import com.lemon.erp.modules.ecom.dto.PurchasePlanRequest;
import com.lemon.erp.modules.ecom.entity.EcomPurchasePlan;
import com.lemon.erp.modules.ecom.model.PurchasePlanStatus;
import com.lemon.erp.modules.ecom.service.EcomPurchasePlanService;
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
 * 采购计划 /ecom/purchase-plan
 */
@RestController
@RequestMapping("/ecom/purchase-plan")
@RequiredArgsConstructor
public class EcomPurchasePlanController {

    private final EcomPurchasePlanService planService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:list')")
    public R<PageResult<EcomPurchasePlan>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) PurchasePlanStatus status,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(planService.page(current, size, status, keyword));
    }

    /** 详情含 items（arrivedQty 为生成的采购订单中本物料的已入库数量） */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:list')")
    public R<EcomPurchasePlan> get(@PathVariable Long id) {
        return R.ok(planService.detail(id));
    }

    /** 由补货建议（本地仓采购）勾选的物料生成，返回计划 ID */
    @PostMapping("/from-replenish")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:add')")
    @OperLog(title = "采购计划", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> fromReplenish(@Validated @RequestBody PurchasePlanFromReplenish req) {
        return R.ok(planService.createFromReplenish(req));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:add')")
    @OperLog(title = "采购计划", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody PurchasePlanRequest req) {
        return R.ok(planService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:edit')")
    @OperLog(title = "采购计划", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody PurchasePlanRequest req) {
        planService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:remove')")
    @OperLog(title = "采购计划", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        planService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:approve')")
    @OperLog(title = "采购计划审核", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> approve(@PathVariable Long id) {
        planService.approve(id);
        return R.ok();
    }

    @PostMapping("/{id}/unapprove")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:approve')")
    @OperLog(title = "采购计划反审核", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> unapprove(@PathVariable Long id) {
        planService.unapprove(id);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:edit')")
    @OperLog(title = "采购计划关闭", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> close(@PathVariable Long id) {
        planService.close(id);
        return R.ok();
    }

    /** 按供应商拆单生成采购订单（草稿），返回生成的订单号 */
    @PostMapping("/{id}/orders")
    @PreAuthorize("@ss.hasPerm('ecom:purchasePlan:order')")
    @OperLog(title = "采购计划生成采购订单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<List<String>> generateOrders(@PathVariable Long id) {
        return R.ok(planService.generateOrders(id));
    }
}
