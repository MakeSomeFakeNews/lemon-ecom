package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoDispatcher;
import com.lemon.erp.modules.ecom.model.SyncType;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 店铺与渠道触发器：店铺授权失效（刷新令牌 / 测试授权 / 授权回调失败）、渠道同步失败（拉取订单、回传发货 / 库存、拉取商品、客服等），
 * 写入失败的同步日志后触发。防抖按「店铺 + 同步类型」判断，避免每小时的定时同步重复提醒
 */
@Component
@RequiredArgsConstructor
public class StoreTriggerSource implements AutoTriggerSource {

    private final EcomAutoScanMapper scanMapper;
    private final AutoDispatcher dispatcher;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        for (AutoTrigger t : new AutoTrigger[]{AutoTrigger.STORE_AUTH_INVALID, AutoTrigger.SYNC_FAILED}) {
            List<AutoField> f = new ArrayList<>(List.of(AutoField.store()));
            f.add(AutoField.text("storeCode", "DEMO-AMZ-US"));
            f.add(AutoField.enumOf("syncType", "ecom_sync_type", t == AutoTrigger.STORE_AUTH_INVALID ? "AUTH" : "ORDERS"));
            f.add(AutoField.text("message", "invalid_grant: The refresh token is invalid"));
            f.add(AutoField.text("triggerBy", "JOB"));
            map.put(t, f);
        }
        return map;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSyncFailed(EcomSyncService.SyncFailedEvent e) {
        AutoTrigger t = e.type() == SyncType.AUTH ? AutoTrigger.STORE_AUTH_INVALID : AutoTrigger.SYNC_FAILED;
        if (dispatcher.active(t)) {
            dispatcher.dispatch(() -> scanMapper.selectFailedSyncLogs(e.logId(), false, 1).stream().map(StoreTriggerSource::event).toList());
        }
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        return scanMapper.selectFailedSyncLogs(null, trigger == AutoTrigger.STORE_AUTH_INVALID, limit).stream().map(StoreTriggerSource::event).toList();
    }

    static AutoEvent event(Map<String, Object> g) {
        String type = AutoRows.str(g, "syncType");
        AutoTrigger t = SyncType.AUTH.name().equals(type) ? AutoTrigger.STORE_AUTH_INVALID : AutoTrigger.SYNC_FAILED;
        Long storeId = AutoRows.id(g, "storeId");
        return AutoEvent.builder(t, "STORE:" + storeId + ":" + type).label(AutoRows.str(g, "storeName"))
                .store(storeId, AutoRows.str(g, "storeName"), AutoRows.platform(g, "platform"), AutoRows.str(g, "site"))
                .field("storeCode", AutoRows.str(g, "storeCode"))
                .field("syncType", type)
                .field("message", AutoRows.str(g, "message"))
                .field("triggerBy", AutoRows.str(g, "triggerBy"))
                .link(t == AutoTrigger.STORE_AUTH_INVALID ? "/ecom/store-auth" : "/ecom/channel")
                .build();
    }
}
