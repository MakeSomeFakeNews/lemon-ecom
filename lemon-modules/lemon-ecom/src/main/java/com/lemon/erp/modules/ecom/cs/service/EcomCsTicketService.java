package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.cs.entity.EcomFeedback;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.mapper.EcomCsTicketMapper;
import com.lemon.erp.modules.ecom.mapper.EcomFeedbackMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReviewMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomOrderManualService;
import com.lemon.erp.modules.ecom.service.EcomReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 售后工单：会话 / 评价 / 店铺反馈「转售后」。
 * <ul>
 *   <li>退货退款、仅退款：调用退货退款的新增（{@code EcomReturnService.create}，与在退货退款页面新增相同的校验），之后在退货退款中入库、退款</li>
 *   <li>补发：按原订单的收件信息和所选行件数新建一张手工订单（平台订单号 = 原订单号 + -RS 序号，金额 0，自发货，备注标明补发与原订单），
 *       走正常的审核、打单、发货流程，出库成本计入这张订单（利润为负，即补发成本）</li>
 * </ul>
 * 来源是待处理的评价 / 反馈时改为处理中。数据权限：原订单按订单的数据权限读取
 */
@Service
@RequiredArgsConstructor
public class EcomCsTicketService {

    /** 补发订单号的后缀 */
    static final String RESEND_SUFFIX = "-RS";

    private final EcomCsTicketMapper ticketMapper;
    private final EcomReviewMapper reviewMapper;
    private final EcomFeedbackMapper feedbackMapper;
    private final EcomCsThreadService threadService;
    private final EcomReviewService reviewService;
    private final EcomCsSupport support;
    private final EcomOrderService orderService;
    private final EcomReturnService returnService;
    private final EcomOrderManualService manualService;

    public PageResult<EcomCsTicket> page(long current, long size, Long storeId, CsTicketType ticketType, CsSourceType sourceType, String keyword) {
        Page<EcomCsTicket> page = Page.of(current, size);
        ticketMapper.selectPageList(page, storeId, ticketType, sourceType, keyword);
        return PageResult.of(page);
    }

    public java.util.List<EcomCsTicket> bySource(CsSourceType type, Long sourceId) {
        return ticketMapper.selectBySource(type, sourceId);
    }

    @Transactional
    public Long create(CsRequests.Ticket req) {
        Source src = source(req.sourceType(), req.sourceId());
        Long orderId = req.orderId() != null ? req.orderId() : src.orderId();
        if (orderId == null) {
            throw new BizException("ecom.error.cs.orderRequired");
        }
        EcomOrder order = orderService.getForReturn(orderId);
        if (!order.getStoreId().equals(src.storeId())) {
            throw new BizException("ecom.error.cs.orderNotInStore", order.getPlatformOrderNo());
        }
        EcomCsTicket k = new EcomCsTicket();
        k.setStoreId(src.storeId());
        k.setSourceType(req.sourceType());
        k.setSourceId(req.sourceId());
        k.setTicketType(req.ticketType());
        k.setOrderId(order.getId());
        k.setPlatformOrderNo(order.getPlatformOrderNo());
        k.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
        var user = SecurityUtils.getLoginUser();
        k.setDeptId(user.deptId());
        if (req.ticketType() == CsTicketType.RESEND) {
            String no = nextResendNo(order);
            k.setResendOrderId(createResend(order, no, req));
            k.setResendOrderNo(no);
        } else {
            ReturnType type = req.ticketType() == CsTicketType.RETURN_REFUND ? ReturnType.RETURN_REFUND : ReturnType.REFUND_ONLY;
            Long returnId = returnService.create(new ReturnRequest(order.getId(), type, req.reason(), req.refundAmount(), req.warehouseId(),
                    k.getRemark(), req.items().stream().map(i -> new ReturnRequest.Item(i.orderItemId(), i.qty(), i.refundAmount())).toList()));
            k.setReturnId(returnId);
            k.setReturnNo(returnService.detail(returnId).getDocNo());
        }
        ticketMapper.insert(k);
        manualService.linkTicket(k.getResendOrderId(), k.getId());
        markProcessing(req.sourceType(), req.sourceId());
        return k.getId();
    }

