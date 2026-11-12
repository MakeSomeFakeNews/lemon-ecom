package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 广告触发器（扫描活动报告）：
 * <ul>
 *   <li>ACOS 超过阈值：每天扫描启用的活动近 7 天（截至昨天）的合计，ACOS = 花费 ÷ 广告销售额 × 100（没有销售额时为空，可用「花费」条件），
 *       阈值在条件中设置；</li>
 *   <li>预算耗尽：每 15 分钟扫描启用、设置了日预算的活动最近一天（今天或昨天）的花费，花费达到日预算 80% 及以上的返回，
 *       字段「预算使用率」，条件中一般设为 ≥ 100。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AdTriggerSource implements AutoTriggerSource {

    static final int ACOS_DAYS = 7;
    private static final BigDecimal BUDGET_MIN_USAGE = BigDecimal.valueOf(80);

    private final EcomAutoScanMapper scanMapper;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        List<AutoField> acos = new ArrayList<>(List.of(AutoField.text("campaignName", "SP-Earbuds-Auto"), AutoField.enumOf("adType", "ecom_ad_type", "SP")));
        acos.addAll(List.of(AutoField.store()));
        acos.addAll(List.of(AutoField.number("acos", new BigDecimal("62.5")), AutoField.number("spend", new BigDecimal("125.00")),
                AutoField.number("sales", new BigDecimal("200.00")), AutoField.number("orders", 8), AutoField.number("clicks", 210),
                AutoField.number("impressions", 15200), AutoField.number("cpc", new BigDecimal("0.60")), AutoField.number("dailyBudget", 30),
                AutoField.text("currency", "USD"), AutoField.text("tags", "brand")));
        map.put(AutoTrigger.AD_ACOS_HIGH, acos);
        List<AutoField> budget = new ArrayList<>(List.of(AutoField.text("campaignName", "SP-Earbuds-Exact"),
                AutoField.enumOf("adType", "ecom_ad_type", "SP")));
        budget.addAll(List.of(AutoField.store()));
        budget.addAll(List.of(AutoField.number("dailyBudget", 30), AutoField.number("spend", new BigDecimal("30.20")),
                AutoField.number("usagePercent", new BigDecimal("100.67")), AutoField.text("statDate", "2026-11-12"),
                AutoField.number("sales", new BigDecimal("88.00")), AutoField.number("acos", new BigDecimal("34.32")), AutoField.text("currency", "USD"),
                AutoField.text("tags", "brand")));
        map.put(AutoTrigger.AD_BUDGET_EXHAUSTED, budget);
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
        if (trigger == AutoTrigger.AD_ACOS_HIGH) {
            for (Map<String, Object> c : scanMapper.selectCampaignMetrics(today.minusDays(ACOS_DAYS), today.minusDays(1))) {
                BigDecimal spend = AutoRows.num(c, "spend");
                BigDecimal clicks = AutoRows.num(c, "clicks");
                out.add(base(trigger, c)
                        .field("acos", AutoRows.percent(spend, AutoRows.num(c, "sales")))
                        .field("spend", spend)
                        .field("sales", AutoRows.num(c, "sales"))
                        .field("orders", AutoRows.integer(c, "orders"))
                        .field("clicks", AutoRows.integer(c, "clicks"))
                        .field("impressions", AutoRows.id(c, "impressions"))
                        .field("cpc", clicks == null || clicks.signum() == 0 ? null : spend.divide(clicks, 2, RoundingMode.HALF_UP))
                        .field("dailyBudget", AutoRows.num(c, "dailyBudget"))
                        .field("currency", AutoRows.str(c, "currency"))
                        .field("tags", AutoRows.tags(AutoRows.str(c, "tags")))
                        .build());
            }
        } else if (trigger == AutoTrigger.AD_BUDGET_EXHAUSTED) {
            for (Map<String, Object> c : scanMapper.selectCampaignBudgets(today.minusDays(1), BUDGET_MIN_USAGE)) {
                BigDecimal spend = AutoRows.num(c, "spend");
                LocalDate date = AutoRows.date(c, "statDate");
                // 不同日期分别防抖：同一活动每天的预算耗尽各提醒一次
                out.add(base(trigger, c, date)
                        .field("dailyBudget", AutoRows.num(c, "dailyBudget"))
                        .field("spend", spend)
                        .field("usagePercent", AutoRows.percent(spend, AutoRows.num(c, "dailyBudget")))
                        .field("statDate", date == null ? null : date.toString())
                        .field("sales", AutoRows.num(c, "sales"))
                        .field("acos", AutoRows.percent(spend, AutoRows.num(c, "sales")))
                        .field("currency", AutoRows.str(c, "currency"))
                        .field("tags", AutoRows.tags(AutoRows.str(c, "tags")))
                        .build());
            }
        }
        return out;
    }

    private static AutoEvent.Builder base(AutoTrigger trigger, Map<String, Object> c) {
        return base(trigger, c, null);
    }

    private static AutoEvent.Builder base(AutoTrigger trigger, Map<String, Object> c, LocalDate date) {
        Long id = AutoRows.id(c, "id");
        String name = AutoRows.str(c, "name");
        return AutoEvent.builder(trigger, "CAMPAIGN:" + id + (date == null ? "" : ":" + date)).label(name)
                .store(AutoRows.id(c, "storeId"), AutoRows.str(c, "storeName"), AutoRows.platform(c, "platform"), AutoRows.str(c, "site"))
                .tags(AutoRows.tags(AutoRows.str(c, "tags")))
                .field("campaignName", name)
                .field("adType", AutoRows.str(c, "adType"))
                .ref(AutoRef.CAMPAIGN, id)
                .link("/ecom/ad");
    }
}
