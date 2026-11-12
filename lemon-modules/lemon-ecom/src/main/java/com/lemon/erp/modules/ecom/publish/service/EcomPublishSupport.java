package com.lemon.erp.modules.ecom.publish.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.publish.connector.ListingRef;
import com.lemon.erp.modules.ecom.service.EcomBundleService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 刊登与定价的公共方法：组装调用平台接口的上下文（复用店铺授权，先由订单连接器刷新令牌）、图片公网地址、汇率、成本、JSON 字段读写
 */
@Component
@RequiredArgsConstructor
public class EcomPublishSupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<LinkedHashMap<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final EcomSyncService syncService;
    private final ConnectorRegistry channelRegistry;
    private final ExchangeRateProvider exchangeRateProvider;
    private final WmsMaterialService materialService;
    private final EcomBundleService bundleService;

    /** 后端外网地址（含 /api），图片是相对地址（/api/files/...）时拼成公网地址交给平台 */
    @Value("${lemon.ecom.public-url:}")
    private String publicUrl;

    /** 有授权的上下文（没有授权时报错），平台有订单连接器时先刷新令牌 */
    public ConnectorContext prepare(EcomStore store) {
        ConnectorContext ctx = syncService.requireContext(store);
        channelRegistry.find(store.getPlatform()).ifPresent(c -> c.ensureToken(ctx));
        return ctx;
    }

    /**
     * 演示店铺（ecom_store.demo = 1，由演示数据生成）：不调用平台接口——发布直接记为失败、改价 / 改库存只改本地、不拉取竞品价格，
     * 避免定时任务（刊登队列、自动调价）用演示授权外呼
     */
    public static boolean isDemoStore(EcomStore store) {
        return store != null && store.demoStore();
    }

    /** 已配置后端外网地址（图片相对地址能转为公网地址） */
    public boolean publicUrlConfigured() {
        return publicUrl != null && !publicUrl.isBlank();
    }

    public String publicUrl() {
        return publicUrlConfigured() ? publicUrl.trim() : null;
    }

    /** 图片中是否有系统内的相对地址（需要配置外网地址才能发布） */
    public static boolean hasRelativeImages(List<String> images) {
        return images != null && images.stream().anyMatch(u -> u != null && !u.isBlank() && !u.trim().startsWith("http://") && !u.trim().startsWith("https://"));
    }

    /** 订单连接器（库存推送） */
    public ConnectorRegistry channelRegistry() {
        return channelRegistry;
    }

    /**
     * 图片的公网地址：http(s) 开头的原样返回；/api/files/... 这样的相对地址按 lemon.ecom.public-url 拼接（public-url 含 /api），没有配置时报错
     */
    public List<String> absoluteImages(List<String> images) {
        List<String> out = new ArrayList<>();
        for (String url : images) {
            if (url == null || url.isBlank()) {
                continue;
            }
            String u = url.trim();
            if (u.startsWith("http://") || u.startsWith("https://")) {
                out.add(u);
                continue;
            }
            if (publicUrl == null || publicUrl.isBlank()) {
                throw new BizException("ecom.error.pub.publicUrlMissing");
            }
            String base = publicUrl.replaceAll("/+$", "");
            String path = u.startsWith("/api/") && base.endsWith("/api") ? u.substring(4) : u;
            out.add(base + (path.startsWith("/") ? path : "/" + path));
        }
        return out;
    }

    /** 店铺币种当月汇率（1 店铺币种 = rate 本位币），没有维护时报错 */
    public BigDecimal rate(String currency) {
        LocalDate d = LocalDate.now();
        BigDecimal rate = exchangeRateProvider.rate(currency, d);
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", currency, d.getYear() + "-" + String.format("%02d", d.getMonthValue()));
        }
        return rate;
    }

    /** 当月汇率，没有维护时为 null */
    public BigDecimal rateOrNull(String currency) {
        return currency == null ? null : exchangeRateProvider.rate(currency, LocalDate.now());
    }

    /** 物料的参考成本（本位币）：物料 ID → 成本，没有参考成本的不在结果中 */
    public Map<Long, BigDecimal> materialCosts(Collection<Long> materialIds) {
        Map<Long, BigDecimal> out = new HashMap<>();
        materialService.mapByIds(materialIds).forEach((id, m) -> {
            if (m.getRefCost() != null && m.getRefCost().signum() > 0) {
                out.put(id, m.getRefCost());
            }
        });
        return out;
    }

    /** 在线商品的单件成本（本位币）= 各组成物料的参考成本 × 数量；没有映射或有组件没有成本的不在结果中 */
    public Map<Long, BigDecimal> listingCosts(Collection<Long> listingIds) {
        Map<Long, List<BundleComponent>> components = bundleService.componentsOf(listingIds);
        Map<Long, BigDecimal> costs = materialCosts(components.values().stream().flatMap(List::stream).map(BundleComponent::materialId).distinct().toList());
        Map<Long, BigDecimal> out = new HashMap<>();
        components.forEach((listingId, list) -> {
            BigDecimal sum = BigDecimal.ZERO;
            for (BundleComponent c : list) {
                BigDecimal cost = costs.get(c.materialId());
                if (cost == null) {
                    return;
                }
                sum = sum.add(cost.multiply(Decimals.nz(c.qty())));
            }
            out.put(listingId, sum);
        });
        return out;
    }

    /** 物料：多规格款式返回其下启用的 SKU（按编码），否则返回自己 */
    public List<WmsMaterial> skusOf(WmsMaterial material) {
        if (!material.isTemplateMaterial()) {
            return List.of(material);
        }
        List<WmsMaterial> list = new ArrayList<>(materialService.page(1, 500, null, null, EnableStatus.ENABLED, material.getId()).records());
        list.sort(java.util.Comparator.comparing(WmsMaterial::getCode));
        return list;
    }

    public WmsMaterialService materialService() {
        return materialService;
    }

    /** 在线商品 → 平台引用（ID 与附加信息） */
    public ListingRef ref(EcomListing l) {
        return new ListingRef(l.getSellerSku(), l.getPlatformItemId(), l.getParentItemId(), readMap(l.getPlatformExtra()));
    }

    // ---------------------------------------------------------------- JSON

    public static List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(HttpJson.MAPPER.readValue(json, STRING_LIST));
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    public static Map<String, String> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return HttpJson.MAPPER.readValue(json, STRING_MAP);
        } catch (RuntimeException e) {
            return new LinkedHashMap<>();
        }
    }

    /** 列表 / 对象 → JSON（空为 null），去掉空白项 */
    public static String writeList(List<String> list) {
        if (list == null) {
            return null;
        }
        List<String> clean = list.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();
        return clean.isEmpty() ? null : HttpJson.MAPPER.writeValueAsString(clean);
    }

    public static String writeMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Map<String, String> clean = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k != null && !k.isBlank() && v != null && !v.isBlank()) {
                clean.put(k.trim(), v.trim());
            }
        });
        return clean.isEmpty() ? null : HttpJson.MAPPER.writeValueAsString(clean);
    }
}
