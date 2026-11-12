package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    /**
     * 修改店铺时提交的授权与已保存的合并：提交的键覆盖原值，值为空串的键删除，没提交的键（如 OAuth 换来的令牌）保留；
     * keep 中的键（连接器缓存的令牌、过期时间，见 {@link CredentialField#cached()}）始终保留原值，提交的值忽略
     */
    public static Map<String, String> merge(Map<String, String> saved, Map<String, String> submitted, Set<String> keep) {
        Map<String, String> merged = new LinkedHashMap<>(saved == null ? Map.of() : saved);
        if (submitted != null) {
            submitted.forEach((k, v) -> {
                if (keep != null && keep.contains(k)) {
                    return;
                }
                if (v == null || v.isBlank()) {
                    merged.remove(k);
                } else {
                    merged.put(k, v);
                }
            });
        }
        return merged;
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
