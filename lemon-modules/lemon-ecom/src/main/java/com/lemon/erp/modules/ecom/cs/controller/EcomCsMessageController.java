package com.lemon.erp.modules.ecom.cs.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.dto.ThreadQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.service.EcomCsSyncService;
import com.lemon.erp.modules.ecom.cs.service.EcomCsThreadService;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * 买家消息（统一收件箱）/ecom/cs/thread
 */
@RestController
@RequestMapping("/ecom/cs/thread")
@RequiredArgsConstructor
public class EcomCsMessageController {

    private final EcomCsThreadService threadService;
    private final EcomCsSyncService syncService;

    /** 会话列表：storeId、platform、status、overdue、unread、ownerId、keyword */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:list')")
    public R<PageResult<EcomMessageThread>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
                                                 @ModelAttribute ThreadQuery query) {
        return R.ok(threadService.page(current, size, query));
    }

    /** 各状态的会话数与超时数（条件同列表，不含状态） */
    @GetMapping("/counts")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:list')")
    public R<Map<String, Long>> counts(@ModelAttribute ThreadQuery query) {
        return R.ok(threadService.counts(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:list')")
    public R<CsViews.ThreadDetail> detail(@PathVariable Long id) {
        return R.ok(threadService.detail(id));
    }

    /** 订单侧栏 */
    @GetMapping("/{id}/order")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:list')")
    public R<CsViews.OrderPanel> order(@PathVariable Long id) {
        return R.ok(threadService.orderPanel(id));
    }

    /** 打开会话后标记已读 */
    @PostMapping("/{id}/read")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:list')")
    public R<Void> read(@PathVariable Long id) {
        threadService.markRead(id);
        return R.ok();
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:reply')")
    @OperLog(title = "买家消息", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<EcomMessage> reply(@PathVariable Long id, @Valid @RequestBody CsRequests.Reply req) {
        return R.ok(threadService.reply(id, req));
    }

    /** 重新发送失败的回复 */
    @PostMapping("/message/{messageId}/resend")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:reply')")
    @OperLog(title = "买家消息", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<EcomMessage> resend(@PathVariable Long messageId) {
        return R.ok(threadService.resend(messageId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:edit')")
    @OperLog(title = "买家消息", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CsRequests.ThreadUpdate req) {
        threadService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:edit')")
    @OperLog(title = "买家消息", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> close(@PathVariable Long id) {
        threadService.close(id);
        return R.ok();
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:edit')")
    @OperLog(title = "买家消息", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> reopen(@PathVariable Long id) {
        threadService.reopen(id);
        return R.ok();
    }

    /** 关联订单（平台订单号为空取消关联） */
    @PutMapping("/{id}/order")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:edit')")
    @OperLog(title = "买家消息", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> linkOrder(@PathVariable Long id, @Valid @RequestBody CsRequests.LinkOrder req) {
        threadService.linkOrder(id, req.platformOrderNo());
        return R.ok();
    }

    /** 手工同步店铺的买家消息 */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:sync')")
    @OperLog(title = "买家消息", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<CsViews.SyncOutcome> sync(@PathVariable Long storeId) {
        return R.ok(syncService.syncMessages(storeId));
    }
}
