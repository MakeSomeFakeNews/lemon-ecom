package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

/**
 * 店铺的渠道接口授权（JSON 对象，键值随平台不同）。
 * <ul>
 *   <li>亚马逊 SP-API：region（NA / EU / FE）、marketplaceId、clientId、clientSecret、refreshToken</li>
 *   <li>速卖通开放平台：appKey、appSecret、accessToken</li>
 * </ul>
 */
public record StoreCredentials(Map<String, String> values) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static StoreCredentials parse(String json) {
        if (json == null || json.isBlank()) {
            return new StoreCredentials(Collections.emptyMap());
        }
        try {
            Map<String, String> map = MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
            });
            return new StoreCredentials(map == null ? Collections.emptyMap() : map);
        } catch (RuntimeException e) {
            throw new BizException("ecom.error.credentialsInvalidJson");
        }
    }

    public boolean isEmpty() {
        return values == null || values.isEmpty();
    }

    public String get(String key) {
        String v = values.get(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 必填项，缺少时提示在店铺中补充授权信息 */
    public String require(String key) {
        String v = get(key);
        if (v == null) {
            throw new BizException("ecom.error.credentialMissing", key);
        }
        return v;
    }
}
