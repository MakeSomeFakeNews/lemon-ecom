package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.ads.AdsCapability;
import com.lemon.erp.modules.ecom.ads.AdsConnector;
import com.lemon.erp.modules.ecom.ads.AdsConnectorInfo;
import com.lemon.erp.modules.ecom.ads.AdsContext;
import com.lemon.erp.modules.ecom.ads.AdsProfile;
import com.lemon.erp.modules.ecom.ads.AdsRegistry;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.connector.CredentialField;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.dto.AdsAuthRequest;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 店铺的广告接口授权：与渠道授权分开保存在 ecom_store.ads_*（只追加列，不经过店铺的保存接口），
 * 授权 JSON 由 {@link CredentialCipher} 加密、只写不读；组装调用广告接口的 {@link AdsContext}
 */
@Service
@RequiredArgsConstructor
public class EcomAdsAuthService {

    private final EcomStoreMapper storeMapper;
    private final EcomStoreService storeService;
    private final CredentialCipher cipher;
    private final AdsRegistry registry;

    /** 已实现的广告接口（授权表单） */
    public List<AdsViews.ConnectorView> connectors() {
        List<AdsViews.ConnectorView> list = new ArrayList<>();
        for (AdsConnector c : registry.all()) {
            AdsConnectorInfo i = c.info();
            List<AdsViews.FieldView> fields = i.fields().stream().filter(f -> !f.cached())
                    .map(f -> new AdsViews.FieldView(f.key(), f.labelKey() == null ? f.key() : Messages.get(f.labelKey()), f.secret(), f.required()))
                    .toList();
            list.add(new AdsViews.ConnectorView(i.platform(), i.authType().name(), fields, i.sandboxBaseUrl() != null, i.productionBaseUrl(),
                    i.sandboxBaseUrl(), i.capabilities().stream().map(Enum::name).sorted().toList(), i.docUrl(), i.minBid(), i.needsProfile()));
        }
        return list;
    }

