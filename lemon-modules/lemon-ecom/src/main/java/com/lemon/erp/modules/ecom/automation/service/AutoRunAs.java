package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.LoginUser;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 以规则创建人的身份（制单人、部门、数据权限、店铺权限）和默认语言（存库内容为中文）执行动作，结束后恢复原来的上下文
 */
final class AutoRunAs {

    private AutoRunAs() {
    }

    static <T> T call(LoginUser user, Supplier<T> action) {
        SecurityContext saved = SecurityContextHolder.getContext();
        Locale savedLocale = LocaleContextHolder.getLocale();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        if (user != null) {
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        }
        SecurityContextHolder.setContext(ctx);
        LocaleContextHolder.setLocale(Messages.DEFAULT_LOCALE);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(saved);
            LocaleContextHolder.setLocale(savedLocale);
        }
    }
}
