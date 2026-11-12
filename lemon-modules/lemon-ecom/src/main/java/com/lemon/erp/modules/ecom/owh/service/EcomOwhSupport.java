package com.lemon.erp.modules.ecom.owh.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.datascope.DataScope;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.owh.connector.OwhConnector;
import com.lemon.erp.modules.ecom.owh.connector.OwhContext;
import com.lemon.erp.modules.ecom.owh.connector.OwhRegistry;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 海外仓公共逻辑：授权加解密（CredentialCipher，只写不读）、接口上下文、以系统身份执行（定时任务中生成出入库单需要登录身份）
 */
@Component
@RequiredArgsConstructor
public class EcomOwhSupport {

    /** 错误原因的最大长度（与表字段一致） */
    public static final int ERROR_MAX = 500;

    private final CredentialCipher cipher;
    private final OwhRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OwhConnector connector(OwhProvider provider) {
        return registry.require(provider);
    }

    /** 账号的接口上下文（解密授权）；停用的账号提示 */
    public OwhContext context(EcomOwhAccount a) {
        if (a.getStatus() == EnableStatus.DISABLED) {
            throw new BizException("ecom.error.owh.accountDisabled", a.getName());
        }
        return new OwhContext(a.getName(), decrypt(a.getCredentials()), a.getSandbox() != null && a.getSandbox() == 1, a.getApiBaseUrl(),
                a.getCurrency());
    }

    /** 保存时提交的授权与已保存的合并（空值不修改），加密后返回；服务商变了不保留旧的 */
    public String merge(EcomOwhAccount old, OwhProvider provider, Map<String, String> auth) {
        Map<String, String> merged = new HashMap<>(old == null || old.getProvider() != provider ? Map.of() : decrypt(old.getCredentials()));
        if (auth != null) {
            auth.forEach((k, v) -> {
                if (StringUtils.hasText(k) && StringUtils.hasText(v)) {
                    merged.put(k.trim(), v.trim());
                }
            });
        }
        return merged.isEmpty() ? null : cipher.encrypt(objectMapper.writeValueAsString(merged));
    }

    public Map<String, String> decrypt(String stored) {
        String json = cipher.decrypt(stored);
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JacksonException e) {
            return Map.of();
        }
    }

    /**
     * 没有登录用户（定时任务）时以系统身份执行：全部权限、全部数据，用户 ID 为空；已登录时直接执行
     */
    public static <T> T asSystem(Supplier<T> action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return action.get();
        }
        SecurityContext saved = SecurityContextHolder.getContext();
        LoginUser system = new LoginUser(null, "system", null, Set.of(), Set.of(PermissionService.ALL_PERMISSION), DataScope.ALL,
                null, TenantContext.currentOrPlatform(), null, null);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(system, null, List.of()));
        SecurityContextHolder.setContext(ctx);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(saved);
        }
    }

    public static void asSystem(Runnable action) {
        asSystem(() -> {
            action.run();
            return null;
        });
    }

    public static String abbreviate(String s) {
        return s == null || s.length() <= ERROR_MAX ? s : s.substring(0, ERROR_MAX);
    }
}
