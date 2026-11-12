package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.SyncResult;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.service.EcomChannelHubService;
import com.lemon.erp.modules.ecom.service.EcomListingSyncService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
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

import com.lemon.erp.modules.ecom.dto.ConnectorView;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.model.SyncType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台店铺 /ecom/store
 */
@RestController
@RequestMapping("/ecom/store")
@RequiredArgsConstructor
public class EcomStoreController {

    private final EcomStoreService storeService;
    private final EcomSyncService syncService;
    private final EcomListingSyncService listingSyncService;
    private final EcomChannelHubService hubService;

    /** 后端对外地址（含 /api），平台 OAuth 回调到这里；为空时按当前请求推算（反向代理后应配置） */
    @Value("${lemon.ecom.public-url:}")
    private String publicUrl;

    /** 登录即可访问：已有渠道接口的平台、授权方式、授权字段（名称按当前语言）、是否有沙箱、支持的能力、文档地址 */
    @GetMapping("/connectors")
    public R<List<ConnectorView>> connectors() {
        return R.ok(syncService.connectors());
    }

    /** OAuth 授权页地址（卖家在新窗口打开，同意后平台回调本系统） */
    @PostMapping("/{id}/authorize-url")
    @PreAuthorize("@ss.hasPerm('ecom:store:auth')")
    public R<String> authorizeUrl(@PathVariable Long id, HttpServletRequest request) {
        EcomStore store = storeService.get(id);
        String base = publicUrl == null || publicUrl.isBlank()
                ? ServletUriComponentsBuilder.fromContextPath(request).build().toUriString() : publicUrl.replaceAll("/+$", "");
        return R.ok(syncService.authorizeUrl(id, base + "/ecom/oauth/callback/" + store.getPlatform().name().toLowerCase()));
    }

    /** 同步日志 */
    @GetMapping("/sync-logs")
    @PreAuthorize("@ss.hasPerm('ecom:store:list')")
    public R<PageResult<EcomSyncLog>> logs(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long storeId, @RequestParam(required = false) SyncType syncType,
                                           @RequestParam(required = false) Integer success) {
        return R.ok(syncService.logs(current, size, storeId, syncType, success));
    }

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:store:list')")
    public R<PageResult<EcomStore>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword, @RequestParam(required = false) EcomPlatform platform,
                                         @RequestParam(required = false) EnableStatus status) {
        PageResult<EcomStore> page = storeService.page(current, size, keyword, platform, status);
        hubService.fillAuthStatus(page.records());
        return R.ok(page);
    }

    /** 登录即可访问：各页面选择店铺 */
    @GetMapping("/options")
    public R<List<EcomStore>> options() {
        return R.ok(storeService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:store:list')")
    public R<EcomStore> get(@PathVariable Long id) {
        EcomStore store = storeService.detail(id);
        hubService.fillAuthStatus(List.of(store));
        return R.ok(store);
    }

    /** 店铺已填写的授权字段名（不返回值），编辑表单据此显示「已设置」「已授权」 */
    @GetMapping("/{id}/credential-keys")
    @PreAuthorize("@ss.hasPerm('ecom:store:list')")
    public R<List<String>> credentialKeys(@PathVariable Long id) {
        return R.ok(syncService.credentialKeys(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:store:add')")
    @OperLog(title = "平台店铺", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated(Create.class) @RequestBody EcomStore store) {
        return R.ok(storeService.create(store));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:store:edit')")
    @OperLog(title = "平台店铺", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody EcomStore store) {
        storeService.update(id, store);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:store:edit')")
    @OperLog(title = "平台店铺", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        storeService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}/credentials")
    @PreAuthorize("@ss.hasPerm('ecom:store:edit')")
    @OperLog(title = "平台店铺", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> clearCredentials(@PathVariable Long id) {
        storeService.clearCredentials(id);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:store:remove')")
    @OperLog(title = "平台店铺", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        storeService.delete(ids);
        return R.ok();
    }

    /** 测试渠道授权 */
    @PostMapping("/{id}/test")
    @PreAuthorize("@ss.hasPerm('ecom:store:sync')")
    public R<Void> test(@PathVariable Long id) {
        syncService.test(id);
        return R.ok();
    }

    /** 手工同步订单，since 为空时从上次同步时间开始 */
    @PostMapping("/{id}/sync")
    @PreAuthorize("@ss.hasPerm('ecom:store:sync')")
    @OperLog(title = "平台店铺", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<SyncResult> sync(@PathVariable Long id,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return R.ok(syncService.sync(id, since));
    }

    /** 手工从平台拉取在线商品：按 店铺 + 平台 SKU 新增或更新，不覆盖已有的物料映射 */
    @PostMapping("/{id}/sync-listings")
    @PreAuthorize("@ss.hasPerm('ecom:store:syncListing')")
    @OperLog(title = "平台店铺", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<SyncResult> syncListings(@PathVariable Long id) {
        return R.ok(listingSyncService.syncListings(id));
    }

    /** 手工回传可售库存，all = true 时全部回传，否则只回传和上次不同的 */
    @PostMapping("/{id}/push-inventory")
    @PreAuthorize("@ss.hasPerm('ecom:store:push')")
    @OperLog(title = "平台店铺", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<SyncResult> pushInventory(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean all) {
        return R.ok(listingSyncService.pushInventory(id, all));
    }
}
