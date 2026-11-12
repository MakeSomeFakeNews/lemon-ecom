package com.lemon.erp.modules.ecom.assistant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.message.ChannelMessageSender;
import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportSub;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomOpsReportSubMapper;
import com.lemon.erp.modules.ecom.assistant.model.OpsPushChannel;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运营日报订阅：每个订阅人一条，店铺范围为空时为订阅人可见的全部店铺；推送渠道为站内消息和系统「第三方集成」中已接入、
 * 开启了应用消息的平台（没有接入时只能站内消息）
 */
@Service
@RequiredArgsConstructor
public class EcomOpsReportSubService {

    private final EcomOpsReportSubMapper subMapper;
    private final EcomStoreService storeService;
    private final UserLookup userLookup;
    private final ObjectProvider<ChannelMessageSender> channelSender;

    public List<EcomOpsReportSub> list() {
        List<EcomOpsReportSub> subs = subMapper.selectListWithUser();
        Set<Long> ids = new LinkedHashSet<>();
        subs.forEach(s -> ids.addAll(storeIds(s)));
        Map<Long, String> names = EcomStoreScope.unrestricted(() -> storeService.byIds(ids)).stream()
                .collect(Collectors.toMap(EcomStore::getId, EcomStore::getName));
        for (EcomOpsReportSub s : subs) {
            s.setStoreNames(storeIds(s).stream().map(names::get).filter(Objects::nonNull).toList());
        }
        return subs;
    }

    /** 启用的订阅 */
    public List<EcomOpsReportSub> enabled() {
        return subMapper.selectList(Wrappers.<EcomOpsReportSub>lambdaQuery().eq(EcomOpsReportSub::getStatus, EnableStatus.ENABLED)
                .orderByAsc(EcomOpsReportSub::getId));
    }

    public AssistantViews.SubOptions options() {
        return new AssistantViews.SubOptions(imChannels(), EcomOpsReportJob.KEY, EcomOpsReportJob.CRON);
    }

    /** 已接入并开启应用消息的第三方平台 */
    public List<ImChannel> imChannels() {
        ChannelMessageSender s = channelSender.getIfAvailable();
        return s == null ? List.of() : s.enabledChannels();
    }

    /** 新增或修改（按订阅人） */
    public Long save(AssistantRequests.SubSave req) {
        userLookup.requireActive(req.userId(), "ecom.error.oper.userNotFound");
        Set<Long> stores = new LinkedHashSet<>(req.storeIds() == null ? List.of() : req.storeIds());
        for (Long id : stores) {
            if (!EcomStoreScope.canAccess(id)) {
                throw new BizException("ecom.error.storeNotFound");
            }
            storeService.get(id);
        }
        Set<OpsPushChannel> channels = EnumSet.copyOf(req.channels());
        List<ImChannel> available = imChannels();
        for (OpsPushChannel c : channels) {
            if (c.im() != null && !available.contains(c.im())) {
                throw new BizException("ecom.error.asst.channelUnavailable", c.name());
            }
        }
        EcomOpsReportSub sub = subMapper.selectOne(Wrappers.<EcomOpsReportSub>lambdaQuery().eq(EcomOpsReportSub::getUserId, req.userId()));
        boolean create = sub == null;
        if (create) {
            sub = new EcomOpsReportSub();
            sub.setUserId(req.userId());
            sub.setCreateBy(SecurityUtils.getUserIdOrNull());
        }
        sub.setStoreIds(stores.isEmpty() ? null : stores.stream().map(String::valueOf).collect(Collectors.joining(",")));
        sub.setChannels(channels.stream().map(Enum::name).collect(Collectors.joining(",")));
        sub.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        sub.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
        if (create) {
            subMapper.insert(sub);
        } else {
            subMapper.updateById(sub);
        }
        return sub.getId();
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            if (subMapper.selectById(id) == null) {
                throw new BizException("ecom.error.asst.subNotFound");
            }
        }
        subMapper.deleteByIds(ids);
    }

    public void markPushed(EcomOpsReportSub sub, Long reportId) {
        subMapper.update(Wrappers.<EcomOpsReportSub>lambdaUpdate().set(EcomOpsReportSub::getLastReportId, reportId)
                .set(EcomOpsReportSub::getLastPushTime, LocalDateTime.now()).eq(EcomOpsReportSub::getId, sub.getId()));
    }

    // ---------------------------------------------------------------- 解析

    public static List<Long> storeIds(EcomOpsReportSub s) {
        if (!StringUtils.hasText(s.getStoreIds())) {
            return List.of();
        }
        return Arrays.stream(s.getStoreIds().split(",")).map(String::trim).filter(StringUtils::hasText).map(Long::valueOf).toList();
    }

    public static Set<OpsPushChannel> channels(EcomOpsReportSub s) {
        Set<OpsPushChannel> out = EnumSet.noneOf(OpsPushChannel.class);
        if (StringUtils.hasText(s.getChannels())) {
            for (String c : s.getChannels().split(",")) {
                Arrays.stream(OpsPushChannel.values()).filter(v -> v.name().equals(c.trim())).findFirst().ifPresent(out::add);
            }
        }
        return out;
    }
}
