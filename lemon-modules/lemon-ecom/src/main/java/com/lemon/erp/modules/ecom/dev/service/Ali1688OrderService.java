package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Client;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Context;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.CreateResult;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.OrderPreview;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.PreviewGroup;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.PreviewLine;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SkippedLine;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SyncSummary;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.TraceStep;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Account;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Offer;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688OrderItem;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OrderItemMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OrderMapper;
import com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 1688 采购单：已审核的采购订单「一键下单到 1688」——明细按物料关联的 1688 货源换算下单数量（采购数量 ÷ 换算，向上取整），
 * 按卖家拆单分别调用快速下单接口；成功的订单保存为 1688 采购单并把 1688 订单号回写到采购订单的外部单号。
 * 同步 1688 的交易状态、实付金额（含运费）、物流单号与轨迹（手动或定时任务 ecom.ali1688OrderSync）。
 * 外部调用都不在事务中，每张 1688 订单单独保存，一张失败不影响其他
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ali1688OrderService {

    /** 采购订单外部单号前缀 */
    static final String EXTERNAL_PREFIX = "1688:";
    static final int EXTERNAL_MAX = 64;
    static final int MESSAGE_MAX = 500;

    private final Ecom1688OrderMapper orderMapper;
    private final Ecom1688OrderItemMapper itemMapper;
    private final Ali1688AccountService accountService;
    private final Ali1688OfferService offerService;
    private final Ali1688Client client;
    private final ScmOrderService scmOrderService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public PageResult<Ecom1688Order> page(long current, long size, Ali1688OrderStatus status, String keyword, Long scmOrderId) {
        Page<Ecom1688Order> page = Page.of(current, size);
        orderMapper.selectPageList(page, status, keyword, scmOrderId);
        return PageResult.of(page);
    }

    public Ecom1688Order detail(Long id) {
        Ecom1688Order o = orderMapper.selectDetail(id);
        if (o == null) {
            throw new BizException("ecom.error.dev.orderNotFound");
        }
        o.setItems(itemMapper.selectByOrderId(id));
        return o;
    }

    /** 物流轨迹（详情中的 trace JSON 解析后返回） */
    public List<TraceStep> traceOf(Ecom1688Order o) {
        if (!StringUtils.hasText(o.getTrace())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(o.getTrace(), new TypeReference<List<TraceStep>>() {
            });
        } catch (JacksonException e) {
            return List.of();
        }
    }

    /** 采购订单生成的 1688 采购单（采购订单详情中显示） */
    public List<Ecom1688Order> byScmOrder(Long scmOrderId) {
        scmOrderService.detail(OrderKind.PURCHASE, scmOrderId);
        return orderMapper.selectByScmOrder(scmOrderId);
    }

    // ---------------------------------------------------------------- 一键下单

    /** 下单预览：可下单的明细按卖家分组，列出没有货源或已下过单的明细 */
    public OrderPreview preview(Long scmOrderId) {
        ScmOrder po = scmOrderService.detail(OrderKind.PURCHASE, scmOrderId);
        List<ScmOrderItem> items = po.getItems() == null ? List.of() : po.getItems();
        Map<Long, Ecom1688Offer> offers = offerService.byMaterials(items.stream().map(ScmOrderItem::getMaterialId).collect(Collectors.toSet()));
        Set<Long> ordered = orderedItemIds(scmOrderId);
        Map<String, List<PreviewLine>> groups = new LinkedHashMap<>();
        Map<String, String> sellerNames = new LinkedHashMap<>();
        List<SkippedLine> skipped = new ArrayList<>();
        for (ScmOrderItem it : items) {
            BigDecimal open = Decimals.nz(it.getQty()).subtract(Decimals.nz(it.getDoneQty()));
            if (ordered.contains(it.getId())) {
                skipped.add(new SkippedLine(it.getId(), it.getMaterialCode(), it.getMaterialName(), it.getQty(), Messages.get("ecom.text.dev.lineOrdered")));
                continue;
            }
            if (open.signum() <= 0) {
                skipped.add(new SkippedLine(it.getId(), it.getMaterialCode(), it.getMaterialName(), it.getQty(), Messages.get("ecom.text.dev.lineDone")));
                continue;
            }
            Ecom1688Offer o = offers.get(it.getMaterialId());
            if (o == null) {
                skipped.add(new SkippedLine(it.getId(), it.getMaterialCode(), it.getMaterialName(), open, Messages.get("ecom.text.dev.lineNoOffer")));
                continue;
            }
            BigDecimal ratio = o.getQtyRatio() == null || o.getQtyRatio().signum() <= 0 ? BigDecimal.ONE : o.getQtyRatio();
            long aliQty = open.divide(ratio, 0, RoundingMode.CEILING).longValue();
            BigDecimal amount = o.getPrice() == null ? null : Decimals.money(o.getPrice().multiply(BigDecimal.valueOf(aliQty)));
            String seller = StringUtils.hasText(o.getSellerLoginId()) ? o.getSellerLoginId() : "";
            sellerNames.putIfAbsent(seller, o.getSellerName());
            groups.computeIfAbsent(seller, k -> new ArrayList<>()).add(new PreviewLine(it.getId(), it.getMaterialId(), it.getMaterialCode(),
                    it.getMaterialName(), open, o.getOfferId(), o.getSpecId(), o.getTitle(), o.getSpecText(), o.getImageUrl(), ratio, aliQty,
                    o.getMinOrderQty(), o.getPrice(), amount, o.getMinOrderQty() != null && aliQty < o.getMinOrderQty()));
        }
        List<PreviewGroup> list = new ArrayList<>();
        groups.forEach((seller, lines) -> list.add(new PreviewGroup(seller.isEmpty() ? null : seller, sellerNames.get(seller), lines,
                Decimals.money(Decimals.sum(lines, PreviewLine::amount)))));
        boolean canOrder = po.getStatus().isExecutable() && !list.isEmpty();
        return new OrderPreview(po.getId(), po.getOrderNo(), po.getStatus(), po.getPartnerName(), canOrder, list, skipped,
                orderMapper.selectByScmOrder(scmOrderId));
    }

    /** 采购订单中已经下过 1688 单（未取消 / 终止）的明细 */
    private Set<Long> orderedItemIds(Long scmOrderId) {
        List<Ecom1688Order> orders = orderMapper.selectList(Wrappers.<Ecom1688Order>lambdaQuery().eq(Ecom1688Order::getScmOrderId, scmOrderId)
                .notIn(Ecom1688Order::getStatus, Ali1688OrderStatus.CANCEL, Ali1688OrderStatus.TERMINATED));
        if (orders.isEmpty()) {
            return Set.of();
        }
        return itemMapper.selectList(Wrappers.<Ecom1688OrderItem>lambdaQuery()
                        .in(Ecom1688OrderItem::getOrderId, orders.stream().map(Ecom1688Order::getId).toList()))
                .stream().map(Ecom1688OrderItem::getScmOrderItemId).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 一键下单：采购订单须已审核（可入库），按卖家拆单逐个调用 1688 快速下单；成功的保存并回写外部单号，失败的列出原因
     */
    public CreateResult createFromPo(Long scmOrderId, Long accountId, String message) {
        OrderPreview preview = preview(scmOrderId);
        if (!preview.scmOrderStatus().isExecutable()) {
            throw new BizException("ecom.error.dev.poNotApproved", preview.scmOrderNo());
        }
        if (preview.groups().isEmpty()) {
            throw new BizException("ecom.error.dev.poNothingToOrder", preview.scmOrderNo());
        }
        Long account = accountId == null ? accountService.defaultAccountId() : accountId;
        Ecom1688Account acc = accountService.get(account);
        Ali1688Context ctx = accountService.context(account);
        Ali1688Models.Address address = address(acc);
        List<Ecom1688Order> created = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int seq = 0;
        for (PreviewGroup g : preview.groups()) {
            seq++;
            List<Ali1688Models.Cargo> cargos = g.lines().stream().map(l -> new Ali1688Models.Cargo(l.offerId(), l.specId(), l.aliQty())).toList();
            String seller = g.sellerName() != null ? g.sellerName() : g.sellerLoginId() != null ? g.sellerLoginId() : "-";
            try {
                Ali1688Models.CreatedOrder r = client.createOrder(ctx, address, cargos, message, preview.scmOrderNo() + "-" + seq);
                Ecom1688Order saved = transactionTemplate.execute(s -> saveCreated(account, preview, g, r, message));
                created.add(saved);
            } catch (BizException e) {
                log.warn("1688 order failed for PO {} seller {}: {}", preview.scmOrderNo(), seller, e.getMessage());
                accountService.recordError(account, e.getMessage());
                failures.add(seller + ": " + e.getMessage());
            }
        }
        if (!created.isEmpty()) {
            writeBack(scmOrderId);
        }
        return new CreateResult(created, failures);
    }

    private static Ali1688Models.Address address(Ecom1688Account a) {
        if (!StringUtils.hasText(a.getReceiverName()) || !StringUtils.hasText(a.getReceiverMobile()) || !StringUtils.hasText(a.getProvince())
                || !StringUtils.hasText(a.getCity()) || !StringUtils.hasText(a.getAddress())) {
            throw new BizException("ecom.error.dev.addressMissing", a.getName());
        }
        return new Ali1688Models.Address(a.getReceiverName(), a.getReceiverMobile(), a.getPostCode(), a.getProvince(), a.getCity(),
                a.getDistrict(), a.getTown(), a.getAddress());
    }

    private Ecom1688Order saveCreated(Long accountId, OrderPreview preview, PreviewGroup g, Ali1688Models.CreatedOrder r, String message) {
        Ecom1688Order o = new Ecom1688Order();
        o.setAccountId(accountId);
        o.setScmOrderId(preview.scmOrderId());
        o.setAliOrderId(r.orderId());
        o.setSellerLoginId(g.sellerLoginId());
        o.setSellerName(g.sellerName());
        o.setStatus(Ali1688OrderStatus.WAIT_BUYER_PAY);
        o.setAliStatus("waitbuyerpay");
        o.setShippingFee(r.postFee());
        o.setTotalAmount(r.totalAmount() != null ? r.totalAmount() : g.amount());
        o.setProductAmount(o.getTotalAmount() == null ? g.amount() : o.getTotalAmount().subtract(Decimals.nz(r.postFee())));
        o.setTotalQty(BigDecimal.valueOf(g.lines().stream().mapToLong(PreviewLine::aliQty).sum()));
        o.setMessage(message);
        o.setAliCreateTime(LocalDateTime.now());
        o.setSyncTime(LocalDateTime.now());
        orderMapper.insert(o);
        List<Ecom1688OrderItem> items = new ArrayList<>();
        for (PreviewLine l : g.lines()) {
            Ecom1688OrderItem it = new Ecom1688OrderItem();
            it.setOrderId(o.getId());
            it.setScmOrderItemId(l.scmOrderItemId());
            it.setMaterialId(l.materialId());
            it.setOfferId(l.offerId());
            it.setSpecId(l.specId());
            it.setQty(BigDecimal.valueOf(l.aliQty()));
            it.setMaterialQty(l.materialQty());
            it.setPrice(l.price());
            it.setAmount(l.amount());
            items.add(it);
        }
        itemMapper.insertBatch(items);
        return o;
    }

    /** 把采购订单的 1688 订单号（未取消的）回写到外部单号：1688:第一个订单号，多张时加「+张数」；外部单号已被其他来源占用时不覆盖 */
    void writeBack(Long scmOrderId) {
        ScmOrder po = scmOrderService.find(scmOrderId);
        if (po == null || (StringUtils.hasText(po.getExternalNo()) && !po.getExternalNo().startsWith(EXTERNAL_PREFIX))) {
            return;
        }
        List<String> ids = orderMapper.selectList(Wrappers.<Ecom1688Order>lambdaQuery().eq(Ecom1688Order::getScmOrderId, scmOrderId)
                        .notIn(Ecom1688Order::getStatus, Ali1688OrderStatus.CANCEL, Ali1688OrderStatus.TERMINATED).orderByAsc(Ecom1688Order::getId))
                .stream().map(Ecom1688Order::getAliOrderId).toList();
        String value = ids.isEmpty() ? null : EXTERNAL_PREFIX + ids.get(0) + (ids.size() > 1 ? "+" + (ids.size() - 1) : "");
        if (value != null && value.length() > EXTERNAL_MAX) {
            value = value.substring(0, EXTERNAL_MAX);
        }
        if (!Objects.equals(value, po.getExternalNo())) {
            String v = value;
            transactionTemplate.executeWithoutResult(s -> scmOrderService.updateExternalNo(scmOrderId, v));
        }
    }

    // ---------------------------------------------------------------- 同步

    /** 同步一张：订单详情（状态、金额、时间、物流单）+ 物流轨迹（有运单时） */
    public Ecom1688Order sync(Long id) {
        Ecom1688Order o = detail(id);
        doSync(o);
        return detail(id);
    }

    /** 同步多张（为空时同步全部未结束的，定时任务调用），一张失败不影响其他 */
    public SyncSummary syncMany(List<Long> ids) {
        List<Ecom1688Order> orders = ids == null || ids.isEmpty()
                ? orderMapper.selectList(Wrappers.<Ecom1688Order>lambdaQuery().in(Ecom1688Order::getStatus, Ali1688OrderStatus.OPEN)
                        .orderByAsc(Ecom1688Order::getSyncTime))
                : orderMapper.selectByIds(ids);
        int success = 0;
        int changed = 0;
        List<String> messages = new ArrayList<>();
        for (Ecom1688Order o : orders) {
            try {
                if (doSync(o)) {
                    changed++;
                }
                success++;
            } catch (BizException e) {
                messages.add(o.getAliOrderId() + ": " + e.getMessage());
            }
        }
        return new SyncSummary(orders.size(), success, changed, orders.size() - success, messages);
    }

    /** @return 状态是否变化 */
    private boolean doSync(Ecom1688Order o) {
        Ali1688Models.OrderView v;
        List<Ali1688Models.TraceStep> trace;
        try {
            Ali1688Context ctx = accountService.context(o.getAccountId());
            v = client.order(ctx, o.getAliOrderId());
            trace = v.logistics().isEmpty() ? List.of() : client.trace(ctx, o.getAliOrderId());
        } catch (BizException e) {
            String msg = e.getMessage();
            orderMapper.update(Wrappers.<Ecom1688Order>lambdaUpdate()
                    .set(Ecom1688Order::getSyncMessage, msg == null ? null : msg.length() > MESSAGE_MAX ? msg.substring(0, MESSAGE_MAX) : msg)
                    .set(Ecom1688Order::getSyncTime, LocalDateTime.now()).eq(Ecom1688Order::getId, o.getId()));
            throw e;
        }
        Ali1688OrderStatus status = Ali1688OrderStatus.fromAli(v.status());
        Ali1688OrderStatus newStatus = status == null ? o.getStatus() : status;
        String traceJson;
        try {
            traceJson = trace.isEmpty() ? o.getTrace() : objectMapper.writeValueAsString(trace.stream().map(t -> new TraceStep(t.time(), t.remark())).toList());
        } catch (JacksonException e) {
            traceJson = o.getTrace();
        }
        String companies = v.logistics().stream().map(Ali1688Models.Logistics::companyName).filter(StringUtils::hasText).distinct()
                .collect(Collectors.joining(","));
        String bills = v.logistics().stream().map(Ali1688Models.Logistics::billNo).distinct().collect(Collectors.joining(","));
        String trace0 = traceJson;
        boolean cancelled = newStatus != o.getStatus() && (newStatus == Ali1688OrderStatus.CANCEL || newStatus == Ali1688OrderStatus.TERMINATED);
        transactionTemplate.executeWithoutResult(s -> orderMapper.update(Wrappers.<Ecom1688Order>lambdaUpdate()
                .set(Ecom1688Order::getStatus, newStatus)
                .set(Ecom1688Order::getAliStatus, v.status())
                .set(v.totalAmount() != null, Ecom1688Order::getTotalAmount, v.totalAmount())
                .set(v.shippingFee() != null, Ecom1688Order::getShippingFee, v.shippingFee())
                .set(v.productAmount() != null, Ecom1688Order::getProductAmount, v.productAmount())
                .set(v.createTime() != null, Ecom1688Order::getAliCreateTime, v.createTime())
                .set(v.payTime() != null, Ecom1688Order::getPayTime, v.payTime())
                .set(v.shipTime() != null, Ecom1688Order::getShipTime, v.shipTime())
                .set(v.receiveTime() != null, Ecom1688Order::getReceiveTime, v.receiveTime())
                .set(StringUtils.hasText(v.sellerName()), Ecom1688Order::getSellerName, v.sellerName())
                .set(!companies.isEmpty(), Ecom1688Order::getLogisticsCompany, cut(companies, 64))
                .set(!bills.isEmpty(), Ecom1688Order::getLogisticsNo, cut(bills, 64))
                .set(Ecom1688Order::getTrace, trace0)
                .set(Ecom1688Order::getSyncTime, LocalDateTime.now())
                .set(Ecom1688Order::getSyncMessage, null)
                .eq(Ecom1688Order::getId, o.getId())));
        if (cancelled && o.getScmOrderId() != null) {
            writeBack(o.getScmOrderId());
        }
        return newStatus != o.getStatus();
    }

    /** 支付链接（同一账号的多张待付款订单合并支付） */
    public String payUrl(List<Long> ids) {
        List<Ecom1688Order> orders = orderMapper.selectByIds(ids);
        if (orders.isEmpty()) {
            throw new BizException("ecom.error.dev.orderNotFound");
        }
        Set<Long> accounts = orders.stream().map(Ecom1688Order::getAccountId).collect(Collectors.toSet());
        if (accounts.size() > 1) {
            throw new BizException("ecom.error.dev.payDifferentAccounts");
        }
        for (Ecom1688Order o : orders) {
            if (o.getStatus() != Ali1688OrderStatus.WAIT_BUYER_PAY) {
                throw new BizException("ecom.error.dev.orderNotWaitPay", o.getAliOrderId());
            }
        }
        return client.payUrl(accountService.context(accounts.iterator().next()), orders.stream().map(Ecom1688Order::getAliOrderId).toList());
    }

    /** 取消待付款的订单（1688 上同时取消），回写采购订单外部单号 */
    public void cancel(Long id, String reason) {
        Ecom1688Order o = detail(id);
        if (!o.getStatus().cancellable()) {
            throw new BizException("ecom.error.dev.orderNotWaitPay", o.getAliOrderId());
        }
        client.cancel(accountService.context(o.getAccountId()), o.getAliOrderId(), reason);
        transactionTemplate.executeWithoutResult(s -> orderMapper.update(Wrappers.<Ecom1688Order>lambdaUpdate()
                .set(Ecom1688Order::getStatus, Ali1688OrderStatus.CANCEL).set(Ecom1688Order::getAliStatus, "cancel")
                .set(Ecom1688Order::getSyncTime, LocalDateTime.now()).eq(Ecom1688Order::getId, id)));
        if (o.getScmOrderId() != null) {
            writeBack(o.getScmOrderId());
        }
    }

    /** 待付款的 1688 采购单数（首页待办） */
    public long waitPayCount() {
        return orderMapper.selectCount(Wrappers.<Ecom1688Order>lambdaQuery().eq(Ecom1688Order::getStatus, Ali1688OrderStatus.WAIT_BUYER_PAY));
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
