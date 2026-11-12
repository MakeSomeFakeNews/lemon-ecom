package com.lemon.erp.modules.ecom.cs.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import com.lemon.erp.modules.ecom.cs.service.EcomCsTicketService;

import java.util.List;

/**
 * 售后工单 /ecom/cs/ticket：会话 / 评价 / 店铺反馈转售后（退货退款、仅退款、补发）
 */
@RestController
@RequestMapping("/ecom/cs/ticket")
@RequiredArgsConstructor
public class EcomCsTicketController {

    private final EcomCsTicketService ticketService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:csTicket:list')")
    public R<PageResult<EcomCsTicket>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long storeId, @RequestParam(required = false) CsTicketType ticketType,
                                            @RequestParam(required = false) CsSourceType sourceType, @RequestParam(required = false) String keyword) {
        return R.ok(ticketService.page(current, size, storeId, ticketType, sourceType, keyword));
    }

    /** 来源（会话 / 评价 / 反馈）的售后工单 */
    @GetMapping("/by-source")
    @PreAuthorize("@ss.hasPerm('ecom:csTicket:list') or @ss.hasPerm('ecom:csMessage:list') or @ss.hasPerm('ecom:csReview:list')")
    public R<List<EcomCsTicket>> bySource(@RequestParam CsSourceType sourceType, @RequestParam Long sourceId) {
        return R.ok(ticketService.bySource(sourceType, sourceId));
    }

    /** 转售后，返回工单 ID */
    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:csTicket:add')")
    @OperLog(title = "售后工单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody CsRequests.Ticket req) {
        return R.ok(ticketService.create(req));
    }
}
