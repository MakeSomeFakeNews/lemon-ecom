package com.lemon.erp.modules.ecom.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.publish.connector.CompetitivePrice;
import com.lemon.erp.modules.ecom.publish.connector.PublishCapability;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnector;
import com.lemon.erp.modules.ecom.publish.connector.PublishConnectorRegistry;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomCompetitorPrice;
import com.lemon.erp.modules.ecom.publish.mapper.EcomCompetitorPriceMapper;
import com.lemon.erp.modules.ecom.publish.model.CompetitorSource;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 竞品价格快照：手工录入、Excel 导入（店铺编码 + 平台 SKU）、经平台接口拉取（亚马逊 Product Pricing API），自动调价取每个商品最近的一条
 */
@Service
@RequiredArgsConstructor
public class EcomCompetitorPriceService {

    /** 自动调价只使用这个时间内的快照（小时） */
    public static final int FRESH_HOURS = 72;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EcomCompetitorPriceMapper priceMapper;
    private final EcomListingMapper listingMapper;
    private final EcomStoreService storeService;
    private final PublishConnectorRegistry registry;
    private final EcomPublishSupport support;

    public PageResult<EcomCompetitorPrice> page(long current, long size, Long storeId, Long listingId, String keyword, CompetitorSource source,
                                                boolean latestOnly) {
        Page<EcomCompetitorPrice> page = Page.of(current, size);
        priceMapper.selectPageList(page, storeId, listingId, StringUtils.hasText(keyword) ? keyword.trim() : null, source, latestOnly);
        return PageResult.of(page);
    }

    /** 若干在线商品最近的快照（{@link #FRESH_HOURS} 小时内）：在线商品 ID → 快照 */
    public Map<Long, EcomCompetitorPrice> latest(Collection<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        return priceMapper.selectLatest(listingIds, LocalDateTime.now().minusHours(FRESH_HOURS)).stream()
                .collect(Collectors.toMap(EcomCompetitorPrice::getListingId, Function.identity(), (a, b) -> a));
    }

    /** 手工录入一条快照 */
    public Long create(EcomCompetitorPrice p) {
        EcomListing l = listingMapper.selectById(p.getListingId());
        if (l == null) {
            throw new BizException("ecom.error.listingNotFound");
        }
        fill(p, l, CompetitorSource.MANUAL);
        priceMapper.insert(p);
        return p.getId();
    }

    /** 导入一行（ImportExecutor 逐行调用，任一行出错整批不导入） */
    @Transactional
    public void importRow(PublishRequests.CompetitorImportRow row) {
        EcomStore store = storeService.findByCode(row.getStoreCode().trim());
        if (store == null) {
            throw new BizException("ecom.error.storeCodeNotFound", row.getStoreCode());
        }
        EcomListing l = listingMapper.selectOne(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                .eq(EcomListing::getSellerSku, row.getSellerSku().trim()));
        if (l == null) {
            throw new BizException("ecom.error.pub.listingSkuNotFound", row.getSellerSku());
        }
        EcomCompetitorPrice p = new EcomCompetitorPrice();
        p.setLowestPrice(row.getLowestPrice());
        p.setLowestShipping(row.getLowestShipping());
        p.setBuyboxPrice(row.getBuyboxPrice());
        p.setBuyboxWinner(row.getBuyboxWinner() != null && row.getBuyboxWinner() == 1 ? 1 : 0);
        p.setOfferCount(row.getOfferCount());
        p.setCompetitor(row.getCompetitor());
        p.setRemark(row.getRemark());
        p.setSnapshotTime(parseTime(row.getSnapshotTime()));
        fill(p, l, CompetitorSource.IMPORT);
        priceMapper.insert(p);
    }

    private void fill(EcomCompetitorPrice p, EcomListing l, CompetitorSource source) {
        p.setId(null);
        p.setListingId(l.getId());
        p.setStoreId(l.getStoreId());
        p.setSellerSku(l.getSellerSku());
        p.setPlatformItemId(l.getPlatformItemId());
        p.setCurrency(l.getCurrency());
        p.setSource(source);
        if (p.getBuyboxWinner() == null) {
            p.setBuyboxWinner(0);
        }
        if (p.getSnapshotTime() == null) {
            p.setSnapshotTime(LocalDateTime.now());
        }
        if (p.getLowestPrice() == null && p.getBuyboxPrice() == null) {
            throw new BizException("ecom.error.pub.competitorPriceRequired");
        }
    }

