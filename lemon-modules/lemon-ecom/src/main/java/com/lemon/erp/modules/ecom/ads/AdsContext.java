package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.entity.EcomStore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 调用广告接口的上下文：店铺、解密后的广告授权（与渠道授权分开保存在 ecom_store.ads_credentials）、环境、自定义地址、
 * 广告配置文件 ID，以及保存刷新后令牌的回调（连接器调用 {@link #updateCredentials}，由框架加密写回店铺）
 */
public final class AdsContext {

    private final EcomStore store;
    private StoreCredentials credentials;
    private final boolean sandbox;
    private final String baseUrlOverride;
    private final String profileId;
    private final Consumer<StoreCredentials> saver;

    public AdsContext(EcomStore store, StoreCredentials credentials, boolean sandbox, String baseUrlOverride, String profileId,
                      Consumer<StoreCredentials> saver) {
        this.store = store;
        this.credentials = credentials;
        this.sandbox = sandbox;
        this.baseUrlOverride = baseUrlOverride == null || baseUrlOverride.isBlank() ? null : stripSlash(baseUrlOverride.trim());
        this.profileId = profileId == null || profileId.isBlank() ? null : profileId.trim();
        this.saver = saver;
    }

    public EcomStore store() {
        return store;
    }

    public StoreCredentials credentials() {
        return credentials;
    }

    public boolean sandbox() {
        return sandbox;
    }

    /** 广告配置文件 ID（亚马逊 profileId），没有选择时为 null */
    public String profileId() {
        return profileId;
    }

    /** 自定义地址（私有网关、测试用的模拟服务器），没有为 null */
    public String baseUrlOverride() {
        return baseUrlOverride;
    }

    /** 接口根地址：自定义地址 &gt; 沙箱 / 正式环境默认地址 */
    public String baseUrl(AdsConnectorInfo info) {
        if (baseUrlOverride != null) {
            return baseUrlOverride;
        }
        return stripSlash(sandbox && info.sandboxBaseUrl() != null ? info.sandboxBaseUrl() : info.productionBaseUrl());
    }

    /** 合并更新授权信息（刷新后的访问令牌、过期时间等）并持久化 */
    public void updateCredentials(Map<String, String> changes) {
        Map<String, String> merged = new HashMap<>(credentials.values());
        changes.forEach((k, v) -> {
            if (v == null) {
                merged.remove(k);
            } else {
                merged.put(k, v);
            }
        });
        credentials = new StoreCredentials(merged);
        if (saver != null) {
            saver.accept(credentials);
        }
    }

    private static String stripSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
