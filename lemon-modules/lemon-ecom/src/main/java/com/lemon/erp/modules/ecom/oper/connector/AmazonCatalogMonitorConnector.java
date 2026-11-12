package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 亚马逊商品监控：Selling Partner API 的 Catalog Items API 2022-04-01 与 Product Pricing API v0，可以查询任意 ASIN（包括竞品），
 * 只要授权店铺在该站点（marketplaceId 取授权店铺的）。
 * <ol>
 *   <li>GET /catalog/2022-04-01/items/{asin}?marketplaceIds=…&amp;includedData=summaries,attributes,images,salesRanks：
 *       summaries.itemName / brand → 标题、品牌；attributes.bullet_point[].value → 五点；images 中 variant = MAIN 的 link → 主图；
 *       salesRanks.displayGroupRanks[0] → BSR 大类（title、rank），classificationRanks[0] → 小类。404 视为商品已不存在（下架）</li>
 *   <li>GET /products/pricing/v0/items/{asin}/offers?MarketplaceId=…&amp;ItemCondition=New（getItemOffers）：payload.Summary.TotalOfferCount → 在售卖家数
 *       （没有时按 NumberOfOffers[].OfferCount 合计），BuyBoxPrices[0].ListingPrice（没有时 LowestPrices[0]）→ 售价；在售卖家数为 0 视为下架</li>
 * </ol>
 * 授权复用店铺的渠道授权（region、marketplaceId、clientId、clientSecret、refreshToken），访问令牌由调用方先用 {@code AmazonSpConnector.ensureToken} 换取，
 * 地址按区域与沙箱开关选择，店铺填写了自定义地址时指向它。SP-API 不提供评分和评论数，这两项需要手工录入或导入。
 * <p>
 * 说明：按亚马逊公开文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>应用需要「Product Listing」（Catalog）与「Pricing」角色，未授权时返回 403</li>
 *   <li>getItemOffers 限速每秒 0.5 次（突发 1），监控商品较多时定时任务耗时较长；429 由 {@link HttpJson} 自动退避重试</li>
 *   <li>displayGroupRanks 与 classificationRanks 同时有多个时取第一个是否符合卖家精灵等工具的「大类 / 小类」口径</li>
 *   <li>竞品 ASIN 在 getItemOffers 中被限制（如受限品类）时的返回</li>
 * </ul>
 */
@Component
public class AmazonCatalogMonitorConnector implements MonitorConnector {

