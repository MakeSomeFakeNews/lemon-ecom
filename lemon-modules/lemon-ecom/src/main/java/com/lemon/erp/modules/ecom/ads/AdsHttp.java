package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 下载广告报告文件（二进制，常见为 gzip 压缩的 JSON）：JSON 接口仍用 {@link HttpJson}。
 * 报告地址一般是带签名的对象存储地址，不能带授权请求头，headers 由调用方决定；文件超过 {@value #MAX_BYTES} 字节时报错
 */
public final class AdsHttp {

    /** 解压后的报告最大 200 MB */
    static final long MAX_BYTES = 200L * 1024 * 1024;
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private AdsHttp() {
    }

    /** GET 下载文件，gzip（按文件头 1f 8b 判断）时自动解压 */
    public static byte[] download(String url, Map<String, String> headers, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).GET();
        headers.forEach(b::header);
        try {
            HttpResponse<byte[]> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = resp.body() == null ? new byte[0] : resp.body();
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                Map<String, List<String>> h = new LinkedHashMap<>();
                resp.headers().map().forEach((k, v) -> h.put(k.toLowerCase(), v));
                throw new ConnectorHttpException(platform, resp.statusCode(), h, HttpJson.abbreviate(new String(body, java.nio.charset.StandardCharsets.UTF_8)));
            }
            return gunzipIfNeeded(body);
        } catch (IOException e) {
            throw new BizException("ecom.error.connectorIoError", platform, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        }
    }

    /** 下载并解析为 JSON（报告文件一般是 JSON 数组） */
    public static JsonNode downloadJson(String url, Map<String, String> headers, String platform) {
        byte[] data = download(url, headers, platform);
        if (data.length == 0) {
            return HttpJson.MAPPER.createArrayNode();
        }
        return HttpJson.MAPPER.readTree(data);
    }

    static byte[] gunzipIfNeeded(byte[] data) throws IOException {
        if (data.length < 2 || (data[0] & 0xff) != 0x1f || (data[1] & 0xff) != 0x8b) {
            return data;
        }
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_BYTES) {
                    throw new BizException("ecom.error.ads.reportTooLarge");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