    private static LocalDateTime parseTime(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String v = s.trim();
        try {
            return v.length() <= 10 ? LocalDate.parse(v).atStartOfDay() : LocalDateTime.parse(v.replace('T', ' ').substring(0, 19), TIME);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            throw new BizException("ecom.error.pub.timeInvalid", s);
        }
    }

    public void delete(List<Long> ids) {
        priceMapper.deleteByIds(ids);
    }

    /** 经平台接口拉取竞品价格（在线商品为空时取店铺全部在售商品），保存快照 */
    public PublishViews.FetchResult fetch(PublishRequests.FetchCompetitors req) {
        EcomStore store = storeService.requireEnabled(req.storeId());
        List<EcomListing> listings = req.listingIds() == null || req.listingIds().isEmpty()
                ? listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                .eq(EcomListing::getStatus, ListingStatus.ACTIVE))
                : listingMapper.selectByIds(req.listingIds()).stream().filter(l -> l.getStoreId().equals(store.getId())).toList();
        return fetch(store, listings);
    }

    /** 拉取并保存一个店铺若干在线商品的竞品价格（平台不支持时抛出异常） */
    public PublishViews.FetchResult fetch(EcomStore store, List<EcomListing> listings) {
        PublishConnector c = registry.find(store.getPlatform()).orElse(null);
        if (c == null || !c.supports(PublishCapability.COMPETITIVE_PRICE)) {
            throw new BizException("ecom.error.pub.capabilityUnsupported", store.getPlatform().name(), PublishCapability.COMPETITIVE_PRICE.name());
        }
        if (EcomPublishSupport.isDemoStore(store)) {
            throw new BizException("ecom.error.pub.demoStore", store.getName());
        }
        if (listings.isEmpty()) {
            return new PublishViews.FetchResult(0, 0, null);
        }
        Map<String, EcomListing> bySku = listings.stream().collect(Collectors.toMap(EcomListing::getSellerSku, Function.identity(), (a, b) -> a));
        List<CompetitivePrice> prices = c.competitivePrices(support.prepare(store), listings.stream().map(support::ref).toList());
        LocalDateTime now = LocalDateTime.now();
        Long operator = SecurityUtils.getUserIdOrNull();
        List<EcomCompetitorPrice> rows = new ArrayList<>();
        for (CompetitivePrice cp : prices) {
            EcomListing l = bySku.get(cp.sellerSku());
            if (l == null || (cp.lowestPrice() == null && cp.buyboxPrice() == null)) {
                continue;
            }
            EcomCompetitorPrice p = new EcomCompetitorPrice();
            p.setListingId(l.getId());
            p.setStoreId(store.getId());
            p.setSellerSku(l.getSellerSku());
            p.setPlatformItemId(cp.platformItemId() != null ? cp.platformItemId() : l.getPlatformItemId());
            p.setSource(CompetitorSource.API);
            p.setLowestPrice(cp.lowestPrice());
            p.setLowestShipping(cp.lowestShipping());
            p.setBuyboxPrice(cp.buyboxPrice());
            p.setBuyboxWinner(cp.buyboxWinner() ? 1 : 0);
            p.setOfferCount(cp.offerCount());
            p.setCompetitor(cp.competitor() == null ? null : cp.competitor().length() > 128 ? cp.competitor().substring(0, 128) : cp.competitor());
            p.setCurrency(cp.currency() != null ? cp.currency() : l.getCurrency());
            p.setSnapshotTime(now);
            p.setCreateBy(operator);
            rows.add(p);
        }
        if (!rows.isEmpty()) {
            priceMapper.insertBatch(rows);
        }
        return new PublishViews.FetchResult(listings.size(), rows.size(), null);
    }

    /** 时间格式（导入示例） */
    public static String format(LocalDateTime t) {
        return t.format(TIME);
    }
}
