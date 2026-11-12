package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceRule;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPriceTemplateMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishTemplateMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomRepriceRuleMapper;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格模板：维护、启用的选项、定价试算（{@link PriceCalculator}）
 */
@Service
@RequiredArgsConstructor
public class EcomPriceTemplateService {

    private final EcomPriceTemplateMapper templateMapper;
    private final EcomPublishTemplateMapper publishTemplateMapper;
    private final EcomPublishItemMapper itemMapper;
    private final EcomRepriceRuleMapper ruleMapper;
    private final EcomStoreService storeService;
    private final EcomPublishSupport support;

    public PageResult<EcomPriceTemplate> page(long current, long size, String keyword, EcomPlatform platform, EnableStatus status) {
        return PageResult.of(templateMapper.selectPage(Page.of(current, size), Wrappers.<EcomPriceTemplate>lambdaQuery()
                .like(StringUtils.hasText(keyword), EcomPriceTemplate::getName, keyword == null ? null : keyword.trim())
                .eq(platform != null, EcomPriceTemplate::getPlatform, platform)
                .eq(status != null, EcomPriceTemplate::getStatus, status)
                .orderByDesc(EcomPriceTemplate::getId)));
    }

    /** 启用的模板（下拉），platform 不为空时只列出通用和该平台的 */
    public List<EcomPriceTemplate> options(EcomPlatform platform) {
        return templateMapper.selectList(Wrappers.<EcomPriceTemplate>lambdaQuery().eq(EcomPriceTemplate::getStatus, EnableStatus.ENABLED)
                .and(platform != null, w -> w.isNull(EcomPriceTemplate::getPlatform).or().eq(EcomPriceTemplate::getPlatform, platform))
                .orderByAsc(EcomPriceTemplate::getName));
    }

    public EcomPriceTemplate get(Long id) {
        EcomPriceTemplate t = id == null ? null : templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("ecom.error.pub.priceTemplateNotFound");
        }
        return t;
    }

    /** 模板，id 为空或不存在时为 null */
    public EcomPriceTemplate find(Long id) {
        return id == null ? null : templateMapper.selectById(id);
    }

    public Long create(EcomPriceTemplate t) {
        t.setId(null);
        normalize(t);
        checkName(t.getName(), null);
        templateMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void update(Long id, EcomPriceTemplate t) {
        get(id);
        normalize(t);
        checkName(t.getName(), id);
        t.setId(id);
        templateMapper.updateById(t);
        // 可清空的字段
        templateMapper.update(Wrappers.<EcomPriceTemplate>lambdaUpdate().set(EcomPriceTemplate::getPlatform, t.getPlatform())
                .set(EcomPriceTemplate::getPriceTail, t.getPriceTail()).set(EcomPriceTemplate::getMinPrice, t.getMinPrice())
                .set(EcomPriceTemplate::getRemark, t.getRemark()).eq(EcomPriceTemplate::getId, id));
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            EcomPriceTemplate t = get(id);
            boolean used = publishTemplateMapper.exists(Wrappers.<EcomPublishTemplate>lambdaQuery().eq(EcomPublishTemplate::getPriceTemplateId, id))
                    || itemMapper.exists(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getPriceTemplateId, id))
                    || ruleMapper.exists(Wrappers.<EcomRepriceRule>lambdaQuery().eq(EcomRepriceRule::getPriceTemplateId, id));
            if (used) {
                throw new BizException("ecom.error.pub.priceTemplateInUse", t.getName());
            }
        }
        templateMapper.deleteByIds(ids);
    }

    /**
     * 定价试算：成本为空时取物料参考成本；币种取店铺币种（没有店铺时取传入的币种）
     */
    public PublishViews.PriceQuote quote(PublishRequests.Quote req) {
        EcomPriceTemplate t = get(req.priceTemplateId());
        String currency = req.currency();
        if (req.storeId() != null) {
            EcomStore store = storeService.get(req.storeId());
            currency = store.getCurrency();
        }
        if (!StringUtils.hasText(currency)) {
            throw new BizException("ecom.error.pub.currencyRequired");
        }
        BigDecimal cost = req.cost();
        if (cost == null && req.materialId() != null) {
            cost = support.materialCosts(List.of(req.materialId())).get(req.materialId());
        }
        return quote(t, currency, cost, req.price());
    }

    /** 按模板、币种、成本试算（成本为空时只按售价计算费用，利润为 null） */
    public PublishViews.PriceQuote quote(EcomPriceTemplate t, String currency, BigDecimal cost, BigDecimal price) {
        BigDecimal rate = support.rate(currency);
        PriceCalculator.Params p = PriceCalculator.Params.of(t);
        BigDecimal suggested = cost == null ? null : PriceCalculator.price(cost, rate, p);
        BigDecimal used = price != null ? price : suggested;
        PriceCalculator.Profit profit = used == null || cost == null ? null : PriceCalculator.profit(used, cost, rate, p);
        return new PublishViews.PriceQuote(cost, rate, currency, suggested, used, profit == null ? null : profit.profit(),
                profit == null ? null : profit.margin(), profit == null ? null : profit.fees());
    }

    private void normalize(EcomPriceTemplate t) {
        t.setName(t.getName().trim());
        if (t.getStatus() == null) {
            t.setStatus(EnableStatus.ENABLED);
        }
        BigDecimal total = t.getCommissionRate().add(t.getOtherRate()).add(t.getTargetMargin());
        if (total.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new BizException("ecom.error.pub.rateTooHigh");
        }
    }

    private void checkName(String name, Long excludeId) {
        if (templateMapper.exists(Wrappers.<EcomPriceTemplate>lambdaQuery().eq(EcomPriceTemplate::getName, name)
                .ne(excludeId != null, EcomPriceTemplate::getId, excludeId))) {
            throw new BizException("ecom.error.pub.nameExists", name);
        }
    }
}
