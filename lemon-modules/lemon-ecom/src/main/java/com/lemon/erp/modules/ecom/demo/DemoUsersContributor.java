package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.framework.org.UserAccountProvisioner;
import com.lemon.erp.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据 · 运营账号（order 5）：3 个电商运营演示账号（角色电商运营，初始密码为系统参数「初始密码」，首次登录要求修改），
 * 分别负责不同店铺。账号数超出租户套餐上限时不创建（店铺负责人改为当前用户）
 */
@Component
@RequiredArgsConstructor
public class DemoUsersContributor implements DemoContributor {

    /** 登录名与姓名 */
    static final String[][] USERS = {{"demo_lily", "林晓雨"}, {"demo_jack", "周子航"}, {"demo_mia", "陈思琪"}}; // i18n-ignore 演示数据存库
    static final String ECOM_ROLE = "ecom";

    private final UserAccountProvisioner provisioner;

    @Override
    public int order() {
        return 5;
    }

    @Override
    public String key() {
        return "users";
    }

    @Override
    public void generate(DemoContext ctx) {
        Long deptId = SecurityUtils.getLoginUser().deptId();
        List<Long> ids = new ArrayList<>();
        for (String[] u : USERS) {
            ids.add(provisioner.create(u[0], u[1], deptId, null, u[0] + "@demo.lemon-ecom.local", List.of(ECOM_ROLE)));
        }
        // 全部成功后再共享（失败时事务回滚，店铺负责人改为当前用户）
        ctx.put(DemoKeys.OPERATOR_USER_IDS, ids);
    }
}
