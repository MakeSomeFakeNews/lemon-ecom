package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688OrderRequest;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.CreateResult;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.OrderPreview;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SyncSummary;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.TraceStep;
import com.lemon.erp.modules.ecom.dev.dto.DevRemarkRequest;
import com.lemon.erp.modules.ecom.dev.dto.IdList;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus;
import com.lemon.erp.modules.ecom.dev.service.Ali1688OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 1688 采购单 /ecom/1688/order：列表、详情（含明细、物流轨迹）、采购订单一键下单（预览 / 下单）、同步、支付链接、取消
 */
@RestController
@RequestMapping("/ecom/1688/order")
@RequiredArgsConstructor
public class Ali1688OrderController {

    private static final String TITLE = "1688 采购单"; // i18n-ignore 操作日志标题存库

    private final Ali1688OrderService orderService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:list')")
    public R<PageResult<Ecom1688Order>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Ali1688OrderStatus status, @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Long scmOrderId) {
        return R.ok(orderService.page(current, size, status, keyword, scmOrderId));
    }

    /** 详情：order（含 items）+ trace（物流轨迹） */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:list')")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        Ecom1688Order o = orderService.detail(id);
        List<TraceStep> trace = orderService.traceOf(o);
        o.setTrace(null);
        return R.ok(Map.of("order", o, "trace", trace));
    }

    /** 采购订单生成的 1688 采购单 */
    @GetMapping("/by-po/{scmOrderId}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:list')")
    public R<List<Ecom1688Order>> byPo(@PathVariable Long scmOrderId) {
        return R.ok(orderService.byScmOrder(scmOrderId));
    }

    /** 一键下单预览 */
    @GetMapping("/preview/{scmOrderId}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:create')")
    public R<OrderPreview> preview(@PathVariable Long scmOrderId) {
        return R.ok(orderService.preview(scmOrderId));
    }

    /** 一键下单到 1688（按卖家拆单） */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:create')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<CreateResult> create(@Validated @RequestBody Ali1688OrderRequest req) {
        return R.ok(orderService.createFromPo(req.scmOrderId(), req.accountId(), req.message()));
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:sync')")
    public R<Ecom1688Order> sync(@PathVariable Long id) {
        Ecom1688Order o = orderService.sync(id);
        o.setTrace(null);
        return R.ok(o);
    }

    /** 批量同步：ids 为空时同步全部未结束的 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:sync')")
    public R<SyncSummary> syncMany(@RequestBody(required = false) IdList req) {
        return R.ok(orderService.syncMany(req == null ? null : req.ids()));
    }

    /** 支付链接（同一账号的待付款订单） */
    @PostMapping("/pay-url")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:create')")
    public R<String> payUrl(@Validated @RequestBody IdList req) {
        return R.ok(orderService.payUrl(req.ids()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Order:cancel')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> cancel(@PathVariable Long id, @Validated @RequestBody(required = false) DevRemarkRequest req) {
        orderService.cancel(id, req == null ? null : req.remark());
        return R.ok();
    }
}
