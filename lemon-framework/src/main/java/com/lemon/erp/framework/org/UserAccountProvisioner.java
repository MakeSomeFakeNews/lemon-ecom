package com.lemon.erp.framework.org;

/**
 * 为新员工开通登录账号，由系统模块实现（{@code SysOrgSyncService}）。初始密码取系统参数「初始密码」，首次登录要求修改；
 * 授予内置的「普通员工」角色（员工自助、发起审批），其他角色由管理员在用户管理中分配
 */
public interface UserAccountProvisioner {

    /**
     * @param username 登录名，已存在时报错
     * @return 新账号 ID
     */
    Long create(String username, String nickname, Long deptId, String phone, String email);

    /**
     * 开通账号并授予指定编码的内置角色（不存在或停用的角色跳过，不再授予普通员工角色），用于初始化数据（如电商演示数据的运营账号）
     *
     * @param roleCodes 角色编码，如 ecom（电商运营）
     */
    default Long create(String username, String nickname, Long deptId, String phone, String email, java.util.List<String> roleCodes) {
        return create(username, nickname, deptId, phone, email);
    }
}
