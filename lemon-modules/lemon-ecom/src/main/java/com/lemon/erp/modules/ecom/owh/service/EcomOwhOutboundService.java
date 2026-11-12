package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.CancelRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.PushRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.PackageOwh;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhProduct;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhOutboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhProductMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.service.EcomPackageService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 海外仓出库（一件代发）：发货仓是海外仓的包裹（审核后生成，待打单）不在本地打单，而是「推送海外仓」生成服务商出库单
 * （物流产品按 指定 → 包裹物流渠道对应的产品 → 仓库映射的默认产品 取；仓库映射开启自动推送时由定时任务推送）。
 * 推送失败记录原因、可重推；定时 / 手工同步出库状态，拿到运单号后包裹写入承运商与运单号，按现有流程发货
 * （生成销售出库、成本，订单发完后回传平台）；已推送的可取消，服务商已开始作业时为截单中、之后同步结果。
 * <p>
 * 包裹运费取出库单已同步的操作费 + 尾程运费（本位币），之后同步到的费用再加到订单物流成本（{@link EcomOwhFeeService}）。
 * 服务商接口在事务之外调用；批量推送、同步逐个处理，一个失败不影响其他
 */
@Service
@RequiredArgsConstructor
public class EcomOwhOutboundService {

    /** 待推送标签页的状态键 */
    public static final String PENDING = "PENDING";
    /** 自动推送每次最多处理的包裹数 */
    private static final int AUTO_PUSH_LIMIT = 200;

    private final EcomOwhOutboundMapper outboundMapper;
    private final EcomOwhProductMapper productMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhFeeService feeService;
    private final EcomOwhSupport support;
    private final EcomOwhSkuService skuService;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomPackageService packageService;
    private final DocNoService docNoService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomOwhOutbound> page(long current, long size, Long accountId, Long warehouseId, Long storeId, OwhOutboundStatus status, String keyword) {
        Page<EcomOwhOutbound> page = Page.of(current, size);
        outboundMapper.selectPageList(page, accountId, warehouseId, storeId, status, keyword);
        fillOrderNos(page.getRecords());
        return PageResult.of(page);
    }

    /** 待推送的包裹 */
    public PageResult<EcomPackage> pending(long current, long size, Long accountId, Long warehouseId, Long storeId, String keyword) {
        Page<EcomPackage> page = Page.of(current, size);
        outboundMapper.selectPending(page, accountId, warehouseId, storeId, keyword);
        Map<Long, String> nos = orderNos(page.getRecords().stream().map(EcomPackage::getId).toList());
        page.getRecords().forEach(p -> p.setOrderNos(nos.get(p.getId())));
        return PageResult.of(page);
    }

    /** 各状态数量，含待推送 PENDING */
    public Map<String, Long> statusCounts(Long accountId, Long warehouseId, Long storeId, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        out.put(PENDING, outboundMapper.countPending(accountId, warehouseId, storeId, keyword));
        for (Map<String, Object> row : outboundMapper.countByStatus(accountId, warehouseId, storeId, keyword)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomOwhOutbound detail(Long id) {
        EcomOwhOutbound o = id == null ? null : outboundMapper.selectDetail(id);
        if (o == null) {
            throw new BizException("ecom.error.owh.outboundNotFound");
        }
        fillOrderNos(List.of(o));
        return o;
    }

    /** 包裹的出库明细（物料编码即海外仓 SKU） */
    public List<EcomPackageItem> items(Long id) {
        return packageItemMapper.selectByPackageIds(List.of(detail(id).getPackageId()));
    }

    /** 各包裹最近一张出库单的状态（包裹列表用） */
    public List<PackageOwh> byPackages(Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        Map<Long, EcomOwhOutbound> latest = new LinkedHashMap<>();
        for (EcomOwhOutbound o : outboundMapper.selectByPackageIds(packageIds)) {
            latest.putIfAbsent(o.getPackageId(), o);
        }
        return latest.values().stream().map(o -> new PackageOwh(o.getPackageId(), o.getId(), o.getOutboundNo(), o.getStatus().name(), o.getOwhOrderNo(),
                o.getTrackingNo(), o.getErrorMsg(), o.getAccountName())).toList();
    }

    /** 订单的海外仓出库单（订单详情用） */
    public List<EcomOwhOutbound> byOrder(Long orderId) {
        List<EcomOwhOutbound> list = outboundMapper.selectByOrderId(orderId);
        fillOrderNos(list);
        return list;
    }

    private void fillOrderNos(List<EcomOwhOutbound> list) {
        Map<Long, String> nos = orderNos(list.stream().map(EcomOwhOutbound::getPackageId).toList());
        list.forEach(o -> o.setOrderNos(nos.get(o.getPackageId())));
    }

    private Map<Long, String> orderNos(List<Long> packageIds) {
        Map<Long, String> out = new HashMap<>();
        if (packageIds.isEmpty()) {
            return out;
        }
        Map<Long, Set<String>> sets = new HashMap<>();
        for (Map<String, Object> row : outboundMapper.selectOrderNos(new LinkedHashSet<>(packageIds))) {
            sets.computeIfAbsent(((Number) row.get("packageId")).longValue(), k -> new LinkedHashSet<>()).add(String.valueOf(row.get("orderNo")));
        }
        sets.forEach((k, v) -> out.put(k, String.join(",", v)));
        return out;
    }

    // ---------------------------------------------------------------- 推送

    /**
     * 批量推送（或重推推送失败的）包裹，逐个处理
     *
     * @return 包裹号 → 失败原因
     */
    public Map<String, String> push(PushRequest req) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            try {
                pushOne(id, req.productCode());
            } catch (BizException e) {
                EcomPackage p = packageMapper.selectById(id);
                failures.put(p == null ? String.valueOf(id) : p.getPackageNo(), e.getMessage());
            }
        }
        return failures;
    }

