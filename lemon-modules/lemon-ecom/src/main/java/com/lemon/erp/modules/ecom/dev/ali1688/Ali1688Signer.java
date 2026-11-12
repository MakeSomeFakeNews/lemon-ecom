package com.lemon.erp.modules.ecom.dev.ali1688;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1688 开放平台的请求签名 _aop_signature（公开文档「签名规则」）：
 * <ol>
 *   <li>签名因子 urlPath = {@code param2/1/{namespace}/{name}/{appKey}}（请求地址中 openapi/ 之后的部分）；</li>
 *   <li>除 _aop_signature 外的全部请求参数（含 access_token、_aop_timestamp）各自拼成「key + value」，按字典序排序后依次拼接；</li>
 *   <li>urlPath + 拼接结果，以 AppSecret 为密钥做 HMAC-SHA1，结果转为大写十六进制。</li>
 * </ol>
 */
public final class Ali1688Signer {

    private Ali1688Signer() {
    }

    public static String sign(String urlPath, Map<String, String> params, String appSecret) {
        List<String> pairs = new ArrayList<>();
        params.forEach((k, v) -> {
            if (!"_aop_signature".equals(k) && v != null) {
                pairs.add(k + v);
            }
        });
        Collections.sort(pairs);
        StringBuilder sb = new StringBuilder(urlPath);
        pairs.forEach(sb::append);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8))).toUpperCase(Locale.ROOT);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