    /** 补发订单：原订单的收件信息，所选行的件数（不超过原件数），金额 0 */
    private Long createResend(EcomOrder order, String no, CsRequests.Ticket req) {
        Map<Long, EcomOrderItem> items = order.getItems().stream().collect(Collectors.toMap(EcomOrderItem::getId, Function.identity()));
        OrderDraft d = new OrderDraft();
        d.setStoreId(order.getStoreId());
        d.setPlatformOrderNo(no);
        LocalDateTime now = LocalDateTime.now();
        d.setOrderTime(now);
        d.setPaidTime(now);
        d.setStatus(EcomOrderStatus.UNSHIPPED);
        d.setFulfillment(FulfillmentType.SELLER);
        d.setBuyerName(order.getBuyerName());
        d.setBuyerEmail(order.getBuyerEmail());
        d.setShipName(order.getShipName());
        d.setShipPhone(order.getShipPhone());
        d.setShipCountry(order.getShipCountry());
        d.setShipState(order.getShipState());
        d.setShipCity(order.getShipCity());
        d.setShipPostcode(order.getShipPostcode());
        d.setShipAddress(order.getShipAddress());
        d.setCurrency(order.getCurrency());
        d.setShippingAmount(BigDecimal.ZERO);
        d.setTaxAmount(BigDecimal.ZERO);
        d.setOrderType(EcomOrderType.RESEND);
        d.setSourceOrderId(order.getId());
        for (CsRequests.TicketLine l : req.items()) {
            EcomOrderItem oi = items.get(l.orderItemId());
            if (oi == null) {
                throw new BizException("ecom.error.orderLineNotFound");
            }
            if (l.qty().compareTo(oi.getQty()) > 0) {
                throw new BizException("ecom.error.cs.resendQtyExceeds", oi.getSellerSku(), oi.getQty().stripTrailingZeros().toPlainString());
            }
            OrderDraft.Line line = new OrderDraft.Line();
            line.setPlatformLineId(oi.getPlatformLineId());
            line.setSellerSku(oi.getSellerSku());
            line.setPlatformItemId(oi.getPlatformItemId());
            line.setTitle(oi.getTitle());
            line.setQty(l.qty());
            // 金额为 0（不计收入），单价保留原订单零售价（申报价值）
            line.setPrice(oi.getPrice() == null ? BigDecimal.ZERO : oi.getPrice());
            line.setAmount(BigDecimal.ZERO);
            d.getLines().add(line);
        }
        String remark = "补发：原订单 " + order.getPlatformOrderNo() + (StringUtils.hasText(req.remark()) ? "，" + req.remark().trim() : ""); // i18n-ignore 订单备注存库
        return orderService.createManual(d, remark);
    }

    /** 下一个补发订单号：原订单号-RS1、-RS2 …（店铺内不重复） */
    private String nextResendNo(EcomOrder order) {
        for (int i = 1; i < 100; i++) {
            String no = order.getPlatformOrderNo() + RESEND_SUFFIX + i;
            if (no.length() > 64) {
                no = order.getPlatformOrderNo().substring(0, 64 - RESEND_SUFFIX.length() - String.valueOf(i).length()) + RESEND_SUFFIX + i;
            }
            if (support.findOrder(order.getStoreId(), no) == null) {
                return no;
            }
        }
        throw new BizException("ecom.error.cs.tooManyResends", order.getPlatformOrderNo());
    }

    /** 来源的店铺与关联订单 */
    private record Source(Long storeId, Long orderId) {
    }

    private Source source(CsSourceType type, Long id) {
        return switch (type) {
            case THREAD -> {
                EcomMessageThread t = threadService.require(id);
                yield new Source(t.getStoreId(), t.getOrderId());
            }
            case REVIEW -> {
                EcomReview r = reviewService.detail(id);
                yield new Source(r.getStoreId(), r.getOrderId());
            }
            case FEEDBACK -> {
                EcomFeedback f = reviewService.requireFeedback(id);
                yield new Source(f.getStoreId(), f.getOrderId());
            }
        };
    }

    /** 待处理的评价 / 反馈转售后后改为处理中 */
    private void markProcessing(CsSourceType type, Long id) {
        if (type == CsSourceType.REVIEW) {
            reviewMapper.update(Wrappers.<EcomReview>lambdaUpdate().set(EcomReview::getHandleStatus, CsReviewStatus.PROCESSING)
                    .eq(EcomReview::getId, id).eq(EcomReview::getHandleStatus, CsReviewStatus.PENDING));
        } else if (type == CsSourceType.FEEDBACK) {
            feedbackMapper.update(Wrappers.<EcomFeedback>lambdaUpdate().set(EcomFeedback::getHandleStatus, CsReviewStatus.PROCESSING)
                    .eq(EcomFeedback::getId, id).eq(EcomFeedback::getHandleStatus, CsReviewStatus.PENDING));
        }
    }
}
