package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 亚马逊广告的品牌推广（Sponsored Brands，SB）与展示型推广（Sponsored Display，SD），由 {@link AmazonAdsConnector} 调用：
 * <ul>
 *   <li>结构：SB 活动 / 广告组用 v4（POST /sb/v4/campaigns/list、/sb/v4/adGroups/list，nextToken 分页，vnd.sbcampaignresource.v4+json 等），
 *       关键词 GET /sb/keywords、商品投放 POST /sb/targets/list、否定关键词 GET /sb/negativeKeywords；
 *       SD 活动 / 广告组 / 投放 GET /sd/campaigns、/sd/adGroups、/sd/targets（startIndex + count 分页，状态小写）</li>
 *   <li>报告（v3 异步报告，与 SP 同一套创建 → 轮询 → 下载）：SB 为 sbCampaigns、sbTargeting、sbSearchTerm（没有推广商品报告，
 *       活动报告写活动级日报，利润分析按它扣除花费）；SD 为 sdCampaigns、sdAdvertisedProduct、sdTargeting（没有搜索词）。
 *       SB / SD 的销售额、订单、件数取 sales / purchases / unitsSold（平台默认归因：SB 14 天、SD 按活动的计费方式）</li>
 *   <li>修改：SB 活动 PUT /sb/v4/campaigns（预算、状态，207 多状态）、广告组 PUT /sb/v4/adGroups（只有状态，<b>SB 广告组没有默认竞价</b>）、
 *       关键词 PUT /sb/keywords、商品投放 PUT /sb/targets（都要带 adGroupId、campaignId）、否定关键词 POST /sb/negativeKeywords；
 *       SD 活动 / 广告组 / 投放 PUT /sd/campaigns、/sd/adGroups、/sd/targets（数组报文，逐项返回 code），<b>SD 没有关键词与否定关键词</b></li>
 * </ul>
 * 账号没有开通品牌推广（未做品牌备案）或展示型推广时，平台对这些接口返回 401 / 403 / 404 或空列表：拉取结构与报告前先探测
 * （{@link #available}），不可用的类型跳过，也不会把本地已有的该类型活动标记为归档。
 * <p>
 * <b>按公开文档实现，尚未用真实广告账号联调</b>，需要核对：SB v4 列表与修改接口的 vnd 内容类型与 207 响应结构；SB v3 关键词修改的响应
 * （这里按数组逐项 code）；/sb/targets 修改的响应（updateTargetSuccessResults / updateTargetErrorResults）；SD 列表接口是否需要 tactic；
 * v3 报告中 sbTargeting / sdTargeting 的列名（keywordText、targetingId、targetingExpression、targetingText）与 sdAdvertisedProduct 的
 * promotedSku / promotedAsin；SB / SD 报告的日期列与归因口径
 */
@Slf4j
final class AmazonAdsSbSd {

    static final String V_SB_CAMPAIGN = "application/vnd.sbcampaignresource.v4+json";
    static final String V_SB_AD_GROUP = "application/vnd.sbadgroupresource.v4+json";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 200;

    private final AmazonAdsConnector connector;

    AmazonAdsSbSd(AmazonAdsConnector connector) {
        this.connector = connector;
    }

    // ---------------------------------------------------------------- 能力

    /** 该广告类型、层级是否支持修改 / 添加（SB 广告组没有默认竞价，SD 没有关键词和否定关键词） */
    static boolean supports(EcomAdType adType, EcomAdLevel level, AdsCapability capability) {
        if (adType == EcomAdType.SB && level == EcomAdLevel.AD_GROUP && capability == AdsCapability.UPDATE_BID) {
            return false;
        }
        return !(adType == EcomAdType.SD && capability == AdsCapability.NEGATIVE_KEYWORDS);
    }

    /** 该广告类型有没有这种报告 */
    static boolean hasReport(EcomAdType adType, EcomAdReportType type) {
        return switch (adType) {
            case SB -> type != EcomAdReportType.PRODUCT;
            case SD -> type != EcomAdReportType.SEARCH_TERM;
            default -> true;
        };
    }

    /** 账号是否开通了该广告类型且有活动（探测一页活动；401 / 403 / 404 视为没有开通） */
    boolean available(AdsContext ctx, EcomAdType adType) {
        try {
            if (adType == EcomAdType.SB) {
                return !sbList(ctx, "/sb/v4/campaigns/list", V_SB_CAMPAIGN, "campaigns", 1).isEmpty();
            }
            return !sdList(ctx, "/sd/campaigns", 1).isEmpty();
        } catch (ConnectorHttpException e) {
            if (notEnabled(e)) {
                log.info("Amazon Ads {} not available for store {}: {}", adType, ctx.store() == null ? "" : ctx.store().getCode(), e.statusCode());
                return false;
            }
            throw e;
        }
    }

    static boolean notEnabled(ConnectorHttpException e) {
        return e.statusCode() == 401 || e.statusCode() == 403 || e.statusCode() == 404;
    }

    // ---------------------------------------------------------------- 结构

    void sbStructure(AdsContext ctx, List<AdsStructure.Campaign> campaigns, List<AdsStructure.AdGroup> groups, List<AdsStructure.Target> targets) {
        for (JsonNode c : sbList(ctx, "/sb/v4/campaigns/list", V_SB_CAMPAIGN, "campaigns", MAX_PAGES)) {
            campaigns.add(new AdsStructure.Campaign(ConnectorJson.str(c, "campaignId"), ConnectorJson.str(c, "name"), EcomAdType.SB,
                    AmazonAdsConnector.state(ConnectorJson.str(c, "state")), budget(c), ConnectorJson.str(c, "bidding", "bidOptimizationStrategy"),
                    ConnectorJson.firstNonBlank(ConnectorJson.str(c, "goal"), ConnectorJson.str(c, "costType")),
                    AmazonAdsConnector.date(ConnectorJson.str(c, "startDate")), AmazonAdsConnector.date(ConnectorJson.str(c, "endDate"))));
        }
        for (JsonNode g : sbList(ctx, "/sb/v4/adGroups/list", V_SB_AD_GROUP, "adGroups", MAX_PAGES)) {
            groups.add(new AdsStructure.AdGroup(ConnectorJson.str(g, "adGroupId"), ConnectorJson.str(g, "campaignId"), ConnectorJson.str(g, "name"),
                    AmazonAdsConnector.state(ConnectorJson.str(g, "state")), null));
        }
        for (JsonNode k : sdList(ctx, "/sb/keywords", MAX_PAGES)) {
            targets.add(new AdsStructure.Target(ConnectorJson.str(k, "keywordId"), ConnectorJson.str(k, "campaignId"), ConnectorJson.str(k, "adGroupId"),
                    EcomAdTargetType.KEYWORD, ConnectorJson.str(k, "keywordText"), AmazonAdsConnector.matchType(ConnectorJson.str(k, "matchType")),
                    AmazonAdsConnector.state(ConnectorJson.str(k, "state")), ConnectorJson.decimal(k, "bid")));
        }
        for (JsonNode t : sbTargets(ctx)) {
            targets.add(new AdsStructure.Target(ConnectorJson.str(t, "targetId"), ConnectorJson.str(t, "campaignId"), ConnectorJson.str(t, "adGroupId"),
                    EcomAdTargetType.PRODUCT, expressions(t.path("expressions")), null, AmazonAdsConnector.state(ConnectorJson.str(t, "state")),
                    ConnectorJson.decimal(t, "bid")));
        }
        for (JsonNode n : sdList(ctx, "/sb/negativeKeywords", MAX_PAGES)) {
            targets.add(new AdsStructure.Target(ConnectorJson.str(n, "keywordId"), ConnectorJson.str(n, "campaignId"), ConnectorJson.str(n, "adGroupId"),
                    EcomAdTargetType.NEGATIVE, ConnectorJson.str(n, "keywordText"), AmazonAdsConnector.matchType(ConnectorJson.str(n, "matchType")),
                    AmazonAdsConnector.state(ConnectorJson.str(n, "state")), null));
        }
    }

    void sdStructure(AdsContext ctx, List<AdsStructure.Campaign> campaigns, List<AdsStructure.AdGroup> groups, List<AdsStructure.Target> targets) {
        for (JsonNode c : sdList(ctx, "/sd/campaigns", MAX_PAGES)) {
            campaigns.add(new AdsStructure.Campaign(ConnectorJson.str(c, "campaignId"), ConnectorJson.str(c, "name"), EcomAdType.SD,
                    AmazonAdsConnector.state(ConnectorJson.str(c, "state")), budget(c), ConnectorJson.str(c, "costType"), ConnectorJson.str(c, "tactic"),
                    AmazonAdsConnector.date(ConnectorJson.str(c, "startDate")), AmazonAdsConnector.date(ConnectorJson.str(c, "endDate"))));
        }
        for (JsonNode g : sdList(ctx, "/sd/adGroups", MAX_PAGES)) {
            groups.add(new AdsStructure.AdGroup(ConnectorJson.str(g, "adGroupId"), ConnectorJson.str(g, "campaignId"), ConnectorJson.str(g, "name"),
                    AmazonAdsConnector.state(ConnectorJson.str(g, "state")), ConnectorJson.decimal(g, "defaultBid")));
        }
        for (JsonNode t : sdList(ctx, "/sd/targets", MAX_PAGES)) {
            String text = expressions(t.path("resolvedExpression").isArray() && !t.path("resolvedExpression").isEmpty()
                    ? t.path("resolvedExpression") : t.path("expression"));
            targets.add(new AdsStructure.Target(ConnectorJson.str(t, "targetId"), ConnectorJson.str(t, "campaignId"), ConnectorJson.str(t, "adGroupId"),
                    sdTargetType(text), text, null, AmazonAdsConnector.state(ConnectorJson.str(t, "state")), ConnectorJson.decimal(t, "bid")));
        }
    }

    /** SD 投放：商品 / 类目（asinSameAs、asinCategorySameAs 等）为商品投放，受众（views、audience、purchases）为自动投放 */
    static EcomAdTargetType sdTargetType(String expression) {
        String e = expression == null ? "" : expression.toLowerCase();
        return e.contains("views") || e.contains("audience") || e.contains("purchases") || e.contains("similarproduct")
                ? EcomAdTargetType.AUTO : EcomAdTargetType.PRODUCT;
    }

    /** 投放表达式：type="value" 逗号连接，value 为数组（受众的嵌套表达式）时递归 */
    static String expressions(JsonNode exp) {
        List<String> parts = new ArrayList<>();
        for (JsonNode e : exp) {
            String type = ConnectorJson.str(e, "type");
            JsonNode v = e.path("value");
            if (v.isArray()) {
                parts.add(type + "(" + expressions(v) + ")");
            } else {
                String value = v.isMissingNode() || v.isNull() ? null : v.asString();
                parts.add(value == null ? type : type + "=\"" + value + "\"");
            }
        }
        String s = String.join(",", parts);
        return s.isEmpty() ? null : s;
    }

    private static BigDecimal budget(JsonNode c) {
        JsonNode b = c.path("budget");
        return b.isObject() ? ConnectorJson.decimal(b, "budget") : ConnectorJson.decimal(c, "budget");
    }

    /** SB v4 列表：POST + nextToken 分页，只取启用、暂停的 */
    private List<JsonNode> sbList(AdsContext ctx, String path, String type, String field, int maxPages) {
        Map<String, String> headers = connector.scoped(ctx, type);
        List<JsonNode> out = new ArrayList<>();
        String next = null;
        int pages = 0;
        do {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("maxResults", maxPages == 1 ? 1 : PAGE_SIZE);
            body.put("stateFilter", Map.of("include", List.of("ENABLED", "PAUSED")));
            if (next != null) {
                body.put("nextToken", next);
            }
            JsonNode resp = HttpJson.requestRaw("POST", AmazonAdsConnector.endpoint(ctx) + path, headers, HttpJson.MAPPER.writeValueAsString(body), type,
                    AmazonAdsConnector.PLATFORM);
            resp.path(field).forEach(out::add);
            next = ConnectorJson.str(resp, "nextToken");
            pages++;
        } while (next != null && pages < maxPages);
        return out;
    }

    /** SB 商品投放：POST /sb/targets/list（nextToken 分页） */
    private List<JsonNode> sbTargets(AdsContext ctx) {
        Map<String, String> headers = connector.scoped(ctx, "application/json");
        List<JsonNode> out = new ArrayList<>();
        String next = null;
        int pages = 0;
        do {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("maxResults", PAGE_SIZE);
            body.put("filters", List.of(Map.of("filterType", "STATE", "values", List.of("enabled", "paused"))));
            if (next != null) {
                body.put("nextToken", next);
            }
            JsonNode resp = HttpJson.requestRaw("POST", AmazonAdsConnector.endpoint(ctx) + "/sb/targets/list", headers,
                    HttpJson.MAPPER.writeValueAsString(body), "application/json", AmazonAdsConnector.PLATFORM);
            resp.path("targets").forEach(out::add);
            next = ConnectorJson.str(resp, "nextToken");
            pages++;
        } while (next != null && pages < MAX_PAGES);
        return out;
    }

    /** SD 与 SB v3 的 GET 列表：startIndex + count 分页，返回数组，只取启用、暂停的 */
    private List<JsonNode> sdList(AdsContext ctx, String path, int maxPages) {
        Map<String, String> headers = connector.scoped(ctx, null);
        headers.put("Accept", "application/json");
        List<JsonNode> out = new ArrayList<>();
        int count = maxPages == 1 ? 1 : PAGE_SIZE;
        for (int page = 0; page < maxPages; page++) {
            JsonNode arr = HttpJson.get(AmazonAdsConnector.endpoint(ctx) + path + "?stateFilter=enabled,paused&startIndex=" + page * count + "&count=" + count,
                    headers, AmazonAdsConnector.PLATFORM);
            int n = 0;
            for (JsonNode x : arr) {
                out.add(x);
                n++;
            }
            if (n < count) {
                break;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 报告

    /** v3 报告请求：SB / SD 各类型的 reportTypeId、groupBy 与列 */
    static Map<String, Object> reportRequest(EcomAdType adType, EcomAdReportType type, LocalDate from, LocalDate to) {
        List<String> metrics = List.of("impressions", "clicks", "cost", "sales", "purchases", "unitsSold");
        List<String> columns = new ArrayList<>(List.of("date", "campaignId", "campaignName"));
        String prefix = adType == EcomAdType.SB ? "sb" : "sd";
        String reportTypeId;
        String groupBy;
        switch (type) {
            case CAMPAIGN -> {
                reportTypeId = prefix + "Campaigns";
                groupBy = "campaign";
                columns.addAll(List.of("campaignStatus", "campaignBudgetAmount"));
            }
            case PRODUCT -> {
                reportTypeId = "sdAdvertisedProduct";
                groupBy = "advertiser";
                columns.addAll(List.of("adGroupId", "adGroupName", "promotedSku", "promotedAsin"));
            }
            case TARGETING -> {
                reportTypeId = prefix + "Targeting";
                groupBy = "targeting";
                columns.addAll(adType == EcomAdType.SB
                        ? List.of("adGroupId", "adGroupName", "keywordId", "keywordText", "keywordType", "matchType", "targetingId", "targetingExpression",
                        "targetingText")
                        : List.of("adGroupId", "adGroupName", "targetingId", "targetingExpression", "targetingText"));
            }
            default -> {
                reportTypeId = "sbSearchTerm";
                groupBy = "searchTerm";
                columns.addAll(List.of("adGroupId", "adGroupName", "keywordId", "keywordText", "matchType", "searchTerm"));
            }
        }
        columns.addAll(metrics);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("adProduct", adType == EcomAdType.SB ? "SPONSORED_BRANDS" : "SPONSORED_DISPLAY");
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

    /** SB / SD 报告的一行（列名有差别的按多个候选取第一个有值的） */
    static AdsReportRow toRow(EcomAdType adType, EcomAdReportType type, JsonNode r) {
        AdsReportRow row = new AdsReportRow();
        row.setAdType(adType);
        row.setDate(AmazonAdsConnector.date(ConnectorJson.str(r, "date")));
        row.setCampaignPlatformId(ConnectorJson.str(r, "campaignId"));
        row.setCampaignName(ConnectorJson.str(r, "campaignName"));
        row.setImpressions(ConnectorJson.parseLong(ConnectorJson.str(r, "impressions"), 0));
        row.setClicks(ConnectorJson.parseLong(ConnectorJson.str(r, "clicks"), 0));
        row.setSpend(nz(ConnectorJson.decimal(r, "cost")));
        row.setSales(nz(first(r, "sales", "sales14d", "attributedSales14d", "salesClicks")));
        row.setOrders((int) ConnectorJson.parseLong(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "purchases"), ConnectorJson.str(r, "purchases14d"),
                ConnectorJson.str(r, "purchasesClicks")), 0));
        row.setUnits((int) ConnectorJson.parseLong(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "unitsSold"), ConnectorJson.str(r, "unitsSoldClicks")), 0));
        if (type == EcomAdReportType.CAMPAIGN) {
            row.setCampaignStatus(AmazonAdsConnector.state(ConnectorJson.str(r, "campaignStatus")));
            row.setCampaignBudget(ConnectorJson.decimal(r, "campaignBudgetAmount"));
            return row;
        }
        row.setAdGroupPlatformId(ConnectorJson.str(r, "adGroupId"));
        row.setAdGroupName(ConnectorJson.str(r, "adGroupName"));
        if (type == EcomAdReportType.PRODUCT) {
            row.setSellerSku(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "promotedSku"), ConnectorJson.str(r, "advertisedSku")));
            row.setAsin(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "promotedAsin"), ConnectorJson.str(r, "advertisedAsin")));
            return row;
        }
        String keyword = ConnectorJson.firstNonBlank(ConnectorJson.str(r, "keywordText"), ConnectorJson.str(r, "keyword"));
        EcomAdMatchType match = AmazonAdsConnector.matchType(ConnectorJson.str(r, "matchType"));
        if (keyword != null && match != null) {
            row.setTargetType(EcomAdTargetType.KEYWORD);
            row.setMatchType(match);
            row.setTargetPlatformId(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "keywordId"), ConnectorJson.str(r, "targetingId")));
            row.setTargetText(keyword);
        } else {
            String text = ConnectorJson.firstNonBlank(ConnectorJson.str(r, "targetingText"), ConnectorJson.str(r, "targetingExpression"),
                    ConnectorJson.str(r, "targeting"), keyword);
            row.setTargetType(adType == EcomAdType.SD ? sdTargetType(text) : EcomAdTargetType.PRODUCT);
            row.setTargetPlatformId(ConnectorJson.firstNonBlank(ConnectorJson.str(r, "targetingId"), ConnectorJson.str(r, "keywordId")));
            row.setTargetText(text);
        }
        if (type == EcomAdReportType.SEARCH_TERM) {
            row.setSearchTerm(ConnectorJson.str(r, "searchTerm"));
        }
        return row;
    }

    private static BigDecimal first(JsonNode r, String... keys) {
        for (String k : keys) {
            BigDecimal v = ConnectorJson.decimal(r, k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 修改

    /** SB / SD 的修改：不支持的项（SB 广告组竞价、SD 关键词）直接返回失败并说明原因 */
    List<AdsChangeResult> applyChanges(AdsContext ctx, EcomAdType adType, List<AdsChange> changes) {
        List<AdsChangeResult> results = new ArrayList<>();
        List<AdsChange> campaigns = new ArrayList<>();
        List<AdsChange> groups = new ArrayList<>();
        List<AdsChange> keywords = new ArrayList<>();
        List<AdsChange> targets = new ArrayList<>();
        for (AdsChange c : changes) {
            if (c.level() == EcomAdLevel.CAMPAIGN) {
                campaigns.add(c);
            } else if (c.level() == EcomAdLevel.AD_GROUP) {
                if (c.bid() != null && !supports(adType, EcomAdLevel.AD_GROUP, AdsCapability.UPDATE_BID)) {
                    results.add(AdsChangeResult.failed(c.localId(), unsupportedText(adType, "ecom.error.fix.adsTypeUnsupported.adGroupBid")));
                } else {
                    groups.add(c);
                }
            } else if (c.targetType() == EcomAdTargetType.NEGATIVE) {
                results.add(AdsChangeResult.failed(c.localId(), "negative keyword"));
            } else if (c.targetType() == EcomAdTargetType.KEYWORD) {
                if (adType == EcomAdType.SD) {
                    results.add(AdsChangeResult.failed(c.localId(), unsupportedText(adType, "ecom.error.fix.adsTypeUnsupported.keyword")));
                } else {
                    keywords.add(c);
                }
            } else {
                targets.add(c);
            }
        }
        if (adType == EcomAdType.SB) {
            results.addAll(putMulti(ctx, "/sb/v4/campaigns", V_SB_CAMPAIGN, "campaigns", campaigns, AmazonAdsConnector.CAMPAIGN_BATCH, c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("campaignId", c.platformId());
                if (c.budget() != null) {
                    m.put("budget", c.budget());
                }
                if (c.status() != null) {
                    m.put("state", c.status().name());
                }
                return m;
            }));
            results.addAll(putMulti(ctx, "/sb/v4/adGroups", V_SB_AD_GROUP, "adGroups", groups, AmazonAdsConnector.BATCH, c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("adGroupId", c.platformId());
                if (c.status() != null) {
                    m.put("state", c.status().name());
                }
                return m;
            }));
            results.addAll(putArray(ctx, "/sb/keywords", keywords, "keywordId", c -> {
                Map<String, Object> m = sbParents(c);
                m.put("keywordId", c.platformId());
                putBidState(m, "bid", c, true);
                return m;
            }));
            results.addAll(putSbTargets(ctx, targets));
        } else {
            results.addAll(putArray(ctx, "/sd/campaigns", campaigns, "campaignId", c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("campaignId", c.platformId());
                if (c.budget() != null) {
                    m.put("budget", c.budget());
                }
                if (c.status() != null) {
                    m.put("state", c.status().name().toLowerCase());
                }
                return m;
            }));
            results.addAll(putArray(ctx, "/sd/adGroups", groups, "adGroupId", c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("adGroupId", c.platformId());
                putBidState(m, "defaultBid", c, true);
                return m;
            }));
            results.addAll(putArray(ctx, "/sd/targets", targets, "targetId", c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("targetId", c.platformId());
                putBidState(m, "bid", c, true);
                return m;
            }));
        }
        return results;
    }

    /** SB 否定关键词：POST /sb/negativeKeywords（数组报文，逐项返回 code 与 keywordId）；SD 不支持 */
    List<AdsChangeResult> createNegativeKeywords(AdsContext ctx, EcomAdType adType, List<AdsNegativeKeyword> keywords) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (adType == EcomAdType.SD) {
            keywords.forEach(k -> results.add(AdsChangeResult.failed(k.localId(), unsupportedText(adType, "ecom.error.fix.adsTypeUnsupported.negative"))));
            return results;
        }
        Map<String, String> headers = connector.scoped(ctx, "application/json");
        for (int i = 0; i < keywords.size(); i += AmazonAdsConnector.BATCH) {
            List<AdsNegativeKeyword> chunk = keywords.subList(i, Math.min(keywords.size(), i + AmazonAdsConnector.BATCH));
            List<Map<String, Object>> body = new ArrayList<>();
            for (AdsNegativeKeyword k : chunk) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("campaignId", k.campaignPlatformId());
                m.put("adGroupId", k.adGroupPlatformId());
                m.put("keywordText", k.text());
                m.put("matchType", k.matchType() == EcomAdMatchType.NEGATIVE_PHRASE ? "negativePhrase" : "negativeExact");
                body.add(m);
            }
            List<Long> ids = chunk.stream().map(AdsNegativeKeyword::localId).toList();
            try {
                JsonNode resp = HttpJson.requestRaw("POST", AmazonAdsConnector.endpoint(ctx) + "/sb/negativeKeywords", headers,
                        HttpJson.MAPPER.writeValueAsString(body), "application/json", AmazonAdsConnector.PLATFORM);
                results.addAll(arrayStatus(resp, ids, "keywordId"));
            } catch (BizException e) {
                ids.forEach(id -> results.add(AdsChangeResult.failed(id, e.getMessage())));
            }
        }
        return results;
    }

    private static Map<String, Object> sbParents(AdsChange c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c.adGroupPlatformId() != null) {
            m.put("adGroupId", c.adGroupPlatformId());
        }
        if (c.campaignPlatformId() != null) {
            m.put("campaignId", c.campaignPlatformId());
        }
        return m;
    }

    private static void putBidState(Map<String, Object> m, String bidField, AdsChange c, boolean lower) {
        if (c.bid() != null) {
            m.put(bidField, c.bid());
        }
        if (c.status() != null) {
            m.put("state", lower ? c.status().name().toLowerCase() : c.status().name());
        }
    }

    /** v4 批量修改（207 多状态，按 index 对应） */
    private List<AdsChangeResult> putMulti(AdsContext ctx, String path, String type, String field, List<AdsChange> items, int batch,
                                           Function<AdsChange, Map<String, Object>> toBody) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (items.isEmpty()) {
            return results;
        }
        Map<String, String> headers = connector.scoped(ctx, type);
        for (int i = 0; i < items.size(); i += batch) {
            List<AdsChange> chunk = items.subList(i, Math.min(items.size(), i + batch));
            try {
                JsonNode resp = HttpJson.requestRaw("PUT", AmazonAdsConnector.endpoint(ctx) + path, headers,
                        HttpJson.MAPPER.writeValueAsString(Map.of(field, chunk.stream().map(toBody).toList())), type, AmazonAdsConnector.PLATFORM);
                results.addAll(AmazonAdsConnector.multiStatus(resp.path(field), chunk.stream().map(AdsChange::localId).toList(), null));
            } catch (BizException e) {
                chunk.forEach(c -> results.add(AdsChangeResult.failed(c.localId(), e.getMessage())));
            }
        }
        return results;
    }

    /** SB v3 / SD 的数组报文修改：响应为与请求同序的数组，每项 code 为 SUCCESS 时成功 */
    private List<AdsChangeResult> putArray(AdsContext ctx, String path, List<AdsChange> items, String idField,
                                           Function<AdsChange, Map<String, Object>> toBody) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (items.isEmpty()) {
            return results;
        }
        Map<String, String> headers = connector.scoped(ctx, "application/json");
        for (int i = 0; i < items.size(); i += AmazonAdsConnector.BATCH) {
            List<AdsChange> chunk = items.subList(i, Math.min(items.size(), i + AmazonAdsConnector.BATCH));
            try {
                JsonNode resp = HttpJson.requestRaw("PUT", AmazonAdsConnector.endpoint(ctx) + path, headers,
                        HttpJson.MAPPER.writeValueAsString(chunk.stream().map(toBody).toList()), "application/json", AmazonAdsConnector.PLATFORM);
                results.addAll(arrayStatus(resp, chunk.stream().map(AdsChange::localId).toList(), null));
            } catch (BizException e) {
                chunk.forEach(c -> results.add(AdsChangeResult.failed(c.localId(), e.getMessage())));
            }
        }
        return results;
    }

    /** SB 商品投放修改：PUT /sb/targets，响应 updateTargetSuccessResults / updateTargetErrorResults 按 targetRequestIndex 对应 */
    private List<AdsChangeResult> putSbTargets(AdsContext ctx, List<AdsChange> items) {
        List<AdsChangeResult> results = new ArrayList<>();
        if (items.isEmpty()) {
            return results;
        }
        Map<String, String> headers = connector.scoped(ctx, "application/json");
        for (int i = 0; i < items.size(); i += AmazonAdsConnector.BATCH) {
            List<AdsChange> chunk = items.subList(i, Math.min(items.size(), i + AmazonAdsConnector.BATCH));
            List<Map<String, Object>> body = new ArrayList<>();
            for (AdsChange c : chunk) {
                Map<String, Object> m = sbParents(c);
                m.put("targetId", c.platformId());
                putBidState(m, "bid", c, true);
                body.add(m);
            }
            List<Long> ids = chunk.stream().map(AdsChange::localId).toList();
            try {
                JsonNode resp = HttpJson.requestRaw("PUT", AmazonAdsConnector.endpoint(ctx) + "/sb/targets", headers,
                        HttpJson.MAPPER.writeValueAsString(Map.of("targets", body)), "application/json", AmazonAdsConnector.PLATFORM);
                Map<Integer, AdsChangeResult> byIndex = new LinkedHashMap<>();
                for (JsonNode s : resp.path("updateTargetSuccessResults")) {
                    int idx = s.path("targetRequestIndex").asInt(-1);
                    if (idx >= 0 && idx < ids.size()) {
                        byIndex.put(idx, AdsChangeResult.ok(ids.get(idx)));
                    }
                }
                for (JsonNode e : resp.path("updateTargetErrorResults")) {
                    int idx = e.path("targetRequestIndex").asInt(-1);
                    if (idx >= 0 && idx < ids.size()) {
                        byIndex.put(idx, AdsChangeResult.failed(ids.get(idx), HttpJson.abbreviate(e.path("targetError").toString())));
                    }
                }
                for (int k = 0; k < ids.size(); k++) {
                    results.add(byIndex.getOrDefault(k, AdsChangeResult.failed(ids.get(k), "no result")));
                }
            } catch (BizException e) {
                ids.forEach(id -> results.add(AdsChangeResult.failed(id, e.getMessage())));
            }
        }
        return results;
    }

    /** 数组响应：第 i 项对应第 i 个请求，code 为 SUCCESS 成功（idField 不为空时取新建对象的 ID），否则失败（取 description / details） */
    static List<AdsChangeResult> arrayStatus(JsonNode arr, List<Long> localIds, String idField) {
        List<AdsChangeResult> out = new ArrayList<>();
        for (int i = 0; i < localIds.size(); i++) {
            JsonNode n = arr.isArray() && i < arr.size() ? arr.get(i) : null;
            if (n == null) {
                out.add(AdsChangeResult.failed(localIds.get(i), "no result"));
            } else if ("SUCCESS".equalsIgnoreCase(ConnectorJson.str(n, "code"))) {
                out.add(new AdsChangeResult(localIds.get(i), true, idField == null ? null : ConnectorJson.str(n, idField), null));
            } else {
                out.add(AdsChangeResult.failed(localIds.get(i), HttpJson.abbreviate(ConnectorJson.firstNonBlank(ConnectorJson.str(n, "description"),
                        ConnectorJson.str(n, "details"), n.toString()))));
            }
        }
        return out;
    }

    /** 不支持的操作说明（按当前语言） */
    static String unsupportedText(EcomAdType adType, String key) {
        return Messages.get(key, adType.name());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
