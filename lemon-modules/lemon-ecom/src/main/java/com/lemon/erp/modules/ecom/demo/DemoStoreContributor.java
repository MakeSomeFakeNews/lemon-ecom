package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.mapper.EcomSyncLogMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.StoreAuthStatus;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 演示数据 · 店铺（order 20）：10 个店铺覆盖亚马逊（美 / 德 / 日，FBA）、虾皮（马来 / 新加坡）、TikTok Shop 美国（海外仓）、Temu、速卖通、
 * Shopify、eBay，编码 DEMO- 开头，每个店铺自动建对应客户。
 * <p>
 * 授权状态有已授权 / 需重新授权 / 未授权几种：授权信息是明显的占位值（不是真实凭证），接口地址指向本机不可用端口
 * {@value #DEAD_URL}，并关闭自动同步订单与库存回传，定时任务即使执行也不会访问真实平台；需重新授权的店铺写一条失败的授权日志
 */
@Component
@RequiredArgsConstructor
public class DemoStoreContributor implements DemoContributor {

    /** 演示店铺的自定义接口地址：本机 discard 端口，连接会被拒绝 */
    static final String DEAD_URL = "http://127.0.0.1:9";
    static final String JOB = "JOB";

    private final EcomStoreService storeService;
    private final EcomSyncLogMapper syncLogMapper;

    @Override
    public int order() {
        return 20;
    }

    @Override
    public String key() {
        return "stores";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> warehouses = ctx.get(DemoContributors.WAREHOUSE_BY_CODE);
        List<Long> operators = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        Long me = SecurityUtils.getUserId();
        Map<String, Long> ids = new LinkedHashMap<>();
        Map<String, Long> owners = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (DemoCatalog.Store d : DemoCatalog.STORES) {
            EcomStore s = new EcomStore();
            s.setCode(d.code());
            s.setName(d.name());
            s.setPlatform(d.platform());
            s.setMarketplace(d.marketplace());
            s.setCurrency(d.currency());
            s.setTimezone(d.timezone());
            s.setWarehouseId(warehouses.get(DemoCatalog.LOCAL_WAREHOUSE.code()));
            s.setPlatformWarehouseId(d.platformWh() == null ? null : warehouses.get(d.platformWh()));
            s.setCommissionRate(BigDecimal.valueOf(d.commission()));
            boolean fba = d.platformWh() != null;
            s.setLeadDays(fba ? 45 : 20);
            s.setSafetyDays(fba ? 20 : 10);
            s.setOwnerId(owner(d, operators, me));
            s.setSellerId(d.auth() == StoreAuthStatus.NONE ? null : "DEMO" + Math.abs(d.code().hashCode() % 100000));
            s.setSandbox(1);
            s.setApiBaseUrl(DEAD_URL);
            s.setOrderSync(0);
            s.setInventorySync(0);
            s.setDemo(1);
            s.setInventoryBuffer(BigDecimal.ZERO);
            s.setStatus(EnableStatus.ENABLED);
            s.setRemark("演示店铺：授权信息为占位值，已关闭自动同步"); // i18n-ignore 演示数据存库
            Map<String, String> cred = credentials(d);
            s.setCredentials(cred == null ? null : HttpJson.MAPPER.writeValueAsString(new TreeMap<>(cred)));
            Long id = storeService.create(s);
            ids.put(d.code(), id);
            owners.put(d.code(), s.getOwnerId());
            if (d.auth() != StoreAuthStatus.NONE) {
                storeService.markAuthorized(id, cred, s.getSellerId());
                log(id, SyncType.AUTH, true, now.minusDays(30), null);
                log(id, SyncType.ORDERS, true, now.minusHours(2), null);
                log(id, SyncType.LISTINGS, true, now.minusHours(20), null);
            }
            if (d.auth() == StoreAuthStatus.REAUTH) {
                log(id, SyncType.AUTH, false, now.minusHours(5), d.platform() == EcomPlatform.AMAZON
                        ? "LWA 换取访问令牌失败：invalid_grant（The provided refresh token is invalid or expired）" // i18n-ignore 演示数据存库
                        : "刷新访问令牌失败：error_auth（refresh_token 已过期，请重新授权）"); // i18n-ignore 演示数据存库
                storeService.markSyncMessage(id, "需要重新授权：刷新令牌已失效"); // i18n-ignore 演示数据存库
            }
        }
        ctx.put(DemoKeys.STORE_BY_CODE, ids);
        ctx.put(DemoKeys.STORE_IDS, new ArrayList<>(ids.values()));
        ctx.put(DemoContributors.STORE_OWNER, owners);
    }

    /** 负责人：美国站林晓雨，欧日站周子航，其他平台陈思琪；独立站为当前用户 */
    private static Long owner(DemoCatalog.Store d, List<Long> operators, Long me) {
        if (operators == null || operators.size() < 3 || d.platform() == EcomPlatform.SHOPIFY) {
            return me;
        }
        if (d.platform() == EcomPlatform.AMAZON) {
            return d == DemoCatalog.STORES.get(0) ? operators.get(0) : operators.get(1);
        }
        return d.platform() == EcomPlatform.TIKTOK ? operators.get(0) : operators.get(2);
    }

    /** 占位授权信息（只为演示授权状态，不能调用真实平台） */
    private static Map<String, String> credentials(DemoCatalog.Store d) {
        if (d.auth() == StoreAuthStatus.NONE) {
            return null;
        }
        Map<String, String> c = new LinkedHashMap<>();
        switch (d.platform()) {
            case AMAZON -> {
                c.put("region", switch (d.marketplace()) {
                    case "DE" -> "EU";
                    case "JP" -> "FE";
                    default -> "NA";
                });
                c.put("marketplaceId", switch (d.marketplace()) {
                    case "DE" -> "A1PA6795UKMFR9";
                    case "JP" -> "A1VC38T7YXB528";
                    default -> "ATVPDKIKX0DER";
                });
                c.put("clientId", "amzn1.application-oa2-client.demo-placeholder");
                c.put("clientSecret", "demo-placeholder-secret");
                c.put("refreshToken", "Atzr|demo-placeholder-refresh-token");
            }
            case SHOPEE -> {
                c.put("partnerId", "100000");
                c.put("partnerKey", "demo-placeholder-partner-key");
                c.put("region", d.marketplace());
                c.put("shopId", "200000");
                c.put("accessToken", "demo-placeholder-access-token");
                c.put("refreshToken", "demo-placeholder-refresh-token");
                c.put("tokenExpireAt", "0");
            }
            case ALIEXPRESS -> {
                c.put("appKey", "demo-placeholder-app-key");
                c.put("appSecret", "demo-placeholder-app-secret");
                c.put("accessToken", "demo-placeholder-access-token");
            }
            case SHOPIFY -> {
                c.put("shopDomain", "lemon-demo.myshopify.com");
                c.put("adminAccessToken", "shpat_demo_placeholder");
            }
            default -> c.put("demo", "placeholder");
        }
        return c;
    }

    private void log(Long storeId, SyncType type, boolean success, LocalDateTime time, String message) {
        EcomSyncLog l = new EcomSyncLog();
        l.setStoreId(storeId);
        l.setSyncType(type);
        l.setTriggerBy(JOB);
        l.setSuccess(success ? 1 : 0);
        l.setFetched(success && type != SyncType.AUTH ? 20 : 0);
        l.setCreated(success && type == SyncType.ORDERS ? 12 : 0);
        l.setUpdated(success && type != SyncType.AUTH ? 8 : 0);
        l.setMessage(message);
        l.setStartTime(time);
        l.setEndTime(time.plusSeconds(3));
        syncLogMapper.insert(l);
    }
}
