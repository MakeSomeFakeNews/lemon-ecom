package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 刊登模板：按平台保存常用属性、运费模板、退货政策等默认值
 */
@Service
@RequiredArgsConstructor
public class EcomPublishTemplateService {

    private final EcomPublishTemplateMapper templateMapper;
    private final EcomPublishItemMapper itemMapper;
    private final EcomPriceTemplateService priceTemplateService;

    public PageResult<EcomPublishTemplate> page(long current, long size, String keyword, EcomPlatform platform, EnableStatus status) {
        Page<EcomPublishTemplate> page = Page.of(current, size);
        templateMapper.selectPageList(page, keyword == null ? null : keyword.trim(), platform, status);
        page.getRecords().forEach(EcomPublishTemplateService::read);
        return PageResult.of(page);
    }

    /** 启用的模板（下拉），可按平台过滤 */
    public List<EcomPublishTemplate> options(EcomPlatform platform) {
        List<EcomPublishTemplate> list = templateMapper.selectList(Wrappers.<EcomPublishTemplate>lambdaQuery()
                .eq(EcomPublishTemplate::getStatus, EnableStatus.ENABLED).eq(platform != null, EcomPublishTemplate::getPlatform, platform)
                .orderByAsc(EcomPublishTemplate::getPlatform).orderByAsc(EcomPublishTemplate::getName));
        list.forEach(EcomPublishTemplateService::read);
        return list;
    }

    public EcomPublishTemplate get(Long id) {
        EcomPublishTemplate t = id == null ? null : templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("ecom.error.pub.publishTemplateNotFound");
        }
        return read(t);
    }

    public Long create(EcomPublishTemplate t) {
        t.setId(null);
        normalize(t);
        checkName(t.getName(), null);
        templateMapper.insert(t);
        return t.getId();
    }

    @Transactional
    public void update(Long id, EcomPublishTemplate t) {
        get(id);
        normalize(t);
        checkName(t.getName(), id);
        t.setId(id);
        templateMapper.updateById(t);
        templateMapper.update(Wrappers.<EcomPublishTemplate>lambdaUpdate().set(EcomPublishTemplate::getCategoryId, t.getCategoryId())
                .set(EcomPublishTemplate::getCategoryName, t.getCategoryName()).set(EcomPublishTemplate::getAttributesJson, t.getAttributesJson())
                .set(EcomPublishTemplate::getBrand, t.getBrand()).set(EcomPublishTemplate::getShippingTemplate, t.getShippingTemplate())
                .set(EcomPublishTemplate::getReturnPolicy, t.getReturnPolicy()).set(EcomPublishTemplate::getHandlingDays, t.getHandlingDays())
                .set(EcomPublishTemplate::getFulfillment, t.getFulfillment()).set(EcomPublishTemplate::getPriceTemplateId, t.getPriceTemplateId())
                .set(EcomPublishTemplate::getExtraJson, t.getExtraJson()).set(EcomPublishTemplate::getRemark, t.getRemark())
                .eq(EcomPublishTemplate::getId, id));
    }

    @Transactional
    public void delete(List<Long> ids) {
        if (itemMapper.exists(Wrappers.<EcomPublishItem>lambdaQuery().in(EcomPublishItem::getPublishTemplateId, ids))) {
            // 刊登记录只引用模板来源，删除模板时解除引用
            itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getPublishTemplateId, null)
                    .in(EcomPublishItem::getPublishTemplateId, ids));
        }
        templateMapper.deleteByIds(ids);
    }

    private void normalize(EcomPublishTemplate t) {
        t.setName(t.getName().trim());
        if (t.getStatus() == null) {
            t.setStatus(EnableStatus.ENABLED);
        }
        if (t.getPriceTemplateId() != null) {
            priceTemplateService.get(t.getPriceTemplateId());
        }
        t.setCategoryId(StringUtils.hasText(t.getCategoryId()) ? t.getCategoryId().trim() : null);
        t.setAttributesJson(EcomPublishSupport.writeMap(t.getAttributes()));
        t.setExtraJson(EcomPublishSupport.writeMap(t.getExtra()));
    }

    private void checkName(String name, Long excludeId) {
        if (templateMapper.exists(Wrappers.<EcomPublishTemplate>lambdaQuery().eq(EcomPublishTemplate::getName, name)
                .ne(excludeId != null, EcomPublishTemplate::getId, excludeId))) {
            throw new BizException("ecom.error.pub.nameExists", name);
        }
    }

    static EcomPublishTemplate read(EcomPublishTemplate t) {
        t.setAttributes(EcomPublishSupport.readMap(t.getAttributesJson()));
        t.setExtra(EcomPublishSupport.readMap(t.getExtraJson()));
        return t;
    }
}
