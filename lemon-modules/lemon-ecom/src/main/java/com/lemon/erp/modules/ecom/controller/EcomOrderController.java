package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.HoldRequest;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.dto.OrderEditRequest;
import com.lemon.erp.modules.ecom.dto.OrderImportRow;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.dto.OutboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipRequest;
import com.lemon.erp.modules.ecom.dto.SplitRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomOrderOpsService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 平台订单 /ecom/order：查询、导入、直接出库 / 发货（不经过包裹的简单情况），以及审单作业（审单规则、审核、缺货重查、挂起、合并 / 拆分）
 */
@RestController
@RequestMapping("/ecom/order")
@RequiredArgsConstructor
public class EcomOrderController {

    private final EcomOrderService orderService;
    private final EcomOrderOpsService opsService;
    private final EcomLogisticsService logisticsService;
    private final EcomSyncService syncService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<PageResult<EcomOrder>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, OrderQuery query) {
        return R.ok(orderService.page(current, size, query));
    }

    /** 各处理状态的订单数（状态标签页）：{ 处理状态: 数量 }，条件与列表相同、不含处理状态 */
    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<Map<String, Long>> statusCounts(OrderQuery query) {
        return R.ok(orderService.statusCounts(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<EcomOrder> get(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:order:import')")
    @OperLog(title = "平台订单", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<OrderImportRow> req) {
        return R.ok(orderService.importRows(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:order:edit')")
    @OperLog(title = "平台订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody OrderEditRequest req) {
        orderService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/rematch")
    @PreAuthorize("@ss.hasPerm('ecom:order:outbound')")
    @OperLog(title = "平台订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> rematch(@PathVariable Long id) {
        orderService.rematch(id);
        return R.ok();
    }

    /** 批量出库，返回失败的订单及原因（平台订单号 → 原因） */
    @PostMapping("/outbound")
    @PreAuthorize("@ss.hasPerm('ecom:order:outbound')")
    @OperLog(title = "平台订单出库", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> outbound(@RequestBody OutboundRequest req) {
        return R.ok(orderService.outbound(req));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("@ss.hasPerm('ecom:order:ship')")
    @OperLog(title = "平台订单发货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> ship(@PathVariable Long id, @Validated @RequestBody ShipRequest req) {
        BigDecimal estimated = null;
        if (req.shippingCost() == null && req.channelId() != null && req.weight() != null) {
            estimated = logisticsService.estimate(req.channelId(), orderService.detail(id).getShipCountry(), req.weight());
        }
        orderService.ship(id, req, estimated);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:order:cancel')")
    @OperLog(title = "平台订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    // ---------------------------------------------------------------- 审单作业

    /** 批量审核：返回未通过的订单及原因（缺货的订单状态已改为缺货） */
    @PostMapping("/audit")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单审核", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> audit(@Validated @RequestBody IdsRequest req) {
        return R.ok(opsService.audit(req.ids()));
    }

    /** 重新检查库存（缺货、部分未分配包裹的订单） */
    @PostMapping("/recheck")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单审核", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> recheck(@Validated @RequestBody IdsRequest req) {
        return R.ok(opsService.recheck(req.ids()));
    }

    /** 重新应用审单规则 */
    @PostMapping("/apply-rules")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单审核", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> applyRules(@Validated @RequestBody IdsRequest req) {
        return R.ok(opsService.applyRules(req.ids()));
    }

    @PostMapping("/hold")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单挂起", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> hold(@Validated @RequestBody HoldRequest req) {
        opsService.hold(req);
        return R.ok();
    }

    @PostMapping("/unhold")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单挂起", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> unhold(@Validated @RequestBody IdsRequest req) {
        opsService.unhold(req.ids());
        return R.ok();
    }

    /** 合并订单为一个包裹，返回包裹 ID */
    @PostMapping("/merge")
    @PreAuthorize("@ss.hasPerm('ecom:order:merge')")
    @OperLog(title = "平台订单合并", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Long> merge(@Validated @RequestBody IdsRequest req) {
        return R.ok(opsService.merge(req.ids()));
    }

    /** 拆分订单为多个包裹，返回包裹 ID */
    @PostMapping("/{id}/split")
    @PreAuthorize("@ss.hasPerm('ecom:order:merge')")
    @OperLog(title = "平台订单拆分", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<Long>> split(@PathVariable Long id, @Validated @RequestBody SplitRequest req) {
        return R.ok(opsService.split(id, req));
    }

    /** 重新回传发货（平台回传失败后手工重试，失败时提示原因） */
    @PostMapping("/{id}/ship-confirm")
    @PreAuthorize("@ss.hasPerm('ecom:order:ship')")
    @OperLog(title = "平台订单回传发货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> shipConfirm(@PathVariable Long id) {
        EcomOrder o = orderService.detail(id);
        if (o.getTrackingNo() == null || o.getShipTime() == null) {
            throw new BizException("ecom.error.ops.orderNotShipped", o.getPlatformOrderNo());
        }
        syncService.retryShipment(id);
        return R.ok();
    }
}
