package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorInfo;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.connector.OAuthResult;
import com.lemon.erp.modules.ecom.connector.OAuthState;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.dto.SyncResult;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.mapper.EcomSyncLogMapper;
import com.lemon.erp.modules.ecom.model.SyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道同步中心：一个 ERP 管理多个平台、多个店铺——按店铺的平台找到连接器，组装上下文（授权、环境、接口地址）后调用；
 * 拉单、回传发货、授权等每次调用都写同步日志。平台接口在事务之外调用，写入订单每个店铺一个事务，单个店铺失败不影响其他店铺
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomSyncService {

    /** 第一次同步往前取的天数 */
    private static final int FIRST_SYNC_DAYS = 30;
    /** 每次同步往前重叠的小时数，避免漏单 */
    private static final int OVERLAP_HOURS = 2;
    static final String JOB = "JOB";
    static final String MANUAL = "MANUAL";

    private final EcomStoreService storeService;
    private final EcomOrderService orderService;
    private final ConnectorRegistry registry;
    private final OAuthState oauthState;
    private final EcomSyncLogMapper logMapper;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 连接器信息与授权

    /** 已有渠道接口的平台及其授权字段、能力（店铺表单使用） */
    public List<ConnectorInfo> connectors() {
        return registry.all().stream().map(ChannelConnector::info).toList();
    }

    public ChannelConnector connector(EcomStore store) {
        return registry.find(store.getPlatform())
                .orElseThrow(() -> new BizException("ecom.error.connectorUnsupported", store.getPlatform().name()));
    }

    /**
     * OAuth 授权页地址
     *
     * @param redirectUri 回调地址（{后端地址}/api/ecom/oauth/callback/{平台}），由控制器按请求或配置生成
     */
    public String authorizeUrl(Long storeId, String redirectUri) {
        EcomStore store = storeService.requireEnabled(storeId);
        ChannelConnector connector = connector(store);
        String state = oauthState.create(TenantContext.currentOrPlatform(), store.getId(), redirectUri);
        return connector.authorizeUrl(storeService.context(store), redirectUri, state);
    }

    /** OAuth 回调：按 state 进入对应租户，换取令牌并保存 */
    public EcomStore oauthCallback(String state, Map<String, String> params) {
        OAuthState.Payload p = oauthState.parse(state);
        return TenantContext.callAs(p.tenantId(), () -> {
            EcomStore store = storeService.get(p.storeId());
            LocalDateTime start = LocalDateTime.now();
            try {
                ChannelConnector connector = connector(store);
                OAuthResult result = connector.exchangeCode(storeService.context(store), params, p.redirectUri());
                Map<String, String> merged = new HashMap<>(storeService.credentials(store).values());
                merged.putAll(result.credentials());
                storeService.markAuthorized(store.getId(), merged, result.sellerId());
                writeLog(store.getId(), SyncType.AUTH, MANUAL, start, true, 0, 0, 0, null);
                return store;
            } catch (BizException e) {
                writeLog(store.getId(), SyncType.AUTH, MANUAL, start, false, 0, 0, 0, e.getMessage());
                throw e;
            }
        });
    }

    /** 测试店铺授权 */
    public void test(Long storeId) {
        EcomStore store = storeService.get(storeId);
        ChannelConnector connector = connector(store);
        ConnectorContext ctx = requireContext(store);
        connector.test(ctx);
    }

    // ---------------------------------------------------------------- 拉取订单

    /** 手工同步一个店铺 */
    public SyncResult sync(Long storeId, LocalDateTime since) {
        EcomStore store = storeService.requireEnabled(storeId);
        return syncStore(store, since, MANUAL);
    }

    /** 同步全部开启自动同步、有授权的店铺（定时任务），单个店铺失败不影响其他 */
    public List<SyncResult> syncAll() {
        List<SyncResult> results = new ArrayList<>();
        for (EcomStore store : storeService.orderSyncable()) {
            results.add(syncStore(store, null, JOB));
        }
        return results;
    }

    private SyncResult syncStore(EcomStore store, LocalDateTime since, String trigger) {
        LocalDateTime start = LocalDateTime.now();
        ChannelConnector connector = registry.find(store.getPlatform()).orElse(null);
        if (connector == null || !connector.supports(ConnectorCapability.ORDERS)) {
            return failed(store, start, trigger, Messages.get("ecom.error.connectorUnsupported", store.getPlatform().name()));
        }
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            return failed(store, start, trigger, Messages.get("ecom.error.storeCredentialsMissing", store.getName()));
        }
        LocalDateTime from = since != null ? since
                : store.getLastSyncTime() != null ? store.getLastSyncTime().minusHours(OVERLAP_HOURS) : start.minusDays(FIRST_SYNC_DAYS);
        List<OrderDraft> drafts;
        try {
            drafts = connector.fetchOrders(ctx, from);
        } catch (BizException e) {
            return failed(store, start, trigger, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Order sync failed for store {}", store.getCode(), e);
            return failed(store, start, trigger, e.getMessage());
        }
        int[] created = {0};
        int[] updated = {0};
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (OrderDraft d : drafts) {
                    if (d.getPlatformOrderNo() == null || d.getLines().isEmpty()) {
                        continue;
                    }
                    d.setStoreId(store.getId());
                    if (orderService.saveDraft(d, true)) {
                        created[0]++;
                    } else {
                        updated[0]++;
                    }
                }
                storeService.markSynced(store.getId(), start);
            });
        } catch (BizException e) {
            return failed(store, start, trigger, e.getMessage());
        }
        storeService.markSyncMessage(store.getId(), null);
        writeLog(store.getId(), SyncType.ORDERS, trigger, start, true, drafts.size(), created[0], updated[0], null);
        return new SyncResult(store.getId(), store.getName(), drafts.size(), created[0], updated[0], null);
    }

    private SyncResult failed(EcomStore store, LocalDateTime start, String trigger, String message) {
        storeService.markSyncMessage(store.getId(), message);
        writeLog(store.getId(), SyncType.ORDERS, trigger, start, false, 0, 0, 0, message);
        return SyncResult.failed(store.getId(), store.getName(), message);
    }

    // ---------------------------------------------------------------- 回传发货

    /** 发货事务提交后回传平台；失败记录日志（可在订单上重新回传或到平台后台手工录入） */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipped(EcomOrderService.OrderShippedEvent e) {
        confirmShipment(e.orderId(), JOB, false);
    }

    /**
     * 回传发货
     *
     * @param strict true 时失败抛出异常（手工重新回传），false 只记日志
     */
    public void confirmShipment(Long orderId, String trigger, boolean strict) {
        EcomOrder order = orderService.detail(orderId);
        EcomStore store = storeService.get(order.getStoreId());
        ChannelConnector connector = registry.find(store.getPlatform()).orElse(null);
        if (connector == null || !connector.supports(ConnectorCapability.SHIP_CONFIRM)) {
            if (strict) {
                throw new BizException("ecom.error.connectorNoCapability", store.getPlatform().name(), ConnectorCapability.SHIP_CONFIRM.name());
            }
            return;
        }
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            if (strict) {
                throw new BizException("ecom.error.storeCredentialsMissing", store.getName());
            }
            return;
        }
        LocalDateTime start = LocalDateTime.now();
        try {
            connector.confirmShipment(ctx, order, order.getItems());
            writeLog(store.getId(), SyncType.SHIP_CONFIRM, trigger, start, true, 1, 0, 1, order.getPlatformOrderNo());
        } catch (RuntimeException ex) {
            writeLog(store.getId(), SyncType.SHIP_CONFIRM, trigger, start, false, 1, 0, 0, order.getPlatformOrderNo() + ": " + ex.getMessage());
            if (strict) {
                throw ex;
            }
            log.warn("Shipment confirmation failed for order {} of store {}: {}", order.getPlatformOrderNo(), store.getCode(), ex.getMessage());
        }
    }

    /** 手工重新回传发货 */
    public void retryShipment(Long orderId) {
        confirmShipment(orderId, MANUAL, true);
    }

    // ---------------------------------------------------------------- 日志

    public PageResult<EcomSyncLog> logs(long current, long size, Long storeId, SyncType type, Integer success) {
        Page<EcomSyncLog> page = Page.of(current, size);
        logMapper.selectPageList(page, storeId, type, success);
        return PageResult.of(page);
    }

    /** 写同步日志（独立于业务事务，失败也要留痕） */
    public void writeLog(Long storeId, SyncType type, String trigger, LocalDateTime start, boolean success, int fetched, int created,
                         int updated, String message) {
        EcomSyncLog l = new EcomSyncLog();
        l.setStoreId(storeId);
        l.setSyncType(type);
        l.setTriggerBy(trigger);
        l.setSuccess(success ? 1 : 0);
        l.setFetched(fetched);
        l.setCreated(created);
        l.setUpdated(updated);
        l.setMessage(message == null || message.length() <= 500 ? message : message.substring(0, 500));
        l.setStartTime(start);
        l.setEndTime(LocalDateTime.now());
        logMapper.insert(l);
    }

    /** 有授权的上下文，没有授权时提示 */
    public ConnectorContext requireContext(EcomStore store) {
        ConnectorContext ctx = storeService.context(store);
        if (ctx.credentials().isEmpty()) {
            throw new BizException("ecom.error.storeCredentialsMissing", store.getName());
        }
        return ctx;
    }
}
