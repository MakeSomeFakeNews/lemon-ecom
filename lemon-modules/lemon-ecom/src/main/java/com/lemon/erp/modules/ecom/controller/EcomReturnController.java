package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.service.EcomReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 退货退款 /ecom/return
 */
@RestController
@RequestMapping("/ecom/return")
@RequiredArgsConstructor
public class EcomReturnController {

    private final EcomReturnService returnService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:return:list')")
    public R<PageResult<EcomReturn>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) Long storeId, @RequestParam(required = false) ReturnType returnType,
                                          @RequestParam(required = false) EcomReturnStatus status, @RequestParam(required = false) String keyword) {
        return R.ok(returnService.page(current, size, storeId, returnType, status, keyword));
    }

    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:return:list')")
    public R<Map<String, Long>> statusCounts(@RequestParam(required = false) Long storeId, @RequestParam(required = false) ReturnType returnType,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(returnService.statusCounts(storeId, returnType, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:return:list')")
    public R<EcomReturn> get(@PathVariable Long id) {
        return R.ok(returnService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:return:add')")
    @OperLog(title = "退货退款", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody ReturnRequest req) {
        return R.ok(returnService.create(req));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('ecom:return:receive')")
    @OperLog(title = "退货入库", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> receive(@PathVariable Long id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        returnService.receive(id, date);
        return R.ok();
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("@ss.hasPerm('ecom:return:refund')")
    @OperLog(title = "退货退款", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> refund(@PathVariable Long id) {
        returnService.refund(id);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('ecom:return:add')")
    @OperLog(title = "退货退款", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> close(@PathVariable Long id) {
        returnService.close(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:return:remove')")
    @OperLog(title = "退货退款", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        returnService.delete(ids);
        return R.ok();
    }
}
