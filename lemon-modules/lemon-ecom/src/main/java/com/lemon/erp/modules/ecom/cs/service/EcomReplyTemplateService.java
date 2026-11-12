package com.lemon.erp.modules.ecom.cs.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.entity.EcomReplyTemplate;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.model.CsConstants;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomReplyTemplateMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 快捷回复模板：多语言（业务字典 ecom_cs_language），可限定平台；回复时按会话 / 评价关联的订单替换变量：
 * {buyerName} 买家、{orderNo} 平台订单号、{trackingNo} 运单号、{carrier} 承运商、{productName} 商品名（第一行商品标题）、{storeName} 店铺。
 * 取不到值的变量替换为空并在结果中列出，由客服补充；不认识的变量原样保留
 */
@Service
@RequiredArgsConstructor
public class EcomReplyTemplateService {

    /** 支持的变量 */
    public static final List<String> VARIABLES = List.of("buyerName", "orderNo", "trackingNo", "carrier", "productName", "storeName");
    private static final Pattern VAR = Pattern.compile("\\{(\\w+)}");

    private final EcomReplyTemplateMapper templateMapper;
    private final EcomCsThreadService threadService;
    private final EcomReviewService reviewService;
    private final EcomOrderService orderService;
    private final EcomStoreService storeService;
    private final DictLookup dictLookup;

    public PageResult<EcomReplyTemplate> page(long current, long size, String language, EcomPlatform platform, String keyword, EnableStatus status) {
        Page<EcomReplyTemplate> page = Page.of(current, size);
        templateMapper.selectPage(page, Wrappers.<EcomReplyTemplate>lambdaQuery()
                .eq(StringUtils.hasText(language), EcomReplyTemplate::getLanguage, language)
                .eq(platform != null, EcomReplyTemplate::getPlatform, platform)
                .eq(status != null, EcomReplyTemplate::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(EcomReplyTemplate::getName, keyword).or().like(EcomReplyTemplate::getScene, keyword)
                        .or().like(EcomReplyTemplate::getContent, keyword))
                .orderByAsc(EcomReplyTemplate::getSort).orderByAsc(EcomReplyTemplate::getId));
        return PageResult.of(page);
    }

    /** 回复时选择：启用的模板，适用平台为空或等于 platform，按语言、顺序 */
    public List<EcomReplyTemplate> options(EcomPlatform platform) {
        return templateMapper.selectList(Wrappers.<EcomReplyTemplate>lambdaQuery().eq(EcomReplyTemplate::getStatus, EnableStatus.ENABLED)
                .and(platform != null, w -> w.isNull(EcomReplyTemplate::getPlatform).or().eq(EcomReplyTemplate::getPlatform, platform))
                .orderByAsc(EcomReplyTemplate::getLanguage).orderByAsc(EcomReplyTemplate::getSort).orderByAsc(EcomReplyTemplate::getId));
    }

