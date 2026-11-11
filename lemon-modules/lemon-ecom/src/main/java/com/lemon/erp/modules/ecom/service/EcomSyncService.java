package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.dto.SyncResult;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 渠道同步：按店铺的授权信息从平台拉取订单写入（已存在的只更新状态），发货后向平台回传运单。
 * 拉取、写入在事务之外调用平台接口，写入每个店铺一个事务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomSyncService {

    /** 第一次同步往前取的天数 */
    private static final int FIRST_SYNC_DAYS = 30;
    /** 每次同步往前重叠的小时数，避免漏单 */
    private static final int OVERLAP_HOURS = 2;

    private final EcomStoreService storeService;
    private final EcomOrderService orderService;
    private final ConnectorRegistry registry;
    private final TransactionTemplate transactionTemplate;

    /** 手工同步一个店铺 */
    public SyncResult sync(Long storeId, LocalDateTime since) {
        EcomStore store = storeService.requireEnabled(storeId);
        return syncStore(store, since);
    }

    /** 同步全部有授权的店铺（定时任务），单个店铺失败不影响其他 */
    public List<SyncResult> syncAll() {
        List<SyncResult> results = new ArrayList<>();
        for (EcomStore store : storeService.syncable()) {
            results.add(syncStore(store, null));
        }
        return results;
    }

    private SyncResult syncStore(EcomStore store, LocalDateTime since) {
        ChannelConnector connector = connector(store).orElse(null);
        if (connector == null) {
            return SyncResult.failed(store.getId(), store.getName(), Messages.get("ecom.error.connectorUnsupported", store.getPlatform().name()));
        }
        StoreCredentials credentials = storeService.credentials(store);
        if (credentials.isEmpty()) {
            return SyncResult.failed(store.getId(), store.getName(), Messages.get("ecom.error.storeCredentialsMissing", store.getName()));
        }
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime from = since != null ? since
                : store.getLastSyncTime() != null ? store.getLastSyncTime().minusHours(OVERLAP_HOURS) : start.minusDays(FIRST_SYNC_DAYS);
        List<OrderDraft> drafts;
        try {
            drafts = connector.fetchOrders(store, credentials, from);
        } catch (BizException e) {
            return SyncResult.failed(store.getId(), store.getName(), e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Order sync failed for store {}", store.getCode(), e);
            return SyncResult.failed(store.getId(), store.getName(), e.getMessage());
        }
        int[] created = {0};
        int[] updated = {0};
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (OrderDraft d : drafts) {
                    if (d.getPlatformOrderNo() == null || d.getLines().isEmpty()) {
                        continue;
                    }
                    if (orderService.saveDraft(d, true)) {
                        created[0]++;
                    } else {
                        updated[0]++;
                    }
                }
                storeService.markSynced(store.getId(), start);
            });
        } catch (BizException e) {
            return SyncResult.failed(store.getId(), store.getName(), e.getMessage());
        }
        return new SyncResult(store.getId(), store.getName(), drafts.size(), created[0], updated[0], null);
    }

    /** 测试店铺授权 */
    public void test(Long storeId) {
        EcomStore store = storeService.get(storeId);
        ChannelConnector connector = connector(store)
                .orElseThrow(() -> new BizException("ecom.error.connectorUnsupported", store.getPlatform().name()));
        StoreCredentials credentials = storeService.credentials(store);
        if (credentials.isEmpty()) {
            throw new BizException("ecom.error.storeCredentialsMissing", store.getName());
        }
        connector.test(store, credentials);
    }

    private Optional<ChannelConnector> connector(EcomStore store) {
        return registry.find(store.getPlatform());
    }

    /** 发货事务提交后回传平台；失败只记录日志（可在平台后台手工录入） */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipped(EcomOrderService.OrderShippedEvent e) {
        EcomOrder order = orderService.detail(e.orderId());
        EcomStore store = storeService.get(order.getStoreId());
        ChannelConnector connector = connector(store).orElse(null);
        if (connector == null || !connector.supportsShipmentConfirm()) {
            return;
        }
        StoreCredentials credentials = storeService.credentials(store);
        if (credentials.isEmpty()) {
            return;
        }
        try {
            connector.confirmShipment(store, credentials, order, order.getItems());
        } catch (RuntimeException ex) {
            log.warn("Shipment confirmation failed for order {} of store {}: {}", order.getPlatformOrderNo(), store.getCode(), ex.getMessage());
        }
    }
}
