package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoDispatcher;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 头程触发器：货件签收（签收的事务提交后触发）、超期未签收（每日扫描已发运、预计到达日期已过的货件）
 */
@Component
@RequiredArgsConstructor
public class InboundTriggerSource implements AutoTriggerSource {

    private static final int SCAN_LIMIT = 2000;

    private final EcomAutoScanMapper scanMapper;
    private final AutoDispatcher dispatcher;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        for (AutoTrigger t : new AutoTrigger[]{AutoTrigger.INBOUND_RECEIVED, AutoTrigger.INBOUND_OVERDUE}) {
            List<AutoField> f = new ArrayList<>(List.of(AutoField.text("docNo", "IB20261101001"), AutoField.text("shipmentId", "FBA17ABCDEF")));
            f.addAll(List.of(AutoField.store()));
            f.addAll(List.of(AutoField.text("toWarehouse", "FBA US"), AutoField.text("carrier", "UPS"), AutoField.text("trackingNo", "1Z999AA10123456784"),
                    AutoField.text("shipDate", "2026-10-20"), AutoField.text("etaDate", "2026-11-05"), AutoField.number("transitDays", 22),
                    AutoField.number("overdueDays", 3), AutoField.number("totalQty", 600), AutoField.number("boxCount", 12)));
            if (t == AutoTrigger.INBOUND_RECEIVED) {
                f.add(AutoField.text("receiveDate", "2026-11-08"));
            }
            map.put(t, f);
        }
        return map;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSigned(EcomInboundService.InboundSignedEvent e) {
        if (dispatcher.active(AutoTrigger.INBOUND_RECEIVED)) {
            dispatcher.dispatch(() -> scanMapper.selectInbounds(List.of(e.inboundId()), null, false, 1).stream()
                    .map(r -> event(AutoTrigger.INBOUND_RECEIVED, r)).toList());
        }
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        return trigger == AutoTrigger.INBOUND_RECEIVED
                ? scanMapper.selectInbounds(null, null, true, limit).stream().map(r -> event(trigger, r)).toList()
                : overdue(limit);
    }

    @Override
    public List<AutoEvent> scan(AutoTrigger trigger) {
        return trigger == AutoTrigger.INBOUND_OVERDUE ? overdue(SCAN_LIMIT) : List.of();
    }

    private List<AutoEvent> overdue(int limit) {
        return scanMapper.selectInbounds(null, LocalDate.now(), false, limit).stream().map(r -> event(AutoTrigger.INBOUND_OVERDUE, r)).toList();
    }

    private static AutoEvent event(AutoTrigger trigger, Map<String, Object> r) {
        Long id = AutoRows.id(r, "id");
        LocalDate ship = AutoRows.date(r, "shipDate");
        LocalDate eta = AutoRows.date(r, "etaDate");
        LocalDate received = AutoRows.date(r, "receiveDate");
        LocalDate end = trigger == AutoTrigger.INBOUND_RECEIVED && received != null ? received : LocalDate.now();
        String docNo = AutoRows.str(r, "docNo");
        AutoEvent.Builder b = AutoEvent.builder(trigger, "INBOUND:" + id).label(docNo)
                .store(AutoRows.id(r, "storeId"), AutoRows.str(r, "storeName"), AutoRows.platform(r, "platform"), AutoRows.str(r, "site"))
                .field("docNo", docNo)
                .field("shipmentId", AutoRows.str(r, "shipmentId"))
                .field("toWarehouse", AutoRows.str(r, "toWarehouse"))
                .field("carrier", AutoRows.str(r, "carrier"))
                .field("trackingNo", AutoRows.str(r, "trackingNo"))
                .field("shipDate", ship == null ? null : ship.toString())
                .field("etaDate", eta == null ? null : eta.toString())
                .field("transitDays", ship == null ? null : ChronoUnit.DAYS.between(ship, end))
                .field("overdueDays", eta == null ? null : Math.max(0, ChronoUnit.DAYS.between(eta, end)))
                .field("totalQty", AutoRows.num(r, "totalQty"))
                .field("boxCount", AutoRows.integer(r, "boxCount"));
        if (trigger == AutoTrigger.INBOUND_RECEIVED) {
            b.field("receiveDate", received == null ? null : received.toString());
        }
        return b.ref(AutoRef.INBOUND, id).link("/ecom/inbound?keyword=" + docNo).build();
    }
}