    /** 自动推送：仓库映射开启了自动推送、从未推送过的待打单包裹（定时任务） */
    public List<SyncResult> autoPush() {
        List<SyncResult> out = new ArrayList<>();
        for (Long id : outboundMapper.selectAutoPushPackageIds(AUTO_PUSH_LIMIT)) {
            EcomPackage p = packageMapper.selectById(id);
            try {
                pushOne(id, null);
                out.add(new SyncResult(p.getPackageNo(), 1, null));
            } catch (BizException e) {
                out.add(new SyncResult(p.getPackageNo(), 0, e.getMessage()));
            }
        }
        return out;
    }

    /**
     * 推送一个包裹：校验后调用服务商创建出库单（事务之外），成功记为已推送，失败记为推送失败（同一包裹的失败记录复用）并抛出原因
     *
     * @return 出库单 ID
     */
    public Long pushOne(Long packageId, String productCode) {
        EcomPackage p = packageId == null ? null : packageMapper.selectDetail(packageId);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
            throw new BizException("ecom.error.owh.packageNotWaiting", p.getPackageNo());
        }
        EcomOwhWarehouse m = accountService.mappingOf(p.getWarehouseId());
        if (m == null) {
            throw new BizException("ecom.error.owh.notOverseasWarehouse");
        }
        if (m.getStatus() == EnableStatus.DISABLED) {
            throw new BizException("ecom.error.owh.mappingDisabled", m.getWarehouseName());
        }
        if (outboundMapper.exists(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getPackageId, packageId)
                .in(EcomOwhOutbound::getStatus, OwhOutboundStatus.ACTIVE))) {
            throw new BizException("ecom.error.owh.packageAlreadyPushed", p.getPackageNo());
        }
        EcomOwhAccount a = accountService.get(m.getAccountId());
        List<EcomPackageItem> items = packageItemMapper.selectByPackageIds(List.of(packageId));
        List<EcomOrder> orders = ordersOf(items);
        String product = resolveProduct(a.getId(), m, p, productCode);
        EcomOwhOutbound failed = outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getPackageId, packageId)
                .eq(EcomOwhOutbound::getStatus, OwhOutboundStatus.FAILED).orderByDesc(EcomOwhOutbound::getId)).stream().findFirst().orElse(null);
        OwhData.OutboundResult result;
        try {
            checkOrders(p, orders);
            if (product == null) {
                throw new BizException("ecom.error.owh.productRequired", p.getPackageNo());
            }
            result = support.connector(a.getProvider()).createOutbound(support.context(a), draft(p, m, product, items, orders,
                    skuService.skus(a.getId(), items.stream().map(EcomPackageItem::getMaterialId).toList())));
        } catch (BizException e) {
            String msg = EcomOwhSupport.abbreviate(e.getMessage());
            transactionTemplate.executeWithoutResult(s -> {
                if (failed == null) {
                    EcomOwhOutbound o = newOutbound(p, a, m, product);
                    o.setStatus(OwhOutboundStatus.FAILED);
                    o.setErrorMsg(msg);
                    o.setPushCount(1);
                    outboundMapper.insert(o);
                } else {
                    outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getErrorMsg, msg)
                            .set(EcomOwhOutbound::getProductCode, product).set(EcomOwhOutbound::getPushCount, count(failed.getPushCount()) + 1)
                            .eq(EcomOwhOutbound::getId, failed.getId()));
                }
            });
            throw e;
        }
        Long id = transactionTemplate.execute(s -> {
            LocalDateTime now = LocalDateTime.now();
            EcomOwhOutbound o = failed == null ? newOutbound(p, a, m, product) : failed;
            o.setProductCode(product);
            o.setOwhOrderNo(result.owhOrderNo());
            o.setStatus(OwhOutboundStatus.PUSHED);
            o.setPushTime(now);
            o.setPushCount(count(o.getPushCount()) + 1);
            o.setEstimateFee(result.estimateFee());
            o.setFeeCurrency(result.feeCurrency());
            o.setErrorMsg(null);
            if (failed == null) {
                outboundMapper.insert(o);
            } else {
                outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getProductCode, product)
                        .set(EcomOwhOutbound::getOwhOrderNo, result.owhOrderNo()).set(EcomOwhOutbound::getStatus, OwhOutboundStatus.PUSHED)
                        .set(EcomOwhOutbound::getPushTime, now).set(EcomOwhOutbound::getPushCount, o.getPushCount())
                        .set(EcomOwhOutbound::getEstimateFee, result.estimateFee()).set(EcomOwhOutbound::getFeeCurrency, result.feeCurrency())
                        .set(EcomOwhOutbound::getErrorMsg, null).eq(EcomOwhOutbound::getId, o.getId()));
            }
            return o.getId();
        });
        if (StringUtils.hasText(result.trackingNo())) {
            applyState(id, new OwhData.OutboundState(OwhOutboundStatus.OUTBOUND, null, result.trackingNo(), null, null));
        }
        return id;
    }

    private EcomOwhOutbound newOutbound(EcomPackage p, EcomOwhAccount a, EcomOwhWarehouse m, String product) {
        EcomOwhOutbound o = new EcomOwhOutbound();
        o.setOutboundNo(docNoService.next(EcomOwhDocNos.OUTBOUND));
        o.setPackageId(p.getId());
        o.setStoreId(p.getStoreId());
        o.setAccountId(a.getId());
        o.setMappingId(m.getId());
        o.setWarehouseId(p.getWarehouseId());
        o.setProductCode(product);
        o.setPushCount(0);
        return o;
    }

    /** 物流产品：指定的 → 包裹物流渠道对应的（启用）→ 仓库映射的默认产品 */
    private String resolveProduct(Long accountId, EcomOwhWarehouse m, EcomPackage p, String productCode) {
        if (StringUtils.hasText(productCode)) {
            return productCode.trim();
        }
        if (p.getChannelId() != null) {
            List<EcomOwhProduct> list = productMapper.selectList(Wrappers.<EcomOwhProduct>lambdaQuery().eq(EcomOwhProduct::getAccountId, accountId)
                    .eq(EcomOwhProduct::getChannelId, p.getChannelId()).eq(EcomOwhProduct::getStatus, EnableStatus.ENABLED));
            EcomOwhProduct match = list.stream().filter(x -> x.getOwhCode() == null || x.getOwhCode().equals(m.getOwhCode())).findFirst().orElse(null);
            if (match != null) {
                return match.getProductCode();
            }
        }
        return m.getDefaultProductCode();
    }

    /** 出库单内容：收件人取第一个订单；明细按物料汇总（SKU 按映射，没有时为物料编码）；申报价值为各行售价 × 件数（订单币种） */
    private static OwhData.OutboundDraft draft(EcomPackage p, EcomOwhWarehouse m, String product, List<EcomPackageItem> items, List<EcomOrder> orders,
                                               Map<Long, String> skus) {
        EcomOrder o = orders.get(0);
        OwhData.Receiver r = new OwhData.Receiver(o.getShipName(), o.getShipPhone(), o.getBuyerEmail(), o.getShipCountry(), o.getShipState(),
                o.getShipCity(), o.getShipPostcode(), o.getShipAddress());
        Map<String, OwhData.Line> lines = new LinkedHashMap<>();
        BigDecimal value = BigDecimal.ZERO;
        for (EcomPackageItem it : items) {
            String sku = skus.getOrDefault(it.getMaterialId(), StringUtils.hasText(it.getMaterialCode()) ? it.getMaterialCode() : String.valueOf(it.getMaterialId()));
            BigDecimal q = it.getMaterialQty().setScale(0, RoundingMode.CEILING);
            lines.merge(sku, new OwhData.Line(sku, it.getMaterialName(), q), (x, y) -> new OwhData.Line(x.sku(), x.name(), x.qty().add(y.qty())));
            if (it.getPartMain() == null || it.getPartMain() == 1) {
                value = value.add(Decimals.nz(it.getPrice()).multiply(it.getQty()));
            }
        }
        String orderNos = orders.stream().map(EcomOrder::getPlatformOrderNo).collect(Collectors.joining(","));
        return new OwhData.OutboundDraft(p.getPackageNo(), m.getOwhCode(), product, r, new ArrayList<>(lines.values()), o.getCurrency(),
                Decimals.money(value), orderNos, p.getRemark());
    }

    private List<EcomOrder> ordersOf(List<EcomPackageItem> items) {
        Set<Long> ids = items.stream().map(EcomPackageItem::getOrderId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        return orderMapper.selectByIds(ids).stream().sorted(Comparator.comparing(EcomOrder::getId)).toList();
    }

    /** 推送前拦截：包裹没有订单、订单已挂起、已被取消 / 退款 */
    private static void checkOrders(EcomPackage p, List<EcomOrder> orders) {
        if (orders.isEmpty()) {
            throw new BizException("ecom.error.ops.packageEmpty", p.getPackageNo());
        }
        for (EcomOrder o : orders) {
            if (o.getLocalStatus() == EcomLocalStatus.ON_HOLD) {
                throw new BizException("ecom.error.ops.packageOrderOnHold", o.getPlatformOrderNo(), Objects.toString(o.getHoldReason(), "-"));
            }
            if (o.getLocalStatus() == EcomLocalStatus.CLOSED || o.getStatus() == EcomOrderStatus.CANCELLED || o.getStatus() == EcomOrderStatus.REFUNDED) {
                throw new BizException("ecom.error.ops.packageOrderCancelled", o.getPlatformOrderNo());
            }
        }
    }

    // ---------------------------------------------------------------- 同步与发货

    /** 同步全部处理中的出库单（定时任务），一张失败不影响其他 */
    public List<SyncResult> syncAll() {
        List<SyncResult> out = new ArrayList<>();
        for (Long id : outboundMapper.selectSyncingIds(OwhOutboundStatus.SYNCING)) {
            out.add(syncQuietly(id));
        }
        return out;
    }

    /** 批量同步（页面），返回 出库单号 → 失败原因 */
    public Map<String, String> sync(List<Long> ids) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : ids) {
            SyncResult r = syncQuietly(id);
            if (r.message() != null) {
                failures.put(r.name(), r.message());
            }
        }
        return failures;
    }

    private SyncResult syncQuietly(Long id) {
        EcomOwhOutbound o = outboundMapper.selectById(id);
        String no = o == null ? String.valueOf(id) : o.getOutboundNo();
        try {
            syncOne(id);
            EcomOwhOutbound after = outboundMapper.selectById(id);
            return new SyncResult(no, 1, after == null ? null : after.getStatus() == OwhOutboundStatus.OUTBOUND ? after.getErrorMsg() : null);
        } catch (BizException e) {
            recordError(id, e.getMessage());
            return new SyncResult(no, 0, e.getMessage());
        }
    }

    /**
     * 同步一张：查询服务商的出库状态；已出库且有运单号的发货（已出库但上次发货失败的直接重试发货）
     *
     * @return 同步后的状态
     */
    public OwhOutboundStatus syncOne(Long id) {
        EcomOwhOutbound o = detail(id);
        if (o.getStatus() == OwhOutboundStatus.OUTBOUND) {
            ship(id);
            return outboundMapper.selectById(id).getStatus();
        }
        if (!OwhOutboundStatus.SYNCING.contains(o.getStatus()) || o.getOwhOrderNo() == null) {
            throw new BizException("ecom.error.owh.outboundNotSyncable", o.getOutboundNo());
        }
        EcomOwhAccount a = accountService.get(o.getAccountId());
        OwhData.OutboundState state = support.connector(a.getProvider()).queryOutbound(support.context(a), o.getOwhOrderNo());
        applyState(id, state);
        return outboundMapper.selectById(id).getStatus();
    }

    /**
     * 写入服务商状态（同步、服务商回调使用）：取消、截单中、处理中只更新状态；已出库且有运单号时记录运单号并发货
     */
    public void applyState(Long id, OwhData.OutboundState state) {
        LocalDateTime now = LocalDateTime.now();
        OwhOutboundStatus st = state.status() == null ? OwhOutboundStatus.PUSHED : state.status();
        boolean outbound = st == OwhOutboundStatus.OUTBOUND && StringUtils.hasText(state.trackingNo());
        if (st == OwhOutboundStatus.OUTBOUND && !outbound) {
            // 已出库但还没有运单号：保持已推送，下次同步再取
            st = OwhOutboundStatus.PUSHED;
        }
        OwhOutboundStatus target = st;
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhOutbound locked = outboundMapper.selectForUpdate(id);
            if (!OwhOutboundStatus.SYNCING.contains(locked.getStatus())) {
                return;
            }
            String carrier = StringUtils.hasText(state.carrier()) ? state.carrier().trim() : locked.getCarrierName();
            if (!StringUtils.hasText(carrier)) {
                carrier = productCarrier(locked);
            }
            outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getStatus, target)
                    .set(state.rawStatus() != null, EcomOwhOutbound::getOwhStatus, state.rawStatus())
                    .set(outbound, EcomOwhOutbound::getTrackingNo, outbound ? state.trackingNo().trim() : null)
                    .set(outbound, EcomOwhOutbound::getCarrierName, carrier)
                    .set(outbound, EcomOwhOutbound::getOutboundTime, state.shipTime() == null ? now : state.shipTime())
                    .set(EcomOwhOutbound::getSyncTime, now).set(EcomOwhOutbound::getErrorMsg, null)
                    .eq(EcomOwhOutbound::getId, id));
        });
        if (outbound) {
            ship(id);
        }
    }

    /** 承运商：物流产品设置的承运商 → 产品名称 → 产品代码 */
    private String productCarrier(EcomOwhOutbound o) {
        EcomOwhProduct p = productMapper.selectOne(Wrappers.<EcomOwhProduct>lambdaQuery().eq(EcomOwhProduct::getAccountId, o.getAccountId())
                .eq(EcomOwhProduct::getProductCode, o.getProductCode()), false);
        if (p != null && StringUtils.hasText(p.getCarrierName())) {
            return p.getCarrierName();
        }
        if (p != null && StringUtils.hasText(p.getProductName())) {
            return p.getProductName().length() > 64 ? p.getProductName().substring(0, 64) : p.getProductName();
        }
        return o.getProductCode();
    }

    /**
     * 已出库的出库单发货：包裹写入承运商、运单号与运费（出库单待入账的操作费 + 尾程运费）、状态改为待发货，再按包裹发货的现有流程
     * 生成销售出库并回传平台；发货失败时记录原因（保持已出库，下次同步重试）。包裹已经发货（如在包裹打单页面手工发货）的直接记为已发货
     */
    void ship(Long id) {
        EcomOwhSupport.asSystem(() -> {
            EcomOwhOutbound o = outboundMapper.selectById(id);
            EcomPackage p = packageMapper.selectById(o.getPackageId());
            if (p.getStatus() == EcomPackageStatus.SHIPPED) {
                markShipped(id, p.getShipTime());
                return;
            }
            if (p.getStatus() == EcomPackageStatus.CANCELLED) {
                recordError(id, Messages.get("ecom.text.owh.packageCancelled", p.getPackageNo()));
                return;
            }
            try {
                transactionTemplate.executeWithoutResult(s -> {
                    EcomPackage locked = packageMapper.selectForUpdate(p.getId());
                    if (locked.getStatus() == EcomPackageStatus.WAIT_LABEL) {
                        BigDecimal cost = feeService.takePendingForShipment(id, p.getId());
                        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getCarrier, null)
                                .set(EcomPackage::getCarrierOrderNo, o.getOwhOrderNo()).set(EcomPackage::getCarrierName, o.getCarrierName())
                                .set(EcomPackage::getTrackingNo, o.getTrackingNo()).set(EcomPackage::getLabelTime, LocalDateTime.now())
                                .set(EcomPackage::getShippingCost, cost).set(EcomPackage::getStatus, EcomPackageStatus.LABELED)
                                .set(EcomPackage::getErrorMsg, null).eq(EcomPackage::getId, p.getId()));
                        packageItemMapper.selectByPackageIds(List.of(p.getId())).stream().map(EcomPackageItem::getOrderId).distinct()
                                .forEach(packageService::recomputeOrder);
                    }
                });
            } catch (BizException e) {
                recordError(id, e.getMessage());
                return;
            }
            LocalDateTime shipTime = o.getOutboundTime() == null ? LocalDateTime.now() : o.getOutboundTime();
            Map<String, String> failures = packageService.ship(List.of(p.getId()), shipTime);
            if (failures.isEmpty()) {
                markShipped(id, shipTime);
            } else {
                recordError(id, String.join("; ", failures.values()));
            }
        });
    }

    private void markShipped(Long id, LocalDateTime shipTime) {
        outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getStatus, OwhOutboundStatus.SHIPPED)
                .set(EcomOwhOutbound::getShipTime, shipTime == null ? LocalDateTime.now() : shipTime).set(EcomOwhOutbound::getErrorMsg, null)
                .eq(EcomOwhOutbound::getId, id));
    }

    // ---------------------------------------------------------------- 取消

    /**
     * 取消 / 截单：推送失败的直接取消；已推送 / 截单中的调用服务商取消（事务之外），服务商已开始作业时为截单中，之后同步结果；
     * 取消后包裹回到待推送，选择「同时取消包裹」时包裹一并取消（订单回到待审核）
     *
     * @return 取消后的状态
     */
    public OwhOutboundStatus cancel(Long id, CancelRequest req) {
        EcomOwhOutbound o = detail(id);
        OwhOutboundStatus st;
        if (o.getStatus() == OwhOutboundStatus.FAILED) {
            st = OwhOutboundStatus.CANCELLED;
        } else if (o.getStatus() == OwhOutboundStatus.PUSHED || o.getStatus() == OwhOutboundStatus.INTERCEPTING) {
            EcomOwhAccount a = accountService.get(o.getAccountId());
            String reason = req == null || !StringUtils.hasText(req.reason()) ? null : req.reason().trim();
            boolean done = support.connector(a.getProvider()).cancelOutbound(support.context(a), o.getOwhOrderNo(), reason);
            st = done ? OwhOutboundStatus.CANCELLED : OwhOutboundStatus.INTERCEPTING;
        } else {
            throw new BizException("ecom.error.owh.outboundNotCancellable", o.getOutboundNo());
        }
        String remark = req == null || !StringUtils.hasText(req.reason()) ? null : EcomOwhSupport.abbreviate(req.reason().trim());
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhOutbound locked = outboundMapper.selectForUpdate(id);
            if (locked.getStatus() == OwhOutboundStatus.OUTBOUND || locked.getStatus() == OwhOutboundStatus.SHIPPED) {
                throw new BizException("ecom.error.owh.outboundNotCancellable", locked.getOutboundNo());
            }
            outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getStatus, st)
                    .set(remark != null, EcomOwhOutbound::getRemark, remark).set(EcomOwhOutbound::getErrorMsg, null)
                    .eq(EcomOwhOutbound::getId, id));
        });
        if (st == OwhOutboundStatus.CANCELLED && req != null && Boolean.TRUE.equals(req.cancelPackage())) {
            packageService.cancel(o.getPackageId());
        }
        return st;
    }

    private void recordError(Long id, String message) {
        outboundMapper.update(Wrappers.<EcomOwhOutbound>lambdaUpdate().set(EcomOwhOutbound::getErrorMsg, EcomOwhSupport.abbreviate(message))
                .set(EcomOwhOutbound::getSyncTime, LocalDateTime.now()).eq(EcomOwhOutbound::getId, id));
    }

    private static int count(Integer n) {
        return n == null ? 0 : n;
    }
}
