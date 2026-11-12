package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.AuthType;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.CredentialField;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 亚马逊广告 API（Amazon Ads API）：只实现商品推广（Sponsored Products）。
 * <ul>
 *   <li>授权：LWA（Login with Amazon）刷新令牌换取访问令牌（按区域：NA api.amazon.com、EU api.amazon.co.uk、FE api.amazon.co.jp），
 *       约 1 小时有效，缓存在 accessToken / accessTokenExpiresAt，提前 60 秒刷新；accessTokenOwner 记录令牌对应的 clientId + refreshToken 摘要</li>
 *   <li>请求头：Authorization: Bearer、Amazon-Advertising-API-ClientId、Amazon-Advertising-API-Scope（配置文件 profileId，
 *       在「广告授权」中从 GET /v2/profiles 的结果里选择）</li>
 *   <li>结构（SP v3，POST 列表接口 + nextToken 分页，Content-Type / Accept 为 application/vnd.sp*.v3+json）：
 *       /sp/campaigns/list、/sp/adGroups/list、/sp/keywords/list、/sp/targets/list、/sp/negativeKeywords/list（只取启用、暂停的）</li>
 *   <li>报告（v3 异步报告）：POST /reporting/reports 创建（spCampaigns、spAdvertisedProduct、spTargeting、spSearchTerm，timeUnit DAILY，
 *       format GZIP_JSON）→ 轮询 GET /reporting/reports/{reportId} 直到 COMPLETED（FAILED 报错）→ 下载 url 指向的 gzip JSON
 *       （带签名的 S3 地址，不能带授权请求头）。重复的请求平台返回 425 和已有报告 ID，直接沿用。
 *       销售额 / 订单取 7 天归因（sales7d、purchases7d），与广告后台商品推广的默认口径一致</li>
 *   <li>修改：PUT /sp/keywords、/sp/targets（投放竞价、状态）、/sp/adGroups（默认竞价、状态）、/sp/campaigns（日预算、状态），
 *       返回 207 多状态，按 index 对应每项的成功 / 失败；添加否定关键词 POST /sp/negativeKeywords</li>
 * </ul>
 * 地址：自定义地址优先（测试指向模拟服务器，LWA 也走 {自定义地址}/auth/o2/token），否则按沙箱开关取
 * https://advertising-api-test.amazon.com 或按区域取正式地址（NA advertising-api.amazon.com、EU advertising-api-eu.amazon.com、
 * FE advertising-api-fe.amazon.com）。限流 429 由 {@link HttpJson} 退避重试。
 * <p>
 * <b>按亚马逊广告 API 公开文档实现，尚未用真实广告账号联调</b>，需要确认：
 * <ul>
 *   <li>v3 报告各类型可用的列名（尤其 spTargeting 的 keywordId / keyword / targeting / keywordType，spSearchTerm 的 targeting）
 *       以及 campaignStatus、campaignBudgetAmount 在 spCampaigns 中是否可用</li>
 *   <li>重复报告请求 425 的响应体格式（这里按 detail 中的 UUID 取已有报告 ID）</li>
 *   <li>PUT /sp/campaigns 一次最多修改的数量（这里每批 {@value #CAMPAIGN_BATCH} 个，其他每批 {@value #BATCH} 个）</li>
 *   <li>沙箱（advertising-api-test）需要先用 POST /v2/profiles/register 创建测试配置文件，沙箱报告的内容是否为空</li>
 *   <li>品牌推广（SB）、展示型推广（SD）未实现，它们的接口、报告类型与 SP 不同</li>
 * </ul>
 */
@Slf4j
@Component
public class AmazonAdsConnector implements AdsConnector {

    static final String PLATFORM = "Amazon Ads";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://advertising-api.amazon.com",
            "EU", "https://advertising-api-eu.amazon.com",
            "FE", "https://advertising-api-fe.amazon.com");
    static final String SANDBOX = "https://advertising-api-test.amazon.com";
    private static final Map<String, String> LWA = Map.of(
            "NA", "https://api.amazon.com/auth/o2/token",
            "EU", "https://api.amazon.co.uk/auth/o2/token",
            "FE", "https://api.amazon.co.jp/auth/o2/token");
    private static final AdsConnectorInfo INFO = new AdsConnectorInfo(EcomPlatform.AMAZON, AuthType.KEYS, List.of(
            CredentialField.text("region", "ecom.label.ads.cred.region", true),
            CredentialField.text("clientId", "ecom.label.ads.cred.clientId", true),
            CredentialField.secret("clientSecret", "ecom.label.ads.cred.clientSecret"),
            CredentialField.secret("refreshToken", "ecom.label.ads.cred.refreshToken"),
            CredentialField.cached("accessToken", true),
            CredentialField.cached("accessTokenExpiresAt", false),
            CredentialField.cached("accessTokenOwner", false)),
            ENDPOINTS.get("NA"), SANDBOX, EnumSet.allOf(AdsCapability.class), "https://advertising.amazon.com/API/docs/en-us",
            new BigDecimal("0.02"), EnumSet.allOf(EcomAdReportType.class), 31, true);

    static final int BATCH = 100;
    static final int CAMPAIGN_BATCH = 10;
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 200;
    private static final long TOKEN_SKEW_SECONDS = 60;
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final String V_CAMPAIGN = "application/vnd.spCampaign.v3+json";
    private static final String V_AD_GROUP = "application/vnd.spAdGroup.v3+json";
    private static final String V_KEYWORD = "application/vnd.spKeyword.v3+json";
    private static final String V_TARGET = "application/vnd.spTargetingClause.v3+json";
    private static final String V_NEGATIVE = "application/vnd.spNegativeKeyword.v3+json";
    private static final String V_REPORT = "application/vnd.createasyncreportrequest.v3+json";

    /** 报告轮询：首次等待、最大间隔、总等待上限（毫秒），测试中可调 */
    long pollInitialMillis = 5_000;
    long pollMaxIntervalMillis = 60_000;
    long pollTimeoutMillis = 30 * 60_000L;
    AdsSleeper sleeper = AdsSleeper.DEFAULT;

    @Override
    public AdsConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 授权

    @Override
    public void ensureToken(AdsContext ctx) {
        StoreCredentials c = ctx.credentials();
        String owner = ConnectorSigns.sha256Hex(c.require("clientId") + "|" + c.require("refreshToken")).substring(0, 16);
        String expires = c.get("accessTokenExpiresAt");
        if (c.get("accessToken") != null && owner.equals(c.get("accessTokenOwner")) && expires != null
                && ConnectorJson.parseLong(expires, 0) - TOKEN_SKEW_SECONDS * 1000 > System.currentTimeMillis()) {
            return;
        }
        String form = "grant_type=refresh_token&refresh_token=" + enc(c.require("refreshToken")) + "&client_id=" + enc(c.require("clientId"))
                + "&client_secret=" + enc(c.require("clientSecret"));
        JsonNode resp = HttpJson.postForm(tokenUrl(ctx), Map.of(), form, PLATFORM);
        String token = HttpJson.text(resp, "access_token");
        if (token == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        long expiresIn = resp.path("expires_in").asLong(3600);
        Map<String, String> changes = new HashMap<>();
        changes.put("accessToken", token);
        changes.put("accessTokenExpiresAt", String.valueOf(System.currentTimeMillis() + expiresIn * 1000));
        changes.put("accessTokenOwner", owner);
        ctx.updateCredentials(changes);
    }

    /** LWA 令牌地址：有自定义地址时为 {自定义地址}/auth/o2/token，否则按区域 */
    static String tokenUrl(AdsContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride() + "/auth/o2/token";
        }
        String url = LWA.get(region(ctx));
        if (url == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region(ctx));
        }
        return url;
    }

    /** 业务接口地址：自定义地址 &gt; 沙箱 &gt; 区域正式地址 */
    static String endpoint(AdsContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        if (ctx.sandbox()) {
            return SANDBOX;
        }
        String url = ENDPOINTS.get(region(ctx));
        if (url == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region(ctx));
        }
        return url;
    }

    private static String region(AdsContext ctx) {
        String r = ctx.credentials().get("region");
        return r == null ? "NA" : r.toUpperCase();
    }

    private Map<String, String> baseHeaders(AdsContext ctx) {
        ensureToken(ctx);
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + ctx.credentials().require("accessToken"));
        h.put("Amazon-Advertising-API-ClientId", ctx.credentials().require("clientId"));
        return h;
    }

    private Map<String, String> scoped(AdsContext ctx, String contentType) {
        if (ctx.profileId() == null) {
            throw new BizException("ecom.error.ads.profileMissing", ctx.store() == null ? "" : ctx.store().getName());
        }
        Map<String, String> h = baseHeaders(ctx);
        h.put("Amazon-Advertising-API-Scope", ctx.profileId());
        if (contentType != null) {
            h.put("Content-Type", contentType);
            h.put("Accept", contentType);
        }
        return h;
    }

    @Override
    public List<AdsProfile> profiles(AdsContext ctx) {
        Map<String, String> h = baseHeaders(ctx);
        h.put("Accept", "application/json");
        JsonNode arr = HttpJson.get(endpoint(ctx) + "/v2/profiles", h, PLATFORM);
        List<AdsProfile> list = new ArrayList<>();
        for (JsonNode p : arr) {
            JsonNode acc = p.path("accountInfo");
            list.add(new AdsProfile(ConnectorJson.str(p, "profileId"), ConnectorJson.str(p, "countryCode"), ConnectorJson.str(p, "currencyCode"),
                    ConnectorJson.str(p, "timezone"), ConnectorJson.str(acc, "name"), ConnectorJson.str(acc, "type"),
                    ConnectorJson.str(acc, "marketplaceStringId")));
        }
        return list;
    }

    @Override
    public void test(AdsContext ctx) {
        profiles(ctx);
        if (ctx.profileId() != null) {
            // 有配置文件时再验证一次带 Scope 的接口
            list(ctx, "/sp/campaigns/list", V_CAMPAIGN, "campaigns", 1);
        }
    }

    // ---------------------------------------------------------------- 结构

    @Override
    public AdsStructure fetchStructure(AdsContext ctx) {
        List<AdsStructure.Campaign> campaigns = new ArrayList<>();
        for (JsonNode c : list(ctx, "/sp/campaigns/list", V_CAMPAIGN, "campaigns", MAX_PAGES)) {
            campaigns.add(new AdsStructure.Campaign(ConnectorJson.str(c, "campaignId"), ConnectorJson.str(c, "name"), EcomAdType.SP,
                    state(ConnectorJson.str(c, "state")), ConnectorJson.decimal(c, "budget", "budget"),
                    ConnectorJson.str(c, "dynamicBidding", "strategy"), ConnectorJson.str(c, "targetingType"),
                    date(ConnectorJson.str(c, "startDate")), date(ConnectorJson.str(c, "endDate"))));
        }
        List<AdsStructure.AdGroup> groups = new ArrayList<>();
        for (JsonNode g : list(ctx, "/sp/adGroups/list", V_AD_GROUP, "adGroups", MAX_PAGES)) {
            groups.add(new AdsStructure.AdGroup(ConnectorJson.str(g, "adGroupId"), ConnectorJson.str(g, "campaignId"), ConnectorJson.str(g, "name"),
                    state(ConnectorJson.str(g, "state")), ConnectorJson.decimal(g, "defaultBid")));
        }
        List<AdsStructure.Target> targets = new ArrayList<>();
        for (JsonNode k : list(ctx, "/sp/keywords/list", V_KEYWORD, "keywords", MAX_PAGES)) {
            targets.add(new AdsStructure.Target(ConnectorJson.str(k, "keywordId"), ConnectorJson.str(k, "campaignId"), ConnectorJson.str(k, "adGroupId"),
                    EcomAdTargetType.KEYWORD, ConnectorJson.str(k, "keywordText"), matchType(ConnectorJson.str(k, "matchType")),
                    state(ConnectorJson.str(k, "state")), ConnectorJson.decimal(k, "bid")));
        }
        for (JsonNode t : list(ctx, "/sp/targets/list", V_TARGET, "targetingClauses", MAX_PAGES)) {
            boolean auto = "AUTO".equalsIgnoreCase(ConnectorJson.str(t, "expressionType"));
            targets.add(new AdsStructure.Target(ConnectorJson.str(t, "targetId"), ConnectorJson.str(t, "campaignId"), ConnectorJson.str(t, "adGroupId"),
                    auto ? EcomAdTargetType.AUTO : EcomAdTargetType.PRODUCT, expression(t), null, state(ConnectorJson.str(t, "state")),
                    ConnectorJson.decimal(t, "bid")));
        }
        for (JsonNode n : list(ctx, "/sp/negativeKeywords/list", V_NEGATIVE, "negativeKeywords", MAX_PAGES)) {
            targets.add(new AdsStructure.Target(ConnectorJson.str(n, "keywordId"), ConnectorJson.str(n, "campaignId"), ConnectorJson.str(n, "adGroupId"),
                    EcomAdTargetType.NEGATIVE, ConnectorJson.str(n, "keywordText"), matchType(ConnectorJson.str(n, "matchType")),
                    state(ConnectorJson.str(n, "state")), null));
        }
        return new AdsStructure(campaigns, groups, targets);
    }

    /** SP v3 列表接口：POST + nextToken 分页，只取启用、暂停的 */
    private List<JsonNode> list(AdsContext ctx, String path, String type, String field, int maxPages) {
        Map<String, String> headers = scoped(ctx, type);
        List<JsonNode> out = new ArrayList<>();
        String next = null;
        int pages = 0;
        do {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("maxResults", PAGE_SIZE);
            body.put("stateFilter", Map.of("include", List.of("ENABLED", "PAUSED")));
            if (next != null) {
                body.put("nextToken", next);
            }
            JsonNode resp = HttpJson.requestRaw("POST", endpoint(ctx) + path, headers, HttpJson.MAPPER.writeValueAsString(body), type, PLATFORM);
            resp.path(field).forEach(out::add);
            next = ConnectorJson.str(resp, "nextToken");
            pages++;
        } while (next != null && pages < maxPages);
        if (next != null) {
            ConnectorJson.warnPageLimit(PLATFORM, path, maxPages);
        }
        return out;
    }

    /** 商品 / 自动投放的表达式：type="value" 用逗号连接，自动投放只有 type（如 QUERY_HIGH_REL_MATCHES） */
    static String expression(JsonNode t) {
        JsonNode exp = t.path("resolvedExpression").isArray() && !t.path("resolvedExpression").isEmpty() ? t.path("resolvedExpression") : t.path("expression");
        List<String> parts = new ArrayList<>();
        for (JsonNode e : exp) {
            String type = ConnectorJson.str(e, "type");
            String value = ConnectorJson.str(e, "value");
            parts.add(value == null ? type : type + "=\"" + value + "\"");
        }
        String s = String.join(",", parts);
        return s.isEmpty() ? null : s;
    }

    // ---------------------------------------------------------------- 报告

    @Override
    public Map<EcomAdReportType, List<AdsReportRow>> fetchReports(AdsContext ctx, Set<EcomAdReportType> types, LocalDate from, LocalDate to) {
        Map<String, String> headers = scoped(ctx, V_REPORT);
        Map<EcomAdReportType, String> ids = new EnumMap<>(EcomAdReportType.class);
        for (EcomAdReportType type : types) {
            ids.put(type, createReport(ctx, headers, type, from, to));
        }
        Map<String, String> statusHeaders = scoped(ctx, null);
        Map<EcomAdReportType, String> urls = new EnumMap<>(EcomAdReportType.class);
        long waited = 0;
        long interval = pollInitialMillis;
        while (urls.size() < ids.size()) {
            for (Map.Entry<EcomAdReportType, String> e : ids.entrySet()) {
                if (urls.containsKey(e.getKey())) {
                    continue;
                }
                JsonNode s = HttpJson.get(endpoint(ctx) + "/reporting/reports/" + e.getValue(), statusHeaders, PLATFORM);
                String status = ConnectorJson.str(s, "status");
                if ("COMPLETED".equalsIgnoreCase(status) && ConnectorJson.str(s, "url") != null) {
                    urls.put(e.getKey(), ConnectorJson.str(s, "url"));
                } else if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                    throw new BizException("ecom.error.ads.reportFailed", PLATFORM, e.getKey().name(),
                            HttpJson.abbreviate(ConnectorJson.firstNonBlank(ConnectorJson.str(s, "failureReason"), status)));
                }
            }
            if (urls.size() == ids.size()) {
                break;
            }
            if (waited + interval > pollTimeoutMillis) {
                throw new BizException("ecom.error.ads.reportTimeout", PLATFORM, pollTimeoutMillis / 60_000);
            }
            sleep(interval);
            waited += interval;
            interval = Math.min(interval * 2, pollMaxIntervalMillis);
        }
        Map<EcomAdReportType, List<AdsReportRow>> result = new EnumMap<>(EcomAdReportType.class);
        for (Map.Entry<EcomAdReportType, String> e : urls.entrySet()) {
            JsonNode data = AdsHttp.downloadJson(e.getValue(), Map.of(), PLATFORM);
            List<AdsReportRow> rows = new ArrayList<>();
            for (JsonNode r : data) {
                rows.add(toRow(e.getKey(), r));
            }
            result.put(e.getKey(), rows);
        }
        return result;
    }

    private void sleep(long millis) {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", PLATFORM, ex.getMessage());
        }
    }

    /** 创建报告，重复请求（425）时沿用平台返回的已有报告 ID */
    private String createReport(AdsContext ctx, Map<String, String> headers, EcomAdReportType type, LocalDate from, LocalDate to) {
        Map<String, Object> body = reportRequest(type, from, to);
        try {
            JsonNode resp = HttpJson.requestRaw("POST", endpoint(ctx) + "/reporting/reports", headers, HttpJson.MAPPER.writeValueAsString(body), V_REPORT, PLATFORM);
            String id = ConnectorJson.str(resp, "reportId");
            if (id == null) {
                throw new BizException("ecom.error.ads.reportFailed", PLATFORM, type.name(), HttpJson.abbreviate(resp.toString()));
            }
            return id;
        } catch (ConnectorHttpException e) {
            if (e.statusCode() == 425) {
                Matcher m = UUID.matcher(e.bodySummary() == null ? "" : e.bodySummary());
                if (m.find()) {
                    return m.group();
                }
            }
            throw e;
        }
    }

    /** v3 报告请求：商品推广、按天、gzip JSON；各类型的 groupBy 与列 */
    static Map<String, Object> reportRequest(EcomAdReportType type, LocalDate from, LocalDate to) {
        List<String> metrics = List.of("impressions", "clicks", "cost", "sales7d", "purchases7d", "unitsSoldClicks7d");
        List<String> columns = new ArrayList<>(List.of("date", "campaignId", "campaignName"));
        String reportTypeId;
        String groupBy;
        switch (type) {
            case CAMPAIGN -> {
                reportTypeId = "spCampaigns";
                groupBy = "campaign";
                columns.addAll(List.of("campaignStatus", "campaignBudgetAmount"));
            }
            case PRODUCT -> {
                reportTypeId = "spAdvertisedProduct";
                groupBy = "advertiser";
                columns.addAll(List.of("adGroupId", "adGroupName", "advertisedSku", "advertisedAsin"));
            }
            case TARGETING -> {
                reportTypeId = "spTargeting";
                groupBy = "targeting";
                columns.addAll(List.of("adGroupId", "adGroupName", "keywordId", "keyword", "keywordType", "matchType", "targeting"));
            }
            default -> {
                reportTypeId = "spSearchTerm";
                groupBy = "searchTerm";
                columns.addAll(List.of("adGroupId", "adGroupName", "keywordId", "keyword", "matchType", "targeting", "searchTerm"));
            }
        }
        columns.addAll(metrics);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("adProduct", "SPONSORED_PRODUCTS");
        config.put("groupBy", List.of(groupBy));
        config.put("columns", columns);
        config.put("reportTypeId", reportTypeId);
        config.put("timeUnit", "DAILY");
        config.put("format", "GZIP_JSON");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "lemon-" + reportTypeId + "-" + from + "-" + to);
        body.put("startDate", from.toString());
        body.put("endDate", to.toString());
        body.put("configuration", config);
        return body;
    }

    static AdsReportRow toRow(EcomAdReportType type, JsonNode r) {
        AdsReportRow row = new AdsReportRow();
        row.setDate(date(ConnectorJson.str(r, "date")));
        row.setCampaignPlatformId(ConnectorJson.str(r, "campaignId"));
        row.setCampaignName(ConnectorJson.str(r, "campaignName"));
        row.setImpressions(ConnectorJson.parseLong(ConnectorJson.str(r, "impressions"), 0));
        row.setClicks(ConnectorJson.parseLong(ConnectorJson.str(r, "clicks"), 0));
        row.setSpend(nz(ConnectorJson.decimal(r, "cost")));
        row.setSales(nz(ConnectorJson.decimal(r, "sales7d")));
        row.setOrders((int) ConnectorJson.parseLong(ConnectorJson.str(r, "purchases7d"), 0));
        row.setUnits((int) ConnectorJson.parseLong(ConnectorJson.str(r, "unitsSoldClicks7d"), 0));
        if (type == EcomAdReportType.CAMPAIGN) {
            row.setCampaignStatus(state(ConnectorJson.str(r, "campaignStatus")));
            row.setCampaignBudget(ConnectorJson.decimal(r, "campaignBudgetAmount"));
            return row;
        }
        row.setAdGroupPlatformId(ConnectorJson.str(r, "adGroupId"));
        row.setAdGroupName(ConnectorJson.str(r, "adGroupName"));
        if (type == EcomAdReportType.PRODUCT) {
            row.setSellerSku(ConnectorJson.str(r, "advertisedSku"));
            row.setAsin(ConnectorJson.str(r, "advertisedAsin"));
            return row;
        }
        row.setTargetPlatformId(ConnectorJson.str(r, "keywordId"));
        String keyword = ConnectorJson.str(r, "keyword");
        String targeting = ConnectorJson.str(r, "targeting");
        EcomAdMatchType match = matchType(ConnectorJson.str(r, "matchType"));
        String keywordType = ConnectorJson.firstNonBlank(ConnectorJson.str(r, "keywordType"), ConnectorJson.str(r, "matchType"));
        EcomAdTargetType targetType = targetType(keywordType, match);
        row.setTargetType(targetType);
        row.setMatchType(targetType == EcomAdTargetType.KEYWORD ? match : null);
        row.setTargetText(targetType == EcomAdTargetType.KEYWORD ? ConnectorJson.firstNonBlank(keyword, targeting) : ConnectorJson.firstNonBlank(targeting, keyword));
        if (type == EcomAdReportType.SEARCH_TERM) {
            row.setSearchTerm(ConnectorJson.str(r, "searchTerm"));
        }
        return row;
    }

    /** keywordType / matchType：BROAD / PHRASE / EXACT 为关键词，TARGETING_EXPRESSION 为商品投放，TARGETING_EXPRESSION_PREDEFINED 为自动投放 */
    static EcomAdTargetType targetType(String keywordType, EcomAdMatchType match) {
        if (match != null) {
            return EcomAdTargetType.KEYWORD;
        }
        if (keywordType != null && keywordType.toUpperCase().contains("PREDEFINED")) {
            return EcomAdTargetType.AUTO;
        }
        return EcomAdTargetType.PRODUCT;
    }

    // ---------------------------------------------------------------- 修改

    @Override
    public List<AdsChangeResult> applyChanges(AdsContext ctx, List<AdsChange> changes) {
        List<AdsChangeResult> results = new ArrayList<>();
        List<AdsChange> keywords = new ArrayList<>();
        List<AdsChange> targets = new ArrayList<>();
        List<AdsChange> groups = new ArrayList<>();
        List<AdsChange> campaigns = new ArrayList<>();
        for (AdsChange c : changes) {
            if (c.platformId() == null) {
                results.add(AdsChangeResult.failed(c.localId(), "no platform id"));
                continue;
            }
            switch (c.level()) {
                case CAMPAIGN -> campaigns.add(c);
                case AD_GROUP -> groups.add(c);
                default -> {
                    if (c.targetType() == EcomAdTargetType.KEYWORD) {
                        keywords.add(c);
                    } else if (c.targetType() == EcomAdTargetType.NEGATIVE) {
                        results.add(AdsChangeResult.failed(c.localId(), "negative keyword"));
                    } else {
                        targets.add(c);
                    }
                }
            }
        }
        results.addAll(put(ctx, "/sp/keywords", V_KEYWORD, "keywords", keywords, BATCH, c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("keywordId", c.platformId());
            putBidState(m, "bid", c);
            return m;
        }));
        results.addAll(put(ctx, "/sp/targets", V_TARGET, "targetingClauses", targets, BATCH, c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("targetId", c.platformId());
            putBidState(m, "bid", c);
            return m;
        }));
        results.addAll(put(ctx, "/sp/adGroups", V_AD_GROUP, "adGroups", groups, BATCH, c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("adGroupId", c.platformId());
            putBidState(m, "defaultBid", c);
            return m;
        }));
        results.addAll(put(ctx, "/sp/campaigns", V_CAMPAIGN, "campaigns", campaigns, CAMPAIGN_BATCH, c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("campaignId", c.platformId());
            if (c.budget() != null) {
                m.put("budget", Map.of("budget", c.budget(), "budgetType", "DAILY"));
            }
            if (c.status() != null) {
                m.put("state", c.status().name());
            }
            return m;
        }));
        return results;
    }

    private static void putBidState(Map<String, Object> m, String bidField, AdsChange c) {
        if (c.bid() != null) {
            m.put(bidField, c.bid());
        }
        if (c.status() != null) {
            m.put("state", c.status().name());
        }
    }

    /** 分批 PUT，207 响应按 index 对应 success / error */
    private List<AdsChangeResult> put(AdsContext ctx, String path, String type, String field, List<AdsChange> items, int batch,
                                      java.util.function.Function<AdsChange, Map<String, Object>> toBody) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (items.isEmpty()) {
            return results;
        }
        Map<String, String> headers = scoped(ctx, type);
        for (int i = 0; i < items.size(); i += batch) {
            List<AdsChange> chunk = items.subList(i, Math.min(items.size(), i + batch));
            List<Map<String, Object>> body = chunk.stream().map(toBody).toList();
            try {
                JsonNode resp = HttpJson.requestRaw("PUT", endpoint(ctx) + path, headers, HttpJson.MAPPER.writeValueAsString(Map.of(field, body)), type, PLATFORM);
                results.addAll(multiStatus(resp.path(field), chunk.stream().map(AdsChange::localId).toList(), null));
            } catch (BizException e) {
                chunk.forEach(c -> results.add(AdsChangeResult.failed(c.localId(), e.getMessage())));
            }
        }
        return results;
    }

    /**
     * 解析 207 多状态：success[].index 成功（idField 不为空时取新建对象的 ID），error[].index 失败（原因取 errors 摘要）；
     * 响应中没有提到的项按失败处理
     */
    static List<AdsChangeResult> multiStatus(JsonNode node, List<Long> localIds, String idField) {
        Map<Integer, AdsChangeResult> byIndex = new HashMap<>();
        for (JsonNode s : node.path("success")) {
            int idx = s.path("index").asInt(-1);
            if (idx >= 0 && idx < localIds.size()) {
                byIndex.put(idx, new AdsChangeResult(localIds.get(idx), true, idField == null ? null : ConnectorJson.str(s, idField), null));
            }
        }
        for (JsonNode e : node.path("error")) {
            int idx = e.path("index").asInt(-1);
            if (idx >= 0 && idx < localIds.size()) {
                byIndex.put(idx, AdsChangeResult.failed(localIds.get(idx), HttpJson.abbreviate(e.path("errors").toString())));
            }
        }
        List<AdsChangeResult> out = new ArrayList<>();
        for (int i = 0; i < localIds.size(); i++) {
            out.add(byIndex.getOrDefault(i, AdsChangeResult.failed(localIds.get(i), "no result")));
        }
        return out;
    }

    @Override
    public List<AdsChangeResult> createNegativeKeywords(AdsContext ctx, List<AdsNegativeKeyword> keywords) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (keywords.isEmpty()) {
            return results;
        }
        Map<String, String> headers = scoped(ctx, V_NEGATIVE);
        for (int i = 0; i < keywords.size(); i += BATCH) {
            List<AdsNegativeKeyword> chunk = keywords.subList(i, Math.min(keywords.size(), i + BATCH));
            List<Map<String, Object>> body = new ArrayList<>();
            for (AdsNegativeKeyword k : chunk) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("campaignId", k.campaignPlatformId());
                m.put("adGroupId", k.adGroupPlatformId());
                m.put("keywordText", k.text());
                m.put("matchType", (k.matchType() == null ? EcomAdMatchType.NEGATIVE_EXACT : k.matchType()).name());
                m.put("state", "ENABLED");
                body.add(m);
            }
            List<Long> ids = chunk.stream().map(AdsNegativeKeyword::localId).toList();
            try {
                JsonNode resp = HttpJson.requestRaw("POST", endpoint(ctx) + "/sp/negativeKeywords", headers,
                        HttpJson.MAPPER.writeValueAsString(Map.of("negativeKeywords", body)), V_NEGATIVE, PLATFORM);
                results.addAll(multiStatus(resp.path("negativeKeywords"), ids, "negativeKeywordId"));
            } catch (BizException e) {
                ids.forEach(id -> results.add(AdsChangeResult.failed(id, e.getMessage())));
            }
        }
        return results;
    }

    // ---------------------------------------------------------------- 工具

    static EcomAdStatus state(String s) {
        if (s == null) {
            return EcomAdStatus.ENABLED;
        }
        return switch (s.toUpperCase()) {
            case "PAUSED" -> EcomAdStatus.PAUSED;
            case "ARCHIVED" -> EcomAdStatus.ARCHIVED;
            default -> EcomAdStatus.ENABLED;
        };
    }

    static EcomAdMatchType matchType(String s) {
        if (s == null) {
            return null;
        }
        return switch (s.toUpperCase()) {
            case "EXACT" -> EcomAdMatchType.EXACT;
            case "PHRASE" -> EcomAdMatchType.PHRASE;
            case "BROAD" -> EcomAdMatchType.BROAD;
            case "NEGATIVE_EXACT", "NEGATIVEEXACT" -> EcomAdMatchType.NEGATIVE_EXACT;
            case "NEGATIVE_PHRASE", "NEGATIVEPHRASE" -> EcomAdMatchType.NEGATIVE_PHRASE;
            default -> null;
        };
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        try {
            return v.length() == 8 && v.chars().allMatch(Character::isDigit)
                    ? LocalDate.parse(v, java.time.format.DateTimeFormatter.BASIC_ISO_DATE) : LocalDate.parse(v.substring(0, Math.min(10, v.length())));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
