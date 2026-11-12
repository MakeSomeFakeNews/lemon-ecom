package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.ShipmentResult;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.SyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多包裹订单按包裹回传发货（部分发货）：一张订单拆成多个包裹时，每个包裹发货后（事务提交后）——
 * <ul>
 *   <li>平台支持部分发货（{@link ChannelConnector#partialShipment()}：亚马逊、eBay、Shopify、TikTok Shop、Walmart、Temu）：
 *       按包裹内的订单行和件数、包裹的运单号 / 承运商 / 发货时间回传，结果记在包裹上（ship_confirm、ship_confirm_msg），失败的可在包裹上重新回传</li>
 *   <li>不支持的平台：全部包裹发完（订单已发货）后整单回传一次（运单号为最后发出的包裹）</li>
 * </ul>
 * 订单全部包裹发完后才为已发货。手工订单、补发单不回传
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomPartialShipService {

    private static final String JOB = "JOB";
    private static final String MANUAL = "MANUAL";
    private static final int MSG_MAX = 500;

    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper orderItemMapper;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomStoreService storeService;
    private final ConnectorRegistry registry;
    private final EcomSyncService syncService;

    /**
     * 多包裹订单的一个包裹已发货
     *
     * @param allShipped 订单的全部包裹是否都已发完
     */
    public record PackageShippedEvent(Long orderId, Long packageId, boolean allShipped) {
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPackageShipped(PackageShippedEvent e) {
        try {
            handle(e.orderId(), e.packageId(), e.allShipped(), JOB, false);
        } catch (RuntimeException ex) {
            log.warn("Package shipment confirmation failed for order {} package {}: {}", e.orderId(), e.packageId(), ex.getMessage());
        }
    }

    /** 包裹上「重新回传」：支持部分发货的平台按包裹回传，失败时提示原因 */
    public void retry(Long packageId) {
        EcomPackage p = packageMapper.selectById(packageId);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        if (p.getStatus() != EcomPackageStatus.SHIPPED) {
            throw new BizException("ecom.error.ops.packageNotLabeled", p.getPackageNo());
        }
        List<Long> orderIds = packageItemMapper.selectByPackageIds(List.of(packageId)).stream().map(EcomPackageItem::getOrderId).distinct().toList();
        for (Long orderId : orderIds) {
            handle(orderId, packageId, false, MANUAL, true);
        }
    }

    private void handle(Long orderId, Long packageId, boolean allShipped, String trigger, boolean strict) {
        EcomOrder order = orderMapper.selectById(orderId);
        EcomPackage pkg = packageMapper.selectById(packageId);
        if (order == null || pkg == null) {
            return;
        }
        if (order.getOrderType() == EcomOrderType.MANUAL || order.getOrderType() == EcomOrderType.RESEND) {
            if (strict) {
                throw new BizException("ecom.error.ord.manualNoShipConfirm", order.getPlatformOrderNo());
            }
            return;
        }
        EcomStore store = storeService.get(order.getStoreId());
        ChannelConnector connector = registry.find(store.getPlatform()).orElse(null);
        boolean partial = connector != null && connector.supports(ConnectorCapability.SHIP_CONFIRM) && connector.partialShipment();
        if (!partial) {
            if (strict) {
                throw new BizException("ecom.error.ord.partialShipUnsupported", store.getPlatform().name());
            }
            mark(packageId, null, Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ord.partialWaitAll"));
            if (allShipped) {
                syncService.confirmShipment(orderId, trigger, false);
            }
            return;
        }
        // 演示店铺不回传平台（手工重新回传时提示）
        if (store.demoStore()) {
            if (strict) {
                EcomStoreService.requireReal(store);
            }
            return;
        }
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            if (strict) {
                throw new BizException("ecom.error.storeCredentialsMissing", store.getName());
            }
            mark(packageId, 0, Messages.get(Messages.DEFAULT_LOCALE, "ecom.error.storeCredentialsMissing", store.getName()));
            return;
        }
        EcomOrder copy = new EcomOrder();
        BeanUtils.copyProperties(order, copy);
        copy.setTrackingNo(pkg.getTrackingNo());
        copy.setCarrier(pkg.getCarrierName());
        copy.setShipTime(pkg.getShipTime() == null ? LocalDateTime.now() : pkg.getShipTime());
        copy.setShipPackageRef(pkg.getPackageNo());
        List<EcomOrderItem> lines = packageLines(orderId, packageId);
        LocalDateTime start = LocalDateTime.now();
        try {
            ShipmentResult r = connector.confirmShipment(ctx, copy, lines);
            String msg = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ord.partialConfirmed", order.getPlatformOrderNo(), pkg.getPackageNo(),
                    pkg.getTrackingNo(), r == null || r.message() == null ? "" : r.message());
            mark(packageId, 1, msg);
            syncService.writeLog(store.getId(), SyncType.SHIP_CONFIRM, trigger, start, true, 1, 0, 1, msg);
        } catch (RuntimeException ex) {
            String msg = order.getPlatformOrderNo() + " / " + pkg.getPackageNo() + ": " + ex.getMessage();
            mark(packageId, 0, msg);
            syncService.writeLog(store.getId(), SyncType.SHIP_CONFIRM, trigger, start, false, 1, 0, 0, msg);
            if (strict) {
                throw ex;
            }
        }
    }

    /** 包裹内属于该订单的行：订单行（平台行 ID 等）的副本，数量为包裹中的件数（组合商品只计主组件行） */
    List<EcomOrderItem> packageLines(Long orderId, Long packageId) {
        Map<Long, BigDecimal> qty = new LinkedHashMap<>();
        for (EcomPackageItem pi : packageItemMapper.selectByPackageIds(List.of(packageId))) {
            if (orderId.equals(pi.getOrderId()) && (pi.getPartMain() == null || pi.getPartMain() == 1)) {
                qty.merge(pi.getOrderItemId(), pi.getQty(), BigDecimal::add);
            }
        }
        List<EcomOrderItem> out = new ArrayList<>();
        Map<Long, EcomOrderItem> items = new LinkedHashMap<>();
        orderItemMapper.selectByOrderId(orderId).forEach(i -> items.put(i.getId(), i));
        qty.forEach((itemId, q) -> {
            EcomOrderItem src = items.get(itemId);
            if (src != null) {
                EcomOrderItem c = new EcomOrderItem();
                BeanUtils.copyProperties(src, c);
                c.setQty(q);
                out.add(c);
            }
        });
        return out;
    }

    private void mark(Long packageId, Integer ok, String msg) {
        packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getShipConfirm, ok)
                .set(EcomPackage::getShipConfirmMsg, msg == null || msg.length() <= MSG_MAX ? msg : msg.substring(0, MSG_MAX)).eq(EcomPackage::getId, packageId));
    }
}
