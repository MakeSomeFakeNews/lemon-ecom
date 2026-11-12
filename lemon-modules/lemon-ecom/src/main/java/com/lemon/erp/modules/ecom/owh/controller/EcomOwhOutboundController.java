package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.CancelRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.IdsRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.PushRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.PackageOwh;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhOutboundService;
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
 * 海外仓出库 /ecom/owh/outbound：待推送包裹、推送 / 重推、同步出库状态、取消 / 截单；包裹列表与订单详情的海外仓状态
 */
@RestController
@RequestMapping("/ecom/owh/outbound")
@RequiredArgsConstructor
public class EcomOwhOutboundController {

    private final EcomOwhOutboundService outboundService;

    /** 出库单分页：accountId、warehouseId、storeId、status、keyword（出库单号 / 服务商单号 / 运单号 / 包裹号 / 平台订单号） */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:list')")
    public R<PageResult<EcomOwhOutbound>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               Long accountId, Long warehouseId, Long storeId, OwhOutboundStatus status, String keyword) {
        return R.ok(outboundService.page(current, size, accountId, warehouseId, storeId, status, keyword));
    }

    /** 待推送的包裹 */
    @GetMapping("/pending")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:list')")
    public R<PageResult<EcomPackage>> pending(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              Long accountId, Long warehouseId, Long storeId, String keyword) {
        return R.ok(outboundService.pending(current, size, accountId, warehouseId, storeId, keyword));
    }

    /** 各状态数量（含待推送 PENDING） */
    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:list')")
    public R<Map<String, Long>> statusCounts(Long accountId, Long warehouseId, Long storeId, String keyword) {
        return R.ok(outboundService.statusCounts(accountId, warehouseId, storeId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:list')")
    public R<EcomOwhOutbound> get(@PathVariable Long id) {
        return R.ok(outboundService.detail(id));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:list')")
    public R<List<EcomPackageItem>> items(@PathVariable Long id) {
        return R.ok(outboundService.items(id));
    }

    /** 包裹的海外仓出库状态（包裹打单页面用）：ids 为包裹 ID，逗号分隔 */
    @GetMapping("/by-packages")
    @PreAuthorize("@ss.hasPerm('ecom:package:list') or @ss.hasPerm('ecom:owhOutbound:list')")
    public R<List<PackageOwh>> byPackages(@RequestParam List<Long> ids) {
        return R.ok(outboundService.byPackages(ids));
    }

    /** 订单的海外仓出库单（订单详情用） */
    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("@ss.hasPerm('ecom:order:list') or @ss.hasPerm('ecom:owhOutbound:list')")
    public R<List<EcomOwhOutbound>> byOrder(@PathVariable Long orderId) {
        return R.ok(outboundService.byOrder(orderId));
    }

    /** 推送海外仓（包裹 ID），返回 包裹号 → 失败原因 */
    @PostMapping("/push")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:push')")
    @OperLog(title = "推送海外仓出库单", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> push(@Validated @RequestBody PushRequest req) {
        return R.ok(outboundService.push(req));
    }

    /** 同步出库状态（出库单 ID），返回 出库单号 → 失败原因 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:sync')")
    @OperLog(title = "同步海外仓出库单", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> sync(@Validated @RequestBody IdsRequest req) {
        return R.ok(outboundService.sync(req.ids()));
    }

    /** 取消 / 截单，返回取消后的状态（CANCELLED / INTERCEPTING） */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:owhOutbound:cancel')")
    @OperLog(title = "取消海外仓出库单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<OwhOutboundStatus> cancel(@PathVariable Long id, @Validated @RequestBody(required = false) CancelRequest req) {
        return R.ok(outboundService.cancel(id, req));
    }
}
