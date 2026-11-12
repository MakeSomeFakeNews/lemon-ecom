package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomCsTicketMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderEnhanceMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 手工订单与补发单。
 * <ul>
 *   <li>手工订单（线下 / 站外 / 独立站）：选店铺（客户、币种、汇率按店铺）、买家、收件地址、商品（平台 SKU 按店铺的在线商品映射到物料），
 *       订单号不填时自动生成（MO + 时间）；之后与平台订单一样走审单规则、审核、打单、发货，发货不回传平台</li>
 *   <li>补发 / 重发单：从原订单选择补发的行和件数，订单号 = 原订单号-RS 序号，金额为 0（不计收入），单价保留原订单的零售价（申报价值用），
 *       出库成本与运费计入补发单（利润为负）并在原订单上汇总为「售后成本」；关联原订单、售后工单、退货退款单，发货不回传平台</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EcomOrderManualService {

    /** 补发订单号的后缀 */
    public static final String RESEND_SUFFIX = "-RS";
    private static final String MANUAL_PREFIX = "MO";
    private static final int NO_MAX = 64;
    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EcomOrderService orderService;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderEnhanceMapper enhanceMapper;
    private final EcomStoreService storeService;
    private final EcomReturnMapper returnMapper;
    private final EcomCsTicketMapper ticketMapper;

    // ---------------------------------------------------------------- 手工订单

    @Transactional
    public Long createManual(OrderEnhanceViews.ManualOrder req) {
        EcomStore store = storeService.requireEnabled(req.storeId());
        String no = StringUtils.hasText(req.platformOrderNo()) ? req.platformOrderNo().trim() : nextManualNo(store.getId());
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(no);
        LocalDateTime time = req.orderTime() == null ? LocalDateTime.now() : req.orderTime();
        d.setOrderTime(time);
        d.setPaidTime(time);
        d.setStatus(EcomOrderStatus.UNSHIPPED);
        d.setFulfillment(req.fulfillment() == null ? FulfillmentType.SELLER : req.fulfillment());
        d.setBuyerName(trim(req.buyerName()));
        d.setBuyerEmail(trim(req.buyerEmail()));
        d.setShipName(trim(req.shipName()));
        d.setShipPhone(trim(req.shipPhone()));
        d.setShipCountry(trim(req.shipCountry()));
        d.setShipState(trim(req.shipState()));
        d.setShipCity(trim(req.shipCity()));
        d.setShipPostcode(trim(req.shipPostcode()));
        d.setShipAddress(trim(req.shipAddress()));
        d.setCurrency(trim(req.currency()));
        d.setShippingAmount(Decimals.money(Decimals.nz(req.shippingAmount())));
        d.setTaxAmount(BigDecimal.ZERO);
        d.setWeight(req.weight());
        d.setOrderType(EcomOrderType.MANUAL);
        int i = 1;
        for (OrderEnhanceViews.ManualLine l : req.lines()) {
            OrderDraft.Line line = new OrderDraft.Line();
            line.setPlatformLineId("M" + i++);
            line.setSellerSku(l.sellerSku().trim());
            line.setTitle(trim(l.title()));
            line.setQty(l.qty());
            line.setPrice(Decimals.nz(l.price()));
            d.getLines().add(line);
        }
        return orderService.createManual(d, trim(req.remark()));
    }

    /** 自动生成的手工订单号：MO + 年月日时分秒，同一秒有重复时加序号 */
    private String nextManualNo(Long storeId) {
        String base = MANUAL_PREFIX + LocalDateTime.now().format(NO_TIME);
        String no = base;
        for (int i = 2; exists(storeId, no); i++) {
            no = base + "-" + i;
        }
        return no;
    }

    private boolean exists(Long storeId, String no) {
        return orderMapper.exists(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, storeId).eq(EcomOrder::getPlatformOrderNo, no));
    }

    // ---------------------------------------------------------------- 补发单

    /**
     * 从原订单创建补发单：校验补发件数不超过原件数、关联的售后工单 / 退货退款单属于原订单
     *
     * @return 补发单 ID
     */
    @Transactional
    public Long createResend(Long orderId, OrderEnhanceViews.Resend req) {
        EcomOrder order = orderService.getForReturn(orderId);
        if (order.getOrderType() == EcomOrderType.RESEND) {
            throw new BizException("ecom.error.ord.resendOfResend", order.getPlatformOrderNo());
        }
        if (req.returnId() != null) {
            EcomReturn r = returnMapper.selectById(req.returnId());
            if (r == null || !orderId.equals(r.getOrderId())) {
                throw new BizException("ecom.error.ord.resendSourceInvalid", order.getPlatformOrderNo());
            }
        }
        if (req.ticketId() != null) {
            EcomCsTicket k = ticketMapper.selectById(req.ticketId());
            if (k == null || !orderId.equals(k.getOrderId())) {
                throw new BizException("ecom.error.ord.resendSourceInvalid", order.getPlatformOrderNo());
            }
        }
        Map<Long, BigDecimal> qty = new LinkedHashMap<>();
        req.lines().forEach(l -> qty.merge(l.orderItemId(), l.qty(), BigDecimal::add));
        OrderDraft d = resendDraft(order, qty, nextResendNo(order));
        boolean override = StringUtils.hasText(req.shipAddress());
        if (override) {
            d.setShipName(StringUtils.hasText(req.shipName()) ? req.shipName().trim() : order.getShipName());
            d.setShipPhone(trim(req.shipPhone()));
            d.setShipCountry(StringUtils.hasText(req.shipCountry()) ? req.shipCountry().trim() : order.getShipCountry());
            d.setShipState(trim(req.shipState()));
            d.setShipCity(trim(req.shipCity()));
            d.setShipPostcode(trim(req.shipPostcode()));
            d.setShipAddress(req.shipAddress().trim());
        }
        d.setSourceTicketId(req.ticketId());
        d.setSourceReturnId(req.returnId());
        String remark = "补发：原订单 " + order.getPlatformOrderNo() + (StringUtils.hasText(req.reason()) ? "，" + req.reason().trim() : ""); // i18n-ignore 订单备注存库
        return orderService.createManual(d, remark);
    }

    /**
     * 补发单的草稿（售后工单「补发」也用它）：原订单的收件信息，所选行的件数（不超过原件数），金额为 0，单价保留原订单零售价（申报价值）
     *
     * @param qty 原订单行 → 补发件数
     */
    public OrderDraft resendDraft(EcomOrder order, Map<Long, BigDecimal> qty, String no) {
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
        for (Map.Entry<Long, BigDecimal> e : qty.entrySet()) {
            EcomOrderItem oi = items.get(e.getKey());
            if (oi == null) {
                throw new BizException("ecom.error.orderLineNotFound");
            }
            if (e.getValue().compareTo(oi.getQty()) > 0) {
                throw new BizException("ecom.error.cs.resendQtyExceeds", oi.getSellerSku(), oi.getQty().stripTrailingZeros().toPlainString());
            }
            OrderDraft.Line line = new OrderDraft.Line();
            line.setPlatformLineId(oi.getPlatformLineId());
            line.setSellerSku(oi.getSellerSku());
            line.setPlatformItemId(oi.getPlatformItemId());
            line.setTitle(oi.getTitle());
            line.setQty(e.getValue());
            line.setPrice(Decimals.nz(oi.getPrice()));
            line.setAmount(BigDecimal.ZERO);
            d.getLines().add(line);
        }
        return d;
    }

    /** 下一个补发订单号：原订单号-RS1、-RS2 …（店铺内不重复） */
    public String nextResendNo(EcomOrder order) {
        for (int i = 1; i < 100; i++) {
            String suffix = RESEND_SUFFIX + i;
            String no = order.getPlatformOrderNo() + suffix;
            if (no.length() > NO_MAX) {
                no = order.getPlatformOrderNo().substring(0, NO_MAX - suffix.length()) + suffix;
            }
            if (!exists(order.getStoreId(), no)) {
                return no;
            }
        }
        throw new BizException("ecom.error.cs.tooManyResends", order.getPlatformOrderNo());
    }

    /** 售后工单生成补发单后关联工单（内部） */
    public void linkTicket(Long resendOrderId, Long ticketId) {
        if (resendOrderId != null && ticketId != null) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getSourceTicketId, ticketId).eq(EcomOrder::getId, resendOrderId));
        }
    }

    /** 原订单的补发单与售后成本（补发单已发生的出库成本 + 运费） */
    public OrderEnhanceViews.ResendSummary resends(Long orderId) {
        orderService.require(orderId);
        List<EcomOrder> list = enhanceMapper.selectResends(orderId);
        BigDecimal cost = list.stream().filter(o -> o.getLocalStatus() != EcomLocalStatus.CLOSED)
                .map(o -> Decimals.nz(o.getCostAmount()).add(Decimals.nz(o.getShippingCost()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderEnhanceViews.ResendSummary(list, Decimals.money(cost));
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
