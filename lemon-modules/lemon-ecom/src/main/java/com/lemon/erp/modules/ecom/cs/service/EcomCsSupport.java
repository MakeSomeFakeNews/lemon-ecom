package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSyncLogMapper;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 客服模块的公共方法：组装调用平台接口的上下文（复用店铺授权，先由订单连接器刷新令牌）、增量水位、按订单号 / SKU 关联订单和在线商品
 */
@Component
@RequiredArgsConstructor
public class EcomCsSupport {

    private final EcomSyncService syncService;
    private final ConnectorRegistry channelRegistry;
    private final EcomSyncLogMapper syncLogMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomListingMapper listingMapper;

    /**
     * 有授权的上下文（没有授权时提示只能手工处理），平台有订单连接器时先刷新令牌（刷新后的令牌加密写回店铺）
     */
    public ConnectorContext prepare(EcomStore store) {
        ConnectorContext ctx = syncService.requireContext(store);
        channelRegistry.find(store.getPlatform()).ifPresent(c -> c.ensureToken(ctx));
        return ctx;
    }

    /** 写同步日志（复用渠道同步日志 ecom_sync_log） */
    public void log(Long storeId, SyncType type, String trigger, LocalDateTime start, boolean success, int fetched, int created, int updated,
                    String message) {
        syncService.writeLog(storeId, type, trigger, start, success, fetched, created, updated, message);
    }

    /**
     * 增量同步的起点：该类型最近一次成功同步的开始时间往前重叠 overlapMinutes 分钟；没有成功过时往前 firstDays 天
     */
    public LocalDateTime since(Long storeId, SyncType type, LocalDateTime now, int overlapMinutes, int firstDays) {
        EcomSyncLog last = Queries.first(syncLogMapper, Wrappers.<EcomSyncLog>lambdaQuery().eq(EcomSyncLog::getStoreId, storeId)
                .eq(EcomSyncLog::getSyncType, type).eq(EcomSyncLog::getSuccess, 1).orderByDesc(EcomSyncLog::getStartTime));
        return last == null ? now.minusDays(firstDays) : last.getStartTime().minusMinutes(overlapMinutes);
    }

    /** 店铺中平台订单号对应的订单，没有为 null */
    public EcomOrder findOrder(Long storeId, String platformOrderNo) {
        if (!StringUtils.hasText(platformOrderNo)) {
            return null;
        }
        return orderMapper.selectOne(Wrappers.<EcomOrder>lambdaQuery().select(EcomOrder::getId, EcomOrder::getStoreId, EcomOrder::getPlatformOrderNo,
                        EcomOrder::getBuyerName).eq(EcomOrder::getStoreId, storeId).eq(EcomOrder::getPlatformOrderNo, platformOrderNo.trim()));
    }

    /** 订单的基本信息（店铺、订单号、平台状态），没有为 null */
    public EcomOrder order(Long id) {
        return orderMapper.selectOne(Wrappers.<EcomOrder>lambdaQuery().select(EcomOrder::getId, EcomOrder::getStoreId, EcomOrder::getPlatformOrderNo,
                EcomOrder::getStatus).eq(EcomOrder::getId, id));
    }

    /** 买家在店铺中最近的一张订单（会话没有带订单号时按买家名称关联），没有为 null */
    public EcomOrder latestOrderOfBuyer(Long storeId, String buyerName) {
        if (!StringUtils.hasText(buyerName)) {
            return null;
        }
        return Queries.first(orderMapper, Wrappers.<EcomOrder>lambdaQuery().select(EcomOrder::getId, EcomOrder::getPlatformOrderNo)
                .eq(EcomOrder::getStoreId, storeId).eq(EcomOrder::getBuyerName, buyerName).orderByDesc(EcomOrder::getOrderTime));
    }

    /**
     * 在线商品：平台 SKU 优先，否则按平台商品 ID（有规格时为规格 ID，同时比对所属商品 ID），没有为 null
     */
    public EcomListing findListing(Long storeId, String sellerSku, String platformItemId, String parentItemId) {
        if (StringUtils.hasText(sellerSku)) {
            EcomListing l = listingMapper.selectOne(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, storeId)
                    .eq(EcomListing::getSellerSku, sellerSku.trim()));
            if (l != null) {
                return l;
            }
        }
        if (!StringUtils.hasText(platformItemId)) {
            return null;
        }
        return Queries.first(listingMapper, Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, storeId)
                .eq(EcomListing::getPlatformItemId, platformItemId.trim())
                .eq(StringUtils.hasText(parentItemId), EcomListing::getParentItemId, parentItemId).orderByAsc(EcomListing::getId));
    }
}
