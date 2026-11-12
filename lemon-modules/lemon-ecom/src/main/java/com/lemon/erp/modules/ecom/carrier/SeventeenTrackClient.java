package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 17TRACK 通用轨迹查询（API v2.2，<a href="https://api.17track.net/zh-cn/doc">api.17track.net</a>），没有对接物流商接口或物流商查不到时兜底：
 * <ul>
 *   <li>认证：请求头 {@code 17token: <API 密钥>}（17TRACK 后台「API 设置」中获取）</li>
 *   <li>查询 POST /gettrackinfo，报文 [{"number": 运单号}]：data.accepted[].track_info.latest_status.status / sub_status 为主状态与子状态，
 *       track_info.tracking.providers[0].events[] 为轨迹（time_iso、location、description）</li>
 *   <li>运单号还没有注册时返回 data.rejected[].error（code -18019902），此时 POST /register 注册（自动识别承运商），本次返回暂无轨迹，
 *       17TRACK 抓取后下次查询即有结果；注册会消耗额度，每个运单号只注册一次</li>
 * </ul>
 * 地址：默认 https://api.17track.net/track/v2.2，订单作业设置中可填写自定义地址（代理或测试）。
 * 说明：按 17TRACK 公开文档实现，尚未用真实密钥联调；需要核对 rejected 的错误码、免费额度的限速（每秒 3 次）与 events 的时区字段
 */
@Component
public class SeventeenTrackClient {

    public static final String NAME = "17TRACK";
    public static final String DEFAULT_URL = "https://api.17track.net/track/v2.2";
    /** 运单号未注册 */
    static final String NOT_REGISTERED = "-18019902";

    /**
     * 查询一个运单号的轨迹；未注册时注册并返回暂无轨迹
     *
     * @param baseUrl 接口地址，为空用默认地址
     */
    public CarrierTrackInfo track(String baseUrl, String apiKey, String trackingNo) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BizException("ecom.error.ord.track17KeyMissing");
        }
        String url = endpoint(baseUrl);
        Map<String, String> headers = Map.of("17token", apiKey.trim());
        JsonNode resp = CarrierHttp.postJson(url + "/gettrackinfo", headers, CarrierHttp.write(List.of(Map.of("number", trackingNo))), NAME);
        check(resp);
        JsonNode data = resp.path("data");
        JsonNode accepted = data.path("accepted");
        if (accepted.isArray() && !accepted.isEmpty()) {
            return parse(accepted.get(0));
        }
        JsonNode rejected = data.path("rejected");
        if (rejected.isArray() && !rejected.isEmpty()) {
            JsonNode err = rejected.get(0).path("error");
            String code = CarrierHttp.text(err, "code");
            if (NOT_REGISTERED.equals(code)) {
                check(CarrierHttp.postJson(url + "/register", headers, CarrierHttp.write(List.of(Map.of("number", trackingNo))), NAME));
                return new CarrierTrackInfo(EcomTrackStatus.NOT_FOUND, List.of());
            }
            throw new BizException("ecom.error.ops.carrierRejected", NAME, CarrierHttp.abbreviate(code + " " + CarrierHttp.text(err, "message")));
        }
        return new CarrierTrackInfo(EcomTrackStatus.NOT_FOUND, List.of());
    }

    static CarrierTrackInfo parse(JsonNode item) {
        JsonNode info = item.path("track_info");
        JsonNode latest = info.path("latest_status");
        List<CarrierTrackEvent> events = new ArrayList<>();
        JsonNode providers = info.path("tracking").path("providers");
        if (providers.isArray()) {
            for (JsonNode p : providers) {
                for (JsonNode e : p.path("events")) {
                    String time = CarrierHttp.text(e, "time_iso");
                    events.add(new CarrierTrackEvent(time != null ? time : CarrierHttp.text(e, "time_utc"), CarrierHttp.text(e, "location"),
                            CarrierHttp.text(e, "description")));
                }
                if (!events.isEmpty()) {
                    break;
                }
            }
        }
        String status = CarrierHttp.text(latest, "status");
        return new CarrierTrackInfo(status == null ? null : TrackStatusNormalizer.from17Track(status, CarrierHttp.text(latest, "sub_status")), events);
    }

    private static void check(JsonNode resp) {
        String code = CarrierHttp.text(resp, "code");
        if (code != null && !"0".equals(code)) {
            String msg = CarrierHttp.text(resp.path("data").path("errors").path(0), "message");
            throw new BizException("ecom.error.ops.carrierRejected", NAME, CarrierHttp.abbreviate(code + " " + (msg == null ? "" : msg)));
        }
    }

    private static String endpoint(String baseUrl) {
        String url = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_URL;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
