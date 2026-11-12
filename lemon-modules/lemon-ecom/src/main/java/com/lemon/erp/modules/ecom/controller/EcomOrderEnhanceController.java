package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomOrderNote;
import com.lemon.erp.modules.ecom.service.EcomOrderBatchService;
import com.lemon.erp.modules.ecom.service.EcomOrderCheckService;
import com.lemon.erp.modules.ecom.service.EcomOrderManualService;
import com.lemon.erp.modules.ecom.service.EcomOrderNoteService;
import com.lemon.erp.modules.ecom.service.EcomOrderTagService;
import com.lemon.erp.modules.ecom.service.EcomPartialShipService;
import com.lemon.erp.modules.ecom.service.EcomTrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 订单增强 /ecom/order/*：手工建单、补发单与售后成本、打标签 / 去标签、内部备注、地址校验与利润预估（查看 / 重新校验 / 修改地址）、
 * 批量指定发货仓与物流渠道、订单的物流轨迹；/ecom/package/{id}/ship-confirm 按包裹重新回传发货（部分发货）
 */
@RestController
@RequestMapping("/ecom")
@RequiredArgsConstructor
public class EcomOrderEnhanceController {

    private final EcomOrderManualService manualService;
    private final EcomOrderTagService tagService;
    private final EcomOrderNoteService noteService;
    private final EcomOrderCheckService checkService;
    private final EcomOrderBatchService batchService;
    private final EcomTrackService trackService;
    private final EcomPartialShipService partialShipService;

    /** 手工创建订单，返回订单 ID */
    @PostMapping("/order/manual")
    @PreAuthorize("@ss.hasPerm('ecom:order:add')")
    @OperLog(title = "手工订单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> createManual(@Validated @RequestBody OrderEnhanceViews.ManualOrder req) {
        return R.ok(manualService.createManual(req));
    }

    /** 从原订单创建补发单，返回补发单 ID */
    @PostMapping("/order/{id}/resend")
    @PreAuthorize("@ss.hasPerm('ecom:order:resend')")
    @OperLog(title = "补发单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> createResend(@PathVariable Long id, @Validated @RequestBody OrderEnhanceViews.Resend req) {
        return R.ok(manualService.createResend(id, req));
    }

    /** 原订单的补发单与售后成本 */
    @GetMapping("/order/{id}/resends")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<OrderEnhanceViews.ResendSummary> resends(@PathVariable Long id) {
        return R.ok(manualService.resends(id));
    }

    /** 批量打标签，返回新增的打标数 */
    @PostMapping("/order/tag")
    @PreAuthorize("@ss.hasPerm('ecom:order:tag')")
    @OperLog(title = "订单标签", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> tag(@Validated @RequestBody OrderEnhanceViews.TagRequest req) {
        return R.ok(tagService.tag(req.ids(), req.tagIds()));
    }

    /** 批量去标签，返回去掉的打标数 */
    @PostMapping("/order/untag")
    @PreAuthorize("@ss.hasPerm('ecom:order:tag')")
    @OperLog(title = "订单标签", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> untag(@Validated @RequestBody OrderEnhanceViews.TagRequest req) {
        return R.ok(tagService.untag(req.ids(), req.tagIds()));
    }

    @GetMapping("/order/{id}/notes")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<List<EcomOrderNote>> notes(@PathVariable Long id) {
        return R.ok(noteService.list(id));
    }

    @PostMapping("/order/{id}/notes")
    @PreAuthorize("@ss.hasPerm('ecom:order:tag')")
    @OperLog(title = "订单备注", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> addNote(@PathVariable Long id, @Validated @RequestBody OrderEnhanceViews.NoteRequest req) {
        return R.ok(noteService.add(id, req.content()));
    }

    /** 地址校验、黑名单与利润预估的当前结果 */
    @GetMapping("/order/{id}/check")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<OrderEnhanceViews.CheckResult> check(@PathVariable Long id) {
        return R.ok(checkService.result(id));
    }

    /** 重新校验地址、匹配黑名单、预估利润（地址异常修复后回到待审核） */
    @PostMapping("/order/{id}/check")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    public R<OrderEnhanceViews.CheckResult> recheck(@PathVariable Long id) {
        return R.ok(checkService.recheck(id));
    }

    /** 修改收件地址（保存后自动重新校验） */
    @PutMapping("/order/{id}/address")
    @PreAuthorize("@ss.hasPerm('ecom:order:edit')")
    @OperLog(title = "平台订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> updateAddress(@PathVariable Long id, @Validated @RequestBody OrderEnhanceViews.AddressRequest req) {
        checkService.updateAddress(id, req);
        return R.ok();
    }

    /** 批量指定发货仓 / 物流渠道，返回 { 平台订单号: 原因 } */
    @PostMapping("/order/assign")
    @PreAuthorize("@ss.hasPerm('ecom:order:audit')")
    @OperLog(title = "平台订单审核", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> assign(@Validated @RequestBody OrderEnhanceViews.AssignRequest req) {
        return R.ok(batchService.assign(req.ids(), req.warehouseId(), req.channelId()));
    }

    /** 订单已发货包裹的物流轨迹 */
    @GetMapping("/order/{id}/tracks")
    @PreAuthorize("@ss.hasPerm('ecom:order:list')")
    public R<List<OrderEnhanceViews.PackageTracks>> tracks(@PathVariable Long id) {
        checkService.result(id);
        return R.ok(trackService.orderTracks(id));
    }

    /** 按包裹重新回传发货（支持部分发货的平台） */
    @PostMapping("/package/{id}/ship-confirm")
    @PreAuthorize("@ss.hasPerm('ecom:package:ship')")
    @OperLog(title = "包裹回传发货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> packageShipConfirm(@PathVariable Long id) {
        partialShipService.retry(id);
        return R.ok();
    }
}
