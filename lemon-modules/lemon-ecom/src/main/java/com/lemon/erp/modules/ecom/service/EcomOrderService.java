package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.dto.OrderEditRequest;
import com.lemon.erp.modules.ecom.dto.OrderImportRow;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.dto.OutboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomInboundItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderTagMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 平台订单：导入 / 同步写入 → 匹配在线商品得到本地物料 → 出库（直接开单生成销售出库，客户为店铺对应的客户，金额按汇率折为本位币）
 * → 发货（填写运单，回传平台）。利润 = 本位币金额 − (平台费用 + 退款) × 汇率 − 商品成本 − 物流成本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomOrderService {

    /** 可以直接出库（不经过包裹）的处理状态 */
    private static final Set<EcomLocalStatus> OUTBOUND_ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK);
    /** 不能取消的处理状态（已出库的走退货退款） */
    private static final Set<EcomLocalStatus> CANCEL_DENIED = EnumSet.of(EcomLocalStatus.OUTBOUND, EcomLocalStatus.SHIPPED, EcomLocalStatus.PARTIAL,
            EcomLocalStatus.CLOSED);
    /** 不能修改收件信息的处理状态（已打单的面单上已是原地址） */
    private static final Set<EcomLocalStatus> EDIT_DENIED = EnumSet.of(EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED, EcomLocalStatus.PARTIAL,
            EcomLocalStatus.LABELED);

    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomInboundItemMapper inboundItemMapper;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomOrderTagMapper tagMapper;
    private final EcomStoreService storeService;
    private final EcomOutboundSupport outboundSupport;
    private final EcomPackageService packageService;
    private final ExchangeRateProvider rateProvider;
    private final EcomTaxService taxService;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final EcomOrderLogService logService;
    private final DictLookup dictLookup;

    // ---------------------------------------------------------------- 查询

    // 数据范围按店铺授权（EcomStoreScope，接口执行期间自动过滤），不再按部门 / 制单人
    public PageResult<EcomOrder> page(long current, long size, OrderQuery query) {
        Page<EcomOrder> page = Page.of(current, size);
        orderMapper.selectPageList(page, query);
        attachTags(page.getRecords());
        attachBriefItems(page.getRecords());
        return PageResult.of(page);
    }

    /** 列表行内显示的商品（缩略图、SKU × 数量） */
    private void attachBriefItems(List<EcomOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }
        Map<Long, List<EcomOrderItem>> byOrder = itemMapper.selectBriefByOrderIds(orders.stream().map(EcomOrder::getId).toList())
                .stream().collect(Collectors.groupingBy(EcomOrderItem::getOrderId));
        orders.forEach(o -> o.setItems(byOrder.getOrDefault(o.getId(), List.of())));
    }

    /** 带出订单的标签 */
    private void attachTags(List<EcomOrder> orders) {
        if (orders.isEmpty()) {
            return;
        }
        Map<Long, List<EcomOrderTag>> byOrder = tagMapper.selectByOrderIds(orders.stream().map(EcomOrder::getId).toList())
                .stream().collect(Collectors.groupingBy(EcomOrderTag::getOrderId));
        orders.forEach(o -> o.setTags(byOrder.getOrDefault(o.getId(), List.of())));
    }

    /**
     * 各处理状态的订单数（列表的状态标签页），条件与列表相同、不含处理状态
     *
     * @return 处理状态 → 订单数
     */
    // 数据范围按店铺授权（EcomStoreScope，接口执行期间自动过滤），不再按部门 / 制单人
    public Map<String, Long> statusCounts(OrderQuery query) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.countByLocalStatus(query)) {
            out.put(String.valueOf(row.get("localStatus")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomOrder detail(Long id) {
        EcomOrder o = EcomStoreScope.require(() -> orderMapper.selectDetail(id), "ecom.error.orderNotFound");
        List<EcomOrderItem> items = outboundSupport.attachParts(itemMapper.selectByOrderId(id));
        Map<Long, Map<String, Object>> alloc = new HashMap<>();
        packageItemMapper.selectAllocated(id).forEach(r -> alloc.put(((Number) r.get("orderItemId")).longValue(), r));
        for (EcomOrderItem it : items) {
            Map<String, Object> r = alloc.get(it.getId());
            it.setAllocatedQty(r == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r.get("qty"))));
            it.setLockedQty(r == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r.get("locked"))));
        }
        o.setItems(items);
        o.setPackages(packageMapper.selectByOrderId(id));
        attachTags(List.of(o));
        return o;
    }

    EcomOrder require(Long id) {
        return EcomStoreScope.require(() -> orderMapper.selectById(id), "ecom.error.orderNotFound");
    }

    public List<EcomOrderItem> items(Long orderId) {
        return itemMapper.selectByOrderId(orderId);
    }

    // ---------------------------------------------------------------- 导入 / 同步写入

    /**
     * Excel 导入：逐行校验后按 店铺 + 平台订单号 合并为订单，任何一行出错整批不导入；已存在的订单跳过
     */
    public ImportResult importRows(ImportRequest<OrderImportRow> req) {
        List<ImportResult.RowError> errors = new ArrayList<>();
        Map<String, OrderDraft> drafts = new LinkedHashMap<>();
        Map<String, Integer> firstRow = new LinkedHashMap<>();
        for (ImportRow<OrderImportRow> row : req.rows()) {
            if (row == null || row.data() == null) {
                continue;
            }
            Set<ConstraintViolation<OrderImportRow>> violations = validator.validate(row.data());
            if (!violations.isEmpty()) {
                errors.add(new ImportResult.RowError(row.rowNum(), violations.stream().map(ConstraintViolation::getMessage).distinct()
                        .sorted().collect(Collectors.joining("；"))));
                continue;
            }
            OrderImportRow r = row.data();
            EcomStore store = storeService.findByCode(r.getStoreCode());
            if (store == null) {
                errors.add(new ImportResult.RowError(row.rowNum(), Messages.get("ecom.error.storeCodeNotFound", r.getStoreCode())));
                continue;
            }
            String key = store.getId() + "|" + r.getPlatformOrderNo().trim();
            OrderDraft d = drafts.computeIfAbsent(key, k -> toDraft(store, r));
            firstRow.putIfAbsent(key, row.rowNum());
            d.getLines().add(toLine(r));
        }
        if (!errors.isEmpty()) {
            return new ImportResult(req.rows().size(), 0, errors);
        }
        int[] created = {0};
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (Map.Entry<String, OrderDraft> e : drafts.entrySet()) {
                    try {
                        if (saveDraft(e.getValue(), false)) {
                            created[0]++;
                        }
                    } catch (BizException ex) {
                        errors.add(new ImportResult.RowError(firstRow.get(e.getKey()), ex.getMessage()));
                    }
                }
                if (!errors.isEmpty()) {
                    status.setRollbackOnly();
                }
            });
        } catch (RuntimeException ex) {
            if (errors.isEmpty()) {
                throw ex;
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult(req.rows().size(), 0, errors);
        }
        return new ImportResult(drafts.size(), created[0], List.of());
    }

    private static OrderDraft toDraft(EcomStore store, OrderImportRow r) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(r.getPlatformOrderNo().trim());
        d.setOrderTime(r.getOrderTime());
        d.setPaidTime(r.getPaidTime());
        d.setStatus(r.getStatus());
        d.setFulfillment(r.getFulfillment());
        d.setBuyerName(r.getBuyerName());
        d.setBuyerEmail(r.getBuyerEmail());
        d.setShipName(r.getShipName());
        d.setShipPhone(r.getShipPhone());
        d.setShipCountry(r.getShipCountry());
        d.setShipState(r.getShipState());
        d.setShipCity(r.getShipCity());
        d.setShipPostcode(r.getShipPostcode());
        d.setShipAddress(r.getShipAddress());
        d.setCurrency(r.getCurrency());
        d.setShipBy(r.getShipBy());
        d.setNote(r.getNote());
        return d;
    }

    private static OrderDraft.Line toLine(OrderImportRow r) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(r.getPlatformLineId());
        l.setSellerSku(r.getSellerSku().trim());
        l.setPlatformItemId(r.getPlatformItemId());
        l.setTitle(r.getTitle());
        l.setQty(r.getQty());
        l.setPrice(r.getPrice());
        l.setShipping(r.getShipping());
        l.setTax(r.getTax());
        l.setDiscount(r.getDiscount());
        return l;
    }

    /**
     * 写入一张订单：店铺中已有同号订单时不重复写入（同步时平台状态变化则更新状态，取消的订单关闭）
     *
     * @param updateExisting 已存在时是否更新平台状态（渠道同步 true，Excel 导入 false）
     * @return 是否新增
     */
    @Transactional
    public boolean saveDraft(OrderDraft d, boolean updateExisting) {
        EcomStore store = storeService.requireEnabled(d.getStoreId());
        EcomOrder existing = orderMapper.selectOne(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, store.getId())
                .eq(EcomOrder::getPlatformOrderNo, d.getPlatformOrderNo()));
        if (existing != null) {
            if (updateExisting && d.getStatus() != null && d.getStatus() != existing.getStatus()) {
                syncStatus(existing, d.getStatus());
            }
            return false;
        }
        if (d.getLines().isEmpty()) {
            throw new BizException("ecom.error.orderHasNoLines", d.getPlatformOrderNo());
        }
        EcomOrder o = new EcomOrder();
        o.setStoreId(store.getId());
        o.setPlatformOrderNo(d.getPlatformOrderNo());
        o.setOrderTime(d.getOrderTime() == null ? LocalDateTime.now() : d.getOrderTime());
        o.setPaidTime(d.getPaidTime());
        o.setStatus(d.getStatus() == null ? EcomOrderStatus.UNSHIPPED : d.getStatus());
        o.setFulfillment(d.getFulfillment() == null ? FulfillmentType.SELLER : d.getFulfillment());
        o.setBuyerName(d.getBuyerName());
        o.setBuyerEmail(d.getBuyerEmail());
        o.setShipName(d.getShipName());
        o.setShipPhone(d.getShipPhone());
        o.setShipCountry(d.getShipCountry() == null ? null : d.getShipCountry().trim().toUpperCase());
        o.setShipState(d.getShipState());
        o.setShipCity(d.getShipCity());
        o.setShipPostcode(d.getShipPostcode());
        o.setShipAddress(d.getShipAddress());
        o.setCurrency(StringUtils.hasText(d.getCurrency()) ? d.getCurrency().trim().toUpperCase() : store.getCurrency());
        o.setShipBy(d.getShipBy());
        o.setDeliverBy(d.getDeliverBy());
        o.setNote(d.getNote());
        o.setWeight(d.getWeight());
        o.setOrderType(d.getOrderType());
        o.setSourceOrderId(d.getSourceOrderId());
        o.setSourceTicketId(d.getSourceTicketId());
        o.setSourceReturnId(d.getSourceReturnId());
        o.setExchangeRate(rateOf(o.getCurrency(), o.getOrderTime().toLocalDate()));

        List<EcomOrderItem> items = new ArrayList<>();
        BigDecimal item = BigDecimal.ZERO, shipping = BigDecimal.ZERO, tax = BigDecimal.ZERO, discount = BigDecimal.ZERO;
        for (OrderDraft.Line l : d.getLines()) {
            EcomOrderItem it = new EcomOrderItem();
            it.setPlatformLineId(l.getPlatformLineId());
            it.setSellerSku(l.getSellerSku());
            it.setPlatformItemId(l.getPlatformItemId());
            it.setTitle(l.getTitle());
            it.setQty(Decimals.qty(l.getQty()));
            it.setPrice(l.getPrice() == null ? BigDecimal.ZERO : l.getPrice());
            it.setAmount(Decimals.money(l.getAmount() != null ? l.getAmount() : it.getQty().multiply(it.getPrice())));
            it.setShipping(Decimals.money(Decimals.nz(l.getShipping())));
            it.setTax(Decimals.money(Decimals.nz(l.getTax())));
            it.setDiscount(Decimals.money(Decimals.nz(l.getDiscount())));
            items.add(it);
            item = item.add(it.getAmount());
            shipping = shipping.add(it.getShipping());
            tax = tax.add(it.getTax());
            discount = discount.add(it.getDiscount());
        }
        // 平台只给订单级运费 / 税时用订单级金额（明细上为 0）
        if (d.getShippingAmount() != null) {
            shipping = Decimals.money(d.getShippingAmount());
        }
        if (d.getTaxAmount() != null) {
            tax = Decimals.money(d.getTaxAmount());
        }
        o.setItemAmount(item);
        o.setShippingAmount(shipping);
        o.setTaxAmount(tax);
        o.setDiscountAmount(discount);
        o.setTotalAmount(item.add(shipping).add(tax).subtract(discount));
        o.setTotalBase(Decimals.money(o.getTotalAmount().multiply(o.getExchangeRate())));
        o.setPlatformFee(estimateCommission(store, o.getTotalAmount()));
        o.setFulfillmentFee(BigDecimal.ZERO);
        o.setOtherFee(BigDecimal.ZERO);
        o.setFeeActual(0);
        o.setRefundAmount(BigDecimal.ZERO);
        o.setCostAmount(BigDecimal.ZERO);
        o.setShippingCost(BigDecimal.ZERO);
        o.setReturnFee(BigDecimal.ZERO);
        EcomOrderTax.assign(o, taxService.resolve(store.getMarketplace(), o.getShipCountry()));
        String exception = outboundSupport.match(store.getId(), items);
        o.setException(exception);
        if (o.getStatus() == EcomOrderStatus.CANCELLED) {
            o.setLocalStatus(EcomLocalStatus.CLOSED);
        } else {
            o.setLocalStatus(exception == null ? EcomLocalStatus.PENDING : EcomLocalStatus.EXCEPTION);
        }
        o.setProfit(profitOf(o));
        LoginUser user = SecurityUtils.getLoginUser();
        o.setDeptId(user == null ? null : user.deptId());
        orderMapper.insert(o);
        items.forEach(i -> i.setOrderId(o.getId()));
        itemMapper.insertBatch(items);
        outboundSupport.saveParts(o.getId(), items);
        logService.log(o.getId(), EcomOrderLogAction.CREATE, createLogText(o, updateExisting));
        if (o.getOrderType() == EcomOrderType.RESEND && o.getSourceOrderId() != null) {
            logService.log(o.getSourceOrderId(), EcomOrderLogAction.RESEND, EcomOrderLogService.text("ecom.log.resendCreated", o.getPlatformOrderNo()));
        }
        // 审单规则（同一事务中同步执行，规则出错不影响写入）
        eventPublisher.publishEvent(new OrderSavedEvent(o.getId()));
        return true;
    }

    /** 写入日志的内容：来源（同步 / 导入 / 手工 / 补发），SKU 未匹配时带上异常 */
    private static String createLogText(EcomOrder o, boolean synced) {
        String source = o.getOrderType() == EcomOrderType.MANUAL ? EcomOrderLogService.text("ecom.log.createManual")
                : o.getOrderType() == EcomOrderType.RESEND ? EcomOrderLogService.text("ecom.log.createResend")
                : EcomOrderLogService.text(synced ? "ecom.log.createSync" : "ecom.log.createImport");
        return o.getException() == null ? source : source + EcomOrderLogService.text("ecom.log.withException", o.getException());
    }

    /** 新订单写入后执行审单规则（EcomOrderOpsService 同步处理） */
    public record OrderSavedEvent(Long orderId) {
    }

    /** 渠道同步发现平台状态变化：取消的未出库订单关闭；平台已发货（FBA）的订单记为平台状态已发货，本地仍需出库记账 */
    private void syncStatus(EcomOrder o, EcomOrderStatus status) {
        logService.log(o.getId(), EcomOrderLogAction.PLATFORM_STATUS, EcomOrderLogService.text("ecom.log.platformStatus",
                dictLookup.options("ecom_order_status").getOrDefault(status.name(), status.name())));
        if (status == EcomOrderStatus.CANCELLED && !CANCEL_DENIED.contains(o.getLocalStatus())) {
            if (packageService.hasLabeled(o.getId())) {
                // 已打单的订单被买家取消：拦截（挂起），由人工取消包裹面单后再关闭
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getStatus, status)
                        .set(EcomOrder::getHoldPrevStatus, o.getLocalStatus() == EcomLocalStatus.ON_HOLD ? o.getHoldPrevStatus() : o.getLocalStatus())
                        .set(EcomOrder::getLocalStatus, EcomLocalStatus.ON_HOLD).set(EcomOrder::getHoldRuleId, null)
                        .set(EcomOrder::getHoldReason, Messages.get("ecom.text.ops.platformCancelledHold")).eq(EcomOrder::getId, o.getId()));
                return;
            }
            packageService.releaseOrder(o.getId());
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getStatus, status).set(EcomOrder::getLocalStatus, EcomLocalStatus.CLOSED)
                    .eq(EcomOrder::getId, o.getId()));
            return;
        }
        if (o.getStatus() != EcomOrderStatus.SHIPPED && o.getStatus() != EcomOrderStatus.REFUNDED) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getStatus, status).eq(EcomOrder::getId, o.getId()));
        }
    }

    private BigDecimal rateOf(String currency, LocalDate date) {
        BigDecimal rate = rateProvider.rate(currency, date);
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", currency, date.getYear() + "-" + String.format("%02d", date.getMonthValue()));
        }
        return rate;
    }

    private static BigDecimal estimateCommission(EcomStore store, BigDecimal total) {
        BigDecimal rate = Decimals.nz(store.getCommissionRate());
        return Decimals.money(total.multiply(rate).divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP));
    }

    /** 按 {@link #profitOf} 计算利润（演示数据按日汇总出库后回写订单时使用） */
    public static BigDecimal computeProfit(EcomOrder o) {
        return profitOf(o);
    }

    /**
     * 利润（本位币）= 本位币金额 − (平台代扣税 + 需申报 VAT + 佣金 + 配送费 + 退货处理费 + 其他费用 + 退款) × 汇率 − 商品成本 − 物流成本。
     * 先按订单上的税率重算税额（{@link EcomOrderTax#apply}，写回 vatAmount / withheldTax）；没有税率时税额为 0
     */
    static BigDecimal profitOf(EcomOrder o) {
        EcomOrderTax.apply(o);
        BigDecimal feesFc = Decimals.nz(o.getWithheldTax()).add(Decimals.nz(o.getVatAmount()))
                .add(Decimals.nz(o.getPlatformFee())).add(Decimals.nz(o.getFulfillmentFee())).add(Decimals.nz(o.getReturnFee()))
                .add(Decimals.nz(o.getOtherFee())).add(Decimals.nz(o.getRefundAmount()));
        return Decimals.money(Decimals.nz(o.getTotalBase()).subtract(feesFc.multiply(Decimals.nz(o.getExchangeRate())))
                .subtract(Decimals.nz(o.getCostAmount())).subtract(Decimals.nz(o.getShippingCost())));
    }

    // ---------------------------------------------------------------- 处理

    /** 异常订单补充映射后重新匹配 */
    @Transactional
    public void rematch(Long id) {
        EcomOrder o = require(id);
        if (o.getLocalStatus() != EcomLocalStatus.EXCEPTION && o.getLocalStatus() != EcomLocalStatus.PENDING) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        List<EcomOrderItem> items = itemMapper.selectByOrderId(id);
        String exception = outboundSupport.matchAndSave(o.getStoreId(), items);
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getException, exception)
                .set(EcomOrder::getLocalStatus, exception == null ? EcomLocalStatus.PENDING : EcomLocalStatus.EXCEPTION).eq(EcomOrder::getId, id));
        logService.log(id, EcomOrderLogAction.REMATCH, exception == null ? EcomOrderLogService.text("ecom.log.matched") : exception);
    }

    /** 订单是否已全部匹配物料（不是异常） */
    public boolean isMatched(Long id) {
        EcomOrder o = orderMapper.selectById(id);
        return o != null && o.getLocalStatus() != EcomLocalStatus.EXCEPTION;
    }

    /**
     * 批量出库：每张订单一个事务，失败的订单记为异常并返回原因，不影响其他订单
     *
     * @return 平台订单号 → 失败原因（成功的不在其中）
     */
    public Map<String, String> outbound(OutboundRequest req) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            try {
                transactionTemplate.executeWithoutResult(s -> outboundOne(id, req.warehouseId(), req.bizDate()));
            } catch (BizException e) {
                EcomOrder o = orderMapper.selectById(id);
                String no = o == null ? String.valueOf(id) : o.getPlatformOrderNo();
                failures.put(no, e.getMessage());
                if (o != null && (o.getLocalStatus() == EcomLocalStatus.PENDING || o.getLocalStatus() == EcomLocalStatus.EXCEPTION)) {
                    orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, EcomLocalStatus.EXCEPTION)
                            .set(EcomOrder::getException, e.getMessage()).eq(EcomOrder::getId, id));
                }
            }
        }
        return failures;
    }

    /**
     * 出库：按配送方式取仓库（平台配送从店铺的平台仓、自发货从默认发货仓），直接开单生成销售出库（客户为店铺对应的客户，
     * 明细单价 = 本行本位币金额 ÷ 物料数量，税率 0），成本取出库成本；平台已发货的订单（FBA）出库后直接为已发货
     */
    @Transactional
    public void outboundOne(Long id, Long warehouseId, LocalDate bizDate) {
        EcomOrder o = require(id);
        if (!OUTBOUND_ALLOWED.contains(o.getLocalStatus())) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        if (o.getStatus() == EcomOrderStatus.CANCELLED || o.getStatus() == EcomOrderStatus.REFUNDED) {
            throw new BizException("ecom.error.orderCancelledCannotOutbound", o.getPlatformOrderNo());
        }
        if (o.getStatus() == EcomOrderStatus.PENDING) {
            throw new BizException("ecom.error.orderUnpaid", o.getPlatformOrderNo());
        }
        if (packageService.hasActive(id)) {
            throw new BizException("ecom.error.ops.orderHasPackages", o.getPlatformOrderNo());
        }
        EcomStore store = storeService.requireEnabled(o.getStoreId());
        List<EcomOrderItem> items = itemMapper.selectByOrderId(id);
        String exception = outboundSupport.matchAndSave(store.getId(), items);
        if (exception != null) {
            throw new BizException("ecom.error.orderSkuUnmapped", exception);
        }
        // 仓库：指定的 > 平台配送取店铺平台仓 > 审单规则指定的发货仓 > 店铺默认发货仓
        Long wh = warehouseId != null ? warehouseId : o.getFulfillment() == FulfillmentType.PLATFORM ? store.getPlatformWarehouseId()
                : o.getWarehouseId() != null ? o.getWarehouseId() : store.getWarehouseId();
        if (wh == null) {
            throw new BizException("ecom.error.storePlatformWarehouseMissing", store.getName());
        }
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        // 组合商品每个组件一行出库
        List<EcomOutboundSupport.Line> lines = items.stream().flatMap(it -> EcomOutboundSupport.Line.wholeExpanded(it).stream()).toList();
        EcomOutboundSupport.Result r = outboundSupport.outbound(o, store, wh, date, lines,
                Messages.get("ecom.text.outboundRemark", store.getName(), o.getPlatformOrderNo()));
        Map<Long, BigDecimal> itemCosts = EcomOutboundSupport.costByItem(lines, r);
        for (EcomOrderItem it : items) {
            it.setCost(itemCosts.getOrDefault(it.getId(), BigDecimal.ZERO));
            itemMapper.updateById(it);
        }
        EcomOutboundSupport.costByPart(lines, r).forEach(outboundSupport::addPartCost);
        BigDecimal shippingCost = o.getFulfillment() == FulfillmentType.PLATFORM ? landedCost(store.getId(), items) : Decimals.nz(o.getShippingCost());
        o.setTradeDocId(r.docId());
        o.setTradeDocNo(r.docNo());
        o.setWarehouseId(wh);
        o.setCostAmount(r.totalCost());
        o.setShippingCost(shippingCost);
        o.setException(null);
        o.setLocalStatus(o.getStatus() == EcomOrderStatus.SHIPPED ? EcomLocalStatus.SHIPPED : EcomLocalStatus.OUTBOUND);
        if (o.getLocalStatus() == EcomLocalStatus.SHIPPED && o.getShipTime() == null) {
            o.setShipTime(LocalDateTime.now());
        }
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getException, null).eq(EcomOrder::getId, id));
        logService.log(id, EcomOrderLogAction.OUTBOUND, EcomOrderLogService.text("ecom.log.outbound", r.docNo()));
    }

    /** 平台配送订单的头程分摊：各物料最近一次货件的单件头程成本 × 数量 */
    private BigDecimal landedCost(Long storeId, List<EcomOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (EcomOrderItem it : items) {
            for (EcomOutboundSupport.Line l : EcomOutboundSupport.Line.wholeExpanded(it)) {
                BigDecimal unit = inboundItemMapper.selectLatestUnitLanded(storeId, l.materialId());
                if (unit != null) {
                    total = total.add(unit.multiply(l.materialQty()));
                }
            }
        }
        return Decimals.money(total);
    }

    /**
     * 发货：已出库的订单填写承运商、运单号，物流成本按选择的渠道和重量试算（或直接填写）；提交后向平台回传发货（有渠道接口时）
     */
    @Transactional
    public void ship(Long id, ShipRequest req, BigDecimal estimatedCost) {
        EcomOrder o = require(id);
        if (o.getLocalStatus() != EcomLocalStatus.OUTBOUND) {
            throw new BizException("ecom.error.orderNotOutbound", o.getPlatformOrderNo());
        }
        BigDecimal cost = req.shippingCost() != null ? req.shippingCost() : estimatedCost != null ? estimatedCost : Decimals.nz(o.getShippingCost());
        o.setCarrier(req.carrier().trim());
        o.setTrackingNo(req.trackingNo().trim());
        o.setShipTime(req.shipTime() == null ? LocalDateTime.now() : req.shipTime());
        o.setChannelId(req.channelId());
        o.setShippingCost(Decimals.money(cost));
        o.setStatus(EcomOrderStatus.SHIPPED);
        o.setLocalStatus(EcomLocalStatus.SHIPPED);
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
        logService.log(id, EcomOrderLogAction.SHIP, o.getCarrier() + " " + o.getTrackingNo());
        eventPublisher.publishEvent(new OrderShippedEvent(o.getId()));
    }

    /** 发货后回传平台（事务提交后由渠道同步服务处理） */
    public record OrderShippedEvent(Long orderId) {
    }

    /** 取消：只能取消还没有出库的订单；已出库的走退货退款 */
    @Transactional
    public void cancel(Long id) {
        EcomOrder o = require(id);
        if (CANCEL_DENIED.contains(o.getLocalStatus())) {
            throw new BizException("ecom.error.orderOutboundCannotCancel", o.getPlatformOrderNo());
        }
        if (packageService.hasLabeled(id)) {
            throw new BizException("ecom.error.ops.orderHasLabeledPackage", o.getPlatformOrderNo());
        }
        packageService.releaseOrder(id);
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getStatus, EcomOrderStatus.CANCELLED)
                .set(EcomOrder::getLocalStatus, EcomLocalStatus.CLOSED).set(EcomOrder::getHoldReason, null).eq(EcomOrder::getId, id));
        logService.log(id, EcomOrderLogAction.CANCEL, null);
    }

    /** 修改收件信息、备注（未发货时） */
    @Transactional
    public void update(Long id, OrderEditRequest req) {
        EcomOrder o = require(id);
        if (EDIT_DENIED.contains(o.getLocalStatus())) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        o.setShipName(req.shipName());
        o.setShipPhone(req.shipPhone());
        o.setShipCountry(req.shipCountry() == null ? null : req.shipCountry().trim().toUpperCase());
        o.setShipState(req.shipState());
        o.setShipCity(req.shipCity());
        o.setShipPostcode(req.shipPostcode());
        o.setShipAddress(req.shipAddress());
        o.setRemark(req.remark());
        if (req.fulfillment() != null) {
            o.setFulfillment(req.fulfillment());
        }
        // 收件国家可能变化：重新匹配税率并重算利润
        EcomOrderTax.assign(o, taxService.resolve(storeService.get(o.getStoreId()).getMarketplace(), o.getShipCountry()));
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
        if (o.getTaxType() == null) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getTaxType, null).set(EcomOrder::getVatRate, null).eq(EcomOrder::getId, id));
        }
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getWeight, req.weight()).eq(EcomOrder::getId, id));
        logService.log(id, EcomOrderLogAction.EDIT, Stream.of(o.getShipName(), o.getShipCountry(), o.getShipState(), o.getShipCity(), o.getShipPostcode(),
                o.getShipAddress()).filter(StringUtils::hasText).collect(Collectors.joining(", ")));
        // 地址可能变化：重新校验地址、黑名单与利润预估
        eventPublisher.publishEvent(new EcomOrderCheckService.AddressChangedEvent(id));
    }

    // ---------------------------------------------------------------- 费用与退款回写

    /**
     * 结算单确认后回写实际费用（结算金额为负数，这里记为正数的费用），有退款时以结算为准
     */
    @Transactional
    public void applySettlementFees(Long storeId, String platformOrderNo, BigDecimal commission, BigDecimal fulfillment, BigDecimal refund, BigDecimal other) {
        applySettlementFees(storeId, platformOrderNo, commission, fulfillment, refund, null, other);
    }

    /**
     * 结算单确认后回写实际费用：结算金额为负数的费用记为正数，退佣、赔偿等正数冲减费用；退货处理费单独一栏
     */
    @Transactional
    public void applySettlementFees(Long storeId, String platformOrderNo, BigDecimal commission, BigDecimal fulfillment, BigDecimal refund,
                                    BigDecimal returnFee, BigDecimal other) {
        EcomOrder o = orderMapper.selectOne(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, storeId)
                .eq(EcomOrder::getPlatformOrderNo, platformOrderNo));
        if (o == null) {
            return;
        }
        if (commission != null && commission.signum() != 0) o.setPlatformFee(commission.negate());
        if (fulfillment != null && fulfillment.signum() != 0) o.setFulfillmentFee(fulfillment.negate());
        if (returnFee != null && returnFee.signum() != 0) o.setReturnFee(returnFee.negate());
        if (other != null && other.signum() != 0) o.setOtherFee(other.negate());
        if (refund != null && refund.signum() != 0) {
            o.setRefundAmount(refund.abs());
            if (o.getRefundAmount().compareTo(Decimals.nz(o.getTotalAmount())) >= 0) {
                o.setStatus(EcomOrderStatus.REFUNDED);
            }
        }
        o.setFeeActual(1);
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
    }

    /** 退货退款登记退款：累加退款金额，全额退款后平台状态为已退款 */
    @Transactional
    public void addRefund(Long orderId, BigDecimal amount) {
        EcomOrder o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BizException("ecom.error.orderNotFound");
        }
        o.setRefundAmount(Decimals.money(Decimals.nz(o.getRefundAmount()).add(Decimals.nz(amount))));
        if (o.getRefundAmount().compareTo(Decimals.nz(o.getTotalAmount())) >= 0) {
            o.setStatus(EcomOrderStatus.REFUNDED);
        }
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
    }

    /** 退货入库后冲减商品成本 */
    @Transactional
    public void subtractCost(Long orderId, BigDecimal cost) {
        EcomOrder o = orderMapper.selectById(orderId);
        if (o == null || cost == null || cost.signum() == 0) {
            return;
        }
        o.setCostAmount(Decimals.money(Decimals.nz(o.getCostAmount()).subtract(cost.abs())));
        if (o.getCostAmount().signum() < 0) {
            o.setCostAmount(BigDecimal.ZERO);
        }
        o.setProfit(profitOf(o));
        orderMapper.updateById(o);
    }

    /** 店铺的已出库 / 已发货订单明细中各物料已退货数量之外可退的行（退货退款校验用） */
    public EcomOrder getForReturn(Long orderId) {
        EcomOrder o = require(orderId);
        o.setItems(outboundSupport.attachParts(itemMapper.selectByOrderId(orderId)));
        return o;
    }

    /** 承诺发货时间最早的待处理订单（看板） */
    public List<EcomOrder> urgent(int top) {
        Page<EcomOrder> page = Page.of(1, top);
        OrderQuery q = new OrderQuery();
        q.setOverdue(true);
        orderMapper.selectPageList(page, q);
        return page.getRecords().stream().sorted(Comparator.comparing(EcomOrder::getShipBy, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    /**
     * 手工创建订单（客服补发等）：按草稿写入，与导入 / 同步的订单相同（匹配在线商品、汇率、审单规则），之后走正常的审核、打单、发货；
     * 写入备注（标记来源，如「补发：原订单 xxx」），返回订单 ID。店铺中已有同号订单时报错
     */
    @Transactional
    public Long createManual(OrderDraft d, String remark) {
        if (orderMapper.exists(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, d.getStoreId())
                .eq(EcomOrder::getPlatformOrderNo, d.getPlatformOrderNo()))) {
            throw new BizException("ecom.error.cs.manualOrderExists", d.getPlatformOrderNo());
        }
        saveDraft(d, false);
        EcomOrder o = orderMapper.selectOne(Wrappers.<EcomOrder>lambdaQuery().eq(EcomOrder::getStoreId, d.getStoreId())
                .eq(EcomOrder::getPlatformOrderNo, d.getPlatformOrderNo()));
        if (StringUtils.hasText(remark)) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getRemark, remark.length() <= 255 ? remark : remark.substring(0, 255))
                    .eq(EcomOrder::getId, o.getId()));
        }
        return o.getId();
    }

    static boolean same(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
