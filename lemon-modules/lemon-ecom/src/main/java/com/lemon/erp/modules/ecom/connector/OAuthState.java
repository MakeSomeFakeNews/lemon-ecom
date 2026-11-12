package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * OAuth 授权的 state 参数：租户、店铺、回调地址、过期时间，签名防篡改。
 * 回调接口不需要登录，只凭 state 确定是哪个租户的哪个店铺
 */
@Component
@RequiredArgsConstructor
public class OAuthState {

    /** 授权链接有效期（毫秒） */
    static final long TTL_MILLIS = 30 * 60 * 1000L;

    private final CredentialCipher cipher;

    public record Payload(Long tenantId, Long storeId, String redirectUri) {
    }

    public String create(Long tenantId, Long storeId, String redirectUri) {
        long expire = System.currentTimeMillis() + TTL_MILLIS;
        String body = tenantId + "|" + storeId + "|" + expire + "|" + redirectUri;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + cipher.sign(encoded);
    }

    public Payload parse(String state) {
        if (state == null || state.indexOf('.') < 0) {
            throw new BizException("ecom.error.oauthStateInvalid");
        }
        String encoded = state.substring(0, state.indexOf('.'));
        String sig = state.substring(state.indexOf('.') + 1);
        if (!MessageDigest.isEqual(cipher.sign(encoded).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException("ecom.error.oauthStateInvalid");
        }
        String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\|", 4);
        if (parts.length != 4 || Long.parseLong(parts[2]) < System.currentTimeMillis()) {
            throw new BizException("ecom.error.oauthStateExpired");
        }
        return new Payload(Long.valueOf(parts[0]), Long.valueOf(parts[1]), parts[3]);
    }
}