    static final String PLATFORM = "Amazon";
    static final String CATALOG_PATH = "/catalog/2022-04-01/items/";
    static final String OFFERS_PATH = "/products/pricing/v0/items/";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://sellingpartnerapi-na.amazon.com",
            "EU", "https://sellingpartnerapi-eu.amazon.com",
            "FE", "https://sellingpartnerapi-fe.amazon.com");
    private static final Map<String, String> SANDBOX_ENDPOINTS = Map.of(
            "NA", "https://sandbox.sellingpartnerapi-na.amazon.com",
            "EU", "https://sandbox.sellingpartnerapi-eu.amazon.com",
            "FE", "https://sandbox.sellingpartnerapi-fe.amazon.com");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.AMAZON;
    }

    @Override
    public MonitorData fetch(ConnectorContext ctx, String asin) {
        StoreCredentials c = ctx.credentials();
        String marketplace = c.require("marketplaceId");
        Map<String, String> headers = Map.of("x-amz-access-token", c.require("accessToken"), "Accept", "application/json");
        String base = endpoint(ctx);
        JsonNode item;
        try {
            item = HttpJson.get(base + CATALOG_PATH + path(asin) + "?marketplaceIds=" + ConnectorSigns.enc(marketplace)
                    + "&includedData=" + ConnectorSigns.enc("summaries,attributes,images,salesRanks"), headers, PLATFORM);
        } catch (ConnectorHttpException e) {
            if (e.statusCode() == 404) {
                return MonitorData.delisted();
            }
            throw e;
        }
        JsonNode summary = forMarketplace(item.path("summaries"), marketplace);
        JsonNode ranks = forMarketplace(item.path("salesRanks"), marketplace);
        JsonNode main = ranks == null ? null : first(ranks.path("displayGroupRanks"));
        JsonNode sub = ranks == null ? null : first(ranks.path("classificationRanks"));
        JsonNode offers = HttpJson.get(base + OFFERS_PATH + path(asin) + "/offers?MarketplaceId=" + ConnectorSigns.enc(marketplace)
                + "&ItemCondition=New", headers, PLATFORM).path("payload");
        Integer sellers = sellerCount(offers.path("Summary"));
        JsonNode price = price(offers.path("Summary"));
        ListingStatus status = sellers != null && sellers == 0 ? ListingStatus.INACTIVE : ListingStatus.ACTIVE;
        return new MonitorData(summary == null ? null : HttpJson.text(summary, "itemName"),
                summary == null ? null : firstText(HttpJson.text(summary, "brand"), HttpJson.text(summary, "brandName")),
                mainImage(item.path("images"), marketplace), bullets(item.path("attributes").path("bullet_point"), marketplace),
                main == null ? null : HttpJson.text(main, "title"), main == null ? null : intOrNull(main.path("rank")),
                sub == null ? null : HttpJson.text(sub, "title"), sub == null ? null : intOrNull(sub.path("rank")),
                null, null, sellers,
                price == null ? null : HttpJson.decimal(price, "Amount"), price == null ? null : HttpJson.text(price, "CurrencyCode"), status);
    }

    /** 在售卖家数：TotalOfferCount，没有时按 NumberOfOffers 合计 */
    static Integer sellerCount(JsonNode summary) {
        if (summary.has("TotalOfferCount")) {
            return summary.path("TotalOfferCount").asInt(0);
        }
        if (!summary.path("NumberOfOffers").isArray()) {
            return null;
        }
        int n = 0;
        for (JsonNode o : summary.path("NumberOfOffers")) {
            n += o.path("OfferCount").asInt(0);
        }
        return n;
    }

    /** 售价：Buy Box 价，没有时取最低价 */
    static JsonNode price(JsonNode summary) {
        JsonNode bb = first(summary.path("BuyBoxPrices"));
        if (bb != null && bb.has("ListingPrice")) {
            return bb.path("ListingPrice");
        }
        JsonNode low = first(summary.path("LowestPrices"));
        return low != null && low.has("ListingPrice") ? low.path("ListingPrice") : null;
    }

    static String mainImage(JsonNode images, String marketplace) {
        JsonNode group = forMarketplace(images, marketplace);
        if (group == null) {
            return null;
        }
        String any = null;
        for (JsonNode img : group.path("images")) {
            String link = HttpJson.text(img, "link");
            if ("MAIN".equals(HttpJson.text(img, "variant"))) {
                return link;
            }
            any = any == null ? link : any;
        }
        return any;
    }

    static String bullets(JsonNode list, String marketplace) {
        List<String> out = new ArrayList<>();
        for (JsonNode b : list) {
            String mp = HttpJson.text(b, "marketplace_id");
            String v = HttpJson.text(b, "value");
            if (v != null && !v.isBlank() && (mp == null || mp.equals(marketplace))) {
                out.add(v.trim());
            }
        }
        return out.isEmpty() ? null : String.join("\n", out);
    }

    private static JsonNode forMarketplace(JsonNode list, String marketplace) {
        JsonNode first = null;
        for (JsonNode n : list) {
            if (marketplace.equals(HttpJson.text(n, "marketplaceId"))) {
                return n;
            }
            first = first == null ? n : first;
        }
        return first;
    }

    private static JsonNode first(JsonNode list) {
        return list != null && list.isArray() && !list.isEmpty() ? list.get(0) : null;
    }

    private static Integer intOrNull(JsonNode v) {
        return v == null || v.isMissingNode() || v.isNull() ? null : v.asInt();
    }

    private static String firstText(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String endpoint(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ctx.credentials().get("region") == null ? "NA" : ctx.credentials().get("region").toUpperCase();
        String endpoint = (ctx.sandbox() ? SANDBOX_ENDPOINTS : ENDPOINTS).get(region);
        if (endpoint == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region);
        }
        return endpoint;
    }

    private static String path(String s) {
        return ConnectorSigns.enc(s).replace("+", "%20");
    }
}
