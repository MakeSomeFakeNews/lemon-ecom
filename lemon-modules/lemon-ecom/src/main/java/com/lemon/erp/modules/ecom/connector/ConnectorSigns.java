package com.lemon.erp.modules.ecom.connector;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * 各平台签名常用的摘要算法与参数编码
 */
public final class ConnectorSigns {

    private ConnectorSigns() {
    }

    /** HMAC-SHA256，小写十六进制 */
    public static String hmacSha256Hex(String secret, String data) {
        return HexFormat.of().formatHex(hmac("HmacSHA256", secret, data));
    }

    /** HMAC-SHA256，Base64 */
    public static String hmacSha256Base64(String secret, String data) {
        return java.util.Base64.getEncoder().encodeToString(hmac("HmacSHA256", secret, data));
    }

    /** MD5，大写十六进制 */
    public static String md5Upper(String data) {
        return digest("MD5", data).toUpperCase();
    }

    /** SHA-256，小写十六进制 */
    public static String sha256Hex(String data) {
        return digest("SHA-256", data);
    }

    /** 按键排序后拼成 k1v1k2v2…（TOP 协议、Temu、Lazada 的签名原文） */
    public static String concatSorted(SortedMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append(v == null ? "" : v));
        return sb.toString();
    }

    /** URL 编码的查询串 k1=v1&amp;k2=v2（按 map 的顺序） */
    public static String query(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static byte[] hmac(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String digest(String algorithm, String data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
