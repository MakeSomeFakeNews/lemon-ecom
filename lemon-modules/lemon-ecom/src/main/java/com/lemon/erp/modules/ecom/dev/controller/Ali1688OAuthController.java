package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Account;
import com.lemon.erp.modules.ecom.dev.service.Ali1688AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/**
 * 1688 OAuth 授权回调 /ecom/oauth/callback/alibaba1688：与店铺授权回调同一路径前缀（安全配置已对 GET /ecom/oauth/callback/** 放行，
 * 比店铺回调的 {platform} 更具体，优先匹配），凭签名的 state 确定租户和账号，返回授权结果页面
 */
@RestController
@RequiredArgsConstructor
public class Ali1688OAuthController {

    public static final String CALLBACK_PATH = "/ecom/oauth/callback/alibaba1688";

    private final Ali1688AccountService accountService;

    @GetMapping(value = CALLBACK_PATH, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(@RequestParam(required = false) String state, @RequestParam(required = false) String code) {
        String title;
        String detail;
        boolean ok;
        try {
            Ecom1688Account a = accountService.oauthCallback(state, code);
            ok = true;
            title = Messages.get("ecom.text.dev.oauthSuccess");
            detail = Messages.get("ecom.text.dev.oauthSuccessDetail", a.getName());
        } catch (BizException e) {
            ok = false;
            title = Messages.get("ecom.text.dev.oauthFailed");
            detail = e.getMessage();
        }
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\">"
                + "<title>" + HtmlUtils.htmlEscape(title) + "</title></head>"
                + "<body style=\"font-family:system-ui,sans-serif;text-align:center;padding:64px 16px;color:#333\">"
                + "<h2 style=\"color:" + (ok ? "#52c41a" : "#f5222d") + "\">" + HtmlUtils.htmlEscape(title) + "</h2>"
                + "<p>" + HtmlUtils.htmlEscape(detail == null ? "" : detail) + "</p></body></html>";
        return ResponseEntity.ok().contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
    }
}
