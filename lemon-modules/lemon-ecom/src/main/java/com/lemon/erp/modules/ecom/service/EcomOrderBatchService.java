package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 批量指定发货仓 / 物流渠道（自发货、还没有打单的订单）：
 * <ul>
 *   <li>待审核、异常、缺货、挂起（挂起前未打单）的订单：写到订单上，审核时按它分配包裹</li>
 *   <li>待打单的订单：物流渠道同时改到它的待打单包裹；发货仓变化时取消原来的待打单包裹，按新仓库重新检查库存生成包裹（不够为缺货）</li>
 * </ul>
 * 改完后重新预估利润（运费按新渠道）。逐张订单一个事务，返回 { 平台订单号: 原因 }
 */
@Service
@RequiredArgsConstructor
public class EcomOrderBatchService {

    private static final Set<EcomLocalStatus> ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK,
            EcomLocalStatus.AUDITED, EcomLocalStatus.ON_HOLD);
    private static final Set<EcomLocalStatus> HOLD_ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK);

    private final EcomOrderService orderService;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final EcomPackageService packageService;
    private final EcomLogisticsService logisticsService;
    private final WmsWarehouseService warehouseService;
    private final EcomOutboundSupport outboundSupport;
    private final EcomOrderCheckService checkService;
    private final TransactionTemplate transactionTemplate;
    private final EcomOrderLogService logService;

    public Map<String, String> assign(List<Long> ids, Long warehouseId, Long channelId) {
        if (warehouseId == null && channelId == null) {
            throw new BizException("ecom.error.ord.assignNothing");
        }
        List<String> names = new ArrayList<>();
        if (warehouseId != null) {
            names.add(EcomOrderLogService.text("ecom.log.assignWarehouse", warehouseService.get(warehouseId).getName()));
        }
        if (channelId != null) {
            names.add(EcomOrderLogService.text("ecom.log.assignChannel", logisticsService.get(channelId).getName()));
        }
        String logText = String.join("; ", names);
        Map<String, String> out = new LinkedHashMap<>();
        for (Long id : ids.stream().distinct().toList()) {
            try {
                String msg = transactionTemplate.execute(s -> {
                    String shortage = assignOne(id, warehouseId, channelId);
                    logService.log(id, EcomOrderLogAction.ASSIGN, logText);
                    if (shortage != null) {
                        logService.log(id, EcomOrderLogAction.OUT_OF_STOCK, shortage);
                    }
                    return shortage;
                });
                if (msg != null) {
                    out.put(orderNo(id), msg);
                }
            } catch (BizException e) {
                out.put(orderNo(id), e.getMessage());
            }
        }
        return out;
    }

    /** @return 改仓后缺货的说明，否则 null */
    private String assignOne(Long id, Long warehouseId, Long channelId) {
        EcomOrder o = orderService.require(id);
        if (o.getFulfillment() != FulfillmentType.SELLER || !ALLOWED.contains(o.getLocalStatus())
                || o.getLocalStatus() == EcomLocalStatus.ON_HOLD && !HOLD_ALLOWED.contains(o.getHoldPrevStatus())) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        List<EcomPackage> active = packageMapper.selectByOrderId(id).stream().filter(p -> p.getStatus() != EcomPackageStatus.CANCELLED).toList();
        if (active.stream().anyMatch(p -> p.getStatus() != EcomPackageStatus.WAIT_LABEL)) {
            throw new BizException("ecom.error.ops.orderHasLabeledPackage", o.getPlatformOrderNo());
        }
        var u = Wrappers.<EcomOrder>lambdaUpdate().eq(EcomOrder::getId, id);
        if (warehouseId != null) {
            u.set(EcomOrder::getWarehouseId, warehouseId);
        }
        if (channelId != null) {
            u.set(EcomOrder::getChannelId, channelId);
            for (EcomPackage p : active) {
                packageMapper.update(Wrappers.<EcomPackage>lambdaUpdate().set(EcomPackage::getChannelId, channelId).eq(EcomPackage::getId, p.getId()));
            }
        }
        orderMapper.update(u);
        String shortage = null;
        boolean moved = warehouseId != null && active.stream().anyMatch(p -> !Objects.equals(p.getWarehouseId(), warehouseId));
        if (moved) {
            for (EcomPackage p : active) {
                boolean merged = packageItemMapper.selectByPackageIds(List.of(p.getId())).stream().map(EcomPackageItem::getOrderId).anyMatch(x -> !x.equals(id));
                if (merged) {
                    throw new BizException("ecom.error.ops.splitMergedPackage", p.getPackageNo());
                }
            }
            packageService.releaseOrder(id);
            EcomOrder fresh = orderMapper.selectById(id);
            shortage = packageService.allocateRemainder(fresh, warehouseId);
            if (shortage != null) {
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, EcomLocalStatus.OUT_OF_STOCK)
                        .set(EcomOrder::getException, shortage.length() <= 255 ? shortage : shortage.substring(0, 252) + "...").eq(EcomOrder::getId, id));
            } else {
                packageService.recomputeOrder(id);
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getAuditTime, LocalDateTime.now()).eq(EcomOrder::getId, id));
            }
        }
        EcomOrder fresh = orderMapper.selectById(id);
        checkService.check(fresh, outboundSupport.attachParts(itemMapper.selectByOrderId(id)), false);
        return shortage;
    }

    private String orderNo(Long id) {
        EcomOrder o = orderMapper.selectById(id);
        return o == null ? String.valueOf(id) : o.getPlatformOrderNo();
    }
}
