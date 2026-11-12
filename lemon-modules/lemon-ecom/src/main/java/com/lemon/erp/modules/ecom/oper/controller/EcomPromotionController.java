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
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.dto.PromotionQuery;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.service.EcomPromotionService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 促销管理 /ecom/oper/promotion：活动维护、生效 / 取消、推送平台、促销日历、效果分析
 */
@RestController
@RequestMapping("/ecom/oper/promotion")
@RequiredArgsConstructor
public class EcomPromotionController {

    private final EcomPromotionService promotionService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:promotion:list')")
    public R<PageResult<EcomPromotion>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, PromotionQuery query) {
        return R.ok(promotionService.page(current, size, query));
    }

    /** 促销日历：与 [beginDate, endDate] 有交集、未取消的活动 */
    @GetMapping("/calendar")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:list')")
    public R<List<OperViews.CalendarItem>> calendar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                    @RequestParam(required = false) Long storeId) {
        return R.ok(promotionService.calendar(beginDate, endDate, storeId));
    }

    /** 能推送活动的平台 */
    @GetMapping("/platforms")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:list')")
    public R<List<EcomPlatform>> platforms() {
        return R.ok(promotionService.pushPlatforms());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:list')")
    public R<EcomPromotion> detail(@PathVariable Long id) {
        return R.ok(promotionService.detail(id));
    }

    /** 效果分析：活动前 / 中 / 后同等天数的对比 */
    @GetMapping("/{id}/effect")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:list')")
    public R<OperViews.PromotionEffect> effect(@PathVariable Long id) {
        return R.ok(promotionService.effect(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:promotion:add')")
    @OperLog(title = "促销活动", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody EcomPromotion p) {
        return R.ok(promotionService.create(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:edit')")
    @OperLog(title = "促销活动", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomPromotion p) {
        promotionService.update(id, p);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:remove')")
    @OperLog(title = "促销活动", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        promotionService.delete(ids);
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:confirm')")
    @OperLog(title = "促销活动", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> confirm(@PathVariable Long id) {
        promotionService.confirm(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:confirm')")
    @OperLog(title = "促销活动", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        promotionService.cancel(id);
        return R.ok();
    }

    /** 推送到平台（虾皮折扣活动、TikTok 限时价） */
    @PostMapping("/{id}/push")
    @PreAuthorize("@ss.hasPerm('ecom:promotion:push')")
    @OperLog(title = "促销活动", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<EcomPromotion> push(@PathVariable Long id) {
        return R.ok(promotionService.push(id));
    }
}
