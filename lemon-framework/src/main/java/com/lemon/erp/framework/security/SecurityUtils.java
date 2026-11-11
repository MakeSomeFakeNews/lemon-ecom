package com.lemon.erp.framework.security;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.datascope.DataScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
            return user;
        }
        throw new BizException(R.UNAUTHORIZED, "framework.error.notSigned");
    }

    public static Long getUserId() {
        return getLoginUser().userId();
    }

    /** 当前登录用户 ID，未登录（如异步任务中）返回 null */
    public static Long getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user ? user.userId() : null;
    }

    public static DataScope getDataScope() {
        return getLoginUser().dataScope();
    }

    /** 当前登录用户名，未登录返回 null（不抛异常，供日志等场景使用） */
    public static String getUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof LoginUser user ? user.username() : null;
    }
}
