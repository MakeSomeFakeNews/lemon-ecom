package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishDraft;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishVariant;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishDraftMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishLogMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishVariantMapper;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 刊登草稿与刊登记录的维护：从物料 / SPU 创建草稿并按店铺生成刊登记录（套用刊登模板、价格模板计算建议价），
 * 主内容应用到店铺、单店内容与变体的修改、重新计算建议价、删除。发布、下架见 {@link EcomPublishService}
 */
@Service
@RequiredArgsConstructor
public class EcomPublishDraftService {

    /** 可以修改内容的状态 */
    static final Set<PublishStatus> EDITABLE = EnumSet.of(PublishStatus.DRAFT, PublishStatus.FAILED, PublishStatus.OFFLINE);
    /** 主内容中可以应用到店铺的字段 */
    static final List<String> MASTER_FIELDS = List.of("title", "bullets", "description", "images", "brand", "barcode");

    private final EcomPublishDraftMapper draftMapper;
    private final EcomPublishItemMapper itemMapper;
    private final EcomPublishVariantMapper variantMapper;
    private final EcomPublishLogMapper logMapper;
    private final EcomStoreService storeService;
    private final EcomPublishTemplateService publishTemplateService;
    private final EcomPriceTemplateService priceTemplateService;
    private final WmsMaterialImageService imageService;
    private final EcomPublishSupport support;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomPublishItem> page(long current, long size, Long storeId, EcomPlatform platform, PublishStatus status, String keyword,
                                            Long draftId) {
        Page<EcomPublishItem> page = Page.of(current, size);
        itemMapper.selectPageList(page, storeId, platform, status, StringUtils.hasText(keyword) ? keyword.trim() : null, draftId);
        page.getRecords().forEach(EcomPublishDraftService::read);
        return PageResult.of(page);
    }

    /** 各状态数量（键为状态名，另有 ALL 为合计） */
    public Map<String, Long> counts(Long storeId, EcomPlatform platform, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        long total = 0;
        for (PublishViews.StatusCount c : itemMapper.countByStatus(storeId, platform, StringUtils.hasText(keyword) ? keyword.trim() : null)) {
            out.put(c.getStatus().name(), c.getCnt());
            total += c.getCnt();
        }
        out.put("ALL", total);
        return out;
    }

