package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.InboundRequest;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 头程货件 /ecom/inbound
 */
@RestController
@RequestMapping("/ecom/inbound")
@RequiredArgsConstructor
public class EcomInboundController {

    private final EcomInboundService inboundService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:inbound:list')")
    public R<PageResult<EcomInbound>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long storeId, @RequestParam(required = false) InboundStatus status,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(inboundService.page(current, size, storeId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:list')")
    public R<EcomInbound> get(@PathVariable Long id) {
        return R.ok(inboundService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:inbound:add')")
    @OperLog(title = "头程货件", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody InboundRequest req) {
        return R.ok(inboundService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:edit')")
    @OperLog(title = "头程货件", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody InboundRequest req) {
        inboundService.update(id, req);
        return R.ok();
    }

    /** 已发运 / 已签收后补录物流与费用 */
    @PutMapping("/{id}/costs")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:edit')")
    @OperLog(title = "头程货件", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> updateCosts(@PathVariable Long id, @Validated @RequestBody InboundRequest req) {
        inboundService.updateCosts(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:remove')")
    @OperLog(title = "头程货件", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        inboundService.delete(ids);
        return R.ok();
    }

    /** 提交发运：返回 true 表示进入审批 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:ship')")
    @OperLog(title = "头程货件发运", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(inboundService.submit(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:receive')")
    @OperLog(title = "头程货件签收", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> receive(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate) {
        inboundService.receive(id, receiveDate);
        return R.ok();
    }

    /** 装箱（整体替换）：各箱尺寸、重量与箱内平台 SKU 件数 */
    @PutMapping("/{id}/boxes")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:box')")
    @OperLog(title = "头程货件", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> saveBoxes(@PathVariable Long id, @org.springframework.validation.annotation.Validated @RequestBody
                             com.lemon.erp.modules.ecom.dto.InboundBoxRequest req) {
        inboundService.saveBoxes(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:inbound:edit')")
    @OperLog(title = "头程货件", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        inboundService.cancel(id);
        return R.ok();
    }
}
