package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomFeeMapping;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.service.EcomFeeMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 结算费用类型映射 /ecom/fee-mapping
 */
@RestController
@RequestMapping("/ecom/fee-mapping")
@RequiredArgsConstructor
public class EcomFeeMappingController {

    private final EcomFeeMappingService mappingService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:feeMapping:list')")
    public R<PageResult<EcomFeeMapping>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) EcomPlatform platform, @RequestParam(required = false) SettlementItemType itemType,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(mappingService.page(current, size, platform, itemType, keyword));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:feeMapping:add')")
    @OperLog(title = "费用映射", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody EcomFeeMapping m) {
        return R.ok(mappingService.create(m));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:feeMapping:edit')")
    @OperLog(title = "费用映射", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomFeeMapping m) {
        mappingService.update(id, m);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:feeMapping:edit')")
    @OperLog(title = "费用映射", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        mappingService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:feeMapping:remove')")
    @OperLog(title = "费用映射", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        mappingService.delete(ids);
        return R.ok();
    }
}
