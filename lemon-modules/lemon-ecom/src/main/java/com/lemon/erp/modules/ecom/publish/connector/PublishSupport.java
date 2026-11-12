package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 刊登连接器的公共工具：接口地址、属性值拆分、图片下载与 multipart 上传（虾皮 / TikTok 要先把图片上传到平台图片空间）
 */
public final class PublishSupport {

    /** 单张图片的最大字节数（平台一般限制 2 – 10 MB） */
    static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private PublishSupport() {
    }

    /** 接口根地址：店铺填写了自定义地址时用它，否则按沙箱开关取默认地址 */
    public static String baseUrl(ConnectorContext ctx, String production, String sandbox) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        return ctx.sandbox() && sandbox != null ? sandbox : production;
    }

    /** 属性值拆成多个（英文逗号分隔，去掉空白） */
    public static List<String> values(String v) {
        List<String> out = new ArrayList<>();
        if (v == null) {
            return out;
        }
        for (String s : v.split(",")) {
            if (!s.isBlank()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    /** 以 [ 或 { 开头的属性值按 JSON 提交 */
    public static boolean isJson(String v) {
        if (v == null) {
            return false;
        }
        String s = v.trim();
        return s.startsWith("[") || s.startsWith("{");
    }

    public static JsonNode json(String v) {
        try {
            return HttpJson.MAPPER.readTree(v.trim());
        } catch (RuntimeException e) {
            throw new BizException("ecom.error.pub.attributeJsonInvalid", HttpJson.abbreviate(v));
        }
    }

    /** 金额按两位小数输出为字符串（平台要求字符串金额时） */
    public static String money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** 文件名：URL 最后一段（去掉查询串），没有时为 image.jpg */
    static String fileName(String url) {
        String s = url;
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        int slash = s.lastIndexOf('/');
        String name = slash >= 0 ? s.substring(slash + 1) : s;
        return name.isBlank() ? "image.jpg" : name;
    }

    /** 下载图片（刊登内容中的公网地址） */
    public static byte[] download(String url, String platform) {
        try {
            HttpResponse<byte[]> resp = CLIENT.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.pub.imageDownloadFailed", url, String.valueOf(resp.statusCode()));
            }
            byte[] body = resp.body();
            if (body.length > MAX_IMAGE_BYTES) {
                throw new BizException("ecom.error.pub.imageTooLarge", url);
            }
            return body;
        } catch (IOException | IllegalArgumentException e) {
            throw new BizException("ecom.error.pub.imageDownloadFailed", url, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        }
    }

    /**
     * multipart/form-data 上传一个文件（加上普通字段），返回 JSON；非 2xx 抛出 {@link ConnectorHttpException}
     */
    public static JsonNode multipart(String url, Map<String, String> headers, String fileField, String fileName, byte[] content,
                                     Map<String, String> fields, String platform) {
        String boundary = "----lemon" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, String> f : fields.entrySet()) {
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + f.getKey() + "\"\r\n\r\n" + f.getValue() + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName
                    + "\"\r\nContent-Type: " + contentType(fileName) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
            headers.forEach(b::header);
            HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new ConnectorHttpException(platform, resp.statusCode(), resp.headers().map(), HttpJson.abbreviate(resp.body()));
            }
            return resp.body() == null || resp.body().isBlank() ? HttpJson.MAPPER.createObjectNode() : HttpJson.MAPPER.readTree(resp.body());
        } catch (IOException e) {
            throw new BizException("ecom.error.connectorIoError", platform, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        }
    }

    private static String contentType(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".png")) {
            return "image/png";
        }
        if (n.endsWith(".gif")) {
            return "image/gif";
        }
        if (n.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
