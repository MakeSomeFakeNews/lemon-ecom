package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FBA 触发器：索赔临近截止（每日扫描待提交、30 天内到期的索赔，字段「剩余天数」，阈值在条件中设置）
 */
@Component
@RequiredArgsConstructor
public class FbaTriggerSource implements AutoTriggerSource {

    static final int LOOKAHEAD_DAYS = 30;

    private final EcomAutoScanMapper scanMapper;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        List<AutoField> f = new ArrayList<>(List.of(AutoField.text("claimNo", "CL20261101001"),
                AutoField.enumOf("claimType", "ecom_fba_claim_type", "LOST_WAREHOUSE")));
        f.addAll(List.of(AutoField.store()));
        f.addAll(List.of(AutoField.text("sku", "DM-EARBUD-BK"), AutoField.text("fnsku", "X00DEMO123"), AutoField.number("qty", 3),
                AutoField.number("estAmount", new BigDecimal("45.60")), AutoField.text("currency", "USD"), AutoField.text("deadline", "2026-11-18"),
                AutoField.number("daysLeft", 6)));
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        map.put(AutoTrigger.FBA_CLAIM_DEADLINE, f);
        return map;
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        return scan(trigger).stream().limit(limit).toList();
    }

    @Override
    public List<AutoEvent> scan(AutoTrigger trigger) {
        LocalDate today = LocalDate.now();
        List<AutoEvent> out = new ArrayList<>();
        for (Map<String, Object> c : scanMapper.selectClaimsDue(today, today.plusDays(LOOKAHEAD_DAYS))) {
            Long id = AutoRows.id(c, "id");
            LocalDate deadline = AutoRows.date(c, "deadline");
            String sku = AutoRows.str(c, "sku");
            out.add(AutoEvent.builder(trigger, "CLAIM:" + id).label(AutoRows.str(c, "claimNo") + (sku == null ? "" : " / " + sku))
                    .store(AutoRows.id(c, "storeId"), AutoRows.str(c, "storeName"), AutoRows.platform(c, "platform"), AutoRows.str(c, "site"))
                    .skus(sku == null ? List.of() : List.of(sku))
                    .field("claimNo", AutoRows.str(c, "claimNo"))
                    .field("claimType", AutoRows.str(c, "claimType"))
                    .field("sku", sku)
                    .field("fnsku", AutoRows.str(c, "fnsku"))
                    .field("qty", AutoRows.num(c, "qty"))
                    .field("estAmount", AutoRows.num(c, "estAmount"))
                    .field("currency", AutoRows.str(c, "currency"))
                    .field("deadline", deadline == null ? null : deadline.toString())
                    .field("daysLeft", deadline == null ? null : ChronoUnit.DAYS.between(today, deadline))
                    .ref(AutoRef.CLAIM, id)
                    .link("/ecom/fba-claim")
                    .build());
        }
        return out;
    }
}
