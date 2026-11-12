package com.lemon.erp.modules.ecom.carrier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物流商签名常用的摘要与编码
 */
public final class CarrierSigns {

    private CarrierSigns() {
    }

    /** MD5，小写十六进制 */
    public static String md5Lower(String data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HTTP Basic 认证头的值 */
    public static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + "&" + password).getBytes(StandardCharsets.UTF_8));
    }

    /** URL 编码的查询串（按 map 的顺序，跳过空值） */
    public static String query(Map<String, String> params) {
        return params.entrySet().stream().filter(e -> e.getValue() != null)
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue())).collect(Collectors.joining("&"));
    }

    public static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
