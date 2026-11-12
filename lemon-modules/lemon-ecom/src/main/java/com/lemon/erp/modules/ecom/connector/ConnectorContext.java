package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.modules.ecom.entity.EcomStore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 调用渠道接口的上下文：店铺、解密后的授权信息、接口地址（店铺填写的地址 &gt; 沙箱 / 正式环境默认地址），
 * 以及保存刷新后令牌的回调（连接器刷新 access_token 后调用 {@link #updateCredentials}，由框架加密写回店铺）
 */
public final class ConnectorContext {

    private final EcomStore store;
    private StoreCredentials credentials;
    private final boolean sandbox;
    private final String baseUrlOverride;
    private final Consumer<StoreCredentials> saver;

    public ConnectorContext(EcomStore store, StoreCredentials credentials, boolean sandbox, String baseUrlOverride,
                            Consumer<StoreCredentials> saver) {
        this.store = store;
        this.credentials = credentials;
        this.sandbox = sandbox;
        this.baseUrlOverride = baseUrlOverride == null || baseUrlOverride.isBlank() ? null : stripSlash(baseUrlOverride.trim());
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

    /**
     * 接口根地址：店铺上填写了自定义地址时用它（私有化网关、测试用的模拟服务器），否则按沙箱开关取连接器的默认地址
     */
    public String baseUrl(ConnectorInfo info) {
        if (baseUrlOverride != null) {
            return baseUrlOverride;
        }
        String url = sandbox && info.sandboxBaseUrl() != null ? info.sandboxBaseUrl() : info.productionBaseUrl();
        return stripSlash(url);
    }

    /** 店铺填写的自定义地址（没有为 null），按地区区分接口地址的平台在没有自定义地址时自行选择 */
    public String baseUrlOverride() {
        return baseUrlOverride;
    }

    /** 合并更新授权信息（刷新令牌后的 access_token、过期时间等）并持久化 */
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
