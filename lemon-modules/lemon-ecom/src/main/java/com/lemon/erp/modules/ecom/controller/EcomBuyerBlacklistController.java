package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomBuyerBlacklist;
import com.lemon.erp.modules.ecom.model.BlacklistType;
import com.lemon.erp.modules.ecom.service.EcomBuyerBlacklistService;
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
 * 买家黑名单 /ecom/buyer-blacklist
 */
@RestController
@RequestMapping("/ecom/buyer-blacklist")
@RequiredArgsConstructor
public class EcomBuyerBlacklistController {

    private final EcomBuyerBlacklistService blacklistService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:blacklist:list')")
    public R<PageResult<EcomBuyerBlacklist>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) BlacklistType matchType, @RequestParam(required = false) EnableStatus status,
                                                  @RequestParam(required = false) String keyword) {
        return R.ok(blacklistService.page(current, size, matchType, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:blacklist:list')")
    public R<EcomBuyerBlacklist> get(@PathVariable Long id) {
        return R.ok(blacklistService.get(id));
    }

    /** 新增（订单详情「拉黑买家」也调用它） */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:blacklist:add')")
    @OperLog(title = "买家黑名单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody EcomBuyerBlacklist b) {
        return R.ok(blacklistService.create(b));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:blacklist:edit')")
    @OperLog(title = "买家黑名单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody EcomBuyerBlacklist b) {
        blacklistService.update(id, b);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:blacklist:remove')")
    @OperLog(title = "买家黑名单", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        blacklistService.delete(ids);
        return R.ok();
    }
}
