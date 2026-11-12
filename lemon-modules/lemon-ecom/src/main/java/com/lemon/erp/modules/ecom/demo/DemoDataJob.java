package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.LoginUserLoader;
import com.lemon.erp.framework.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 演示数据的执行入口：
 * <ul>
 *   <li>页面触发：{@link #generateAsync} 在异步线程中以发起人的身份执行（@Async 传递租户，登录用户在这里设置）</li>
 *   <li>启动时自动生成：配置 {@code lemon.ecom.demo-data.auto-generate=true}（环境变量 LEMON_ECOM_DEMO_DATA）时，应用启动完成后
 *       给平台租户（单租户模式即当前库）以租户管理员的身份生成一次，已经生成过则跳过；本地 {@code ./gradlew bootTestRun} 默认开启</li>
 * </ul>
 * 生成的内容存库，按默认语言（中文）书写。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataJob {

    /** 平台租户的管理员（init.sql 中的 admin） */
    static final long PLATFORM_ADMIN_ID = 1L;

    private final DemoDataService demoDataService;
    private final LoginUserLoader loginUserLoader;

    @Value("${lemon.ecom.demo-data.auto-generate:false}")
    private boolean autoGenerate;

    /** 已调用 {@link DemoDataService#begin()} 后在异步线程中生成 */
    @Async
    public void generateAsync(LoginUser user) {
        runAs(user, demoDataService::run);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!autoGenerate) {
            return;
        }
        // 不阻塞启动：在后台线程中给平台租户生成
        Thread t = new Thread(() -> {
            try {
                TenantContext.runAs(TenantContext.PLATFORM_TENANT_ID, this::autoGenerate);
            } finally {
                TenantContext.clear();
            }
        }, "ecom-demo-data");
        t.setDaemon(true);
        t.start();
    }

    private void autoGenerate() {
        try {
            if (demoDataService.generated()) {
                log.info("Demo data already exists, skip auto generation");
                return;
            }
            LoginUser admin = loginUserLoader.loadLoginUser(PLATFORM_ADMIN_ID);
            if (admin == null) {
                log.warn("Demo data auto generation skipped: admin user {} not found", PLATFORM_ADMIN_ID);
                return;
            }
            demoDataService.begin();
            log.info("Generating ecom demo data ...");
            runAs(admin, demoDataService::run);
        } catch (RuntimeException e) {
            log.warn("Demo data auto generation failed", e);
        }
    }

    /** 以指定登录用户（制单人、部门、数据权限）和默认语言执行，结束后恢复 */
    static void runAs(LoginUser user, Runnable action) {
        SecurityContext saved = SecurityContextHolder.getContext();
        Locale savedLocale = LocaleContextHolder.getLocale();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        SecurityContextHolder.setContext(ctx);
        LocaleContextHolder.setLocale(Messages.DEFAULT_LOCALE);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(saved);
            LocaleContextHolder.setLocale(savedLocale);
        }
    }
}
