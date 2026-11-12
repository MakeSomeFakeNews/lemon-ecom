package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Client;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Context;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688AccountRequest;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Account;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688AccountMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 1688 采购账号：AppKey / AppSecret（AES-256-GCM 加密保存，与店铺授权同一密钥）、OAuth 授权（回调地址
 * {后端地址}/api/ecom/oauth/callback/alibaba1688，state 带租户、账号与过期时间并签名）、令牌自动刷新（过期前 10 分钟）、默认收货地址。
 * 调用 1688 接口前用 {@link #context(Long)} 取得已解密的上下文；外部调用不在事务中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ali1688AccountService {

    /** state 前缀（与店铺授权的 state 区分） */
    static final String STATE_PREFIX = "A1688";
    static final long STATE_TTL_MILLIS = 30 * 60 * 1000L;
    /** 令牌提前刷新的时间（分钟） */
    static final long REFRESH_AHEAD_MINUTES = 10;

    private final Ecom1688AccountMapper accountMapper;
    private final Ecom1688OrderMapper orderMapper;
    private final CredentialCipher cipher;
    private final Ali1688Client client;

    public List<Ecom1688Account> list() {
        List<Ecom1688Account> list = accountMapper.selectList(Wrappers.<Ecom1688Account>lambdaQuery()
                .orderByDesc(Ecom1688Account::getIsDefault).orderByAsc(Ecom1688Account::getId));
        list.forEach(Ali1688AccountService::mark);
        return list;
    }

    public Ecom1688Account get(Long id) {
        Ecom1688Account a = id == null ? null : accountMapper.selectById(id);
        if (a == null) {
            throw new BizException("ecom.error.dev.accountNotFound");
        }
        return mark(a);
    }

    private static Ecom1688Account mark(Ecom1688Account a) {
        a.setSecretSet(StringUtils.hasText(a.getAppSecret()));
        a.setAuthorized(StringUtils.hasText(a.getAccessToken()));
        return a;
    }

    @Transactional
    public Long create(Ali1688AccountRequest req) {
        checkName(req.name(), null);
        Ecom1688Account a = new Ecom1688Account();
        apply(a, req);
        a.setAppSecret(cipher.encrypt(req.appSecret()));
        accountMapper.insert(a);
        if (a.getIsDefault() == 1) {
            clearOtherDefaults(a.getId());
        }
        return a.getId();
    }

    /** 修改：AppSecret 留空不变；AppKey 变化时清除已授权的令牌 */
    @Transactional
    public void update(Long id, Ali1688AccountRequest req) {
        Ecom1688Account old = get(id);
        checkName(req.name(), id);
        Ecom1688Account a = new Ecom1688Account();
        apply(a, req);
        boolean keyChanged = !old.getAppKey().equals(a.getAppKey());
        accountMapper.update(Wrappers.<Ecom1688Account>lambdaUpdate()
                .set(Ecom1688Account::getName, a.getName())
                .set(Ecom1688Account::getAppKey, a.getAppKey())
                .set(StringUtils.hasText(req.appSecret()), Ecom1688Account::getAppSecret, cipher.encrypt(req.appSecret()))
                .set(Ecom1688Account::getApiUrl, a.getApiUrl())
                .set(Ecom1688Account::getReceiverName, a.getReceiverName())
                .set(Ecom1688Account::getReceiverMobile, a.getReceiverMobile())
                .set(Ecom1688Account::getProvince, a.getProvince())
                .set(Ecom1688Account::getCity, a.getCity())
                .set(Ecom1688Account::getDistrict, a.getDistrict())
                .set(Ecom1688Account::getTown, a.getTown())
                .set(Ecom1688Account::getAddress, a.getAddress())
                .set(Ecom1688Account::getPostCode, a.getPostCode())
                .set(Ecom1688Account::getIsDefault, a.getIsDefault())
                .set(Ecom1688Account::getStatus, a.getStatus())
                .set(Ecom1688Account::getRemark, a.getRemark())
                .set(keyChanged, Ecom1688Account::getAccessToken, null)
                .set(keyChanged, Ecom1688Account::getRefreshToken, null)
                .set(keyChanged, Ecom1688Account::getAuthorizedTime, null)
                .set(Ecom1688Account::getUpdateTime, LocalDateTime.now())
                .eq(Ecom1688Account::getId, id));
        if (a.getIsDefault() == 1) {
            clearOtherDefaults(id);
        }
    }

    private void apply(Ecom1688Account a, Ali1688AccountRequest r) {
        a.setName(r.name().trim());
        a.setAppKey(r.appKey().trim());
        a.setApiUrl(StringUtils.hasText(r.apiUrl()) ? r.apiUrl().trim().replaceAll("/+$", "") : null);
        a.setReceiverName(trim(r.receiverName()));
        a.setReceiverMobile(trim(r.receiverMobile()));
        a.setProvince(trim(r.province()));
        a.setCity(trim(r.city()));
        a.setDistrict(trim(r.district()));
        a.setTown(trim(r.town()));
        a.setAddress(trim(r.address()));
        a.setPostCode(trim(r.postCode()));
        a.setIsDefault(r.isDefault() != null && r.isDefault() == 1 ? 1 : 0);
        a.setStatus(r.status() == null ? EnableStatus.ENABLED : r.status());
        a.setRemark(trim(r.remark()));
    }

    private void checkName(String name, Long excludeId) {
        if (accountMapper.exists(Wrappers.<Ecom1688Account>lambdaQuery().eq(Ecom1688Account::getName, name.trim())
                .ne(excludeId != null, Ecom1688Account::getId, excludeId))) {
            throw new BizException("ecom.error.dev.accountNameExists", name.trim());
        }
    }

    private void clearOtherDefaults(Long id) {
        accountMapper.update(Wrappers.<Ecom1688Account>lambdaUpdate().set(Ecom1688Account::getIsDefault, 0)
                .ne(Ecom1688Account::getId, id).eq(Ecom1688Account::getIsDefault, 1));
    }

    /** 删除：已经下过 1688 采购单的账号不能删除（可以停用） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            Ecom1688Account a = get(id);
            if (orderMapper.exists(Wrappers.<Ecom1688Order>lambdaQuery().eq(Ecom1688Order::getAccountId, id))) {
                throw new BizException("ecom.error.dev.accountInUse", a.getName());
            }
        }
        accountMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- OAuth

    /** 授权页地址（卖家在新窗口登录 1688 同意授权后回调本系统） */
    public String authorizeUrl(Long id, String redirectUri) {
        Ecom1688Account a = get(id);
        if (!a.getSecretSet()) {
            throw new BizException("ecom.error.dev.appSecretMissing", a.getName());
        }
        return client.authorizeUrl(a.getAppKey(), redirectUri, createState(TenantContext.currentOrPlatform(), id, redirectUri));
    }

    /** OAuth 回调：按 state 进入对应租户，授权码换令牌并保存 */
    public Ecom1688Account oauthCallback(String state, String code) {
        StatePayload p = parseState(state);
        if (!StringUtils.hasText(code)) {
            throw new BizException("ecom.error.dev.oauthCodeMissing");
        }
        return TenantContext.callAs(p.tenantId(), () -> {
            Ecom1688Account a = get(p.accountId());
            Ali1688Context ctx = new Ali1688Context(gateway(a), a.getAppKey(), cipher.decrypt(a.getAppSecret()), null);
            try {
                saveToken(a.getId(), client.exchangeCode(ctx, code, p.redirectUri()), true);
            } catch (BizException e) {
                recordError(a.getId(), e.getMessage());
                throw e;
            }
            return a;
        });
    }

    /** 手动刷新令牌（同时用于测试授权是否有效） */
    public void refresh(Long id) {
        Ecom1688Account a = get(id);
        String refresh = cipher.decrypt(a.getRefreshToken());
        if (!StringUtils.hasText(refresh)) {
            throw new BizException("ecom.error.dev.ali1688NotAuthorized");
        }
        Ali1688Context ctx = new Ali1688Context(gateway(a), a.getAppKey(), cipher.decrypt(a.getAppSecret()), null);
        try {
            saveToken(id, client.refresh(ctx, refresh), false);
        } catch (BizException e) {
            recordError(id, e.getMessage());
            throw e;
        }
    }

    /** 清除授权（令牌） */
    public void revoke(Long id) {
        get(id);
        accountMapper.update(Wrappers.<Ecom1688Account>lambdaUpdate().set(Ecom1688Account::getAccessToken, null)
                .set(Ecom1688Account::getRefreshToken, null).set(Ecom1688Account::getTokenExpire, null).set(Ecom1688Account::getRefreshExpire, null)
                .set(Ecom1688Account::getAuthorizedTime, null).eq(Ecom1688Account::getId, id));
    }

    private void saveToken(Long id, Ali1688Models.Token t, boolean authorize) {
        accountMapper.update(Wrappers.<Ecom1688Account>lambdaUpdate()
                .set(Ecom1688Account::getAccessToken, cipher.encrypt(t.accessToken()))
                .set(t.refreshToken() != null, Ecom1688Account::getRefreshToken, cipher.encrypt(t.refreshToken()))
                .set(Ecom1688Account::getTokenExpire, t.expireTime())
                .set(t.refreshExpireTime() != null, Ecom1688Account::getRefreshExpire, t.refreshExpireTime())
                .set(t.memberId() != null, Ecom1688Account::getMemberId, t.memberId())
                .set(t.loginId() != null, Ecom1688Account::getLoginId, t.loginId())
                .set(authorize, Ecom1688Account::getAuthorizedTime, LocalDateTime.now())
                .set(Ecom1688Account::getLastMessage, null)
                .eq(Ecom1688Account::getId, id));
    }

    /** 记录最近一次调用失败的原因 */
    void recordError(Long id, String message) {
        accountMapper.update(Wrappers.<Ecom1688Account>lambdaUpdate()
                .set(Ecom1688Account::getLastMessage, message == null ? null : message.length() > 500 ? message.substring(0, 500) : message)
                .eq(Ecom1688Account::getId, id));
    }

    // ---------------------------------------------------------------- 调用上下文

    /**
     * 调用接口用的上下文：账号须启用并已授权；访问令牌快过期时先刷新
     *
     * @param id 为空时取默认账号（默认 → 第一个启用且已授权的账号）
     */
    public Ali1688Context context(Long id) {
        Ecom1688Account a = id == null ? defaultAccount() : get(id);
        if (a.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("ecom.error.dev.accountDisabled", a.getName());
        }
        String token = cipher.decrypt(a.getAccessToken());
        if (!StringUtils.hasText(token)) {
            throw new BizException("ecom.error.dev.ali1688NotAuthorized");
        }
        if (a.getTokenExpire() != null && a.getTokenExpire().isBefore(LocalDateTime.now().plusMinutes(REFRESH_AHEAD_MINUTES))
                && StringUtils.hasText(a.getRefreshToken())) {
            refresh(a.getId());
            token = cipher.decrypt(accountMapper.selectById(a.getId()).getAccessToken());
        }
        return new Ali1688Context(gateway(a), a.getAppKey(), cipher.decrypt(a.getAppSecret()), token);
    }

    /** 默认账号 ID（没有可用账号时报错） */
    public Long defaultAccountId() {
        return defaultAccount().getId();
    }

    /** 有没有启用且已授权的账号 */
    public boolean hasUsableAccount() {
        return accountMapper.exists(Wrappers.<Ecom1688Account>lambdaQuery().eq(Ecom1688Account::getStatus, EnableStatus.ENABLED)
                .isNotNull(Ecom1688Account::getAccessToken));
    }

    private Ecom1688Account defaultAccount() {
        List<Ecom1688Account> list = accountMapper.selectList(Wrappers.<Ecom1688Account>lambdaQuery()
                .eq(Ecom1688Account::getStatus, EnableStatus.ENABLED).isNotNull(Ecom1688Account::getAccessToken)
                .orderByDesc(Ecom1688Account::getIsDefault).orderByAsc(Ecom1688Account::getId));
        if (list.isEmpty()) {
            throw new BizException("ecom.error.dev.noAccount");
        }
        return mark(list.get(0));
    }

    private static String gateway(Ecom1688Account a) {
        return StringUtils.hasText(a.getApiUrl()) ? a.getApiUrl() : Ali1688Client.DEFAULT_GATEWAY;
    }

    // ---------------------------------------------------------------- state

    record StatePayload(Long tenantId, Long accountId, String redirectUri) {
    }

    String createState(Long tenantId, Long accountId, String redirectUri) {
        long expire = System.currentTimeMillis() + STATE_TTL_MILLIS;
        String body = STATE_PREFIX + "|" + tenantId + "|" + accountId + "|" + expire + "|" + redirectUri;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + cipher.sign(encoded);
    }

    StatePayload parseState(String state) {
        if (state == null || state.indexOf('.') < 0) {
            throw new BizException("ecom.error.oauthStateInvalid");
        }
        String encoded = state.substring(0, state.indexOf('.'));
        String sig = state.substring(state.indexOf('.') + 1);
        if (!MessageDigest.isEqual(cipher.sign(encoded).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException("ecom.error.oauthStateInvalid");
        }
        String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\|", 5);
        if (parts.length != 5 || !STATE_PREFIX.equals(parts[0])) {
            throw new BizException("ecom.error.oauthStateInvalid");
        }
        if (Long.parseLong(parts[3]) < System.currentTimeMillis()) {
            throw new BizException("ecom.error.oauthStateExpired");
        }
        return new StatePayload(Long.valueOf(parts[1]), Long.valueOf(parts[2]), parts[4]);
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
