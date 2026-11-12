package com.lemon.erp.modules.ecom.dev.service;

import java.util.Locale;
import java.util.Map;

/**
 * 站点（业务字典 ecom_marketplace 的内置取值）对应的币种和国家代码，利润试算在没有填写币种、佣金费率也没有币种时使用；
 * 租户自行扩展的站点按站点代码当作国家代码
 */
public final class DevMarketplaces {

    private static final Map<String, String> CURRENCIES = Map.ofEntries(
            Map.entry("US", "USD"), Map.entry("CA", "CAD"), Map.entry("MX", "MXN"), Map.entry("BR", "BRL"),
            Map.entry("UK", "GBP"), Map.entry("GB", "GBP"), Map.entry("DE", "EUR"), Map.entry("FR", "EUR"), Map.entry("IT", "EUR"),
            Map.entry("ES", "EUR"), Map.entry("NL", "EUR"), Map.entry("PL", "PLN"), Map.entry("SE", "SEK"), Map.entry("AE", "AED"),
            Map.entry("SA", "SAR"), Map.entry("JP", "JPY"), Map.entry("AU", "AUD"), Map.entry("SG", "SGD"), Map.entry("MY", "MYR"),
            Map.entry("TH", "THB"), Map.entry("PH", "PHP"), Map.entry("VN", "VND"), Map.entry("ID", "IDR"));

    private DevMarketplaces() {
    }

    /** 站点币种，未知站点返回 null */
    public static String currency(String marketplace) {
        return marketplace == null ? null : CURRENCIES.get(marketplace.trim().toUpperCase(Locale.ROOT));
    }

    /** 站点对应的目的国代码（物流费率、税率按国家匹配）：UK → GB，全球站为空 */
    public static String country(String marketplace) {
        if (marketplace == null || marketplace.isBlank()) {
            return null;
        }
        String m = marketplace.trim().toUpperCase(Locale.ROOT);
        return switch (m) {
            case "UK" -> "GB";
            case "GLOBAL" -> null;
            default -> m;
        };
    }
}
