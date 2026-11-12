package com.lemon.erp.modules.ecom.oper.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.entity.EcomStoreMember;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.oper.service.EcomStoreMemberService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 店铺授权 /ecom/oper/store-auth：把店铺分配给用户（运营、客服、助理），用户在电商各页面只能看到负责和被授权的店铺
 */
@RestController
@RequestMapping("/ecom/oper/store-auth")
@RequiredArgsConstructor
public class EcomStoreAuthController {

    private final EcomStoreMemberService memberService;

    /** 店铺与授权成员；userId 只看某用户负责或被授权的店铺 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:storeAuth:list')")
    public R<PageResult<OperViews.StoreAuthRow>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword, @RequestParam(required = false) EcomPlatform platform,
                                                      @RequestParam(required = false) Long userId) {
        return R.ok(memberService.page(current, size, keyword, platform, userId));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:storeAuth:list')")
    public R<List<EcomStoreMember>> members(@PathVariable Long storeId) {
        return R.ok(memberService.members(storeId));
    }

    /** 保存店铺的授权成员（整体替换） */
    @PutMapping("/store/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:storeAuth:edit')")
    @OperLog(title = "店铺授权", type = OperType.GRANT) // i18n-ignore 操作日志标题存库
    public R<Void> saveMembers(@PathVariable Long storeId, @Valid @RequestBody OperRequests.MembersRequest req) {
        memberService.saveMembers(storeId, req);
        return R.ok();
    }

    /** 用户负责和被授权的店铺 */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@ss.hasPerm('ecom:storeAuth:list')")
    public R<OperViews.UserStores> userStores(@PathVariable Long userId) {
        return R.ok(memberService.userStores(userId));
    }

    /** 按用户授权店铺（整体替换该用户在可见店铺上的授权） */
    @PutMapping("/user")
    @PreAuthorize("@ss.hasPerm('ecom:storeAuth:edit')")
    @OperLog(title = "店铺授权", type = OperType.GRANT) // i18n-ignore 操作日志标题存库
    public R<Void> saveUserStores(@Valid @RequestBody OperRequests.UserStoresRequest req) {
        memberService.saveUserStores(req);
        return R.ok();
    }

    /** 当前用户的店铺范围：restricted 为 false 时不受限制，否则 storeIds 为可见的店铺（登录即可访问） */
    @GetMapping("/mine")
    public R<Map<String, Object>> mine() {
        Set<Long> ids = EcomStoreScope.visibleStoreIds();
        return R.ok(ids == null ? Map.of("restricted", false) : Map.of("restricted", true, "storeIds", ids));
    }
}
