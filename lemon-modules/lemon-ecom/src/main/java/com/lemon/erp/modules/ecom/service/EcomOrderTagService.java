package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import com.lemon.erp.modules.ecom.entity.EcomOrderTagLink;
import com.lemon.erp.modules.ecom.mapper.EcomOrderTagLinkMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderTagMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderLogAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单标签：维护（名称唯一、颜色）、订单批量打标 / 去标、审单规则自动打标、列表与详情带出订单的标签。
 * 删除标签时一并去掉订单上的该标签；审单规则引用的标签 ID 在匹配时忽略已删除 / 停用的
 */
@Service
@RequiredArgsConstructor
public class EcomOrderTagService {

    private static final String DEFAULT_COLOR = "blue";

    private final EcomOrderTagMapper tagMapper;
    private final EcomOrderTagLinkMapper linkMapper;
    private final EcomOrderService orderService;
    private final EcomOrderLogService logService;

    public List<EcomOrderTag> list(String keyword, EnableStatus status) {
        return tagMapper.selectListWithCount(keyword, status);
    }

    /** 启用的标签（下拉选项） */
    public List<EcomOrderTag> options() {
        return tagMapper.selectList(Wrappers.<EcomOrderTag>lambdaQuery().eq(EcomOrderTag::getStatus, EnableStatus.ENABLED)
                .orderByAsc(EcomOrderTag::getSort).orderByAsc(EcomOrderTag::getId));
    }

    public EcomOrderTag get(Long id) {
        EcomOrderTag t = id == null ? null : tagMapper.selectById(id);
        if (t == null) {
            throw new BizException("ecom.error.ord.tagNotFound");
        }
        return t;
    }

    public Long create(EcomOrderTag t) {
        t.setId(null);
        normalize(t, null);
        if (t.getStatus() == null) {
            t.setStatus(EnableStatus.ENABLED);
        }
        tagMapper.insert(t);
        return t.getId();
    }

    public void update(Long id, EcomOrderTag t) {
        EcomOrderTag old = get(id);
        normalize(t, id);
        t.setId(id);
        if (t.getStatus() == null) {
            t.setStatus(old.getStatus());
        }
        tagMapper.updateById(t);
        tagMapper.update(Wrappers.<EcomOrderTag>lambdaUpdate().set(EcomOrderTag::getRemark, t.getRemark()).eq(EcomOrderTag::getId, id));
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        linkMapper.delete(Wrappers.<EcomOrderTagLink>lambdaQuery().in(EcomOrderTagLink::getTagId, ids));
        tagMapper.deleteByIds(ids);
    }

    private void normalize(EcomOrderTag t, Long excludeId) {
        t.setName(t.getName().trim());
        t.setColor(StringUtils.hasText(t.getColor()) ? t.getColor().trim() : DEFAULT_COLOR);
        if (t.getSort() == null) {
            t.setSort(0);
        }
        if (tagMapper.exists(Wrappers.<EcomOrderTag>lambdaQuery().eq(EcomOrderTag::getName, t.getName())
                .ne(excludeId != null, EcomOrderTag::getId, excludeId))) {
            throw new BizException("ecom.error.ord.tagNameExists", t.getName());
        }
    }

    // ---------------------------------------------------------------- 打标

    /** 批量打标：订单按数据权限校验，已有的标签不重复 */
    @Transactional
    public int tag(List<Long> orderIds, List<Long> tagIds) {
        Set<Long> tags = new LinkedHashSet<>(tagIds);
        tags.forEach(this::get);
        Set<Long> orders = new LinkedHashSet<>(orderIds);
        orders.forEach(orderService::require);
        int n = addLinks(orders, tags, 0);
        logService.log(orders, EcomOrderLogAction.TAG, tagNames(tags));
        return n;
    }

    /** 批量去标 */
    @Transactional
    public int untag(List<Long> orderIds, List<Long> tagIds) {
        orderIds.stream().distinct().forEach(orderService::require);
        int n = linkMapper.delete(Wrappers.<EcomOrderTagLink>lambdaQuery().in(EcomOrderTagLink::getOrderId, orderIds)
                .in(EcomOrderTagLink::getTagId, tagIds));
        logService.log(orderIds, EcomOrderLogAction.UNTAG, tagNames(tagIds));
        return n;
    }

    /**
     * 审单规则自动打标（内部，调用方事务中执行）：只打启用的标签，已有的不重复
     */
    public void autoTag(Long orderId, Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<EcomOrderTag> enabledTags = tagMapper.selectByIds(tagIds).stream().filter(t -> t.getStatus() == EnableStatus.ENABLED).toList();
        Set<Long> enabled = enabledTags.stream().map(EcomOrderTag::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (addLinks(Set.of(orderId), enabled, 1) > 0) {
            logService.log(orderId, EcomOrderLogAction.TAG, EcomOrderLogService.text("ecom.log.autoTag",
                    enabledTags.stream().map(EcomOrderTag::getName).collect(Collectors.joining(", "))));
        }
    }

    private String tagNames(Collection<Long> tagIds) {
        return tagIds.isEmpty() ? null : tagMapper.selectByIds(tagIds).stream().map(EcomOrderTag::getName).collect(Collectors.joining(", "));
    }

    private int addLinks(Collection<Long> orders, Collection<Long> tags, int auto) {
        if (orders.isEmpty() || tags.isEmpty()) {
            return 0;
        }
        Set<String> existing = new HashSet<>();
        linkMapper.selectList(Wrappers.<EcomOrderTagLink>lambdaQuery().in(EcomOrderTagLink::getOrderId, orders).in(EcomOrderTagLink::getTagId, tags))
                .forEach(l -> existing.add(l.getOrderId() + "|" + l.getTagId()));
        List<EcomOrderTagLink> add = new ArrayList<>();
        for (Long o : orders) {
            for (Long t : tags) {
                if (existing.add(o + "|" + t)) {
                    EcomOrderTagLink l = new EcomOrderTagLink();
                    l.setOrderId(o);
                    l.setTagId(t);
                    l.setAutoTagged(auto);
                    add.add(l);
                }
            }
        }
        if (!add.isEmpty()) {
            linkMapper.insertBatch(add);
        }
        return add.size();
    }
}
