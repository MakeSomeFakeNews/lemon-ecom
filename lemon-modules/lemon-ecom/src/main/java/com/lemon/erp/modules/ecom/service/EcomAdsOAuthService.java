package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.ads.AdsCapability;
import com.lemon.erp.modules.ecom.ads.AdsConnector;
import com.lemon.erp.modules.ecom.ads.AdsProfile;
import com.lemon.erp.modules.ecom.connector.OAuthState;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 广告接口的 OAuth 授权（亚马逊 Login with Amazon）：沿用渠道授权的签名 state（{@link OAuthState}：租户、店铺、回调地址、30 分钟有效），
 * 回调接口 /api/ecom/oauth/callback/amazon-ads 不需要登录（已在 SecurityConfig 放行 /ecom/oauth/callback/**）。
 * 回调换到刷新令牌后自动获取广告配置文件：只有一个时直接选中，多个时由用户回到「广告授权」中选择。
 * 平台调用不在事务中；手工填写刷新令牌的方式仍然可用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomAdsOAuthService {

    /** 回调路径中的标识（/ecom/oauth/callback/amazon-ads） */
    public static final String CALLBACK_PATH = "/ecom/oauth/callback/amazon-ads";

    private final EcomAdsAuthService authService;
    private final EcomStoreService storeService;
    private final OAuthState oauthState;

    /**
     * 回调结果
     *
     * @param profiles        授权账号下的配置文件（获取失败时为空）
     * @param selectedProfile 自动选中的配置文件（只有一个时），没有为 null
     * @param profileError    获取配置文件失败的原因（授权本身已成功）
     */
    public record CallbackResult(EcomStore store, List<AdsProfile> profiles, String selectedProfile, String profileError) {
    }

    /**
     * 授权页地址：需要先保存应用的 clientId / clientSecret 与区域
     *
     * @param redirectUri 回调地址（{后端对外地址}/ecom/oauth/callback/amazon-ads）
     */
    public String authorizeUrl(Long storeId, String redirectUri) {
        EcomStore store = storeService.requireEnabled(storeId);
        AdsConnector connector = authService.requireConnector(store);
        if (!StringUtils.hasText(authService.credentials(store).get("clientId"))) {
            throw new BizException("ecom.error.fix.adsClientIdFirst");
        }
        String state = oauthState.create(TenantContext.currentOrPlatform(), store.getId(), redirectUri);
        return connector.authorizeUrl(authService.context(store), redirectUri, state);
    }

    /** OAuth 回调：按 state 进入对应租户，用授权码换取令牌并保存，再获取配置文件（只有一个时自动选中） */
    public CallbackResult callback(Map<String, String> params) {
        OAuthState.Payload p = oauthState.parse(params.get("state"));
        return TenantContext.callAs(p.tenantId(), () -> {
            EcomStore store = storeService.get(p.storeId());
            if (StringUtils.hasText(params.get("error"))) {
                throw new BizException("ecom.error.fix.adsOauthDenied",
                        StringUtils.hasText(params.get("error_description")) ? params.get("error_description") : params.get("error"));
            }
            String code = params.get("code");
            if (!StringUtils.hasText(code)) {
                throw new BizException("ecom.error.fix.adsOauthNoCode");
            }
            AdsConnector connector = authService.requireConnector(store);
            Map<String, String> creds;
            try {
                creds = connector.exchangeCode(authService.context(store), code, p.redirectUri());
            } catch (BizException e) {
                throw e;
            } catch (RuntimeException e) {
                log.warn("Ads OAuth exchange failed for store {}", store.getCode(), e);
                throw new BizException("ecom.error.hub.oauthFailed", e.getMessage());
            }
            authService.mergeCredentials(store.getId(), creds);
            if (!connector.supports(AdsCapability.PROFILES)) {
                return new CallbackResult(store, List.of(), null, null);
            }
            try {
                EcomStore fresh = storeService.get(store.getId());
                List<AdsProfile> profiles = connector.profiles(authService.requireContext(fresh));
                String selected = null;
                if (profiles.size() == 1) {
                    selected = profiles.get(0).profileId();
                    authService.selectProfile(store.getId(), selected);
                }
                return new CallbackResult(store, profiles, selected, null);
            } catch (RuntimeException e) {
                log.warn("Ads profiles failed after OAuth for store {}: {}", store.getCode(), e.getMessage());
                return new CallbackResult(store, List.of(), null, e.getMessage());
            }
        });
    }

    /** 回调页显示的说明 */
    public static String detail(CallbackResult r) {
        String name = r.store().getName();
        if (r.selectedProfile() != null) {
            return Messages.get("ecom.text.fix.adsOauthProfileSelected", name, r.selectedProfile());
        }
        if (r.profiles().size() > 1) {
            return Messages.get("ecom.text.fix.adsOauthChooseProfile", name, r.profiles().size());
        }
        if (r.profileError() != null) {
            return Messages.get("ecom.text.fix.adsOauthProfileFailed", name, r.profileError());
        }
        return Messages.get("ecom.text.fix.adsOauthDone", name);
    }
}
