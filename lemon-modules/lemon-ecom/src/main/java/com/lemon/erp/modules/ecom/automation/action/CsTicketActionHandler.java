package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import com.lemon.erp.modules.ecom.cs.service.EcomCsTicketService;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 建售后工单（经售后工单服务）：来源为事件的评价或买家消息会话，订单取事件关联的订单（评价、会话关联了订单时）；
 * 售后类型默认「仅退款」，明细为订单的全部行（数量为原件数、退款金额为 0，生成待处理的退货退款单，由客服确认金额后处理），
 * 补发生成手工补发订单。来源已有售后工单时跳过
 */
@Component
@RequiredArgsConstructor
public class CsTicketActionHandler implements AutoActionHandler {

    private final EcomCsTicketService ticketService;
    private final EcomOrderService orderService;

    @Override
    public AutoActionType type() {
        return AutoActionType.CS_TICKET;
    }

    @Override
    public void validate(AutoRuleParts.Action a) {
        // 售后类型为空时默认仅退款，原因、备注可为空
    }

    @Override
    public String describe(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        if (e.ref(AutoRef.ORDER) == null) {
            return Messages.get("ecom.text.auto.desc.ticketNoOrder");
        }
        return Messages.get("ecom.text.auto.desc.ticket", Messages.get(AutoText.key("ecom.enum.auto.ticket", type(a))), e.field("orderNo"));
    }

    @Override
    public AutoOutcome execute(AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        CsSourceType sourceType = e.ref(AutoRef.REVIEW) != null ? CsSourceType.REVIEW : CsSourceType.THREAD;
        Long sourceId = AutoActionSupport.require(e, sourceType == CsSourceType.REVIEW ? AutoRef.REVIEW : AutoRef.THREAD);
        List<EcomCsTicket> existing = ticketService.bySource(sourceType, sourceId);
        if (!existing.isEmpty()) {
            return AutoOutcome.skipped(AutoText.t("ecom.text.auto.out.ticketExists"));
        }
        Long orderId = e.ref(AutoRef.ORDER);
        if (orderId == null) {
            throw new BizException("ecom.error.auto.ticketNoOrder");
        }
        EcomOrder order = orderService.getForReturn(orderId);
        List<CsRequests.TicketLine> lines = order.getItems().stream().filter(i -> i.getQty() != null && i.getQty().signum() > 0)
                .map(i -> new CsRequests.TicketLine(i.getId(), i.getQty(), BigDecimal.ZERO)).toList();
        String remark = AutoText.cut(StringUtils.hasText(a.content()) ? AutoText.render(a.content(), e, rule)
                : AutoText.t("ecom.text.auto.ticketRemark", rule.getName()), 255);
        Long id = ticketService.create(new CsRequests.Ticket(sourceType, sourceId, type(a), orderId, StringUtils.hasText(a.reason()) ? a.reason() : null,
                type(a) == CsTicketType.RESEND ? null : BigDecimal.ZERO, null, remark, lines));
        return AutoOutcome.success(AutoText.t("ecom.text.auto.out.ticketCreated", id, order.getPlatformOrderNo()));
    }

    private static CsTicketType type(AutoRuleParts.Action a) {
        return a.ticketType() == null ? CsTicketType.REFUND_ONLY : a.ticketType();
    }
}
