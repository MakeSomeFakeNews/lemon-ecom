package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688AccountRequest;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Account;
import com.lemon.erp.modules.ecom.dev.service.Ali1688AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * 1688 采购账号 /ecom/1688/account：维护、OAuth 授权地址、刷新令牌、清除授权。授权回调见 {@link Ali1688OAuthController}
 */
@RestController
@RequestMapping("/ecom/1688/account")
@RequiredArgsConstructor
public class Ali1688AccountController {

    private static final String TITLE = "1688 账号"; // i18n-ignore 操作日志标题存库

    private final Ali1688AccountService accountService;

    @Value("${lemon.ecom.public-url:}")
    private String publicUrl;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('ecom:ali1688Account:list', 'ecom:ali1688Order:create', 'ecom:ali1688Offer:list')")
    public R<List<Ecom1688Account>> list() {
        return R.ok(accountService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:list')")
    public R<Ecom1688Account> get(@PathVariable Long id) {
        return R.ok(accountService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Validated @RequestBody Ali1688AccountRequest req) {
        return R.ok(accountService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody Ali1688AccountRequest req) {
        accountService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        accountService.delete(ids);
        return R.ok();
    }

    /** OAuth 授权页地址：回调地址 {后端地址}/api/ecom/oauth/callback/alibaba1688（需在 1688 开放平台应用中登记） */
    @PostMapping("/{id}/authorize-url")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    public R<String> authorizeUrl(@PathVariable Long id, HttpServletRequest request) {
        String base = publicUrl == null || publicUrl.isBlank()
                ? ServletUriComponentsBuilder.fromContextPath(request).build().toUriString() : publicUrl.replaceAll("/+$", "");
        return R.ok(accountService.authorizeUrl(id, base + Ali1688OAuthController.CALLBACK_PATH));
    }

    /** 刷新令牌（同时检验授权是否有效） */
    @PostMapping("/{id}/refresh")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> refresh(@PathVariable Long id) {
        accountService.refresh(id);
        return R.ok();
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Account:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> revoke(@PathVariable Long id) {
        accountService.revoke(id);
        return R.ok();
    }
}
