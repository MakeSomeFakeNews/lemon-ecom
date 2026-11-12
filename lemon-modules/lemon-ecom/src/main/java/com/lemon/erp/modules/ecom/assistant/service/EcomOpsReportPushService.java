package com.lemon.erp.modules.ecom.assistant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.message.ChannelMessageSender;
import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.LoginUserLoader;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportSub;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomOpsReportMapper;
import com.lemon.erp.modules.ecom.assistant.model.OpsPushChannel;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportSource;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.oper.service.EcomStoreMemberService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运营日报的定时生成与推送：每天按「全部店铺」生成一份，再按订阅人的店铺范围（与订阅人可见的店铺取交集）分组生成，
 * 推送摘要和详情链接到订阅人选择的渠道（站内消息 / 钉钉 / 企业微信 / 飞书，经系统的 {@link ChannelMessageSender}；
 * 没有系统模块时只发站内消息）。推送失败不影响日报
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomOpsReportPushService {

    private final EcomOpsReportService reportService;
    private final EcomOpsReportSubService subService;
    private final EcomOpsReportMapper reportMapper;
    private final EcomStoreService storeService;
    private final EcomStoreMemberService memberService;
    private final ObjectProvider<LoginUserLoader> loginUserLoader;
    private final ObjectProvider<ChannelMessageSender> channelSender;
    private final ObjectProvider<MessageSender> messageSender;
    private final TransactionTemplate transactionTemplate;

    /** 一次定时执行的结果 */
    public record RunResult(int reports, int pushed, int skipped) {
    }

    /**
     * 定时任务：生成某天（一般为昨天）的日报并推送给订阅人
     */
    public RunResult runScheduled(LocalDate day) {
        List<EcomStore> all = EcomStoreScope.unrestricted(() -> storeService.options());
        if (all.isEmpty()) {
            return new RunResult(0, 0, 0);
        }
        Map<String, EcomOpsReport> reports = new LinkedHashMap<>();
        EcomOpsReport allReport = reportService.generate(day, all, OpsReportSource.SCHEDULE, true);
        reports.put(allReport.getScopeKey(), allReport);
        int pushed = 0;
        int skipped = 0;
        for (EcomOpsReportSub sub : subService.enabled()) {
            try {
                List<EcomStore> stores = resolveStores(sub, all);
                if (stores.isEmpty()) {
                    skipped++;
                    continue;
                }
                String key = reportService.scopeKey(stores.stream().map(EcomStore::getId).toList());
                EcomOpsReport report = reports.get(key);
                if (report == null) {
                    report = reportService.generate(day, stores, OpsReportSource.SCHEDULE, true);
                    reports.put(key, report);
                }
                push(report, sub, true);
                pushed++;
            } catch (RuntimeException e) {
                log.warn("运营日报推送失败（订阅 {}）：{}", sub.getId(), e.getMessage()); // i18n-ignore 日志
                skipped++;
            }
        }
        return new RunResult(reports.size(), pushed, skipped);
    }

    /**
     * 手动推送：发给店铺范围与日报一致的订阅人，返回推送人数
     */
    public int pushToSubscribers(Long reportId) {
        EcomOpsReport report = reportService.get(reportId);
        return EcomStoreScope.unrestricted(() -> {
            List<EcomStore> all = storeService.options();
            int n = 0;
            for (EcomOpsReportSub sub : subService.enabled()) {
                List<EcomStore> stores = resolveStores(sub, all);
                if (!stores.isEmpty() && report.getScopeKey().equals(reportService.scopeKey(stores.stream().map(EcomStore::getId).toList()))) {
                    push(report, sub, false);
                    n++;
                }
            }
            return n;
        });
    }

    /**
     * 订阅人的店铺范围：订阅指定的店铺（为空时全部启用店铺）与订阅人可见店铺的交集；订阅人不存在或已停用时为空
     */
    List<EcomStore> resolveStores(EcomOpsReportSub sub, List<EcomStore> all) {
        Set<Long> chosen = new HashSet<>(EcomOpsReportSubService.storeIds(sub));
        Set<Long> visible = visibleFor(sub.getUserId());
        List<EcomStore> out = new ArrayList<>();
        for (EcomStore s : all) {
            if ((chosen.isEmpty() || chosen.contains(s.getId())) && (visible == null || visible.contains(s.getId()))) {
                out.add(s);
            }
        }
        return out;
    }

    /** 用户可见的店铺：不受限制时为 null；用户不存在或停用时为空集合 */
    private Set<Long> visibleFor(Long userId) {
        LoginUserLoader loader = loginUserLoader.getIfAvailable();
        LoginUser user = loader == null ? null : loader.loadLoginUser(userId);
        if (user == null) {
            return Set.of();
        }
        if (EcomStoreMemberService.unrestrictedUser(user)) {
            return null;
        }
        return memberService.ownedOrGrantedStoreIds(userId);
    }

    /**
     * 推送摘要与详情链接
     *
     * @param dedup 定时推送时同一份日报对同一订阅人只发一次（手动推送不去重）
     */
    void push(EcomOpsReport report, EcomOpsReportSub sub, boolean dedup) {
        // 每个订阅人一个事务：站内消息、订阅与日报的推送记录一起提交，第三方平台在提交后异步推送
        transactionTemplate.executeWithoutResult(s -> doPush(report, sub, dedup));
    }

    private void doPush(EcomOpsReport report, EcomOpsReportSub sub, boolean dedup) {
        Set<OpsPushChannel> channels = EcomOpsReportSubService.channels(sub);
        String title = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.asst.pushTitle", report.getReportDate().toString(),
                report.getScopeName() == null ? "" : report.getScopeName());
        MessageSender.Message message = MessageSender.Message.of(MessageCategory.REMIND, title.length() > 120 ? title.substring(0, 120) : title,
                report.getSummary(), "/ecom/ops-report?id=" + report.getId());
        if (dedup) {
            message = message.dedup("ecom-ops-report:" + report.getId());
        }
        Set<ImChannel> im = EnumSet.noneOf(ImChannel.class);
        channels.stream().filter(c -> c.im() != null).forEach(c -> im.add(c.im()));
        boolean site = channels.contains(OpsPushChannel.SITE);
        ChannelMessageSender sender = channelSender.getIfAvailable();
        if (sender != null) {
            sender.send(List.of(sub.getUserId()), message, site, im);
        } else if (site && messageSender.getIfAvailable() != null) {
            messageSender.getObject().send(List.of(sub.getUserId()), message);
        }
        subService.markPushed(sub, report.getId());
        int count = (report.getPushCount() == null ? 0 : report.getPushCount()) + 1;
        report.setPushCount(count);
        reportMapper.update(Wrappers.<EcomOpsReport>lambdaUpdate().set(EcomOpsReport::getPushCount, count)
                .set(EcomOpsReport::getPushedTime, LocalDateTime.now()).eq(EcomOpsReport::getId, report.getId()));
    }
}
