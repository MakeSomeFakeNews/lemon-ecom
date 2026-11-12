package com.lemon.erp.modules.ecom.assistant.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.OpsReportContent;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomOpsReportMapper;
import com.lemon.erp.modules.ecom.assistant.model.OpsPushChannel;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportAiStatus;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportSource;
import com.lemon.erp.modules.ecom.assistant.model.OpsTodoKind;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportBuilder;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportService;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportSubService;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据 · 运营日报（order 180）：最近 7 天「全部店铺」与「亚马逊三站」两个范围的日报（数据由程序按演示订单、广告、退货、评价、
 * 补货建议汇总），「要点与建议」为按当天数据拼出的示例文本（标记为示例，配置 AI 后可重新生成）；以及订阅：当前用户订阅全部店铺，
 * 两个运营演示账号分别订阅亚马逊、虾皮店铺（站内消息）
 */
@Component
@RequiredArgsConstructor
public class AssistantReportDemoContributor implements DemoContributor {

    static final int DAYS = 7;
    static final String DEMO = "ecom.text.asst.demo.";

    private final EcomOpsReportService reportService;
    private final EcomOpsReportSubService subService;
    private final EcomOpsReportMapper reportMapper;
    private final EcomStoreService storeService;

    @Override
    public int order() {
        return 180;
    }

    @Override
    public String key() {
        return "asst-report";
    }

    @Override
    public void generate(DemoContext ctx) {
        List<EcomStore> stores = storeService.options().stream().filter(s -> s.getCode().startsWith(DemoKeys.STORE_CODE_PREFIX)).toList();
        if (stores.isEmpty()) {
            return;
        }
        List<EcomStore> amazon = stores.stream().filter(s -> s.getPlatform() == EcomPlatform.AMAZON).toList();
        for (int i = DAYS; i >= 1; i--) {
            LocalDate day = ctx.today().minusDays(i);
            sample(reportService.generate(day, stores, OpsReportSource.SCHEDULE, false));
            if (!amazon.isEmpty()) {
                sample(reportService.generate(day, amazon, OpsReportSource.SCHEDULE, false));
            }
        }
        Long me = SecurityUtils.getUserIdOrNull();
        if (me != null) {
            subService.save(new AssistantRequests.SubSave(me, List.of(), List.of(OpsPushChannel.SITE), EnableStatus.ENABLED, null));
        }
        List<Long> operators = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        if (operators != null && !operators.isEmpty()) {
            subService.save(new AssistantRequests.SubSave(operators.get(0), amazon.stream().map(EcomStore::getId).toList(),
                    List.of(OpsPushChannel.SITE), EnableStatus.ENABLED, null));
            if (operators.size() > 1) {
                List<Long> shopee = stores.stream().filter(s -> s.getPlatform() == EcomPlatform.SHOPEE).map(EcomStore::getId).toList();
                subService.save(new AssistantRequests.SubSave(operators.get(1), shopee, List.of(OpsPushChannel.SITE), EnableStatus.ENABLED, null));
            }
        }
    }

    /** 写入示例的「要点与建议」 */
    private void sample(EcomOpsReport report) {
        EcomOpsReport full = reportMapper.selectById(report.getId());
        String advice = sampleAdvice(reportService.content(full));
        reportMapper.update(Wrappers.<EcomOpsReport>lambdaUpdate().set(EcomOpsReport::getAiStatus, OpsReportAiStatus.SUCCESS)
                .set(EcomOpsReport::getAiAdvice, advice).set(EcomOpsReport::getAiError, null).set(EcomOpsReport::getAiSample, 1)
                .eq(EcomOpsReport::getId, report.getId()));
    }

    /** 按日报数据拼出的示例建议（默认语言） */
    static String sampleAdvice(OpsReportContent c) {
        List<String> lines = new ArrayList<>();
        lines.add(m("prefix"));
        OpsReportContent.Kpi k = c.getKpi();
        BigDecimal change = EcomOpsReportBuilder.change(k.getSales(), c.getPrev().getSales());
        lines.add(m("overview", k.getOrders(), EcomOpsReportBuilder.money(k.getSales()), change == null ? "-" : (change.signum() > 0 ? "+" : "") + change + "%",
                EcomOpsReportBuilder.money(k.getProfit()), k.getMarginPercent() == null ? "-" : k.getMarginPercent().toPlainString()));
        int n = 0;
        for (OpsReportContent.AbnormalSku a : c.getAbnormalSkus()) {
            if (n++ >= 2) {
                break;
            }
            lines.add(m("abnormal", a.getStoreName(), a.getSellerSku(), m("type." + a.getType().name()), plain(a.getCurrent()), plain(a.getBaseline())));
        }
        if (!c.getBadReviews().isEmpty()) {
            OpsReportContent.BadReview r = c.getBadReviews().get(0);
            lines.add(m("review", r.getStoreName(), r.getRating(), r.getSellerSku() == null ? "-" : r.getSellerSku()));
        }
        c.getStockRisks().stream().filter(s -> s.getLevel() == ReplenishLevel.URGENT).findFirst().or(() -> c.getStockRisks().stream().findFirst())
                .ifPresent(s -> lines.add(m("stock", s.getStoreName(), s.getSellerSku(), plain(s.getCoverDays()), plain(s.getSuggestQty()))));
        if (!c.getAdAlerts().isEmpty()) {
            OpsReportContent.AdAlert a = c.getAdAlerts().get(0);
            lines.add(m("ad", a.getCampaignName(), a.getStoreName(), m("adType." + a.getType().name()), plain(a.getSpend()), a.getCurrency()));
        }
        c.getTodos().stream().filter(t -> t.kind() == OpsTodoKind.ORDER_OVERDUE).findFirst()
                .ifPresent(t -> lines.add(m("todo", t.count())));
        if (lines.size() <= 2) {
            lines.add(m("fallback"));
        }
        return String.join("\n", lines);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String m(String key, Object... args) {
        return Messages.get(Messages.DEFAULT_LOCALE, DEMO + key, args);
    }
}
