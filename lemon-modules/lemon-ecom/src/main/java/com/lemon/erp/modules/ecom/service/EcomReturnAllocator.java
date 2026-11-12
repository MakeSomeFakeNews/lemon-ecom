package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomReturnItem;
import com.lemon.erp.modules.ecom.mapper.EcomOrderEnhanceMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 多包裹订单的退货：按包裹（发货单）分配退回的行。订单拆成多个包裹发货时每个包裹各有一张销售出库单，退货入库要引用对应出库单生成销售退货：
 * <ul>
 *   <li>退货行指定了包裹：只从该包裹的发货行退回（不能超过该包裹发出的件数）</li>
 *   <li>没有指定：按发货顺序从各包裹分配，扣除之前已入库的退货</li>
 * </ul>
 * 不经过包裹直接出库的订单只有一张出库单（订单上的出库单）
 */
@Component
@RequiredArgsConstructor
public class EcomReturnAllocator {

    private final EcomOrderEnhanceMapper enhanceMapper;
    private final EcomReturnItemMapper returnItemMapper;

    /** 一张发货单上退回的部分：退货行与件数 */
    public record Portion(EcomReturnItem item, BigDecimal qty) {
    }

    /** 发货来源：包裹的一个订单行（只计主组件行的件数） */
    record Source(Long packageId, String packageNo, Long orderItemId, Long tradeDocId, BigDecimal qty) {
    }

    /** 订单已发货包裹的发货来源，按发货顺序 */
    List<Source> sources(Long orderId) {
        List<Source> out = new ArrayList<>();
        for (Map<String, Object> row : enhanceMapper.selectShippedLines(orderId)) {
            Object main = row.get("partMain");
            if (main != null && ((Number) main).intValue() != 1) {
                continue;
            }
            out.add(new Source(((Number) row.get("packageId")).longValue(), (String) row.get("packageNo"), ((Number) row.get("orderItemId")).longValue(),
                    ((Number) row.get("tradeDocId")).longValue(), new BigDecimal(row.get("qty").toString())));
        }
        return out;
    }

    /** 新增退货时校验指定的包裹：属于该订单、已发货、包含该订单行，件数不超过该包裹发出的件数 */
    public void validate(EcomOrder order, List<ReturnRequest.Item> items) {
        List<Source> sources = null;
        for (ReturnRequest.Item ri : items) {
            if (ri.packageId() == null) {
                continue;
            }
            if (sources == null) {
                sources = sources(order.getId());
            }
            BigDecimal shipped = sources.stream().filter(s -> s.packageId().equals(ri.packageId()) && s.orderItemId().equals(ri.orderItemId()))
                    .map(Source::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (shipped.signum() <= 0) {
                throw new BizException("ecom.error.ord.returnPackageInvalid", order.getPlatformOrderNo());
            }
            if (ri.qty().compareTo(shipped) > 0) {
                throw new BizException("ecom.error.ord.returnPackageQtyExceeds", shipped.stripTrailingZeros().toPlainString());
            }
        }
    }

    /**
     * 退货入库时按发货单分组
     *
     * @return 销售出库单 ID → 退回的部分；订单没有包裹发货记录时返回 null（按订单上的出库单处理）
     */
    public Map<Long, List<Portion>> allocate(EcomOrder order, Long returnId, List<EcomReturnItem> items, Map<Long, EcomOrderItem> orderItems) {
        List<Source> sources = sources(order.getId());
        if (sources.isEmpty()) {
            return null;
        }
        // 之前已入库的退货：指定了包裹的扣该包裹，没有指定的按发货顺序扣
        Map<String, BigDecimal> used = new HashMap<>();
        List<Object[]> unassigned = new ArrayList<>();
        for (Map<String, Object> row : returnItemMapper.selectReceivedQty(order.getId(), returnId)) {
            Long itemId = ((Number) row.get("orderItemId")).longValue();
            BigDecimal q = new BigDecimal(row.get("qty").toString());
            Object pkg = row.get("packageId");
            if (pkg == null) {
                unassigned.add(new Object[]{itemId, q});
            } else {
                used.merge(itemId + "|" + ((Number) pkg).longValue(), q, BigDecimal::add);
            }
        }
        for (Object[] u : unassigned) {
            take(sources, used, (Long) u[0], null, (BigDecimal) u[1], null);
        }
        Map<Long, List<Portion>> out = new LinkedHashMap<>();
        for (EcomReturnItem it : items) {
            BigDecimal rest = take(sources, used, it.getOrderItemId(), it.getPackageId(), it.getQty(), (docId, q) ->
                    out.computeIfAbsent(docId, k -> new ArrayList<>()).add(new Portion(it, q)));
            if (rest.signum() > 0) {
                EcomOrderItem oi = orderItems.get(it.getOrderItemId());
                throw new BizException("ecom.error.returnLineNotInOutbound", oi == null ? it.getSellerSku() : oi.getSellerSku());
            }
        }
        return out;
    }

    /** 从发货来源中依次取件数，返回没有取到的件数 */
    private static BigDecimal take(List<Source> sources, Map<String, BigDecimal> used, Long itemId, Long packageId, BigDecimal qty,
                                   java.util.function.BiConsumer<Long, BigDecimal> sink) {
        BigDecimal rest = qty;
        for (Source s : sources) {
            if (rest.signum() <= 0) {
                break;
            }
            if (!s.orderItemId().equals(itemId) || packageId != null && !Objects.equals(packageId, s.packageId())) {
                continue;
            }
            String k = itemId + "|" + s.packageId();
            BigDecimal avail = s.qty().subtract(Decimals.nz(used.get(k)));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal q = rest.min(avail);
            used.merge(k, q, BigDecimal::add);
            if (sink != null) {
                sink.accept(s.tradeDocId(), q);
            }
            rest = rest.subtract(q);
        }
        return rest;
    }
}
