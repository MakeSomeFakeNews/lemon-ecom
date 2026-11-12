package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.file.FileStorageService;
import com.lemon.erp.modules.ecom.carrier.CarrierConnector;
import com.lemon.erp.modules.ecom.carrier.CarrierContext;
import com.lemon.erp.modules.ecom.carrier.CarrierHttp;
import com.lemon.erp.modules.ecom.carrier.CarrierShipment;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackEvent;
import com.lemon.erp.modules.ecom.carrier.CarrierRegistry;
import com.lemon.erp.modules.ecom.carrier.CarrierWaybill;
import com.lemon.erp.modules.ecom.dto.LabelRequest;
import com.lemon.erp.modules.ecom.dto.PackageQuery;
import com.lemon.erp.modules.ecom.dto.PackageUpdateRequest;
import com.lemon.erp.modules.ecom.dto.PickList;
import com.lemon.erp.modules.ecom.dto.SplitRequest;
import com.lemon.erp.modules.ecom.dto.TrackingRequest;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 包裹：一个仓库、一个物流渠道、一个运单号发出的一票货，包含一个或多个平台订单（合并）的全部或部分行（拆分）。
 * <p>
 * 流转：审核通过生成包裹（待打单）→ 申请运单号（对接了物流商的渠道在线下单并保存面单 PDF；没有对接的手工填写运单号）→ 待发货
 * → 发货（逐个订单按包裹中的行生成销售出库、取出库成本，运费按订单金额分摊到物流成本，回传平台）→ 已发货。未发货的包裹可取消，
 * 已在物流商下单的同时取消物流商订单，包裹中的订单没有其他包裹时回到待审核。订单的处理状态由其包裹汇总（{@link #recomputeOrder}）。
 * <p>
 * 物流商接口在事务之外调用；批量申请运单号、批量发货逐个包裹处理，每个包裹一个事务，一个失败不影响其他。
 * 以「内部」标注的方法不开启事务，由调用方（审单、订单服务）在自己的事务中调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomPackageService {

    /** 未取消的包裹 */
    private static final Set<EcomPackageStatus> ACTIVE = EnumSet.of(EcomPackageStatus.WAIT_LABEL, EcomPackageStatus.LABELED, EcomPackageStatus.SHIPPED);
    /** 包裹可以发货时订单的处理状态 */
    private static final Set<EcomLocalStatus> SHIPPABLE = EnumSet.of(EcomLocalStatus.AUDITED, EcomLocalStatus.LABELED, EcomLocalStatus.PARTIAL);
    /** 面单在附件目录下的子目录 */
    private static final String LABEL_DIR = "ecom-label";
    /** 申报英文品名的最大长度 */
    private static final int NAME_MAX = 60;
    private static final int ERROR_MAX = 500;
    private static final BigDecimal MIN_UNIT_WEIGHT = new BigDecimal("0.001");

    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper orderItemMapper;
    private final EcomStoreService storeService;
    private final EcomLogisticsService logisticsService;
    private final CarrierRegistry carrierRegistry;
    private final EcomOutboundSupport outboundSupport;
    private final StockReservationService reservationService;
    private final DocNoService docNoService;
    private final FileStorageService fileStorageService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomPackage> page(long current, long size, PackageQuery query) {
        Page<EcomPackage> page = Page.of(current, size);
        packageMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    /** 各状态的包裹数（状态标签页），条件与列表相同、不含状态 */
    public Map<String, Long> statusCounts(PackageQuery query) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : packageMapper.countByStatus(query)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomPackage detail(Long id) {
        EcomPackage p = require(id);
        p.setItems(packageItemMapper.selectByPackageIds(List.of(id)));
        p.setLabelFile(null);
        return p;
    }

    private EcomPackage require(Long id) {
        EcomPackage p = id == null ? null : packageMapper.selectDetail(id);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        return p;
    }

    // ---------------------------------------------------------------- 内部：订单与包裹（调用方的事务中执行）

    /** 订单是否有未取消的包裹（内部） */
    public boolean hasActive(Long orderId) {
        return activeOf(orderId).stream().findAny().isPresent();
    }

    /** 订单是否有已打单未发货的包裹（内部） */
    public boolean hasLabeled(Long orderId) {
        return activeOf(orderId).stream().anyMatch(p -> p.getStatus() == EcomPackageStatus.LABELED);
    }

    private List<EcomPackage> activeOf(Long orderId) {
        return packageMapper.selectByOrderId(orderId).stream().filter(p -> ACTIVE.contains(p.getStatus())).toList();
    }

    /** 订单各行已分配到未取消包裹的件数 */
    private Map<Long, BigDecimal> allocated(Long orderId) {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (Map<String, Object> row : packageItemMapper.selectAllocated(orderId)) {
            out.put(((Number) row.get("orderItemId")).longValue(), Decimals.nz(dec(row.get("qty"))));
        }
        return out;
    }

    /**
     * 库存检查：仓库空闲库存（可用 − 已预留）再减去其他未发货包裹占用的数量，不够时返回各物料的缺口说明
     *
     * @param need       物料 → 需要的数量
     * @param names      物料 → 显示名称
     * @param excludeIds 不计占用的包裹
     */
    public List<String> shortages(Long warehouseId, Map<Long, BigDecimal> need, Map<Long, String> names, Collection<Long> excludeIds) {
        if (need.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> free = reservationService.free(warehouseId, need.keySet());
        Map<Long, BigDecimal> reserved = new HashMap<>();
        for (Map<String, Object> row : packageMapper.selectReserved(warehouseId, need.keySet(), excludeIds)) {
            reserved.put(((Number) row.get("materialId")).longValue(), Decimals.nz(dec(row.get("qty"))));
        }
        List<String> out = new ArrayList<>();
        need.forEach((m, q) -> {
            BigDecimal avail = Decimals.nz(free.get(m)).subtract(Decimals.nz(reserved.get(m)));
            if (q.compareTo(avail) > 0) {
                out.add(Messages.get("ecom.text.ops.shortageItem", names.getOrDefault(m, String.valueOf(m)), Decimals.qty(q).stripTrailingZeros().toPlainString(),
                        Decimals.qty(avail.max(BigDecimal.ZERO)).stripTrailingZeros().toPlainString()));
            }
        });
        return out;
    }

    /**
     * 把订单还没有分配到包裹的件数生成一个包裹（审核、补货后重新检查库存）（内部）
     *
     * @return 库存不足时的缺口说明（不生成包裹）；生成了包裹或没有需要分配的件数时为 null
     */
    public String allocateRemainder(EcomOrder o, Long warehouseId) {
        Map<Long, BigDecimal> alloc = allocated(o.getId());
        List<EcomPackageItem> lines = new ArrayList<>();
        for (EcomOrderItem it : outboundSupport.attachParts(orderItemMapper.selectByOrderId(o.getId()))) {
            BigDecimal rest = it.getQty().subtract(Decimals.nz(alloc.get(it.getId())));
            if (rest.signum() > 0) {
                lines.addAll(line(o.getId(), it, rest));
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        List<String> short_ = shortages(warehouseId, need(lines), names(o.getId()), List.of());
        if (!short_.isEmpty()) {
            return Messages.get("ecom.text.ops.outOfStock", String.join("; ", short_));
        }
        BigDecimal weight = alloc.isEmpty() ? o.getWeight() : null;
        create(List.of(o), lines, warehouseId, o.getChannelId(), weight);
        recomputeOrder(o.getId());
        return null;
    }

    /**
     * 包裹明细（物料数量按件数折算）：单品一行；组合商品每个组件一行（件数相同，只有第一行计入订单行的已分配件数）。
     * 订单行需已带上 parts
     */
    private static List<EcomPackageItem> line(Long orderId, EcomOrderItem it, BigDecimal qty) {
        if (!EcomOutboundSupport.mapped(it)) {
            throw new BizException("ecom.error.orderSkuUnmapped", it.getSellerSku());
        }
        if (!EcomOutboundSupport.isBundle(it)) {
            EcomPackageItem pi = new EcomPackageItem();
            pi.setOrderId(orderId);
            pi.setOrderItemId(it.getId());
            pi.setMaterialId(it.getMaterialId());
            pi.setQty(Decimals.qty(qty));
            pi.setMaterialQty(qty.compareTo(it.getQty()) == 0 ? it.getMaterialQty()
                    : Decimals.qty(it.getMaterialQty().multiply(qty).divide(it.getQty(), 6, RoundingMode.HALF_UP)));
            pi.setPartMain(1);
            return List.of(pi);
        }
        List<EcomPackageItem> out = new ArrayList<>();
        for (EcomOrderItemPart part : it.getParts()) {
            EcomPackageItem pi = new EcomPackageItem();
            pi.setOrderId(orderId);
            pi.setOrderItemId(it.getId());
            pi.setMaterialId(part.getMaterialId());
            pi.setQty(Decimals.qty(qty));
            pi.setMaterialQty(qty.compareTo(it.getQty()) == 0 ? part.getMaterialQty() : Decimals.qty(part.getUnitQty().multiply(qty)));
            pi.setPartId(part.getId());
            pi.setPartMain(out.isEmpty() ? 1 : 0);
            out.add(pi);
        }
        return out;
    }

    private static Map<Long, BigDecimal> need(List<EcomPackageItem> lines) {
        Map<Long, BigDecimal> need = new LinkedHashMap<>();
        lines.forEach(l -> need.merge(l.getMaterialId(), l.getMaterialQty(), BigDecimal::add));
        return need;
    }

    /** 订单各物料的显示名称（编码 名称，含组合商品的组件） */
    private Map<Long, String> names(Long orderId) {
        Map<Long, String> names = new HashMap<>();
        for (EcomOrderItem it : outboundSupport.attachParts(orderItemMapper.selectByOrderId(orderId))) {
            if (it.getMaterialId() != null) {
                names.putIfAbsent(it.getMaterialId(), (Objects.toString(it.getMaterialCode(), "") + " " + Objects.toString(it.getMaterialName(), "")).trim());
            }
            for (EcomOrderItemPart p : it.getParts()) {
                names.putIfAbsent(p.getMaterialId(), (Objects.toString(p.getMaterialCode(), "") + " " + Objects.toString(p.getMaterialName(), "")).trim());
            }
        }
        return names;
    }

    /** 新建包裹（内部） */
    private Long create(List<EcomOrder> orders, List<EcomPackageItem> lines, Long warehouseId, Long channelId, BigDecimal weight) {
        EcomOrder first = orders.get(0);
        EcomPackage p = new EcomPackage();
        p.setPackageNo(docNoService.next(EcomOpsDocNos.PACKAGE));
        p.setStoreId(first.getStoreId());
        p.setWarehouseId(warehouseId);
        p.setChannelId(channelId);
        p.setStatus(EcomPackageStatus.WAIT_LABEL);
        p.setShipName(first.getShipName());
        p.setShipCountry(first.getShipCountry());
        p.setWeight(weight);
        p.setPrintCount(0);
        packageMapper.insert(p);
        List<EcomPackageItem> items = new ArrayList<>();
        for (EcomPackageItem l : lines) {
            EcomPackageItem pi = new EcomPackageItem();
            pi.setPackageId(p.getId());
            pi.setOrderId(l.getOrderId());
            pi.setOrderItemId(l.getOrderItemId());
            pi.setMaterialId(l.getMaterialId());
            pi.setQty(l.getQty());
            pi.setMaterialQty(l.getMaterialQty());
            pi.setPartId(l.getPartId());
            pi.setPartMain(l.getPartMain() == null ? 1 : l.getPartMain());
            items.add(pi);
        }
        packageItemMapper.insertBatch(items);
        return p.getId();
    }

    private void setStatus(Long packageId, EcomPackageStatus status) {
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getStatus, status).eq(EcomPackage::getId, packageId));
    }

    /**
     * 按包裹汇总订单的处理状态（内部）：没有包裹 → 待审核；有已发货的 → 全部件数都已发货为已发货，否则部分发货；
     * 都已打单且件数分配完 → 待发货；否则 → 待打单（还有件数没有分配时记在异常说明中）。挂起的订单只更新挂起前的状态；已关闭的不变
     */
    public EcomLocalStatus recomputeOrder(Long orderId) {
        EcomOrder o = orderMapper.selectById(orderId);
        if (o == null || o.getLocalStatus() == EcomLocalStatus.CLOSED) {
            return o == null ? null : o.getLocalStatus();
        }
        List<EcomPackage> pkgs = activeOf(orderId);
        Map<Long, BigDecimal> alloc = allocated(orderId);
        boolean uncovered = orderItemMapper.selectByOrderId(orderId).stream()
                .anyMatch(it -> it.getQty().compareTo(Decimals.nz(alloc.get(it.getId()))) > 0);
        EcomLocalStatus st;
        if (pkgs.isEmpty()) {
            st = EcomLocalStatus.PENDING;
        } else if (pkgs.stream().anyMatch(p -> p.getStatus() == EcomPackageStatus.SHIPPED)) {
            boolean allShipped = pkgs.stream().allMatch(p -> p.getStatus() == EcomPackageStatus.SHIPPED);
            st = allShipped && !uncovered ? EcomLocalStatus.SHIPPED : EcomLocalStatus.PARTIAL;
        } else if (!uncovered && pkgs.stream().allMatch(p -> p.getStatus() == EcomPackageStatus.LABELED)) {
            st = EcomLocalStatus.LABELED;
        } else {
            st = EcomLocalStatus.AUDITED;
        }
        String exception = uncovered && !pkgs.isEmpty() ? Messages.get("ecom.text.ops.partlyUnallocated") : null;
        var u = Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getException, exception).eq(EcomOrder::getId, orderId);
        if (o.getLocalStatus() == EcomLocalStatus.ON_HOLD) {
            u.set(EcomOrder::getHoldPrevStatus, st);
        } else {
            u.set(EcomOrder::getLocalStatus, st);
        }
        if (st == EcomLocalStatus.SHIPPED) {
            u.set(EcomOrder::getStatus, EcomOrderStatus.SHIPPED);
        }
        orderMapper.update(u);
        return st;
    }

    /**
     * 订单取消（本地或平台）时释放它在待打单包裹中的行（内部）：只有它一个订单的包裹取消，合并的包裹去掉它的行；
     * 已打单的由调用方先拦截（需要先取消包裹面单）
     */
    public void releaseOrder(Long orderId) {
        for (EcomPackage p : activeOf(orderId)) {
            if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                continue;
            }
            Set<Long> orders = packageItemMapper.selectByPackageIds(List.of(p.getId())).stream().map(EcomPackageItem::getOrderId).collect(Collectors.toSet());
            if (orders.size() <= 1) {
                setStatus(p.getId(), EcomPackageStatus.CANCELLED);
            } else {
                packageItemMapper.delete(Wrappers.<EcomPackageItem>lambdaQuery().eq(EcomPackageItem::getPackageId, p.getId())
                        .eq(EcomPackageItem::getOrderId, orderId));
            }
        }
    }

    // ---------------------------------------------------------------- 合并 / 拆分（内部，审单服务在事务中调用）

    /**
     * 合并：同店铺、同收件人地址、都是待打单且包裹都还没有打单的订单，原来的包裹取消，所有行合并为一个包裹（一个运单号）；
     * 原平台订单号都保留，发货时每个订单分别出库、分别回传同一运单号
     *
     * @return 新包裹 ID
     */
    public Long merge(List<EcomOrder> orders) {
        if (orders.size() < 2) {
            throw new BizException("ecom.error.ops.mergeNeedTwo");
        }
        EcomOrder first = orders.get(0);
        String address = addressKey(first);
        List<Long> oldPackages = new ArrayList<>();
        Long warehouse = null;
        Long channel = null;
        BigDecimal weight = BigDecimal.ZERO;
        boolean weightKnown = true;
        for (EcomOrder o : orders) {
            if (!Objects.equals(o.getStoreId(), first.getStoreId())) {
                throw new BizException("ecom.error.ops.mergeStoreDiffers", o.getPlatformOrderNo());
            }
            if (o.getLocalStatus() != EcomLocalStatus.AUDITED) {
                throw new BizException("ecom.error.ops.mergeStatusNotAllow", o.getPlatformOrderNo());
            }
            if (!address.equals(addressKey(o))) {
                throw new BizException("ecom.error.ops.mergeAddressDiffers", o.getPlatformOrderNo());
            }
            for (EcomPackage p : activeOf(o.getId())) {
                if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                    throw new BizException("ecom.error.ops.mergeLabeled", o.getPlatformOrderNo(), p.getPackageNo());
                }
                if (warehouse != null && !warehouse.equals(p.getWarehouseId())) {
                    throw new BizException("ecom.error.ops.mergeWarehouseDiffers", o.getPlatformOrderNo());
                }
                warehouse = p.getWarehouseId();
                if (channel == null) {
                    channel = p.getChannelId();
                }
                if (!oldPackages.contains(p.getId())) {
                    oldPackages.add(p.getId());
                    if (p.getWeight() == null) {
                        weightKnown = false;
                    } else {
                        weight = weight.add(p.getWeight());
                    }
                }
            }
        }
        if (oldPackages.isEmpty() || warehouse == null) {
            throw new BizException("ecom.error.ops.mergeStatusNotAllow", first.getPlatformOrderNo());
        }
        List<EcomPackageItem> lines = packageItemMapper.selectByPackageIds(oldPackages);
        Set<Long> covered = lines.stream().map(EcomPackageItem::getOrderId).collect(Collectors.toSet());
        for (EcomOrder o : orders) {
            if (!covered.contains(o.getId())) {
                throw new BizException("ecom.error.ops.mergeStatusNotAllow", o.getPlatformOrderNo());
            }
        }
        oldPackages.forEach(id -> setStatus(id, EcomPackageStatus.CANCELLED));
        Long id = create(orders, lines, warehouse, channel, weightKnown ? weight : null);
        orders.forEach(o -> recomputeOrder(o.getId()));
        return id;
    }

    /** 收件人地址比较：收件人、国家、州、城市、邮编、地址，忽略大小写和多余空白 */
    static String addressKey(EcomOrder o) {
        return String.join("|", norm(o.getShipName()), norm(o.getShipCountry()), norm(o.getShipState()), norm(o.getShipCity()),
                norm(o.getShipPostcode()), norm(o.getShipAddress()));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * 拆分：订单还没有打单的包裹（只能是它自己的，合并的包裹先取消）作废，按请求生成多个包裹，每个包裹检查库存；
     * 没有分配的件数留待之后（补货后「重新检查库存」生成包裹）
     */
    public List<Long> split(EcomOrder o, SplitRequest req, Long warehouseId) {
        List<Long> released = new ArrayList<>();
        for (EcomPackage p : activeOf(o.getId())) {
            if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                continue;
            }
            boolean merged = packageItemMapper.selectByPackageIds(List.of(p.getId())).stream().anyMatch(i -> !i.getOrderId().equals(o.getId()));
            if (merged) {
                throw new BizException("ecom.error.ops.splitMergedPackage", p.getPackageNo());
            }
            setStatus(p.getId(), EcomPackageStatus.CANCELLED);
            released.add(p.getId());
        }
        Map<Long, BigDecimal> alloc = allocated(o.getId());
        Map<Long, EcomOrderItem> items = outboundSupport.attachParts(orderItemMapper.selectByOrderId(o.getId())).stream()
                .collect(Collectors.toMap(EcomOrderItem::getId, i -> i, (a, b) -> a, LinkedHashMap::new));
        Map<Long, BigDecimal> remaining = new HashMap<>();
        items.values().forEach(i -> remaining.put(i.getId(), i.getQty().subtract(Decimals.nz(alloc.get(i.getId())))));
        List<List<EcomPackageItem>> groups = new ArrayList<>();
        for (List<SplitRequest.Line> g : req.packages()) {
            Map<Long, BigDecimal> merged = new LinkedHashMap<>();
            g.forEach(l -> merged.merge(l.orderItemId(), l.qty(), BigDecimal::add));
            List<EcomPackageItem> lines = new ArrayList<>();
            merged.forEach((itemId, qty) -> {
                EcomOrderItem it = items.get(itemId);
                if (it == null) {
                    throw new BizException("ecom.error.ops.splitLineInvalid", o.getPlatformOrderNo());
                }
                BigDecimal rest = remaining.get(itemId);
                if (qty.compareTo(rest) > 0) {
                    throw new BizException("ecom.error.ops.splitQtyExceeds", it.getSellerSku(), rest.stripTrailingZeros().toPlainString());
                }
                remaining.put(itemId, rest.subtract(qty));
                lines.addAll(line(o.getId(), it, qty));
            });
            groups.add(lines);
        }
        List<EcomPackageItem> all = groups.stream().flatMap(List::stream).toList();
        List<String> short_ = shortages(warehouseId, need(all), names(o.getId()), released);
        if (!short_.isEmpty()) {
            throw new BizException("ecom.error.ops.stockShort", String.join("; ", short_));
        }
        List<Long> ids = new ArrayList<>();
        for (List<EcomPackageItem> g : groups) {
            ids.add(create(List.of(o), g, warehouseId, o.getChannelId(), null));
        }
        recomputeOrder(o.getId());
        return ids;
    }

    // ---------------------------------------------------------------- 打单

    /**
     * 批量申请运单号：逐个包裹向渠道对接的物流商下单、取面单（事务之外），成功后包裹为待发货。
     * 下单成功但面单获取失败的也记为待发货，返回提示，之后可「重新获取面单」
     *
     * @return 包裹号 → 失败原因或提示
     */
    public Map<String, String> label(LabelRequest req) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            EcomPackage p = packageMapper.selectById(id);
            String no = p == null ? String.valueOf(id) : p.getPackageNo();
            try {
                String warning = labelOne(id, req.channelId(), req.weight());
                if (warning != null) {
                    out.put(no, warning);
                }
            } catch (BizException e) {
                out.put(no, e.getMessage());
                if (p != null) {
                    recordError(id, e.getMessage());
                }
            }
        }
        return out;
    }

    private String labelOne(Long id, Long channelId, BigDecimal weight) {
        EcomPackage p = require(id);
        if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
            throw new BizException("ecom.error.ops.packageStatusNotAllow", p.getPackageNo());
        }
        List<EcomPackageItem> items = packageItemMapper.selectByPackageIds(List.of(id));
        List<EcomOrder> orders = ordersOf(items);
        checkOrders(p, orders);
        Long ch = channelId != null ? channelId : p.getChannelId();
        if (ch == null) {
            throw new BizException("ecom.error.ops.packageChannelRequired", p.getPackageNo());
        }
        EcomLogisticsChannel channel = logisticsService.get(ch);
        BigDecimal w = weight != null ? weight : p.getWeight();
        if (w == null || w.signum() <= 0) {
            throw new BizException("ecom.error.ops.packageWeightRequired", p.getPackageNo());
        }
        CarrierContext ctx = logisticsService.carrierContext(channel);
        CarrierConnector connector = carrierRegistry.require(channel.getCarrier());
        CarrierWaybill wb = connector.createOrder(ctx, shipment(p, orders, items, w));
        byte[] pdf = null;
        String labelError = null;
        try {
            pdf = wb.labelUrl() != null ? CarrierHttp.download(wb.labelUrl(), connector.info().displayName())
                    : connector.label(ctx, wb);
        } catch (BizException e) {
            labelError = e.getMessage();
        }
        String file = pdf == null ? null : storeLabel(pdf);
        BigDecimal cost = p.getShippingCost() != null ? p.getShippingCost() : logisticsService.estimate(ch, p.getShipCountry(), w);
        String carrierName = StringUtils.hasText(channel.getProvider()) ? channel.getProvider().trim() : connector.info().displayName();
        String error = labelError;
        transactionTemplate.executeWithoutResult(s -> {
            EcomPackage locked = packageMapper.selectForUpdate(id);
            if (locked.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                throw new BizException("ecom.error.ops.packageStatusNotAllow", locked.getPackageNo());
            }
            packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate()
                    .set(EcomPackage::getChannelId, ch).set(EcomPackage::getCarrier, channel.getCarrier()).set(EcomPackage::getCarrierName, carrierName)
                    .set(EcomPackage::getCarrierOrderNo, wb.carrierOrderNo()).set(EcomPackage::getTrackingNo, wb.trackingNo())
                    .set(EcomPackage::getLabelFile, file).set(EcomPackage::getLabelTime, LocalDateTime.now()).set(EcomPackage::getWeight, w)
                    .set(EcomPackage::getShippingCost, cost).set(EcomPackage::getStatus, EcomPackageStatus.LABELED)
                    .set(EcomPackage::getErrorMsg, error == null ? null : abbreviate(error))
                    .eq(EcomPackage::getId, id));
            orders.forEach(o -> recomputeOrder(o.getId()));
        });
        return error == null ? null : Messages.get("ecom.text.ops.labelFetchFailed", error);
    }

    /** 已在物流商下单、面单获取失败的包裹重新获取面单 */
    public void refetchLabel(Long id) {
        EcomPackage p = require(id);
        if (p.getStatus() != EcomPackageStatus.LABELED || p.getCarrier() == null || p.getCarrierOrderNo() == null) {
            throw new BizException("ecom.error.ops.packageNoCarrierOrder", p.getPackageNo());
        }
        EcomLogisticsChannel channel = logisticsService.get(p.getChannelId());
        CarrierConnector connector = carrierRegistry.require(p.getCarrier());
        byte[] pdf = connector.label(logisticsService.carrierContext(channel), new CarrierWaybill(p.getCarrierOrderNo(), p.getTrackingNo(), null));
        String file = storeLabel(pdf);
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getLabelFile, file).set(EcomPackage::getErrorMsg, null)
                .eq(EcomPackage::getId, id));
    }

    /** 手工填写运单号（渠道没有对接物流商，在物流商后台下单后填写）：包裹为待发货 */
    @Transactional
    public void manualTracking(Long id, TrackingRequest req) {
        EcomPackage p = packageMapper.selectForUpdate(id);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        boolean online = p.getStatus() == EcomPackageStatus.LABELED && p.getCarrierOrderNo() != null;
        if (p.getStatus() != EcomPackageStatus.WAIT_LABEL && (p.getStatus() != EcomPackageStatus.LABELED || online)) {
            throw new BizException("ecom.error.ops.packageStatusNotAllow", p.getPackageNo());
        }
        List<EcomPackageItem> items = packageItemMapper.selectByPackageIds(List.of(id));
        List<EcomOrder> orders = ordersOf(items);
        checkOrders(p, orders);
        Long ch = req.channelId() != null ? req.channelId() : p.getChannelId();
        if (ch != null) {
            logisticsService.get(ch);
        }
        BigDecimal w = req.weight() != null ? req.weight() : p.getWeight();
        BigDecimal cost = req.shippingCost() != null ? req.shippingCost() : p.getShippingCost() != null ? p.getShippingCost()
                : w == null ? null : logisticsService.estimate(ch, p.getShipCountry(), w);
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate()
                .set(EcomPackage::getChannelId, ch).set(EcomPackage::getCarrier, null).set(EcomPackage::getCarrierOrderNo, null)
                .set(EcomPackage::getCarrierName, req.carrier().trim()).set(EcomPackage::getTrackingNo, req.trackingNo().trim())
                .set(EcomPackage::getLabelTime, LocalDateTime.now()).set(EcomPackage::getWeight, w).set(EcomPackage::getShippingCost, cost)
                .set(EcomPackage::getStatus, EcomPackageStatus.LABELED).set(EcomPackage::getErrorMsg, null)
                .eq(EcomPackage::getId, id));
        orders.forEach(o -> recomputeOrder(o.getId()));
    }

    /**
     * 平台分配的运单号回写到订单的包裹（渠道接入回传发货后，平台物流托管的订单由平台生成运单号）：
     * 只写未取消、还没有运单号的包裹，承运商为空时一并写入；不改变包裹状态
     *
     * @return 写入的包裹数
     */
    @Transactional
    public int fillPlatformTracking(Long orderId, String carrier, String trackingNo) {
        if (orderId == null || !StringUtils.hasText(trackingNo)) {
            return 0;
        }
        int n = 0;
        for (EcomPackage p : packageMapper.selectByOrderId(orderId)) {
            if (p.getStatus() == EcomPackageStatus.CANCELLED || StringUtils.hasText(p.getTrackingNo())) {
                continue;
            }
            packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getTrackingNo, trackingNo.trim())
                    .set(!StringUtils.hasText(p.getCarrierName()) && StringUtils.hasText(carrier), EcomPackage::getCarrierName,
                            carrier == null ? null : carrier.trim())
                    .eq(EcomPackage::getId, p.getId()));
            n++;
        }
        return n;
    }

    /** 修改包裹：渠道只能在打单前改，重量、运费、备注在发货前都可以改 */
    @Transactional
    public void update(Long id, PackageUpdateRequest req) {
        EcomPackage p = packageMapper.selectForUpdate(id);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        if (p.getStatus() != EcomPackageStatus.WAIT_LABEL && p.getStatus() != EcomPackageStatus.LABELED) {
            throw new BizException("ecom.error.ops.packageStatusNotAllow", p.getPackageNo());
        }
        Long ch = p.getChannelId();
        if (!Objects.equals(req.channelId(), p.getChannelId())) {
            if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                throw new BizException("ecom.error.ops.packageChannelLocked", p.getPackageNo());
            }
            if (req.channelId() != null) {
                logisticsService.get(req.channelId());
            }
            ch = req.channelId();
        }
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getChannelId, ch).set(EcomPackage::getWeight, req.weight())
                .set(EcomPackage::getShippingCost, req.shippingCost()).set(EcomPackage::getRemark, req.remark()).eq(EcomPackage::getId, id));
    }

    /** 物流商下单的包裹信息：收件人取第一个订单，申报明细为各行（英文品名取平台标题、中文取物料名称），单件重量按包裹重量平均 */
    private static CarrierShipment shipment(EcomPackage p, List<EcomOrder> orders, List<EcomPackageItem> items, BigDecimal weight) {
        EcomOrder o = orders.get(0);
        CarrierShipment.Receiver r = new CarrierShipment.Receiver(o.getShipName(), o.getShipPhone(), o.getBuyerEmail(), o.getShipCountry(),
                o.getShipState(), o.getShipCity(), o.getShipPostcode(), o.getShipAddress());
        // 组合商品按平台 SKU 申报（只取每个订单行的第一个组件行）
        items = items.stream().filter(i -> i.getPartMain() == null || i.getPartMain() == 1).toList();
        int totalQty = items.stream().mapToInt(i -> Math.max(1, i.getQty().setScale(0, RoundingMode.CEILING).intValue())).sum();
        BigDecimal unitWeight = totalQty <= 0 ? weight : weight.divide(BigDecimal.valueOf(totalQty), 3, RoundingMode.HALF_UP).max(MIN_UNIT_WEIGHT);
        List<CarrierShipment.Item> lines = new ArrayList<>();
        for (EcomPackageItem i : items) {
            String title = StringUtils.hasText(i.getTitle()) ? i.getTitle().trim() : i.getSellerSku();
            String nameEn = title.length() > NAME_MAX ? title.substring(0, NAME_MAX) : title;
            lines.add(new CarrierShipment.Item(i.getSellerSku(), nameEn, i.getMaterialName(), Math.max(1, i.getQty().setScale(0, RoundingMode.CEILING).intValue()),
                    Decimals.nz(i.getPrice()), unitWeight));
        }
        return new CarrierShipment(p.getPackageNo(), weight, o.getCurrency(), r, lines);
    }

    // ---------------------------------------------------------------- 面单文件与打印

    private String storeLabel(byte[] pdf) {
        String relative = LABEL_DIR + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + ".pdf";
        Path target = fileStorageService.resolveAttachment(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pdf);
        } catch (IOException e) {
            throw new BizException("ecom.error.ops.labelSaveFailed", e.getMessage());
        }
        return relative;
    }

    private byte[] readLabel(EcomPackage p) {
        if (p.getLabelFile() == null) {
            throw new BizException("ecom.error.ops.labelMissing", p.getPackageNo());
        }
        try {
            return Files.readAllBytes(fileStorageService.resolveAttachment(p.getLabelFile()));
        } catch (IOException e) {
            throw new BizException("ecom.error.ops.labelMissing", p.getPackageNo());
        }
    }

    /** 单个包裹的面单 PDF */
    public byte[] labelPdf(Long id) {
        return readLabel(require(id));
    }

    /** 批量打印面单：有面单的包裹按顺序合并为一个 PDF，打印次数加 1 */
    @Transactional
    public byte[] labelsPdf(List<Long> ids) {
        List<EcomPackage> list = packageMapper.selectDetails(ids).stream().filter(p -> p.getLabelFile() != null).toList();
        if (list.isEmpty()) {
            throw new BizException("ecom.error.ops.noLabels");
        }
        List<byte[]> files = new ArrayList<>();
        for (EcomPackage p : list) {
            files.add(readLabel(p));
        }
        byte[] merged = mergePdf(files);
        for (EcomPackage p : list) {
            packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getPrintCount, (p.getPrintCount() == null ? 0 : p.getPrintCount()) + 1)
                    .eq(EcomPackage::getId, p.getId()));
        }
        return merged;
    }

    static byte[] mergePdf(List<byte[]> files) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        try {
            PdfCopy copy = new PdfCopy(doc, out);
            doc.open();
            for (byte[] f : files) {
                PdfReader reader = new PdfReader(f);
                for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }
                copy.freeReader(reader);
                reader.close();
            }
            doc.close();
        } catch (IOException | DocumentException e) {
            throw new BizException("ecom.error.ops.labelMergeFailed", e.getMessage());
        }
        return out.toByteArray();
    }

    /** 拣货单：按仓库分组，每个仓库按库位（区域 → 拣货顺序 → 编码）汇总物料数量，没有库位的排在最后 */
    public List<PickList> pickList(List<Long> ids) {
        List<EcomPackage> packages = packageMapper.selectDetails(ids).stream().filter(p -> p.getStatus() != EcomPackageStatus.CANCELLED).toList();
        if (packages.isEmpty()) {
            throw new BizException("ecom.error.ops.selectPackages");
        }
        Map<Long, EcomPackage> byId = packages.stream().collect(Collectors.toMap(EcomPackage::getId, p -> p));
        List<EcomPackageItem> items = packageItemMapper.selectByPackageIds(byId.keySet());
        Map<Long, List<EcomPackage>> byWarehouse = packages.stream().collect(Collectors.groupingBy(EcomPackage::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        List<PickList> out = new ArrayList<>();
        byWarehouse.forEach((wh, pkgs) -> {
            Set<Long> pkgIds = pkgs.stream().map(EcomPackage::getId).collect(Collectors.toSet());
            Map<Long, List<EcomPackageItem>> byMaterial = items.stream().filter(i -> pkgIds.contains(i.getPackageId()))
                    .collect(Collectors.groupingBy(EcomPackageItem::getMaterialId, LinkedHashMap::new, Collectors.toList()));
            Map<Long, List<String>> locations = new HashMap<>();
            Map<Long, Integer> order = new HashMap<>();
            if (!byMaterial.isEmpty()) {
                int idx = 0;
                for (Map<String, Object> row : packageMapper.selectPickLocations(wh, byMaterial.keySet())) {
                    Long m = ((Number) row.get("materialId")).longValue();
                    Object code = row.get("locationCode");
                    order.putIfAbsent(m, code == null ? Integer.MAX_VALUE : idx);
                    if (code != null) {
                        locations.computeIfAbsent(m, k -> new ArrayList<>()).add(String.valueOf(code));
                    }
                    idx++;
                }
            }
            List<PickList.Row> rows = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Long, List<EcomPackageItem>> e : byMaterial.entrySet()) {
                EcomPackageItem f = e.getValue().get(0);
                BigDecimal qty = e.getValue().stream().map(EcomPackageItem::getMaterialQty).reduce(BigDecimal.ZERO, BigDecimal::add);
                total = total.add(qty);
                List<String> locs = locations.getOrDefault(e.getKey(), List.of());
                String skus = e.getValue().stream().map(EcomPackageItem::getSellerSku).filter(Objects::nonNull).distinct().collect(Collectors.joining(", "));
                int pkgCount = (int) e.getValue().stream().map(EcomPackageItem::getPackageId).distinct().count();
                rows.add(new PickList.Row(locs.isEmpty() ? null : locs.get(0), Math.max(0, locs.size() - 1), f.getMaterialCode(), f.getMaterialName(),
                        f.getMaterialSpec(), f.getUnit(), Decimals.qty(qty), skus, pkgCount));
            }
            Map<String, Long> rowMaterial = new HashMap<>();
            byMaterial.forEach((m, list) -> rowMaterial.put(list.get(0).getMaterialCode(), m));
            rows.sort(Comparator.<PickList.Row>comparingInt(r -> order.getOrDefault(rowMaterial.get(r.materialCode()), Integer.MAX_VALUE))
                    .thenComparing(PickList.Row::materialCode, Comparator.nullsLast(Comparator.naturalOrder())));
            String nos = pkgs.stream().map(EcomPackage::getPackageNo).collect(Collectors.joining(", "));
            out.add(new PickList(wh, pkgs.get(0).getWarehouseName(), pkgs.size(), Decimals.qty(total), nos, rows));
        });
        return out;
    }

    // ---------------------------------------------------------------- 发货

    /**
     * 批量发货：逐个包裹一个事务，失败的不影响其他
     *
     * @return 包裹号 → 失败原因
     */
    public Map<String, String> ship(List<Long> ids, LocalDateTime shipTime) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : ids) {
            try {
                transactionTemplate.executeWithoutResult(s -> shipOne(id, shipTime));
            } catch (BizException e) {
                EcomPackage p = packageMapper.selectById(id);
                failures.put(p == null ? String.valueOf(id) : p.getPackageNo(), e.getMessage());
            }
        }
        return failures;
    }

    /**
     * 扫描发货：按运单号（或包裹号）找到待发货的包裹并发货，返回包裹详情；已发货、未打单、订单挂起 / 已取消时提示
     */
    public EcomPackage scan(String code) {
        String c = code.trim();
        List<EcomPackage> list = packageMapper.selectByScanCode(c);
        if (list.isEmpty()) {
            throw new BizException("ecom.error.ops.scanNotFound", c);
        }
        EcomPackage p = list.stream().filter(x -> x.getStatus() == EcomPackageStatus.LABELED).findFirst().orElse(null);
        if (p == null) {
            EcomPackage first = list.get(0);
            if (first.getStatus() == EcomPackageStatus.SHIPPED) {
                throw new BizException("ecom.error.ops.scanAlreadyShipped", first.getPackageNo(),
                        first.getShipTime() == null ? "-" : first.getShipTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            throw new BizException("ecom.error.ops.scanNotLabeled", first.getPackageNo());
        }
        Long id = p.getId();
        transactionTemplate.executeWithoutResult(s -> shipOne(id, null));
        return detail(id);
    }

    /**
     * 发货一个包裹：逐个订单按包裹中的行生成销售出库（成本记入订单行和订单），运费按订单金额分摊到各订单的物流成本，
     * 订单记录运单号并按包裹汇总状态；全部发完的订单平台状态改为已发货，事务提交后回传平台（每个订单分别回传同一运单号）
     */
    private void shipOne(Long id, LocalDateTime shipTime) {
        EcomPackage p = packageMapper.selectForUpdate(id);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        if (p.getStatus() != EcomPackageStatus.LABELED || !StringUtils.hasText(p.getTrackingNo())) {
            throw new BizException(p.getStatus() == EcomPackageStatus.SHIPPED ? "ecom.error.ops.packageShipped" : "ecom.error.ops.packageNotLabeled",
                    p.getPackageNo());
        }
        List<EcomPackageItem> items = packageItemMapper.selectByPackageIds(List.of(id));
        List<EcomOrder> orders = ordersOf(items);
        checkOrders(p, orders);
        for (EcomOrder o : orders) {
            if (!SHIPPABLE.contains(o.getLocalStatus())) {
                throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
            }
        }
        EcomStore store = storeService.get(p.getStoreId());
        LocalDateTime time = shipTime == null ? LocalDateTime.now() : shipTime;
        BigDecimal freight = p.getShippingCost();
        if (freight == null) {
            BigDecimal est = p.getWeight() == null ? null : logisticsService.estimate(p.getChannelId(), p.getShipCountry(), p.getWeight());
            freight = est == null ? BigDecimal.ZERO : est;
        }
        freight = Decimals.money(freight);
        // 运费按订单金额（本位币）分摊，最后一个订单取余数
        BigDecimal baseTotal = orders.stream().map(o -> Decimals.nz(o.getTotalBase())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allotted = BigDecimal.ZERO;
        Map<Long, List<EcomPackageItem>> byOrder = items.stream().collect(Collectors.groupingBy(EcomPackageItem::getOrderId, LinkedHashMap::new, Collectors.toList()));
        for (int k = 0; k < orders.size(); k++) {
            EcomOrder o = orders.get(k);
            BigDecimal share;
            if (k == orders.size() - 1) {
                share = freight.subtract(allotted);
            } else {
                share = baseTotal.signum() > 0 ? Decimals.money(freight.multiply(Decimals.nz(o.getTotalBase())).divide(baseTotal, 6, RoundingMode.HALF_UP))
                        : Decimals.money(freight.divide(BigDecimal.valueOf(orders.size()), 6, RoundingMode.HALF_UP));
                allotted = allotted.add(share);
            }
            List<EcomPackageItem> mine = byOrder.getOrDefault(o.getId(), List.of());
            Map<Long, EcomOrderItem> orderItems = outboundSupport.attachParts(orderItemMapper.selectByOrderId(o.getId())).stream()
                    .collect(Collectors.toMap(EcomOrderItem::getId, i -> i));
            List<EcomOutboundSupport.Line> lines = new ArrayList<>();
            for (EcomPackageItem pi : mine) {
                EcomOrderItem it = orderItems.get(pi.getOrderItemId());
                if (it == null || !EcomOutboundSupport.mapped(it)) {
                    throw new BizException("ecom.error.orderSkuUnmapped", it == null ? "-" : it.getSellerSku());
                }
                // 组合商品的组件行按包裹中记录的组件出库
                EcomOrderItemPart part = pi.getPartId() == null ? null
                        : it.getParts().stream().filter(x -> x.getId().equals(pi.getPartId())).findFirst()
                        .orElseThrow(() -> new BizException("ecom.error.orderSkuUnmapped", it.getSellerSku()));
                lines.add(new EcomOutboundSupport.Line(it, pi.getQty(), pi.getMaterialQty(), part));
            }
            EcomOutboundSupport.Result r = outboundSupport.outbound(o, store, p.getWarehouseId(), time.toLocalDate(), lines,
                    Messages.get("ecom.text.ops.packageOutboundRemark", store.getName(), o.getPlatformOrderNo(), p.getPackageNo()));
            for (int i = 0; i < mine.size(); i++) {
                EcomPackageItem pi = mine.get(i);
                BigDecimal c = r.costs().get(i);
                packageItemMapper.update(Wrappers.<EcomPackageItem>lambdaUpdate().set(EcomPackageItem::getCost, c)
                        .set(EcomPackageItem::getTradeDocId, r.docId()).set(EcomPackageItem::getTradeDocNo, r.docNo()).eq(EcomPackageItem::getId, pi.getId()));
                EcomOrderItem it = orderItems.get(pi.getOrderItemId());
                it.setCost(Decimals.money(Decimals.nz(it.getCost()).add(c)));
                orderItemMapper.update(Wrappers.<EcomOrderItem>lambdaUpdate().set(EcomOrderItem::getCost, it.getCost()).eq(EcomOrderItem::getId, it.getId()));
                if (pi.getPartId() != null) {
                    outboundSupport.addPartCost(pi.getPartId(), c);
                }
            }
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate()
                    .set(EcomOrder::getCostAmount, Decimals.money(Decimals.nz(o.getCostAmount()).add(r.totalCost())))
                    .set(EcomOrder::getShippingCost, Decimals.money(Decimals.nz(o.getShippingCost()).add(share)))
                    .set(EcomOrder::getTradeDocId, o.getTradeDocId() != null ? o.getTradeDocId() : r.docId())
                    .set(EcomOrder::getTradeDocNo, o.getTradeDocNo() != null ? o.getTradeDocNo() : r.docNo())
                    .set(EcomOrder::getWarehouseId, p.getWarehouseId()).set(EcomOrder::getChannelId, p.getChannelId())
                    .set(EcomOrder::getCarrier, p.getCarrierName()).set(EcomOrder::getTrackingNo, p.getTrackingNo()).set(EcomOrder::getShipTime, time)
                    .eq(EcomOrder::getId, o.getId()));
        }
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getStatus, EcomPackageStatus.SHIPPED).set(EcomPackage::getShipTime, time)
                .set(EcomPackage::getShippingCost, freight).set(EcomPackage::getErrorMsg, null).eq(EcomPackage::getId, id));
        for (EcomOrder o : orders) {
            EcomLocalStatus st = recomputeOrder(o.getId());
            EcomOrder fresh = orderMapper.selectById(o.getId());
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getProfit, EcomOrderService.profitOf(fresh)).eq(EcomOrder::getId, o.getId()));
            if (st == EcomLocalStatus.SHIPPED) {
                eventPublisher.publishEvent(new EcomOrderService.OrderShippedEvent(o.getId()));
            }
        }
    }

    /** 包裹中的订单（按 ID） */
    private List<EcomOrder> ordersOf(List<EcomPackageItem> items) {
        Set<Long> ids = items.stream().map(EcomPackageItem::getOrderId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return List.of();
        }
        return orderMapper.selectByIds(ids).stream().sorted(Comparator.comparing(EcomOrder::getId)).toList();
    }

    /** 打单、发货前拦截：订单已挂起、已被取消 / 退款 */
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

    // ---------------------------------------------------------------- 取消、轨迹

    /**
     * 取消包裹：已在物流商下单的先取消物流商订单（事务之外，失败时不取消包裹）；包裹中的订单没有其他包裹时回到待审核
     */
    public void cancel(Long id) {
        EcomPackage p = require(id);
        if (p.getStatus() != EcomPackageStatus.WAIT_LABEL && p.getStatus() != EcomPackageStatus.LABELED) {
            throw new BizException("ecom.error.ops.packageStatusNotAllow", p.getPackageNo());
        }
        if (p.getStatus() == EcomPackageStatus.LABELED && p.getCarrier() != null && p.getCarrierOrderNo() != null) {
            EcomLogisticsChannel channel = logisticsService.get(p.getChannelId());
            carrierRegistry.require(p.getCarrier()).cancel(logisticsService.carrierContext(channel),
                    new CarrierWaybill(p.getCarrierOrderNo(), p.getTrackingNo(), null));
        }
        transactionTemplate.executeWithoutResult(s -> {
            EcomPackage locked = packageMapper.selectForUpdate(id);
            if (locked.getStatus() != EcomPackageStatus.WAIT_LABEL && locked.getStatus() != EcomPackageStatus.LABELED) {
                throw new BizException("ecom.error.ops.packageStatusNotAllow", locked.getPackageNo());
            }
            setStatus(id, EcomPackageStatus.CANCELLED);
            packageItemMapper.selectByPackageIds(List.of(id)).stream().map(EcomPackageItem::getOrderId).distinct().forEach(this::recomputeOrder);
        });
    }

    /** 物流轨迹（对接了物流商的包裹） */
    public List<CarrierTrackEvent> track(Long id) {
        EcomPackage p = require(id);
        if (p.getCarrier() == null || !StringUtils.hasText(p.getTrackingNo())) {
            throw new BizException("ecom.error.ops.packageNoCarrierOrder", p.getPackageNo());
        }
        EcomLogisticsChannel channel = logisticsService.get(p.getChannelId());
        return carrierRegistry.require(p.getCarrier()).track(logisticsService.carrierContext(channel), p.getTrackingNo());
    }

    private void recordError(Long id, String message) {
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getErrorMsg, abbreviate(message)).eq(EcomPackage::getId, id));
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= ERROR_MAX ? s : s.substring(0, ERROR_MAX);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? null : v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }
}
