package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews.PlatformGroup;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomChannelHubService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 渠道接入 /ecom/channel：全部店铺的授权与同步状态总览、同步日志（只读）
 */
@RestController
@RequestMapping("/ecom/channel")
@RequiredArgsConstructor
public class EcomChannelController {

    private final EcomChannelHubService hubService;
    private final EcomSyncService syncService;

    /** 按平台分组的店铺授权状态、环境、各类同步最近一次的结果与近 24 小时失败次数 */
    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('ecom:channel:list')")
    public R<List<PlatformGroup>> overview() {
        return R.ok(hubService.overview());
    }

    /** 全部店铺的同步日志：按店铺、类型、成功（1）/ 失败（0）筛选 */
    @GetMapping("/logs")
    @PreAuthorize("@ss.hasPerm('ecom:channel:list')")
    public R<PageResult<EcomSyncLog>> logs(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long storeId, @RequestParam(required = false) SyncType syncType,
                                           @RequestParam(required = false) Integer success) {
        return R.ok(syncService.logs(current, size, storeId, syncType, success));
    }
}
