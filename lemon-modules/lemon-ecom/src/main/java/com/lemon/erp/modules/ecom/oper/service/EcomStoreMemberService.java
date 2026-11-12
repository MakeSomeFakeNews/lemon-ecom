package com.lemon.erp.modules.ecom.oper.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.entity.EcomStoreMember;
import com.lemon.erp.modules.ecom.oper.mapper.EcomStoreMemberMapper;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 店铺授权（店铺数据权限）：用户可见的店铺 = 负责的店铺（ecom_store.owner_id）+ 被授权的店铺（ecom_store_member）。
 * 超级管理员、角色数据权限为「全部数据」的用户不受限制；其余用户在电商各页面只能看到可见店铺的数据（见 {@link EcomStoreScope}）。
 * 可见范围在每次请求时计算，授权修改后立即生效，不需要清理缓存
 */
@Service
@RequiredArgsConstructor
public class EcomStoreMemberService {

    public static final String ROLE_DICT = "ecom_store_member_role";

    private final EcomStoreMemberMapper memberMapper;
    private final EcomStoreService storeService;
    private final UserLookup userLookup;
    private final DictLookup dictLookup;

    /**
     * 当前用户可见的店铺 ID；没有登录用户（定时任务）、超级管理员、全部数据权限时返回 null（不限制）
     */
    public Set<Long> visibleStoreIds() {
        if (SecurityUtils.getUserIdOrNull() == null) {
            return null;
        }
        LoginUser user = SecurityUtils.getLoginUser();
        if (unrestricted(user)) {
            return null;
        }
        return Set.copyOf(memberMapper.selectVisibleStoreIds(user.userId()));
    }

    static boolean unrestricted(LoginUser user) {
        return user.isAdmin() || user.dataScope() == null || user.dataScope().all();
    }

    /**
     * 指定用户负责和被授权的店铺 ID（不判断该用户是否不受限制；定时任务按订阅人生成运营日报时使用，调用方先判断角色数据权限）
     */
    public Set<Long> ownedOrGrantedStoreIds(Long userId) {
        return Set.copyOf(memberMapper.selectVisibleStoreIds(userId));
    }

    /** 用户是否不受店铺限制（超级管理员、全部数据权限） */
    public static boolean unrestrictedUser(LoginUser user) {
        return unrestricted(user);
    }

    // ---------------------------------------------------------------- 按店铺

    /** 店铺授权页：店铺（当前用户可见的）、负责人与授权成员；userId 只看某用户负责或被授权的店铺 */
    public PageResult<OperViews.StoreAuthRow> page(long current, long size, String keyword, EcomPlatform platform, Long userId) {
        Page<OperViews.StoreAuthRow> page = Page.of(current, size);
        memberMapper.selectStorePage(page, keyword, platform, userId);
        List<OperViews.StoreAuthRow> rows = page.getRecords();
        if (!rows.isEmpty()) {
            Map<Long, List<EcomStoreMember>> byStore = memberMapper.selectByStoreIds(rows.stream().map(OperViews.StoreAuthRow::getStoreId).toList())
                    .stream().collect(Collectors.groupingBy(EcomStoreMember::getStoreId, LinkedHashMap::new, Collectors.toList()));
            rows.forEach(r -> r.setMembers(byStore.getOrDefault(r.getStoreId(), List.of())));
        }
        return PageResult.of(page);
    }

    public List<EcomStoreMember> members(Long storeId) {
        storeService.get(storeId);
        return memberMapper.selectByStoreIds(List.of(storeId));
    }

    /** 保存店铺的授权成员（整体替换）；负责人不需要授权 */
    @Transactional
    public void saveMembers(Long storeId, OperRequests.MembersRequest req) {
        EcomStore store = storeService.get(storeId);
        List<EcomStoreMember> items = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (OperRequests.Member m : req.members() == null ? List.<OperRequests.Member>of() : req.members()) {
            if (!seen.add(m.userId()) || m.userId().equals(store.getOwnerId())) {
                continue;
            }
            items.add(member(storeId, m.userId(), m.memberRole(), m.remark()));
        }
        memberMapper.delete(Wrappers.<EcomStoreMember>lambdaQuery().eq(EcomStoreMember::getStoreId, storeId));
        if (!items.isEmpty()) {
            memberMapper.insertBatch(items);
        }
    }

