package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.IdsRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ReceiveRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.TransferRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransfer;
import com.lemon.erp.modules.ecom.owh.model.OwhTransferStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhTransferService;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 海外仓调拨 /ecom/owh/transfer：海外仓之间、海外仓 ↔ 本地仓的调拨单（新建、修改、提交、同步、确认收货、取消）
 */
@RestController
@RequestMapping("/ecom/owh/transfer")
@RequiredArgsConstructor
public class EcomOwhTransferController {

    private final EcomOwhTransferService transferService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:list')")
    public R<PageResult<EcomOwhTransfer>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               Long warehouseId, OwhTransferStatus status, String keyword) {
        return R.ok(transferService.page(current, size, warehouseId, status, keyword));
    }

    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:list')")
    public R<Map<String, Long>> statusCounts(Long warehouseId, String keyword) {
        return R.ok(transferService.statusCounts(warehouseId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:list')")
    public R<EcomOwhTransfer> get(@PathVariable Long id) {
        return R.ok(transferService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:add')")
    @OperLog(title = "海外仓调拨单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody TransferRequest req) {
        return R.ok(transferService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:add')")
    @OperLog(title = "海外仓调拨单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody TransferRequest req) {
        transferService.update(id, req);
        return R.ok();
    }

    /** 提交，返回提交后的状态（OUT_PUSHED / IN_TRANSIT） */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:push')")
    @OperLog(title = "海外仓调拨单提交", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<OwhTransferStatus> submit(@PathVariable Long id) {
        return R.ok(transferService.submit(id));
    }

    /** 同步（批量），返回 调拨单 ID → 失败原因 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:sync')")
    @OperLog(title = "海外仓调拨单同步", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> sync(@Validated @RequestBody IdsRequest req) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            try {
                transferService.sync(id);
            } catch (BizException e) {
                failures.put(String.valueOf(id), e.getMessage());
            }
        }
        return R.ok(failures);
    }

    /** 确认收货（调入仓是本地仓）：items 为各物料实收数量（为空全部收到），与调拨数量不一致的记为收货差异 */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:receive')")
    @OperLog(title = "海外仓调拨单收货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> receive(@PathVariable Long id, @Validated @RequestBody(required = false) ReceiveRequest req) {
        java.util.Map<Long, java.math.BigDecimal> actual = new java.util.LinkedHashMap<>();
        if (req != null && req.items() != null) {
            req.items().forEach(l -> actual.merge(l.materialId(), l.qty(), java.math.BigDecimal::add));
        }
        transferService.receive(id, req == null ? null : req.receiveDate(), actual, req == null ? null : req.reason());
        return R.ok();
    }

    /** 取消，返回取消后的状态（CANCELLED；服务商截单中时仍为 OUT_PUSHED） */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:owhTransfer:cancel')")
    @OperLog(title = "海外仓调拨单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<OwhTransferStatus> cancel(@PathVariable Long id) {
        return R.ok(transferService.cancel(id));
    }
}
