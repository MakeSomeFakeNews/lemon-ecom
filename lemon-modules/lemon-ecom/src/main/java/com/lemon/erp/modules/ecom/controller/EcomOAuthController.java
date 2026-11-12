package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * 平台 OAuth 授权回调 /ecom/oauth/callback/{platform}：不需要登录（平台浏览器跳转过来），凭签名的 state 确定租户和店铺；
 * 返回一个简单页面告诉卖家授权结果，可以关闭页面回到 ERP
 */
@RestController
@RequestMapping("/ecom/oauth")
@RequiredArgsConstructor
public class EcomOAuthController {

    private final EcomSyncService syncService;

    @GetMapping(value = "/callback/{platform}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(@PathVariable String platform, @RequestParam Map<String, String> params) {
        String title;
        String detail;
        boolean ok;
        try {
            EcomStore store = syncService.oauthCallback(platform, params.get("state"), params);
            ok = true;
            title = Messages.get("ecom.text.oauthSuccess");
            detail = Messages.get("ecom.text.oauthSuccessDetail", store.getName());
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
        return ResponseEntity.ok().contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8)).body(html);
    }
}