    public EcomReplyTemplate get(Long id) {
        EcomReplyTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("ecom.error.cs.templateNotFound");
        }
        return t;
    }

    public Long create(EcomReplyTemplate t) {
        t.setId(null);
        normalize(t);
        checkUnique(t, null);
        if (t.getStatus() == null) {
            t.setStatus(EnableStatus.ENABLED);
        }
        templateMapper.insert(t);
        return t.getId();
    }

    public void update(Long id, EcomReplyTemplate t) {
        get(id);
        normalize(t);
        checkUnique(t, id);
        t.setId(id);
        templateMapper.updateById(t);
        if (t.getPlatform() == null) {
            templateMapper.update(Wrappers.<EcomReplyTemplate>lambdaUpdate().set(EcomReplyTemplate::getPlatform, null).eq(EcomReplyTemplate::getId, id));
        }
    }

    public void delete(List<Long> ids) {
        templateMapper.deleteByIds(ids);
    }

    private void normalize(EcomReplyTemplate t) {
        t.setName(t.getName().trim());
        t.setLanguage(t.getLanguage().trim());
        t.setContent(t.getContent().trim());
        t.setScene(StringUtils.hasText(t.getScene()) ? t.getScene().trim() : null);
        if (t.getSort() == null) {
            t.setSort(0);
        }
        dictLookup.check(CsConstants.LANGUAGE_DICT, t.getLanguage(), "ecom.label.cs.language");
    }

    private void checkUnique(EcomReplyTemplate t, Long excludeId) {
        if (templateMapper.exists(Wrappers.<EcomReplyTemplate>lambdaQuery().eq(EcomReplyTemplate::getName, t.getName())
                .eq(EcomReplyTemplate::getLanguage, t.getLanguage()).ne(excludeId != null, EcomReplyTemplate::getId, excludeId))) {
            throw new BizException("ecom.error.cs.templateExists", t.getName(), t.getLanguage());
        }
    }

    // ---------------------------------------------------------------- 变量替换

    /**
     * 按会话 / 评价 / 订单替换模板变量（优先级：threadId &gt; reviewId &gt; orderId，取其关联的订单）
     */
    public CsViews.Rendered render(Long templateId, Long threadId, Long reviewId, Long orderId) {
        EcomReplyTemplate t = get(templateId);
        Map<String, String> vars = new LinkedHashMap<>();
        Long storeId = null;
        Long oid = orderId;
        if (threadId != null) {
            EcomMessageThread th = threadService.require(threadId);
            storeId = th.getStoreId();
            vars.put("buyerName", th.getBuyerName());
            vars.put("orderNo", th.getPlatformOrderNo());
            oid = th.getOrderId();
        } else if (reviewId != null) {
            EcomReview r = reviewService.detail(reviewId);
            storeId = r.getStoreId();
            vars.put("buyerName", r.getReviewer());
            vars.put("orderNo", r.getPlatformOrderNo());
            vars.put("productName", r.getListingTitle());
            oid = r.getOrderId();
        }
        if (oid != null) {
            EcomOrder o = orderOrNull(oid);
            if (o != null) {
                storeId = o.getStoreId();
                vars.merge("buyerName", firstText(o.getBuyerName(), o.getShipName()), (a, b) -> StringUtils.hasText(a) ? a : b);
                vars.put("orderNo", o.getPlatformOrderNo());
                vars.put("trackingNo", o.getTrackingNo());
                vars.put("carrier", o.getCarrier());
                List<EcomOrderItem> items = o.getItems();
                if (items != null && !items.isEmpty()) {
                    vars.merge("productName", items.get(0).getTitle(), (a, b) -> StringUtils.hasText(a) ? a : b);
                }
            }
        }
        if (storeId != null) {
            EcomStore store = storeService.get(storeId);
            vars.put("storeName", store.getName());
        }
        return renderText(t.getContent(), vars);
    }

    /** 订单详情（按订单的数据权限，没有权限或已删除时为 null） */
    private EcomOrder orderOrNull(Long id) {
        try {
            return orderService.detail(id);
        } catch (BizException e) {
            return null;
        }
    }

    /**
     * 替换 {变量}：支持的变量取不到值时替换为空并列入 missing，不认识的变量原样保留
     */
    public static CsViews.Rendered renderText(String content, Map<String, String> vars) {
        Matcher m = VAR.matcher(content == null ? "" : content);
        StringBuilder sb = new StringBuilder();
        List<String> missing = new ArrayList<>();
        while (m.find()) {
            String name = m.group(1);
            String replacement;
            if (!VARIABLES.contains(name)) {
                replacement = m.group(0);
            } else {
                String v = vars.get(name);
                if (StringUtils.hasText(v)) {
                    replacement = v.trim();
                } else {
                    replacement = "";
                    if (!missing.contains(name)) {
                        missing.add(name);
                    }
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return new CsViews.Rendered(sb.toString().replaceAll("[ \\t]{2,}", " ").trim(), missing);
    }

    private static String firstText(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }
}
