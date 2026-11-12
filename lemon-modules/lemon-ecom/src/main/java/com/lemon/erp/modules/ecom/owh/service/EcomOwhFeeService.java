package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.carrier.CarrierSigns;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.FeeSummary;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFee;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFeeAlloc;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhAccountMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhFeeAllocMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhFeeMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhOutboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhStockMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 海外仓费用：从服务商同步费用明细（按服务商流水号去重），按费用日期所在月的汇率折算本位币，并入账：
 * <ul>
 *   <li>出库单的操作费、尾程运费：包裹已发货的按订单金额分摊加到订单的物流成本并重算利润；还没发货的待入账，发货时作为包裹运费
 *   （{@link #takePendingForShipment}）；</li>
 *   <li>仓租、入库费等按 SKU 的费用：按费用当月从该仓库发出的销量分摊到 店铺 × 平台 SKU（没有销量时平均分到映射该物料的在线商品），
 *   写入 ecom_owh_fee_alloc，利润报表作为仓储费（仓租）/ 其他费用扣除；入库单的费用没有 SKU 时计入来源货件的店铺；</li>
 *   <li>都归属不了的为「无法归属」，补充在线商品映射后可重新入账。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EcomOwhFeeService {

    private final EcomOwhFeeMapper feeMapper;
    private final EcomOwhFeeAllocMapper allocMapper;
    private final EcomOwhAccountMapper accountMapper;
    private final EcomOwhOutboundMapper outboundMapper;
    private final EcomOwhInboundMapper inboundMapper;
    private final EcomOwhStockMapper stockMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhSupport support;
    private final EcomOwhSkuService skuService;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomInboundService ecomInboundService;
    private final ExchangeRateProvider exchangeRateProvider;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomOwhFee> page(long current, long size, Long accountId, OwhFeeType feeType, OwhFeeStatus status, LocalDate begin, LocalDate end,
                                       String keyword) {
        Page<EcomOwhFee> page = Page.of(current, size);
        feeMapper.selectPageList(page, accountId, feeType, status, begin, end, keyword);
        return PageResult.of(page);
    }

    /** 按月 × 类型汇总（本位币），以及各类型合计 */
    public FeeSummary summary(Long accountId, LocalDate begin, LocalDate end) {
        List<Map<String, Object>> rows = feeMapper.selectSummary(accountId, begin, end);
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (OwhFeeType t : OwhFeeType.values()) {
            byType.put(t.name(), BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            BigDecimal a = dec(r.get("amount"));
            byType.merge(String.valueOf(r.get("feeType")), a, BigDecimal::add);
            total = total.add(a);
        }
        return new FeeSummary(rows, byType, Decimals.money(total));
    }

    /** 费用的入账明细 */
    public List<EcomOwhFeeAlloc> allocations(Long feeId) {
        return allocMapper.selectByFee(feeId);
    }

    // ---------------------------------------------------------------- 同步

    /** 同步全部启用且开启自动同步的账号的费用（定时任务：最近 7 天） */
    public List<SyncResult> syncAll(LocalDate begin, LocalDate end) {
        List<SyncResult> out = new ArrayList<>();
        for (EcomOwhAccount a : accountMapper.selectList(Wrappers.<EcomOwhAccount>lambdaQuery().eq(EcomOwhAccount::getStatus, EnableStatus.ENABLED)
                .eq(EcomOwhAccount::getAutoSync, 1))) {
            try {
                out.add(new SyncResult(a.getName(), sync(a.getId(), begin, end), null));
            } catch (BizException e) {
                accountService.recordError(a.getId(), e.getMessage());
                out.add(new SyncResult(a.getName(), 0, e.getMessage()));
            }
        }
        return out;
    }

    /**
     * 同步一个账号在日期区间内的费用（服务商接口在事务之外），新的费用写入并入账
     *
     * @return 新增的费用条数
     */
    public int sync(Long accountId, LocalDate begin, LocalDate end) {
        EcomOwhAccount a = accountService.get(accountId);
        List<OwhData.Fee> fees = support.connector(a.getProvider()).fees(support.context(a), begin, end);
        Integer n = transactionTemplate.execute(s -> save(a, fees));
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getFeeSyncTime, LocalDateTime.now())
                .eq(EcomOwhAccount::getId, accountId));
        return n == null ? 0 : n;
    }

    /** 写入费用（已有的流水号跳过）并入账（内部，调用方的事务中执行） */
    public int save(EcomOwhAccount a, List<OwhData.Fee> fees) {
        Map<String, EcomOwhWarehouse> mappings = new HashMap<>();
        accountService.mappings(a.getId()).forEach(m -> mappings.put(m.getOwhCode(), m));
        Set<String> codes = fees.stream().map(OwhData.Fee::sku).filter(StringUtils::hasText).collect(Collectors.toSet());
        Map<String, Long> materials = skuService.materials(a.getId(), codes);
        int n = 0;
        for (OwhData.Fee f : fees) {
            if (f.amount() == null || f.amount().signum() == 0) {
                continue;
            }
            String externalId = StringUtils.hasText(f.externalId()) ? f.externalId().trim()
                    : CarrierSigns.md5Lower(f.type() + "|" + f.date() + "|" + f.refNo() + "|" + f.sku() + "|" + f.amount().toPlainString());
            if (feeMapper.exists(Wrappers.<EcomOwhFee>lambdaQuery().eq(EcomOwhFee::getAccountId, a.getId()).eq(EcomOwhFee::getExternalId, externalId))) {
                continue;
            }
            String currency = StringUtils.hasText(f.currency()) ? f.currency().trim().toUpperCase()
                    : StringUtils.hasText(a.getCurrency()) ? a.getCurrency() : ExchangeRateProvider.BASE;
            BigDecimal rate = exchangeRateProvider.rate(currency, f.date());
            if (rate == null) {
                throw new BizException("ecom.error.exchangeRateMissing", currency, f.date().getYear() + "-" + String.format("%02d", f.date().getMonthValue()));
            }
            EcomOwhFee fee = new EcomOwhFee();
            fee.setAccountId(a.getId());
            fee.setExternalId(externalId);
            fee.setFeeType(f.type() == null ? OwhFeeType.OTHER : f.type());
            fee.setFeeDate(f.date());
            fee.setRefNo(f.refNo());
            fee.setOwhCode(f.warehouseCode());
            EcomOwhWarehouse m = f.warehouseCode() == null ? null : mappings.get(f.warehouseCode());
            fee.setWarehouseId(m == null ? null : m.getWarehouseId());
            fee.setSku(f.sku());
            fee.setMaterialId(f.sku() == null ? null : materials.get(f.sku()));
            linkDocument(a.getId(), fee);
            fee.setCurrency(currency);
            fee.setAmount(Decimals.money(f.amount()));
            fee.setExchangeRate(rate);
            fee.setAmountBase(Decimals.money(f.amount().multiply(rate)));
            fee.setStatus(OwhFeeStatus.PENDING);
            fee.setRemark(f.remark() == null || f.remark().length() <= 255 ? f.remark() : f.remark().substring(0, 255));
            feeMapper.insert(fee);
            post(fee);
            n++;
        }
        return n;
    }

    /** 按关联单号找出库单（服务商单号、出库单号或包裹号）/ 入库单（服务商单号或入库单号），出库单的仓库补到费用上 */
    private void linkDocument(Long accountId, EcomOwhFee fee) {
        if (!StringUtils.hasText(fee.getRefNo())) {
            return;
        }
        String ref = fee.getRefNo().trim();
        EcomOwhOutbound o = outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, accountId)
                .and(w -> w.eq(EcomOwhOutbound::getOwhOrderNo, ref).or().eq(EcomOwhOutbound::getOutboundNo, ref))
                .orderByDesc(EcomOwhOutbound::getId)).stream().findFirst().orElse(null);
        if (o == null) {
            EcomPackage p = packageMapper.selectOne(Wrappers.<EcomPackage>lambdaQuery().eq(EcomPackage::getPackageNo, ref));
            if (p != null) {
                o = outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, accountId)
                        .eq(EcomOwhOutbound::getPackageId, p.getId()).orderByDesc(EcomOwhOutbound::getId)).stream().findFirst().orElse(null);
            }
        }
        if (o != null) {
            fee.setOutboundId(o.getId());
            if (fee.getWarehouseId() == null) {
                fee.setWarehouseId(o.getWarehouseId());
            }
            return;
        }
        EcomOwhInbound i = inboundMapper.selectList(Wrappers.<EcomOwhInbound>lambdaQuery().eq(EcomOwhInbound::getAccountId, accountId)
                .and(w -> w.eq(EcomOwhInbound::getOwhOrderNo, ref).or().eq(EcomOwhInbound::getInboundNo, ref))
                .orderByDesc(EcomOwhInbound::getId)).stream().findFirst().orElse(null);
        if (i != null) {
            fee.setInboundId(i.getId());
            if (fee.getWarehouseId() == null) {
                fee.setWarehouseId(i.getWarehouseId());
            }
        }
    }

    // ---------------------------------------------------------------- 入账

    /** 重新入账「待入账 / 无法归属」的费用（补充在线商品映射后），返回入账成功的条数 */
    @Transactional
    public int repost(List<Long> ids) {
        int n = 0;
        for (EcomOwhFee f : feeMapper.selectByIds(ids)) {
            if (f.getStatus() == OwhFeeStatus.POSTED) {
                continue;
            }
            allocMapper.delete(Wrappers.<EcomOwhFeeAlloc>lambdaQuery().eq(EcomOwhFeeAlloc::getFeeId, f.getId()));
            post(f);
            if (feeMapper.selectById(f.getId()).getStatus() == OwhFeeStatus.POSTED) {
                n++;
            }
        }
        return n;
    }

    /** 入账一条费用（内部） */
    void post(EcomOwhFee fee) {
        if (fee.getAmountBase() == null) {
            return;
        }
        if (fee.getFeeType().orderCost() && fee.getOutboundId() != null) {
            EcomOwhOutbound o = outboundMapper.selectById(fee.getOutboundId());
            EcomPackage p = o == null ? null : packageMapper.selectById(o.getPackageId());
            if (p == null || p.getStatus() != EcomPackageStatus.SHIPPED) {
                // 还没发货：发货时作为包裹运费
                return;
            }
            List<EcomOrder> orders = ordersOf(p.getId());
            Map<Long, BigDecimal> shares = distribute(orders, fee.getAmountBase());
            List<EcomOwhFeeAlloc> allocs = new ArrayList<>();
            for (EcomOrder ord : orders) {
                BigDecimal share = shares.get(ord.getId());
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate()
                        .set(EcomOrder::getShippingCost, Decimals.money(Decimals.nz(ord.getShippingCost()).add(share))).eq(EcomOrder::getId, ord.getId()));
                EcomOrder fresh = orderMapper.selectById(ord.getId());
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getProfit, EcomOrderService.computeProfit(fresh))
                        .eq(EcomOrder::getId, ord.getId()));
                allocs.add(orderAlloc(fee, ord, share));
            }
            finish(fee, allocs, orders.stream().map(EcomOrder::getPlatformOrderNo).collect(Collectors.joining(",")));
            return;
        }
        if (fee.getMaterialId() != null && fee.getWarehouseId() != null) {
            List<EcomOwhFeeAlloc> allocs = skuAllocs(fee);
            if (!allocs.isEmpty()) {
                finish(fee, allocs, allocs.stream().map(x -> x.getSellerSku() == null ? "-" : x.getSellerSku()).distinct().limit(10)
                        .collect(Collectors.joining(",")));
                return;
            }
        }
        if (fee.getInboundId() != null) {
            EcomOwhInbound i = inboundMapper.selectById(fee.getInboundId());
            if (i != null && i.getSourceType() == OwhInboundSource.INBOUND) {
                EcomInbound ib = ecomInboundService.detail(i.getSourceId());
                EcomOwhFeeAlloc x = new EcomOwhFeeAlloc();
                x.setFeeId(fee.getId());
                x.setStoreId(ib.getStoreId());
                x.setItemType(itemType(fee.getFeeType()));
                x.setAllocDate(fee.getFeeDate());
                x.setAmountBase(fee.getAmountBase().negate());
                finish(fee, List.of(x), Messages.get("ecom.text.owh.postStore", Objects.toString(ib.getStoreName(), "-")));
                return;
            }
        }
        feeMapper.update(Wrappers.<EcomOwhFee>lambdaUpdate().set(EcomOwhFee::getStatus, OwhFeeStatus.UNALLOCATED).set(EcomOwhFee::getPostTarget, null)
                .eq(EcomOwhFee::getId, fee.getId()));
    }

    /**
     * 按 SKU 的费用分摊到店铺 SKU：费用当月从该仓库发出的销量为权重，尾差记到最后一个；当月没有销量时平均分到映射该物料的在线商品
     */
    private List<EcomOwhFeeAlloc> skuAllocs(EcomOwhFee fee) {
        LocalDate begin = fee.getFeeDate().withDayOfMonth(1);
        List<Map<String, Object>> basis = allocMapper.selectShippedQty(fee.getWarehouseId(), fee.getMaterialId(), begin, begin.plusMonths(1));
        if (basis.isEmpty()) {
            basis = allocMapper.selectListings(fee.getMaterialId()).stream().map(r -> {
                Map<String, Object> m = new HashMap<>(r);
                m.put("qty", BigDecimal.ONE);
                return m;
            }).toList();
        }
        List<EcomOwhFeeAlloc> out = new ArrayList<>();
        if (basis.isEmpty()) {
            return out;
        }
        BigDecimal totalQty = basis.stream().map(r -> dec(r.get("qty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = fee.getAmountBase().negate();
        BigDecimal allotted = BigDecimal.ZERO;
        for (int k = 0; k < basis.size(); k++) {
            Map<String, Object> r = basis.get(k);
            BigDecimal q = dec(r.get("qty"));
            BigDecimal share = k == basis.size() - 1 ? amount.subtract(allotted)
                    : totalQty.signum() == 0 ? BigDecimal.ZERO : Decimals.money(amount.multiply(q).divide(totalQty, 6, RoundingMode.HALF_UP));
            allotted = allotted.add(share);
            EcomOwhFeeAlloc x = new EcomOwhFeeAlloc();
            x.setFeeId(fee.getId());
            x.setStoreId(((Number) r.get("storeId")).longValue());
            x.setSellerSku((String) r.get("sellerSku"));
            x.setItemType(itemType(fee.getFeeType()));
            x.setAllocDate(fee.getFeeDate());
            x.setBasisValue(q);
            x.setAmountBase(share);
            out.add(x);
        }
        return out;
    }

    /** 利润报表的费用栏：仓租为仓储费，入库费为入库配置费，出库费用（没有关联出库单时）为配送费，其余为其他 */
    private static SettlementItemType itemType(OwhFeeType t) {
        return switch (t) {
            case STORAGE -> SettlementItemType.STORAGE;
            case INBOUND -> SettlementItemType.INBOUND_FEE;
            case OPERATION, FREIGHT -> SettlementItemType.FULFILLMENT;
            default -> SettlementItemType.OTHER;
        };
    }

    private void finish(EcomOwhFee fee, List<EcomOwhFeeAlloc> allocs, String target) {
        if (!allocs.isEmpty()) {
            allocMapper.insertBatch(allocs);
        }
        feeMapper.update(Wrappers.<EcomOwhFee>lambdaUpdate().set(EcomOwhFee::getStatus, OwhFeeStatus.POSTED)
                .set(EcomOwhFee::getPostTarget, target == null || target.length() <= 255 ? target : target.substring(0, 255))
                .set(EcomOwhFee::getPostTime, LocalDateTime.now()).eq(EcomOwhFee::getId, fee.getId()));
        fee.setStatus(OwhFeeStatus.POSTED);
    }

    private static EcomOwhFeeAlloc orderAlloc(EcomOwhFee fee, EcomOrder o, BigDecimal share) {
        EcomOwhFeeAlloc x = new EcomOwhFeeAlloc();
        x.setFeeId(fee.getId());
        x.setStoreId(o.getStoreId());
        x.setOrderId(o.getId());
        x.setItemType(SettlementItemType.FULFILLMENT);
        x.setAllocDate(fee.getFeeDate());
        x.setAmountBase(share);
        return x;
    }

    /**
     * 包裹发货时取出库单待入账的操作费、尾程运费（本位币合计）作为包裹运费，并标记为已入账（按订单金额分摊，与发货时的运费分摊一致）（内部）
     *
     * @return 合计；没有待入账费用时为 0
     */
    public BigDecimal takePendingForShipment(Long outboundId, Long packageId) {
        List<EcomOwhFee> pending = feeMapper.selectList(Wrappers.<EcomOwhFee>lambdaQuery().eq(EcomOwhFee::getOutboundId, outboundId)
                .eq(EcomOwhFee::getStatus, OwhFeeStatus.PENDING).in(EcomOwhFee::getFeeType, OwhFeeType.OPERATION, OwhFeeType.FREIGHT));
        if (pending.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<EcomOrder> orders = ordersOf(packageId);
        String target = orders.stream().map(EcomOrder::getPlatformOrderNo).collect(Collectors.joining(","));
        BigDecimal total = BigDecimal.ZERO;
        for (EcomOwhFee f : pending) {
            Map<Long, BigDecimal> shares = distribute(orders, f.getAmountBase());
            finish(f, orders.stream().map(o -> orderAlloc(f, o, shares.get(o.getId()))).toList(), target);
            total = total.add(f.getAmountBase());
        }
        return Decimals.money(total);
    }

    /** 包裹中的订单（按 ID） */
    private List<EcomOrder> ordersOf(Long packageId) {
        Set<Long> ids = packageItemMapper.selectByPackageIds(List.of(packageId)).stream().map(EcomPackageItem::getOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        return orderMapper.selectByIds(ids).stream().sorted(Comparator.comparing(EcomOrder::getId)).toList();
    }

    /** 金额按订单本位币金额分摊到各订单，最后一个取余数（与包裹发货的运费分摊一致） */
    static Map<Long, BigDecimal> distribute(List<EcomOrder> orders, BigDecimal amount) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        BigDecimal base = orders.stream().map(o -> Decimals.nz(o.getTotalBase())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allotted = BigDecimal.ZERO;
        for (int k = 0; k < orders.size(); k++) {
            EcomOrder o = orders.get(k);
            BigDecimal share;
            if (k == orders.size() - 1) {
                share = amount.subtract(allotted);
            } else {
                share = base.signum() > 0 ? Decimals.money(amount.multiply(Decimals.nz(o.getTotalBase())).divide(base, 6, RoundingMode.HALF_UP))
                        : Decimals.money(amount.divide(BigDecimal.valueOf(orders.size()), 6, RoundingMode.HALF_UP));
                allotted = allotted.add(share);
            }
            out.put(o.getId(), share);
        }
        return out;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }
}
