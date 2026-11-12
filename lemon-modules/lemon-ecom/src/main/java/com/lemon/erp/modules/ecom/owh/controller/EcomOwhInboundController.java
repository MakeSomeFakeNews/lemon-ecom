package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.IdsRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundUpdateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.VoidRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhInboundService;
import com.lemon.erp.common.exception.BizException;
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
import java.util.List;
import java.util.Map;

/**
 * 海外仓入库单 /ecom/owh/inbound：从头程货件 / 在途调拨单生成、修改装箱、推送、同步收货、取消
 */
@RestController
@RequestMapping("/ecom/owh/inbound")
@RequiredArgsConstructor
public class EcomOwhInboundController {

    private final EcomOwhInboundService inboundService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:list')")
    public R<PageResult<EcomOwhInbound>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              Long accountId, Long warehouseId, OwhInboundStatus status, String keyword) {
        return R.ok(inboundService.page(current, size, accountId, warehouseId, status, keyword));
    }

    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:list')")
    public R<Map<String, Long>> statusCounts(Long accountId, Long warehouseId, String keyword) {
        return R.ok(inboundService.statusCounts(accountId, warehouseId, keyword));
    }

    /** 可生成入库单的来源 */
    @GetMapping("/sources")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:add')")
    public R<List<Map<String, Object>>> sources(String keyword) {
        return R.ok(inboundService.sources(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:list')")
    public R<EcomOwhInbound> get(@PathVariable Long id) {
        return R.ok(inboundService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:add')")
    @OperLog(title = "海外仓入库单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody InboundCreateRequest req) {
        return R.ok(inboundService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:add')")
    @OperLog(title = "海外仓入库单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody InboundUpdateRequest req) {
        inboundService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/push")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:push')")
    @OperLog(title = "海外仓入库单推送", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> push(@PathVariable Long id) {
        inboundService.push(id);
        return R.ok();
    }

    /** 同步收货（批量），返回 入库单 ID → 失败原因 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:sync')")
    @OperLog(title = "海外仓入库单同步", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> sync(@Validated @RequestBody IdsRequest req) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            try {
                inboundService.sync(id);
            } catch (BizException e) {
                failures.put(String.valueOf(id), e.getMessage());
            }
        }
        return R.ok(failures);
    }

    /** 标记已作废：服务商后台已作废（没有取消接口的服务商） */
    @PostMapping("/{id}/void")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:cancel')")
    @OperLog(title = "海外仓入库单作废", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> markVoided(@PathVariable Long id, @Validated @RequestBody(required = false) VoidRequest req) {
        inboundService.markVoided(id, req == null ? null : req.reason());
        return R.ok();
    }

    /** 取消：草稿直接取消，已推送的调用服务商取消接口 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:owhInbound:cancel')")
    @OperLog(title = "海外仓入库单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        inboundService.cancel(id);
        return R.ok();
    }
}
