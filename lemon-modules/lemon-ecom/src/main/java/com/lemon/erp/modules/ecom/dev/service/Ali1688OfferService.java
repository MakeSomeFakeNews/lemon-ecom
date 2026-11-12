package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Client;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Context;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Product;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Sku;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688OfferRequest;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.ParsedOffer;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SyncSummary;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Offer;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688PriceLog;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688OfferMapper;
import com.lemon.erp.modules.ecom.dev.mapper.Ecom1688PriceLogMapper;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物料关联 1688 商品（货源）：粘贴链接解析 offerId / specId，有已授权账号时调用商品详情同步标题、规格、价格、起批量、库存；
 * 定时同步价格，变动超过阈值时记录价格变动并提醒（待确认），一个物料一个货源
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Ali1688OfferService {

    static final int MESSAGE_MAX = 255;
    /** 一次同步最多处理的关联数 */
    static final int SYNC_LIMIT = 500;

    private final Ecom1688OfferMapper offerMapper;
    private final Ecom1688PriceLogMapper priceLogMapper;
    private final Ali1688AccountService accountService;
    private final Ali1688Client client;
    private final WmsMaterialService materialService;
    private final ScmSupplierService supplierService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public PageResult<Ecom1688Offer> page(long current, long size, String keyword, Integer priceAlert, Long supplierId) {
        Page<Ecom1688Offer> page = Page.of(current, size);
        offerMapper.selectPageList(page, keyword, priceAlert, supplierId);
        page.getRecords().forEach(o -> o.setOfferUrl(Ali1688Client.offerUrl(o.getOfferId())));
        return PageResult.of(page);
    }

    /** 物料的货源（下单、采购订单页面使用），key 为物料 ID */
    public Map<Long, Ecom1688Offer> byMaterials(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Ecom1688Offer> map = new HashMap<>();
        offerMapper.selectByMaterials(materialIds).forEach(o -> {
            o.setOfferUrl(Ali1688Client.offerUrl(o.getOfferId()));
            map.put(o.getMaterialId(), o);
        });
        return map;
    }

    public Ecom1688Offer get(Long id) {
        Ecom1688Offer o = id == null ? null : offerMapper.selectById(id);
        if (o == null) {
            throw new BizException("ecom.error.dev.offerNotFound");
        }
        return o;
    }

    /** 解析 1688 链接 / offerId */
    public ParsedOffer parse(String text) {
        String[] r = Ali1688Client.parseOfferUrl(text);
        return new ParsedOffer(r[0], r[1], Ali1688Client.offerUrl(r[0]));
    }

    /** 预览 1688 商品（标题、规格、阶梯价、库存），用于选择规格 */
    public Product preview(String offer, Long accountId) {
        String offerId = Ali1688Client.parseOfferUrl(offer)[0];
        return client.product(accountService.context(accountId), offerId);
    }

    /**
     * 关联（一个物料一个货源，已有时覆盖）：有可用账号时先查 1688 商品详情（接口失败报错，不保存），没有账号时按填写的信息保存
     *
     * @return 关联 ID
     */
    public Long link(Ali1688OfferRequest req) {
        WmsMaterial material = materialService.get(req.materialId());
        if (material.getIsTemplate() != null && material.getIsTemplate() == 1) {
            throw new BizException("ecom.error.materialIsTemplate", material.getCode());
        }
        if (req.supplierId() != null) {
            supplierService.get(req.supplierId());
        }
        String[] parsed = Ali1688Client.parseOfferUrl(req.offer());
        String specId = StringUtils.hasText(req.specId()) ? req.specId().trim() : parsed[1];
        Product product = accountService.hasUsableAccount() ? client.product(accountService.context(null), parsed[0]) : null;
        Ecom1688Offer o = new Ecom1688Offer();
        o.setMaterialId(req.materialId());
        o.setOfferId(parsed[0]);
        o.setSpecId(specId);
        o.setSupplierId(req.supplierId());
        o.setQtyRatio(req.qtyRatio() == null ? BigDecimal.ONE : req.qtyRatio());
        o.setAlertThreshold(req.alertThreshold());
        o.setRemark(req.remark());
        if (product != null) {
            applyProduct(o, product, specId);
            o.setSyncTime(LocalDateTime.now());
        } else {
            o.setTitle(trim(req.title()));
            o.setSpecText(trim(req.specText()));
            o.setSellerLoginId(trim(req.sellerLoginId()));
            o.setSellerName(trim(req.sellerName()));
            o.setPrice(req.price());
            o.setMinOrderQty(req.minOrderQty());
            o.setUnit(trim(req.unit()));
        }
        Ecom1688Offer old = offerMapper.selectOne(Wrappers.<Ecom1688Offer>lambdaQuery().eq(Ecom1688Offer::getMaterialId, req.materialId()));
        if (old == null) {
            offerMapper.insert(o);
            return o.getId();
        }
        o.setId(old.getId());
        o.setPriceAlert(0);
        offerMapper.updateById(o);
        offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate().set(Ecom1688Offer::getSyncMessage, null).set(Ecom1688Offer::getLastPrice, null)
                .set(Ecom1688Offer::getSupplierId, o.getSupplierId()).set(Ecom1688Offer::getAlertThreshold, o.getAlertThreshold())
                .set(Ecom1688Offer::getSpecId, o.getSpecId()).eq(Ecom1688Offer::getId, old.getId()));
        return old.getId();
    }

    /** 修改关联的换算、供应商、提醒阈值、备注（不重新查询 1688） */
    public void update(Long id, Ali1688OfferRequest req) {
        get(id);
        if (req.supplierId() != null) {
            supplierService.get(req.supplierId());
        }
        offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate()
                .set(Ecom1688Offer::getSupplierId, req.supplierId())
                .set(Ecom1688Offer::getQtyRatio, req.qtyRatio() == null ? BigDecimal.ONE : req.qtyRatio())
                .set(Ecom1688Offer::getAlertThreshold, req.alertThreshold())
                .set(Ecom1688Offer::getRemark, req.remark())
                .set(Ecom1688Offer::getUpdateTime, LocalDateTime.now())
                .eq(Ecom1688Offer::getId, id));
    }

    public void delete(List<Long> ids) {
        priceLogMapper.delete(Wrappers.<Ecom1688PriceLog>lambdaQuery().in(Ecom1688PriceLog::getOfferId, ids));
        offerMapper.deleteByIds(ids);
    }

    /** 确认价格变动（清除提醒） */
    public void acknowledge(List<Long> ids) {
        offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate().set(Ecom1688Offer::getPriceAlert, 0).in(Ecom1688Offer::getId, ids));
    }

    /** 价格变动记录（最近的在前） */
    public List<Ecom1688PriceLog> priceLogs(Long offerId) {
        return priceLogMapper.selectList(Wrappers.<Ecom1688PriceLog>lambdaQuery().eq(Ecom1688PriceLog::getOfferId, offerId)
                .orderByDesc(Ecom1688PriceLog::getId));
    }

    /** 待确认的价格变动数（首页待办） */
    public long alertCount() {
        return offerMapper.selectCount(Wrappers.<Ecom1688Offer>lambdaQuery().eq(Ecom1688Offer::getPriceAlert, 1));
    }

    // ---------------------------------------------------------------- 同步

    /**
     * 同步价格与库存：逐个查询 1688 商品详情（不在事务中），每个关联单独保存；价格变动记录日志，超过阈值时提醒
     *
     * @param ids 为空时同步全部（最多 500 个，最久未同步的优先）
     */
    public SyncSummary sync(List<Long> ids) {
        List<Ecom1688Offer> offers = ids == null || ids.isEmpty()
                ? Queries.limit(offerMapper, Wrappers.<Ecom1688Offer>lambdaQuery()
                        .orderByAsc(Ecom1688Offer::getSyncTime).orderByAsc(Ecom1688Offer::getId), SYNC_LIMIT)
                : offerMapper.selectByIds(ids);
        if (offers.isEmpty()) {
            return new SyncSummary(0, 0, 0, 0, List.of());
        }
        Ali1688Context ctx = accountService.context(null);
        int success = 0;
        int changed = 0;
        List<String> messages = new ArrayList<>();
        for (Ecom1688Offer o : offers) {
            try {
                Product p = client.product(ctx, o.getOfferId());
                if (Boolean.TRUE.equals(transactionTemplate.execute(s -> saveSync(o, p)))) {
                    changed++;
                }
                success++;
            } catch (BizException e) {
                String msg = e.getMessage();
                offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate()
                        .set(Ecom1688Offer::getSyncMessage, msg == null ? null : msg.length() > MESSAGE_MAX ? msg.substring(0, MESSAGE_MAX) : msg)
                        .set(Ecom1688Offer::getSyncTime, LocalDateTime.now()).eq(Ecom1688Offer::getId, o.getId()));
                messages.add(o.getOfferId() + ": " + msg);
            }
        }
        return new SyncSummary(offers.size(), success, changed, offers.size() - success, messages);
    }

    /** 保存一次同步结果，返回价格是否变动 */
    Boolean saveSync(Ecom1688Offer old, Product p) {
        Ecom1688Offer o = new Ecom1688Offer();
        applyProduct(o, p, old.getSpecId());
        BigDecimal oldPrice = old.getPrice();
        BigDecimal newPrice = o.getPrice();
        boolean priceChanged = oldPrice != null && newPrice != null && oldPrice.compareTo(newPrice) != 0;
        boolean alert = false;
        BigDecimal rate = null;
        if (priceChanged) {
            rate = oldPrice.signum() == 0 ? null : newPrice.subtract(oldPrice).multiply(Decimals.HUNDRED).divide(oldPrice, 2, RoundingMode.HALF_UP);
            alert = old.getAlertThreshold() == null || rate == null || rate.abs().compareTo(old.getAlertThreshold()) >= 0;
            Ecom1688PriceLog log = new Ecom1688PriceLog();
            log.setOfferId(old.getId());
            log.setMaterialId(old.getMaterialId());
            log.setOldPrice(oldPrice);
            log.setNewPrice(newPrice);
            log.setChangeRate(rate);
            priceLogMapper.insert(log);
        }
        LocalDateTime now = LocalDateTime.now();
        offerMapper.update(Wrappers.<Ecom1688Offer>lambdaUpdate()
                .set(Ecom1688Offer::getTitle, o.getTitle())
                .set(Ecom1688Offer::getSpecText, o.getSpecText())
                .set(Ecom1688Offer::getSkuId, o.getSkuId())
                .set(Ecom1688Offer::getImageUrl, o.getImageUrl())
                .set(Ecom1688Offer::getUnit, o.getUnit())
                .set(Ecom1688Offer::getSellerLoginId, o.getSellerLoginId())
                .set(Ecom1688Offer::getSellerName, o.getSellerName())
                .set(Ecom1688Offer::getPrice, newPrice)
                .set(Ecom1688Offer::getPriceRanges, o.getPriceRanges())
                .set(Ecom1688Offer::getMinOrderQty, o.getMinOrderQty())
                .set(Ecom1688Offer::getStock, o.getStock())
                .set(priceChanged, Ecom1688Offer::getLastPrice, oldPrice)
                .set(priceChanged, Ecom1688Offer::getPriceChangeTime, now)
                .set(alert, Ecom1688Offer::getPriceAlert, 1)
                .set(Ecom1688Offer::getSyncTime, now)
                .set(Ecom1688Offer::getSyncMessage, null)
                .eq(Ecom1688Offer::getId, old.getId()));
        return priceChanged;
    }

    /** 商品详情写入关联：有规格时取规格的价格、库存、属性，否则取起批价与总库存 */
    private void applyProduct(Ecom1688Offer o, Product p, String specId) {
        Sku sku = p.sku(specId);
        if (specId != null && sku == null && p.skus() != null && !p.skus().isEmpty()) {
            throw new BizException("ecom.error.dev.specNotFound", specId);
        }
        o.setTitle(cut(p.title(), 255));
        o.setImageUrl(cut(p.imageUrl(), 500));
        o.setUnit(cut(p.unit(), 16));
        o.setSellerLoginId(cut(p.sellerLoginId(), 64));
        o.setSellerName(cut(p.sellerName(), 128));
        o.setMinOrderQty(p.minOrderQty());
        o.setSpecText(sku == null ? null : cut(sku.specText(), 255));
        o.setSkuId(sku == null ? null : sku.skuId());
        o.setPrice(sku != null && sku.price() != null ? sku.price() : p.basePrice());
        o.setStock(sku != null && sku.stock() != null ? sku.stock() : p.stock());
        try {
            String ranges = p.priceRanges() == null || p.priceRanges().isEmpty() ? null : objectMapper.writeValueAsString(p.priceRanges());
            o.setPriceRanges(ranges != null && ranges.length() > 500 ? null : ranges);
        } catch (JacksonException e) {
            o.setPriceRanges(null);
        }
    }

    private static String cut(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