    /** 各店铺的广告授权状态（启用的店铺） */
    public List<AdsViews.StoreAuth> stores() {
        List<EcomStore> stores = storeMapper.selectList(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getStatus, EnableStatus.ENABLED)
                .orderByAsc(EcomStore::getId));
        return stores.stream().map(this::view).toList();
    }

    public AdsViews.StoreAuth store(Long storeId) {
        return view(storeService.get(storeId));
    }

    public EcomStore storeEntity(Long storeId) {
        return storeService.get(storeId);
    }

    private AdsViews.StoreAuth view(EcomStore s) {
        AdsViews.StoreAuth v = new AdsViews.StoreAuth();
        v.setStoreId(s.getId());
        v.setStoreCode(s.getCode());
        v.setStoreName(s.getName());
        v.setPlatform(s.getPlatform());
        v.setCurrency(s.getCurrency());
        v.setConnectorSupported(registry.find(s.getPlatform()).isPresent());
        StoreCredentials c = credentials(s);
        Set<String> cached = registry.find(s.getPlatform()).map(x -> x.info().cachedKeys()).orElse(Set.of());
        v.setHasCredentials(!c.isEmpty());
        v.setCredentialKeys(c.values().keySet().stream().filter(k -> !cached.contains(k)).sorted().toList());
        v.setAdsSandbox(s.getAdsSandbox());
        v.setAdsBaseUrl(s.getAdsBaseUrl());
        v.setAdsProfileId(s.getAdsProfileId());
        v.setAdsSync(s.getAdsSync());
        v.setAdsLastSyncTime(s.getAdsLastSyncTime());
        v.setAdsLastSyncMessage(s.getAdsLastSyncMessage());
        return v;
    }

    /** 保存授权：提交的字段与已保存的合并后加密，环境、地址、配置文件、自动同步一并保存 */
    public void save(Long storeId, AdsAuthRequest req) {
        EcomStore s = storeService.get(storeId);
        AdsConnector connector = registry.find(s.getPlatform()).orElseThrow(() -> new BizException("ecom.error.ads.unsupported", s.getPlatform().name()));
        Set<String> known = new java.util.HashSet<>(connector.info().fields().stream().map(CredentialField::key).toList());
        Map<String, String> submitted = req.getCredentials() == null ? Map.of() : req.getCredentials();
        for (String k : submitted.keySet()) {
            if (!known.contains(k)) {
                throw new BizException("ecom.error.ads.credentialKeyUnknown", k);
            }
        }
        Map<String, String> merged = StoreCredentials.merge(credentials(s).values(), submitted, connector.info().cachedKeys());
        String json = merged.isEmpty() ? null : HttpJson.MAPPER.writeValueAsString(merged);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate()
                .set(EcomStore::getAdsCredentials, cipher.encrypt(json))
                .set(EcomStore::getAdsSandbox, Integer.valueOf(1).equals(req.getSandbox()) ? 1 : 0)
                .set(EcomStore::getAdsBaseUrl, StringUtils.hasText(req.getBaseUrl()) ? req.getBaseUrl().trim() : null)
                .set(EcomStore::getAdsProfileId, StringUtils.hasText(req.getProfileId()) ? req.getProfileId().trim() : null)
                .set(EcomStore::getAdsSync, Integer.valueOf(0).equals(req.getSync()) ? 0 : 1)
                .eq(EcomStore::getId, storeId));
    }

    /** 合并写入授权字段（OAuth 回调换到的令牌），已有的其他字段保留 */
    public void mergeCredentials(Long storeId, Map<String, String> changes) {
        EcomStore s = storeService.get(storeId);
        Map<String, String> merged = new java.util.HashMap<>(credentials(s).values());
        merged.putAll(changes);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate()
                .set(EcomStore::getAdsCredentials, cipher.encrypt(HttpJson.MAPPER.writeValueAsString(merged)))
                .eq(EcomStore::getId, storeId));
    }

    /** 选择广告配置文件（亚马逊 profileId），为空清除 */
    public void selectProfile(Long storeId, String profileId) {
        storeService.get(storeId);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate()
                .set(EcomStore::getAdsProfileId, StringUtils.hasText(profileId) ? profileId.trim() : null)
                .eq(EcomStore::getId, storeId));
    }

    /** 清除广告授权 */
    public void clear(Long storeId) {
        storeService.get(storeId);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(EcomStore::getAdsCredentials, null).set(EcomStore::getAdsProfileId, null)
                .eq(EcomStore::getId, storeId));
    }

    /** 测试授权（不在事务中） */
    public void test(Long storeId) {
        EcomStore s = storeService.get(storeId);
        AdsConnector c = requireConnector(s);
        c.test(requireContext(s));
    }

    /** 授权账号下的广告配置文件 */
    public List<AdsProfile> profiles(Long storeId) {
        EcomStore s = storeService.get(storeId);
        AdsConnector c = requireConnector(s);
        if (!c.supports(AdsCapability.PROFILES)) {
            return List.of();
        }
        return c.profiles(requireContext(s));
    }

    public StoreCredentials credentials(EcomStore s) {
        return StoreCredentials.parse(cipher.decrypt(s.getAdsCredentials()));
    }

    /** 店铺的广告接口与上下文；没有实现接口或没有授权时为空 */
    public Optional<AdsConnector> connector(EcomStore s) {
        if (s == null || !StringUtils.hasText(s.getAdsCredentials())) {
            return Optional.empty();
        }
        return registry.find(s.getPlatform());
    }

    public AdsConnector requireConnector(EcomStore s) {
        return registry.find(s.getPlatform()).orElseThrow(() -> new BizException("ecom.error.ads.unsupported", s.getPlatform().name()));
    }

    public AdsContext requireContext(EcomStore s) {
        AdsContext ctx = context(s);
        if (ctx.credentials().isEmpty()) {
            throw new BizException("ecom.error.ads.credentialsMissing", s.getName());
        }
        return ctx;
    }

    /** 调用广告接口的上下文：刷新令牌后加密写回 ads_credentials */
    public AdsContext context(EcomStore s) {
        EcomStoreService.requireReal(s);
        return new AdsContext(s, credentials(s), Integer.valueOf(1).equals(s.getAdsSandbox()), s.getAdsBaseUrl(), s.getAdsProfileId(),
                c -> storeMapper.update(Wrappers.<EcomStore>lambdaUpdate()
                        .set(EcomStore::getAdsCredentials, cipher.encrypt(HttpJson.MAPPER.writeValueAsString(c.values())))
                        .eq(EcomStore::getId, s.getId())));
    }

    /** 记录同步结果（成功时 message 为空） */
    public void markSynced(Long storeId, LocalDateTime time, String message) {
        String m = message == null || message.length() <= 500 ? message : message.substring(0, 500);
        storeMapper.update(Wrappers.<EcomStore>lambdaUpdate().set(time != null, EcomStore::getAdsLastSyncTime, time)
                .set(EcomStore::getAdsLastSyncMessage, m).eq(EcomStore::getId, storeId));
    }

    /** 有广告授权、开启自动同步的启用店铺 */
    public List<EcomStore> syncable() {
        return storeMapper.selectList(Wrappers.<EcomStore>lambdaQuery().eq(EcomStore::getStatus, EnableStatus.ENABLED)
                .isNotNull(EcomStore::getAdsCredentials).ne(EcomStore::getAdsSync, 0).ne(EcomStore::getDemo, 1).orderByAsc(EcomStore::getId))
                .stream().filter(s -> registry.find(s.getPlatform()).isPresent() && StringUtils.hasText(s.getAdsCredentials())).toList();
    }
}
