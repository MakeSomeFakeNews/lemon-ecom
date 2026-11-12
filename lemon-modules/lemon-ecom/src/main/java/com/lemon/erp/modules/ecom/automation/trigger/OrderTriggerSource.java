package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoDispatcher;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
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
 * 订单触发器：新订单、订单异常、订单预估亏损。订单写入（同步 / 导入 / 手工 / 补发）的事务提交后读取订单
 * （此时审单规则、利润预估、异常检查都已完成），新订单都触发「新订单」，处理状态为异常的再触发「订单异常」，
 * 预估利润为负的再触发「订单预估亏损」
 */
@Component
@RequiredArgsConstructor
public class OrderTriggerSource implements AutoTriggerSource {

    private static final AutoTrigger[] TRIGGERS = {AutoTrigger.ORDER_NEW, AutoTrigger.ORDER_EXCEPTION, AutoTrigger.ORDER_LOSS};

    private final EcomAutoScanMapper scanMapper;
    private final AutoDispatcher dispatcher;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        List<AutoField> f = new ArrayList<>(List.of(AutoField.text("orderNo", "114-1234567-1234567")));
        f.addAll(List.of(AutoField.store()));
        f.addAll(List.of(
                AutoField.text("country", "US"),
                AutoField.text("currency", "USD"),
                AutoField.number("totalAmount", new BigDecimal("39.99")),
                AutoField.number("totalBase", new BigDecimal("286.30")),
                AutoField.number("itemQty", 2),
                AutoField.text("skus", "DM-EARBUD-BK"),
                AutoField.enumOf("fulfillment", "ecom_fulfillment", "SELLER"),
                AutoField.enumOf("orderType", "ecom_order_type", "PLATFORM"),
                AutoField.enumOf("localStatus", "ecom_local_status", "PENDING"),
                AutoField.text("exception", null),
                AutoField.number("estProfit", new BigDecimal("-12.50")),
                AutoField.number("estProfitRate", new BigDecimal("-4.37")),
                AutoField.number("weight", new BigDecimal("0.35")),
                AutoField.text("buyerName", "John Smith"),
                AutoField.text("tags", "VIP"),
                AutoField.bool("remoteArea", false),
                AutoField.bool("addressInvalid", false),
                AutoField.bool("blacklisted", false)));
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        for (AutoTrigger t : TRIGGERS) {
            map.put(t, f);
        }
        return map;
    }

    /** 订单写入的事务提交后（没有事务时立即）：有相关规则才在后台读取订单并执行 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderSaved(EcomOrderService.OrderSavedEvent e) {
        if (dispatcher.active(TRIGGERS)) {
            dispatcher.dispatch(() -> events(e.orderId()));
        }
    }

    /** 一张订单产生的事件（新订单 + 异常 / 亏损） */
    public List<AutoEvent> events(Long orderId) {
        Map<String, Object> o = scanMapper.selectOrder(orderId);
        if (o == null) {
            return List.of();
        }
        List<AutoEvent> out = new ArrayList<>();
        out.add(event(AutoTrigger.ORDER_NEW, o));
        if (EcomLocalStatus.EXCEPTION.name().equals(AutoRows.str(o, "localStatus"))) {
            out.add(event(AutoTrigger.ORDER_EXCEPTION, o));
        }
        BigDecimal profit = AutoRows.num(o, "estProfit");
        if (profit != null && profit.signum() < 0) {
            out.add(event(AutoTrigger.ORDER_LOSS, o));
        }
        return out;
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        List<AutoEvent> out = new ArrayList<>();
        for (Long id : scanMapper.selectRecentOrderIds(trigger == AutoTrigger.ORDER_EXCEPTION, trigger == AutoTrigger.ORDER_LOSS, limit)) {
            Map<String, Object> o = scanMapper.selectOrder(id);
            if (o != null) {
                out.add(event(trigger, o));
            }
        }
        return out;
    }

    private AutoEvent event(AutoTrigger trigger, Map<String, Object> o) {
        Long id = AutoRows.id(o, "id");
        List<String> skus = scanMapper.selectOrderSkus(id);
        List<String> tags = scanMapper.selectOrderTags(id);
        String no = AutoRows.str(o, "orderNo");
        return AutoEvent.builder(trigger, "ORDER:" + id).label(no)
                .store(AutoRows.id(o, "storeId"), AutoRows.str(o, "storeName"), AutoRows.platform(o, "platform"), AutoRows.str(o, "site"))
                .skus(skus).tags(tags)
                .field("orderNo", no)
                .field("country", AutoRows.str(o, "country"))
                .field("currency", AutoRows.str(o, "currency"))
                .field("totalAmount", AutoRows.num(o, "totalAmount"))
                .field("totalBase", AutoRows.num(o, "totalBase"))
                .field("itemQty", AutoRows.num(o, "itemQty"))
                .field("skus", skus)
                .field("fulfillment", AutoRows.str(o, "fulfillment"))
                .field("orderType", AutoRows.str(o, "orderType"))
                .field("localStatus", AutoRows.str(o, "localStatus"))
                .field("exception", AutoRows.str(o, "exception"))
                .field("estProfit", AutoRows.num(o, "estProfit"))
                .field("estProfitRate", AutoRows.num(o, "estProfitRate"))
                .field("weight", AutoRows.num(o, "weight"))
                .field("buyerName", AutoRows.str(o, "buyerName"))
                .field("tags", tags)
                .field("remoteArea", AutoRows.flag(o, "remoteArea"))
                .field("addressInvalid", AutoRows.flag(o, "addressInvalid"))
                .field("blacklisted", AutoRows.id(o, "blacklistId") != null)
                .ref(AutoRef.ORDER, id)
                .link("/ecom/order?localStatus=" + AutoRows.str(o, "localStatus"))
                .build();
    }
}
