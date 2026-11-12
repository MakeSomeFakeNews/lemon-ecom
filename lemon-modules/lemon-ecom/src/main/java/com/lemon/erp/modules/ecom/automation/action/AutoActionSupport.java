package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoRows;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动作处理器的公共方法：接收人（指定用户、角色下的用户、店铺负责人）、必需的关联对象
 */
@Component
@RequiredArgsConstructor
public class AutoActionSupport {

    private final EcomAutoScanMapper scanMapper;
    private final EcomStoreService storeService;

    /** 接收人：指定用户 + 角色下正常状态的内部用户 + 店铺负责人（事件有店铺且勾选时），去重 */
    public Set<Long> recipients(AutoRuleParts.Action a, AutoEvent e) {
        Set<Long> users = new LinkedHashSet<>();
        if (a.userIds() != null) {
            users.addAll(a.userIds());
        }
        if (a.roleIds() != null && !a.roleIds().isEmpty()) {
            users.addAll(scanMapper.selectRoleUsers(a.roleIds()));
        }
        if (Boolean.TRUE.equals(a.storeOwner())) {
            Long owner = storeOwner(e);
            if (owner != null) {
                users.add(owner);
            }
        }
        users.remove(null);
        return users;
    }

    /** 接收人说明（试运行）：用户昵称、角色人数、店铺负责人 */
    public String recipientText(AutoRuleParts.Action a, AutoEvent e) {
        List<String> parts = new ArrayList<>();
        if (a.userIds() != null && !a.userIds().isEmpty()) {
            parts.add(String.join(", ", userNames(a.userIds()).values()));
        }
        if (a.roleIds() != null && !a.roleIds().isEmpty()) {
            Map<Long, String> roles = new LinkedHashMap<>();
            scanMapper.selectRoles().forEach(r -> roles.put(r.id(), r.name()));
            List<String> names = a.roleIds().stream().map(id -> roles.getOrDefault(id, String.valueOf(id))).toList();
            parts.add(Messages.get("ecom.text.auto.roleUsers", String.join(", ", names), scanMapper.selectRoleUsers(a.roleIds()).size()));
        }
        if (Boolean.TRUE.equals(a.storeOwner())) {
            Long owner = storeOwner(e);
            parts.add(Messages.get("ecom.text.auto.storeOwnerIs", owner == null ? Messages.get("ecom.text.auto.none") : userNames(List.of(owner)).get(owner)));
        }
        return parts.isEmpty() ? Messages.get("ecom.text.auto.none") : String.join("; ", parts);
    }

    public Map<Long, String> userNames(java.util.Collection<Long> ids) {
        Map<Long, String> map = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        Map<Long, String> found = new LinkedHashMap<>();
        scanMapper.selectUserNames(ids).forEach(r -> found.put(AutoRows.id(r, "id"), AutoRows.str(r, "nickname")));
        ids.forEach(id -> map.put(id, found.getOrDefault(id, String.valueOf(id))));
        return map;
    }

    public Long storeOwner(AutoEvent e) {
        if (e.storeId() == null) {
            return null;
        }
        EcomStore s = storeService.byIds(List.of(e.storeId())).stream().findFirst().orElse(null);
        return s == null ? null : s.getOwnerId();
    }

    /** 动作需要的关联对象，事件中没有时失败 */
    public static Long require(AutoEvent e, AutoRef ref) {
        Long id = e.ref(ref);
        if (id == null) {
            throw new BizException("ecom.error.auto.refMissing", Messages.get(AutoText.key("ecom.enum.auto.ref", ref.name())));
        }
        return id;
    }

    /** 接收人配置至少有一项 */
    public static void requireRecipients(AutoRuleParts.Action a) {
        boolean any = a.userIds() != null && !a.userIds().isEmpty() || a.roleIds() != null && !a.roleIds().isEmpty()
                || Boolean.TRUE.equals(a.storeOwner());
        if (!any) {
            throw new BizException("ecom.error.auto.recipientRequired");
        }
    }
}
