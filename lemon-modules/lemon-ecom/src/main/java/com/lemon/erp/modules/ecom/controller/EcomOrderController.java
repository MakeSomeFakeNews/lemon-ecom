package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.OrderEditRequest;
import com.lemon.erp.modules.ecom.dto.OrderImportRow;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.dto.OutboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
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
import java.util.Map;

/**
 * 平台订单 /ecom/order
 */
@RestController
@RequestMapping("/ecom/order")
@RequiredArgsConstructor
public class EcomOrderController {

    private final EcomOrderService orderService;
    private final EcomLogisticsService logisticsService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<PageResult<EcomOrder>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, OrderQuery query) {
        return R.ok(orderService.page(current, size, query));
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
}
