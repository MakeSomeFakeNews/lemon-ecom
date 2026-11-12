package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomRemoteArea;
import com.lemon.erp.modules.ecom.service.EcomRemoteAreaService;
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
 * 偏远地区邮编段 /ecom/remote-area
 */
@RestController
@RequestMapping("/ecom/remote-area")
@RequiredArgsConstructor
public class EcomRemoteAreaController {

    private final EcomRemoteAreaService remoteAreaService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:remoteArea:list')")
    public R<PageResult<EcomRemoteArea>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String country, @RequestParam(required = false) String keyword) {
        return R.ok(remoteAreaService.page(current, size, country, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:remoteArea:list')")
    public R<EcomRemoteArea> get(@PathVariable Long id) {
        return R.ok(remoteAreaService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:remoteArea:add')")
    @OperLog(title = "偏远地区", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody EcomRemoteArea a) {
        return R.ok(remoteAreaService.create(a));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:remoteArea:edit')")
    @OperLog(title = "偏远地区", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody EcomRemoteArea a) {
        remoteAreaService.update(id, a);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:remoteArea:remove')")
    @OperLog(title = "偏远地区", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        remoteAreaService.delete(ids);
        return R.ok();
    }
}
