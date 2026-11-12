package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoDispatcher;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType;
import com.lemon.erp.modules.ecom.oper.service.EcomMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 商品监控触发器：被跟卖（新增跟卖）、排名大跌、被下架，来自 ASIN 监控生成的新预警（接口取数 / 手工录入 / 导入最近几天的快照），
 * 预警写入的事务提交后触发。防抖按监控对象判断（同一个 ASIN 的同类预警）
 */
@Component
@RequiredArgsConstructor
public class MonitorTriggerSource implements AutoTriggerSource {

    private static final Map<EcomMonitorAlertType, AutoTrigger> TRIGGERS = Map.of(
            EcomMonitorAlertType.NEW_SELLER, AutoTrigger.LISTING_HIJACKED,
            EcomMonitorAlertType.RANK_DROP, AutoTrigger.LISTING_RANK_DROP,
            EcomMonitorAlertType.DELISTED, AutoTrigger.LISTING_DELISTED);

    private final EcomAutoScanMapper scanMapper;
    private final AutoDispatcher dispatcher;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        for (AutoTrigger t : TRIGGERS.values()) {
            List<AutoField> f = new ArrayList<>(List.of(
                    AutoField.text("itemId", "B0DEMO1234"),
                    AutoField.text("title", "Wireless Earbuds Bluetooth 5.3"),
                    AutoField.text("sku", "DM-EARBUD-BK"),
                    AutoField.enumOf("monitorType", "ecom_monitor_type", "OWN")));
            f.addAll(List.of(AutoField.store()));
            f.addAll(List.of(AutoField.text("oldValue", "1"), AutoField.text("newValue", "3")));
            if (t == AutoTrigger.LISTING_HIJACKED) {
                f.add(AutoField.number("newSellers", 2));
            }
            if (t == AutoTrigger.LISTING_RANK_DROP) {
                f.add(AutoField.number("rankDropPercent", new BigDecimal("50")));
            }
            f.addAll(List.of(AutoField.number("sellerCount", 3), AutoField.number("rankMain", 1500), AutoField.number("rating", new BigDecimal("4.3")),
                    AutoField.number("price", new BigDecimal("25.99")), AutoField.text("brand", "Lemon")));
            map.put(t, f);
        }
        return map;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAlerts(EcomMonitorService.AlertsCreatedEvent e) {
        if (!e.alertIds().isEmpty() && dispatcher.active(TRIGGERS.values().toArray(AutoTrigger[]::new))) {
            dispatcher.dispatch(() -> events(e.alertIds()));
        }
    }

    public List<AutoEvent> events(List<Long> alertIds) {
        List<AutoEvent> out = new ArrayList<>();
        for (Map<String, Object> a : scanMapper.selectAlerts(alertIds, null, alertIds.size())) {
            AutoTrigger t = TRIGGERS.get(EcomMonitorAlertType.valueOf(AutoRows.str(a, "alertType")));
            if (t != null) {
                out.add(event(t, a));
            }
        }
        return out;
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        EcomMonitorAlertType type = TRIGGERS.entrySet().stream().filter(x -> x.getValue() == trigger).findFirst().orElseThrow().getKey();
        return scanMapper.selectAlerts(null, List.of(type.name()), limit).stream().map(a -> event(trigger, a)).toList();
    }

    private static AutoEvent event(AutoTrigger trigger, Map<String, Object> a) {
        Long monitorId = AutoRows.id(a, "monitorId");
        String itemId = AutoRows.str(a, "itemId");
        String sku = AutoRows.str(a, "sku");
        BigDecimal oldNum = number(AutoRows.str(a, "oldValue"));
        BigDecimal newNum = number(AutoRows.str(a, "newValue"));
        AutoEvent.Builder b = AutoEvent.builder(trigger, "MONITOR:" + monitorId).label(itemId + (sku == null ? "" : " / " + sku))
                .field("itemId", itemId)
                .field("title", AutoRows.str(a, "title"))
                .field("sku", sku)
                .field("monitorType", AutoRows.str(a, "monitorType"));
        Long storeId = AutoRows.id(a, "storeId");
        if (storeId != null) {
            b.store(storeId, AutoRows.str(a, "storeName"), AutoRows.platform(a, "platform"), AutoRows.str(a, "site"));
        } else {
            b.store(null, null, AutoRows.platform(a, "monitorPlatform"), AutoRows.str(a, "marketplace"));
        }
        b.field("oldValue", AutoRows.str(a, "oldValue")).field("newValue", AutoRows.str(a, "newValue"));
        if (trigger == AutoTrigger.LISTING_HIJACKED) {
            b.field("newSellers", oldNum != null && newNum != null ? newNum.subtract(oldNum) : null);
        }
        if (trigger == AutoTrigger.LISTING_RANK_DROP) {
            b.field("rankDropPercent", oldNum != null && newNum != null ? AutoRows.percent(newNum.subtract(oldNum), oldNum) : null);
        }
        return b.field("sellerCount", AutoRows.integer(a, "sellerCount"))
                .field("rankMain", AutoRows.integer(a, "rankMain"))
                .field("rating", AutoRows.num(a, "rating"))
                .field("price", AutoRows.num(a, "price"))
                .field("brand", AutoRows.str(a, "brand"))
                .skus(sku == null ? List.of() : List.of(sku))
                .ref(AutoRef.MONITOR, monitorId)
                .ref(AutoRef.LISTING, AutoRows.id(a, "listingId"))
                .ref(AutoRef.MATERIAL, AutoRows.id(a, "materialId"))
                .link("/ecom/asin-monitor?tab=alert&monitorId=" + monitorId)
                .build();
    }

    private static BigDecimal number(String v) {
        try {
            return v == null ? null : new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