    public EcomPublishDraft getDraft(Long id) {
        EcomPublishDraft d = requireDraft(id);
        WmsMaterial m = support.materialService().mapByIds(List.of(d.getMaterialId())).get(d.getMaterialId());
        if (m != null) {
            d.setMaterialCode(m.getCode());
            d.setMaterialName(m.getName());
            d.setSpu(m.isTemplateMaterial());
        }
        List<Long> ids = itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().select(EcomPublishItem::getId).eq(EcomPublishItem::getDraftId, id))
                .stream().map(EcomPublishItem::getId).toList();
        List<EcomPublishItem> items = ids.isEmpty() ? new ArrayList<>() : itemMapper.selectDetails(ids);
        items.forEach(EcomPublishDraftService::read);
        d.setItems(items);
        return d;
    }

    /** 刊登记录与变体 */
    public EcomPublishItem getItem(Long id) {
        List<EcomPublishItem> list = itemMapper.selectDetails(List.of(id));
        if (list.isEmpty()) {
            throw new BizException("ecom.error.pub.itemNotFound");
        }
        EcomPublishItem item = read(list.get(0));
        item.setVariants(variants(List.of(id)).getOrDefault(id, new ArrayList<>()));
        return item;
    }

    /** 刊登记录 ID → 变体（按排序） */
    public Map<Long, List<EcomPublishVariant>> variants(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<EcomPublishVariant>> out = new HashMap<>();
        for (EcomPublishVariant v : variantMapper.selectByItems(itemIds)) {
            v.setVariantAttrs(EcomPublishSupport.readMap(v.getVariantAttrsJson()));
            out.computeIfAbsent(v.getItemId(), k -> new ArrayList<>()).add(v);
        }
        return out;
    }

    /** 物料的图片（多规格款式时为款式图片，加上各 SKU 的主图），用于刊登时选择 */
    public List<String> materialImages(Long materialId) {
        WmsMaterial m = support.materialService().get(materialId);
        List<WmsMaterial> skus = support.skusOf(m);
        List<Long> ids = new ArrayList<>();
        ids.add(m.getId());
        skus.stream().map(WmsMaterial::getId).filter(id -> !id.equals(m.getId())).limit(100).forEach(ids::add);
        Map<Long, List<String>> urls = imageService.urls(ids);
        LinkedHashSet<String> out = new LinkedHashSet<>(urls.getOrDefault(m.getId(), List.of()));
        for (WmsMaterial s : skus) {
            List<String> list = urls.get(s.getId());
            if (list != null && !list.isEmpty()) {
                out.add(list.get(0));
            }
        }
        if (out.isEmpty() && StringUtils.hasText(m.getImageUrl())) {
            out.add(m.getImageUrl());
        }
        return new ArrayList<>(out);
    }

    // ---------------------------------------------------------------- 创建

    /** 从物料 / SPU 创建草稿，并为选择的店铺各生成一份刊登记录 */
    @Transactional
    public Long createDraft(PublishRequests.CreateDraft req) {
        WmsMaterial m = support.materialService().get(req.materialId());
        if (support.skusOf(m).isEmpty()) {
            throw new BizException("ecom.error.pub.spuNoSku", m.getName());
        }
        EcomPublishDraft d = new EcomPublishDraft();
        d.setMaterialId(m.getId());
        d.setTitle(StringUtils.hasText(req.title()) ? req.title().trim() : m.getName());
        d.setBulletsJson(null);
        List<String> images = materialImages(m.getId());
        d.setImagesJson(EcomPublishSupport.writeList(images.size() > 12 ? images.subList(0, 12) : images));
        d.setBrand(m.getBrand());
        d.setUpcExempt(0);
        d.setDescription(m.getRemark());
        LoginUser user = loginUserOrNull();
        d.setDeptId(user == null ? null : user.deptId());
        draftMapper.insert(d);
        addStores(d.getId(), new PublishRequests.AddStores(req.storeIds(), req.templateIds(), req.priceTemplateId()));
        return d.getId();
    }

    /** 给草稿增加店铺（已有的店铺跳过），返回新增的刊登记录数 */
    @Transactional
    public int addStores(Long draftId, PublishRequests.AddStores req) {
        EcomPublishDraft d = read(requireDraft(draftId));
        Set<Long> existing = itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().select(EcomPublishItem::getStoreId)
                .eq(EcomPublishItem::getDraftId, draftId)).stream().map(EcomPublishItem::getStoreId).collect(Collectors.toSet());
        WmsMaterial material = support.materialService().get(d.getMaterialId());
        List<WmsMaterial> skus = support.skusOf(material);
        int added = 0;
        for (Long storeId : new LinkedHashSet<>(req.storeIds())) {
            if (existing.contains(storeId)) {
                continue;
            }
            EcomStore store = storeService.requireEnabled(storeId);
            Long templateId = req.templateIds() == null ? null : req.templateIds().get(store.getPlatform().name());
            EcomPublishTemplate template = templateId == null ? null : publishTemplateService.get(templateId);
            if (template != null && template.getPlatform() != store.getPlatform()) {
                throw new BizException("ecom.error.pub.templatePlatformMismatch", template.getName(), store.getName());
            }
            EcomPublishItem item = new EcomPublishItem();
            item.setDraftId(draftId);
            item.setStoreId(storeId);
            item.setStatus(PublishStatus.DRAFT);
            copyMaster(d, item, MASTER_FIELDS);
            item.setCurrency(store.getCurrency());
            item.setRetryCount(0);
            Long priceTemplateId = req.priceTemplateId();
            if (template != null) {
                item.setPublishTemplateId(template.getId());
                item.setCategoryId(template.getCategoryId());
                item.setCategoryName(template.getCategoryName());
                item.setAttributesJson(EcomPublishSupport.writeMap(template.getAttributes()));
                item.setShippingTemplate(template.getShippingTemplate());
                item.setReturnPolicy(template.getReturnPolicy());
                item.setHandlingDays(template.getHandlingDays());
                item.setFulfillment(template.getFulfillment());
                item.setExtraJson(EcomPublishSupport.writeMap(template.getExtra()));
                if (!StringUtils.hasText(item.getBrand()) && StringUtils.hasText(template.getBrand())) {
                    item.setBrand(template.getBrand());
                }
                if (priceTemplateId == null) {
                    priceTemplateId = template.getPriceTemplateId();
                }
            }
            if (priceTemplateId != null) {
                priceTemplateService.get(priceTemplateId);
            }
            item.setPriceTemplateId(priceTemplateId);
            if (skus.size() > 1) {
                item.setParentSku(PublishSupportNames.parentSku(material.getCode()));
            }
            itemMapper.insert(item);
            List<EcomPublishVariant> variants = new ArrayList<>();
            int sort = 0;
            for (WmsMaterial s : skus) {
                EcomPublishVariant v = new EcomPublishVariant();
                v.setItemId(item.getId());
                v.setMaterialId(s.getId());
                v.setSellerSku(s.getCode());
                v.setVariantText(s.getVariantText());
                v.setVariantAttrs(PublishSupportNames.variantAttrs(s.getVariantText()));
                v.setBarcode(s.getBarcode());
                v.setQty(0);
                v.setSort(sort++);
                variants.add(v);
            }
            compute(item, variants);
            saveVariants(item.getId(), variants);
            added++;
        }
        return added;
    }

    // ---------------------------------------------------------------- 修改

    @Transactional
    public void updateDraft(Long id, EcomPublishDraft d) {
        EcomPublishDraft old = requireDraft(id);
        d.setId(id);
        d.setMaterialId(old.getMaterialId());
        d.setTitle(d.getTitle().trim());
        d.setBulletsJson(EcomPublishSupport.writeList(d.getBullets()));
        d.setImagesJson(EcomPublishSupport.writeList(d.getImages()));
        d.setUpcExempt(d.getUpcExempt() != null && d.getUpcExempt() == 1 ? 1 : 0);
        draftMapper.updateById(d);
        draftMapper.update(Wrappers.<EcomPublishDraft>lambdaUpdate().set(EcomPublishDraft::getBulletsJson, d.getBulletsJson())
                .set(EcomPublishDraft::getImagesJson, d.getImagesJson()).set(EcomPublishDraft::getDescription, d.getDescription())
                .set(EcomPublishDraft::getBrand, d.getBrand()).set(EcomPublishDraft::getBarcodeType, d.getBarcodeType())
                .set(EcomPublishDraft::getExemptionNote, d.getExemptionNote()).set(EcomPublishDraft::getRemark, d.getRemark())
                .eq(EcomPublishDraft::getId, id));
    }

    /** 把主内容应用到可修改的刊登记录，返回更新的记录数 */
    @Transactional
    public int applyMaster(Long draftId, List<String> fields) {
        EcomPublishDraft d = read(requireDraft(draftId));
        List<String> use = fields == null || fields.isEmpty() ? MASTER_FIELDS : fields.stream().filter(MASTER_FIELDS::contains).toList();
        int n = 0;
        for (EcomPublishItem item : itemMapper.selectList(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getDraftId, draftId)
                .in(EcomPublishItem::getStatus, EDITABLE))) {
            copyMaster(d, item, use);
            itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getTitle, item.getTitle())
                    .set(EcomPublishItem::getBulletsJson, item.getBulletsJson()).set(EcomPublishItem::getDescription, item.getDescription())
                    .set(EcomPublishItem::getImagesJson, item.getImagesJson()).set(EcomPublishItem::getBrand, item.getBrand())
                    .set(EcomPublishItem::getBarcodeType, item.getBarcodeType()).set(EcomPublishItem::getUpcExempt, item.getUpcExempt())
                    .set(EcomPublishItem::getExemptionNote, item.getExemptionNote()).eq(EcomPublishItem::getId, item.getId()));
            n++;
        }
        return n;
    }

    /** 修改一个店铺的刊登内容与变体（变体整体替换，按平台 SKU 保留已回写的平台 ID 与在线商品） */
    @Transactional
    public void updateItem(Long id, EcomPublishItem in) {
        EcomPublishItem old = requireEditable(id);
        if (in.getVariants() == null || in.getVariants().isEmpty()) {
            throw new BizException("ecom.error.pub.variantRequired");
        }
        if (in.getPriceTemplateId() != null) {
            priceTemplateService.get(in.getPriceTemplateId());
        }
        EcomPublishItem u = new EcomPublishItem();
        u.setId(id);
        u.setTitle(in.getTitle().trim());
        u.setBulletsJson(EcomPublishSupport.writeList(in.getBullets()));
        u.setDescription(in.getDescription());
        u.setImagesJson(EcomPublishSupport.writeList(in.getImages()));
        u.setBrand(blank(in.getBrand()));
        u.setBarcodeType(in.getBarcodeType());
        u.setUpcExempt(in.getUpcExempt() != null && in.getUpcExempt() == 1 ? 1 : 0);
        u.setExemptionNote(blank(in.getExemptionNote()));
        u.setCategoryId(blank(in.getCategoryId()));
        u.setCategoryName(blank(in.getCategoryName()));
        u.setAttributesJson(EcomPublishSupport.writeMap(in.getAttributes()));
        u.setPublishTemplateId(in.getPublishTemplateId());
        u.setPriceTemplateId(in.getPriceTemplateId());
        u.setFulfillment(in.getFulfillment());
        u.setShippingTemplate(blank(in.getShippingTemplate()));
        u.setReturnPolicy(blank(in.getReturnPolicy()));
        u.setHandlingDays(in.getHandlingDays());
        u.setExtraJson(EcomPublishSupport.writeMap(in.getExtra()));
        u.setParentSku(blank(in.getParentSku()));
        u.setCurrency(old.getCurrency());
        itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getTitle, u.getTitle())
                .set(EcomPublishItem::getBulletsJson, u.getBulletsJson()).set(EcomPublishItem::getDescription, u.getDescription())
                .set(EcomPublishItem::getImagesJson, u.getImagesJson()).set(EcomPublishItem::getBrand, u.getBrand())
                .set(EcomPublishItem::getBarcodeType, u.getBarcodeType()).set(EcomPublishItem::getUpcExempt, u.getUpcExempt())
                .set(EcomPublishItem::getExemptionNote, u.getExemptionNote()).set(EcomPublishItem::getCategoryId, u.getCategoryId())
                .set(EcomPublishItem::getCategoryName, u.getCategoryName()).set(EcomPublishItem::getAttributesJson, u.getAttributesJson())
                .set(EcomPublishItem::getPublishTemplateId, u.getPublishTemplateId()).set(EcomPublishItem::getPriceTemplateId, u.getPriceTemplateId())
                .set(EcomPublishItem::getFulfillment, u.getFulfillment()).set(EcomPublishItem::getShippingTemplate, u.getShippingTemplate())
                .set(EcomPublishItem::getReturnPolicy, u.getReturnPolicy()).set(EcomPublishItem::getHandlingDays, u.getHandlingDays())
                .set(EcomPublishItem::getExtraJson, u.getExtraJson()).set(EcomPublishItem::getParentSku, u.getParentSku())
                .eq(EcomPublishItem::getId, id));
        Map<String, EcomPublishVariant> previous = variants(List.of(id)).getOrDefault(id, List.of()).stream()
                .collect(Collectors.toMap(EcomPublishVariant::getSellerSku, v -> v, (a, b) -> a));
        Set<Long> materialIds = in.getVariants().stream().map(EcomPublishVariant::getMaterialId).collect(Collectors.toSet());
        Map<Long, WmsMaterial> materials = support.materialService().mapByIds(materialIds);
        Set<String> skus = new HashSet<>();
        List<EcomPublishVariant> list = new ArrayList<>();
        int sort = 0;
        for (EcomPublishVariant v : in.getVariants()) {
            WmsMaterial m = materials.get(v.getMaterialId());
            if (m == null) {
                throw new BizException("ecom.error.pub.materialNotFound");
            }
            if (m.isTemplateMaterial()) {
                throw new BizException("ecom.error.materialIsTemplate", m.getName());
            }
            String sku = v.getSellerSku().trim();
            if (!skus.add(sku)) {
                throw new BizException("ecom.error.pub.skuDuplicate", sku);
            }
            EcomPublishVariant n = new EcomPublishVariant();
            n.setItemId(id);
            n.setMaterialId(m.getId());
            n.setSellerSku(sku);
            n.setVariantText(blank(v.getVariantText()));
            n.setVariantAttrs(v.getVariantAttrs());
            n.setBarcode(blank(v.getBarcode()));
            n.setPrice(v.getPrice());
            n.setQty(v.getQty() == null ? 0 : v.getQty());
            n.setSort(sort++);
            EcomPublishVariant p = previous.get(sku);
            if (p != null) {
                n.setPlatformSkuId(p.getPlatformSkuId());
                n.setListingId(p.getListingId());
            }
            list.add(n);
        }
        compute(u, list);
        variantMapper.delete(Wrappers.<EcomPublishVariant>lambdaQuery().eq(EcomPublishVariant::getItemId, id));
        saveVariants(id, list);
    }

    /** 按价格模板重新计算建议价，并把售价改为建议价 */
    @Transactional
    public EcomPublishItem recalc(Long id) {
        EcomPublishItem item = requireEditable(id);
        if (item.getPriceTemplateId() == null) {
            throw new BizException("ecom.error.pub.priceTemplateRequired");
        }
        EcomPriceTemplate t = priceTemplateService.get(item.getPriceTemplateId());
        BigDecimal rate = support.rate(item.getCurrency());
        List<EcomPublishVariant> list = variants(List.of(id)).getOrDefault(id, new ArrayList<>());
        Map<Long, BigDecimal> costs = support.materialCosts(list.stream().map(EcomPublishVariant::getMaterialId).toList());
        PriceCalculator.Params p = PriceCalculator.Params.of(t);
        for (EcomPublishVariant v : list) {
            BigDecimal cost = costs.get(v.getMaterialId());
            if (cost == null) {
                continue;
            }
            BigDecimal price = PriceCalculator.price(cost, rate, p);
            PriceCalculator.Profit profit = PriceCalculator.profit(price, cost, rate, p);
            variantMapper.update(Wrappers.<EcomPublishVariant>lambdaUpdate().set(EcomPublishVariant::getPrice, price)
                    .set(EcomPublishVariant::getCost, cost).set(EcomPublishVariant::getSuggestedPrice, price)
                    .set(EcomPublishVariant::getEstProfit, profit.profit()).set(EcomPublishVariant::getEstMargin, profit.margin())
                    .eq(EcomPublishVariant::getId, v.getId()));
        }
        return getItem(id);
    }

    /** 删除刊登记录（发布中 / 已成功的不能删除，成功的请先下架）；草稿下没有记录时一并删除草稿 */
    @Transactional
    public void deleteItems(List<Long> ids) {
        List<EcomPublishItem> items = itemMapper.selectByIds(ids);
        Set<Long> drafts = new HashSet<>();
        for (EcomPublishItem i : items) {
            if (i.getStatus() == PublishStatus.PUBLISHING || i.getStatus() == PublishStatus.PENDING || i.getStatus() == PublishStatus.SUCCESS) {
                throw new BizException("ecom.error.pub.cannotDelete", i.getTitle());
            }
            drafts.add(i.getDraftId());
        }
        if (items.isEmpty()) {
            return;
        }
        List<Long> itemIds = items.stream().map(EcomPublishItem::getId).toList();
        variantMapper.delete(Wrappers.<EcomPublishVariant>lambdaQuery().in(EcomPublishVariant::getItemId, itemIds));
        logMapper.delete(Wrappers.<EcomPublishLog>lambdaQuery().in(EcomPublishLog::getItemId, itemIds));
        itemMapper.deleteByIds(itemIds);
        for (Long draftId : drafts) {
            if (!itemMapper.exists(Wrappers.<EcomPublishItem>lambdaQuery().eq(EcomPublishItem::getDraftId, draftId))) {
                draftMapper.deleteById(draftId);
            }
        }
    }

    // ---------------------------------------------------------------- 内部

    /** 计算变体的成本、建议价、预估利润；售价为空时取建议价。没有价格模板 / 汇率 / 成本时不计算 */
    void compute(EcomPublishItem item, List<EcomPublishVariant> list) {
        EcomPriceTemplate t = priceTemplateService.find(item.getPriceTemplateId());
        BigDecimal rate = support.rateOrNull(item.getCurrency());
        Map<Long, BigDecimal> costs = support.materialCosts(list.stream().map(EcomPublishVariant::getMaterialId).toList());
        PriceCalculator.Params p = t == null ? null : PriceCalculator.Params.of(t);
        for (EcomPublishVariant v : list) {
            BigDecimal cost = costs.get(v.getMaterialId());
            v.setCost(cost);
            v.setSuggestedPrice(null);
            v.setEstProfit(null);
            v.setEstMargin(null);
            if (p == null || rate == null || cost == null) {
                continue;
            }
            try {
                BigDecimal suggested = PriceCalculator.price(cost, rate, p);
                v.setSuggestedPrice(suggested);
                if (v.getPrice() == null) {
                    v.setPrice(suggested);
                }
                PriceCalculator.Profit profit = PriceCalculator.profit(v.getPrice(), cost, rate, p);
                v.setEstProfit(profit.profit());
                v.setEstMargin(profit.margin());
            } catch (BizException e) {
                // 费率合计过高等：不给建议价
            }
        }
    }

    private void saveVariants(Long itemId, List<EcomPublishVariant> list) {
        list.forEach(v -> {
            v.setItemId(itemId);
            v.setVariantAttrsJson(EcomPublishSupport.writeMap(v.getVariantAttrs()));
        });
        if (!list.isEmpty()) {
            variantMapper.insertBatch(list);
        }
    }

    private static void copyMaster(EcomPublishDraft d, EcomPublishItem item, List<String> fields) {
        if (fields.contains("title")) {
            item.setTitle(d.getTitle());
        }
        if (fields.contains("bullets")) {
            item.setBulletsJson(d.getBulletsJson());
        }
        if (fields.contains("description")) {
            item.setDescription(d.getDescription());
        }
        if (fields.contains("images")) {
            item.setImagesJson(d.getImagesJson());
        }
        if (fields.contains("brand")) {
            item.setBrand(d.getBrand());
        }
        if (fields.contains("barcode")) {
            item.setBarcodeType(d.getBarcodeType());
            item.setUpcExempt(d.getUpcExempt() == null ? 0 : d.getUpcExempt());
            item.setExemptionNote(d.getExemptionNote());
        }
    }

    EcomPublishDraft requireDraft(Long id) {
        EcomPublishDraft d = id == null ? null : draftMapper.selectById(id);
        if (d == null) {
            throw new BizException("ecom.error.pub.draftNotFound");
        }
        return d;
    }

    EcomPublishItem requireItem(Long id) {
        EcomPublishItem i = id == null ? null : itemMapper.selectById(id);
        if (i == null) {
            throw new BizException("ecom.error.pub.itemNotFound");
        }
        return i;
    }

    private EcomPublishItem requireEditable(Long id) {
        EcomPublishItem i = requireItem(id);
        if (!EDITABLE.contains(i.getStatus())) {
            throw new BizException("ecom.error.pub.notEditable", i.getTitle());
        }
        return i;
    }

    static EcomPublishItem read(EcomPublishItem i) {
        i.setBullets(EcomPublishSupport.readList(i.getBulletsJson()));
        i.setImages(EcomPublishSupport.readList(i.getImagesJson()));
        i.setAttributes(EcomPublishSupport.readMap(i.getAttributesJson()));
        i.setExtra(EcomPublishSupport.readMap(i.getExtraJson()));
        return i;
    }

    static EcomPublishDraft read(EcomPublishDraft d) {
        d.setBullets(EcomPublishSupport.readList(d.getBulletsJson()));
        d.setImages(EcomPublishSupport.readList(d.getImagesJson()));
        return d;
    }

    private static String blank(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static LoginUser loginUserOrNull() {
        try {
            return SecurityUtils.getUserIdOrNull() == null ? null : SecurityUtils.getLoginUser();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