    // ---------------------------------------------------------------- 按用户

    /** 用户负责和被授权的店铺；unrestricted 表示该用户不受店铺限制（只能判断当前登录用户本人） */
    public OperViews.UserStores userStores(Long userId) {
        UserLookup.UserBrief user = userLookup.require(userId, "ecom.error.oper.userNotFound");
        Long me = SecurityUtils.getUserIdOrNull();
        boolean unrestricted = me != null && me.equals(userId) && unrestricted(SecurityUtils.getLoginUser());
        return new OperViews.UserStores(userId, user.nickname(), unrestricted, memberMapper.selectOwnedStores(userId), memberMapper.selectByUser(userId));
    }

    /**
     * 按用户授权（整体替换该用户被授权的店铺）：只处理当前操作人可见的店铺，看不到的店铺上已有的授权保持不变
     */
    @Transactional
    public void saveUserStores(OperRequests.UserStoresRequest req) {
        userLookup.requireActive(req.userId(), "ecom.error.oper.userNotFound");
        dictLookup.check(ROLE_DICT, req.memberRole(), "ecom.label.oper.memberRole");
        Set<Long> target = new LinkedHashSet<>(req.storeIds() == null ? List.of() : req.storeIds());
        List<EcomStore> stores = target.stream().map(storeService::get).toList();
        Set<Long> visible = EcomStoreScope.visibleStoreIds();
        List<EcomStoreMember> existing = memberMapper.selectList(Wrappers.<EcomStoreMember>lambdaQuery().eq(EcomStoreMember::getUserId, req.userId()));
        List<Long> removeIds = existing.stream().filter(m -> visible == null || visible.contains(m.getStoreId()))
                .filter(m -> !target.contains(m.getStoreId())).map(EcomStoreMember::getId).toList();
        if (!removeIds.isEmpty()) {
            memberMapper.deleteByIds(removeIds);
        }
        Set<Long> has = existing.stream().map(EcomStoreMember::getStoreId).collect(Collectors.toSet());
        List<EcomStoreMember> add = new ArrayList<>();
        for (EcomStore s : stores) {
            if (!has.contains(s.getId()) && !req.userId().equals(s.getOwnerId())) {
                add.add(member(s.getId(), req.userId(), req.memberRole(), null));
            }
        }
        if (!add.isEmpty()) {
            memberMapper.insertBatch(add);
        }
    }

    /** 直接授权一个用户（演示数据、测试用），已授权时修改岗位 */
    @Transactional
    public void grant(Long storeId, Long userId, String memberRole) {
        EcomStoreMember old = memberMapper.selectOne(Wrappers.<EcomStoreMember>lambdaQuery().eq(EcomStoreMember::getStoreId, storeId)
                .eq(EcomStoreMember::getUserId, userId));
        if (old != null) {
            old.setMemberRole(memberRole);
            memberMapper.updateById(old);
            return;
        }
        memberMapper.insert(member(storeId, userId, memberRole, null));
    }

    private EcomStoreMember member(Long storeId, Long userId, String role, String remark) {
        userLookup.require(userId, "ecom.error.oper.userNotFound");
        dictLookup.check(ROLE_DICT, role, "ecom.label.oper.memberRole");
        EcomStoreMember m = new EcomStoreMember();
        m.setStoreId(storeId);
        m.setUserId(userId);
        m.setMemberRole(role);
        m.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        m.setCreateBy(SecurityUtils.getUserIdOrNull());
        return m;
    }

    /** 按登录名查找账号 ID（演示数据用），不存在为 null */
    public Long userIdByUsername(String username) {
        return memberMapper.selectUserIdByUsername(username);
    }

    /** 用户昵称，不存在为 null */
    public String nickname(Long userId) {
        return userId == null ? null : memberMapper.selectNickname(userId);
    }

    /** 店铺不存在或不可见时报错 */
    void requireStoreVisible(Long storeId) {
        if (!EcomStoreScope.canAccess(storeId)) {
            throw new BizException("ecom.error.storeNotFound");
        }
    }
}
