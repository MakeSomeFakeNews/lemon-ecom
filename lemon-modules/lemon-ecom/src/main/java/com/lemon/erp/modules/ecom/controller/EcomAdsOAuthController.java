package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.service.EcomAdsAuthService;
import com.lemon.erp.modules.ecom.service.EcomAdsOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 广告接口的 OAuth 授权：授权页地址、授权状态（页面轮询）、选择配置文件，以及不需要登录的回调页
 * （/ecom/oauth/callback/amazon-ads，比渠道授权的 /ecom/oauth/callback/{platform} 更具体，优先匹配）
 */
@RestController
@RequiredArgsConstructor
public class EcomAdsOAuthController {

    private final EcomAdsOAuthService oauthService;
    private final EcomAdsAuthService authService;

    /** 后端对外地址（含 /api），与渠道 OAuth 共用配置；为空时按当前请求推算 */
    @Value("${lemon.ecom.public-url:}")
    private String publicUrl;

    /** 授权页地址（卖家在新窗口打开，同意后回调本系统）；需要先保存应用的 clientId / clientSecret 与区域 */
    @PostMapping("/ecom/ads/oauth/{storeId}/authorize-url")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    public R<String> authorizeUrl(@PathVariable Long storeId, HttpServletRequest request) {
        String base = publicUrl == null || publicUrl.isBlank()
                ? ServletUriComponentsBuilder.fromContextPath(request).build().toUriString() : publicUrl.replaceAll("/+$", "");
        return R.ok(oauthService.authorizeUrl(storeId, base + EcomAdsOAuthService.CALLBACK_PATH));
    }

    /** 店铺的广告授权状态（授权窗口打开后页面轮询，授权完成后提示选择配置文件） */
    @GetMapping("/ecom/ads/oauth/{storeId}/status")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    public R<AdsViews.StoreAuth> status(@PathVariable Long storeId) {
        return R.ok(authService.store(storeId));
    }

    /** 选择广告配置文件 */
    @PutMapping("/ecom/ads/oauth/{storeId}/profile")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    @OperLog(title = "广告授权", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> selectProfile(@PathVariable Long storeId, @RequestBody Map<String, String> body) {
        authService.selectProfile(storeId, body == null ? null : body.get("profileId"));
        return R.ok();
    }

    /** LWA 回调：不需要登录，凭签名的 state 确定租户与店铺；返回简单页面告诉卖家结果 */
    @GetMapping(value = "/ecom/oauth/callback/amazon-ads", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(@RequestParam Map<String, String> params) {
        String title;
        String detail;
        boolean ok;
        try {
            EcomAdsOAuthService.CallbackResult r = oauthService.callback(params);
            ok = true;
            title = Messages.get("ecom.text.oauthSuccess");
            detail = EcomAdsOAuthService.detail(r);
        } catch (BizException e) {
            ok = false;
            title = Messages.get("ecom.text.oauthFailed");
            detail = e.getMessage();
        }
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\">"
                + "<title>" + HtmlUtils.htmlEscape(title) + "</title></head>"
                + "<body style=\"font-family:system-ui,sans-serif;text-align:center;padding:64px 16px;color:#333\">"
                + "<h2 style=\"color:" + (ok ? "#52c41a" : "#f5222d") + "\">" + HtmlUtils.htmlEscape(title) + "</h2>"
                + "<p>" + HtmlUtils.htmlEscape(detail) + "</p></body></html>";
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
    }
}
