package com.lemon.erp.modules.ecom.oper.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.org.UserLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.connector.MonitorConnector;
import com.lemon.erp.modules.ecom.oper.connector.MonitorData;
import com.lemon.erp.modules.ecom.oper.dto.AlertQuery;
import com.lemon.erp.modules.ecom.oper.dto.MonitorQuery;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.dto.SnapshotImportRow;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitor;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorAlert;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import com.lemon.erp.modules.ecom.oper.mapper.EcomMonitorAlertMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomMonitorMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomMonitorSnapshotMapper;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorSource;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorType;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ASIN / 商品监控：维护监控对象（自己的商品、竞品），每天一条快照（平台接口自动取数，或手工录入 / Excel 导入），
 * 与上一次快照比较生成预警（{@link MonitorAlertRules}），新预警发站内消息给通知人（为空时发给店铺负责人 / 创建人）。
 * 竞品不属于店铺（店铺为空时所有电商用户可见）；接口取数用监控指定的店铺授权，没有指定时取同平台同站点任一已授权的店铺
 */
@Slf4j
@Service
public class EcomMonitorService {

    static final String ALERT_PERM = "ecom:monitor:alert";
    static final BigDecimal DEFAULT_RANK_DROP = BigDecimal.valueOf(30);
    static final BigDecimal DEFAULT_RATING_DROP = new BigDecimal("0.1");
    /** 只对最近几天的快照发送预警消息（导入历史数据时不提醒） */
    private static final int NOTIFY_DAYS = 2;
    private static final int TREND_MAX_DAYS = 366;
    private static final int RECENT_ALERTS = 30;

    private final EcomMonitorMapper monitorMapper;
    private final EcomMonitorSnapshotMapper snapshotMapper;
    private final EcomMonitorAlertMapper alertMapper;
    private final EcomStoreService storeService;
    private final EcomListingService listingService;
    private final EcomSyncService syncService;
    private final ConnectorRegistry channelRegistry;
    private final DictLookup dictLookup;
    private final UserLookup userLookup;
    private final MessageSender messageSender;
    private final TransactionTemplate transactionTemplate;
    private final Map<EcomPlatform, MonitorConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public EcomMonitorService(EcomMonitorMapper monitorMapper, EcomMonitorSnapshotMapper snapshotMapper, EcomMonitorAlertMapper alertMapper,
                              EcomStoreService storeService, EcomListingService listingService, EcomSyncService syncService,
                              ConnectorRegistry channelRegistry, DictLookup dictLookup, UserLookup userLookup, MessageSender messageSender,
                              TransactionTemplate transactionTemplate, List<MonitorConnector> monitorConnectors) {
        this.monitorMapper = monitorMapper;
        this.snapshotMapper = snapshotMapper;
        this.alertMapper = alertMapper;
        this.storeService = storeService;
        this.listingService = listingService;
        this.syncService = syncService;
        this.channelRegistry = channelRegistry;
        this.dictLookup = dictLookup;
        this.userLookup = userLookup;
        this.messageSender = messageSender;
        this.transactionTemplate = transactionTemplate;
        monitorConnectors.forEach(c -> connectors.put(c.platform(), c));
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomMonitor> page(long current, long size, MonitorQuery q) {
        Page<EcomMonitor> page = Page.of(current, size);
        monitorMapper.selectPageList(page, q);
        enrich(page.getRecords());
        return PageResult.of(page);
    }

    /** 未处理预警数、较上一次快照的变化 */
    private void enrich(List<EcomMonitor> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(EcomMonitor::getId).toList();
        Map<Long, Integer> alerts = new HashMap<>();
        alertMapper.countNewByMonitor(ids).forEach(r -> alerts.put(((Number) r.get("monitorId")).longValue(), ((Number) r.get("cnt")).intValue()));
        Map<Long, EcomMonitorSnapshot> prev = monitorMapper.selectPreviousSnapshots(ids).stream()
                .collect(Collectors.toMap(EcomMonitorSnapshot::getMonitorId, Function.identity(), (a, b) -> a));
        for (EcomMonitor m : rows) {
            m.setNewAlerts(alerts.getOrDefault(m.getId(), 0));
            EcomMonitorSnapshot p = prev.get(m.getId());
            if (p != null) {
                m.setRankChange(p.getRankMain() != null && m.getRankMain() != null ? p.getRankMain() - m.getRankMain() : null);
                m.setRatingChange(p.getRating() != null && m.getRating() != null ? m.getRating().subtract(p.getRating()) : null);
                m.setReviewChange(p.getReviewCount() != null && m.getReviewCount() != null ? m.getReviewCount() - p.getReviewCount() : null);
            }
        }
    }

    public EcomMonitor detail(Long id) {
        EcomMonitor m = require(id);
        enrich(List.of(m));
        EcomMonitorSnapshot last = Queries.first(snapshotMapper, Wrappers.<EcomMonitorSnapshot>lambdaQuery()
                .eq(EcomMonitorSnapshot::getMonitorId, id).isNotNull(EcomMonitorSnapshot::getBullets).orderByDesc(EcomMonitorSnapshot::getSnapshotDate));
        m.setBullets(last == null ? null : last.getBullets());
        return m;
    }

    /** 监控（店铺不可见时视为不存在） */
    public EcomMonitor require(Long id) {
        return EcomStoreScope.require(() -> monitorMapper.selectDetail(id), "ecom.error.oper.monitorNotFound");
    }

    /** 趋势：最近 days 天的快照（升序）与最近的预警 */
    public OperViews.MonitorTrend trend(Long id, int days) {
        require(id);
        int d = Math.max(1, Math.min(days, TREND_MAX_DAYS));
        LocalDate today = LocalDate.now();
        List<EcomMonitorSnapshot> snapshots = snapshotMapper.selectTrend(id, today.minusDays(d - 1L), today);
        AlertQuery q = new AlertQuery();
        q.setMonitorId(id);
        Page<EcomMonitorAlert> page = Page.of(1, RECENT_ALERTS);
        page.setSearchCount(false);
        alertMapper.selectPageList(page, q);
        return new OperViews.MonitorTrend(snapshots, page.getRecords());
    }

    /** 列表一次最多取多少个监控的迷你趋势（与分页单页上限一致） */
    private static final int SPARKLINE_MAX_IDS = 500;

    /** 列表迷你趋势：一次查询多个监控最近 days 天（默认 30）的排名 / 评分，店铺不可见的监控不返回 */
    public List<OperViews.MonitorSparkline> sparklines(List<Long> ids, int days) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > SPARKLINE_MAX_IDS) {
            throw new BizException("ecom.error.oper.tooManyIds", SPARKLINE_MAX_IDS);
        }
        int d = Math.max(1, Math.min(days, TREND_MAX_DAYS));
        LocalDate today = LocalDate.now();
        Map<Long, List<EcomMonitorSnapshot>> byMonitor = new LinkedHashMap<>();
        snapshotMapper.selectTrends(ids, today.minusDays(d - 1L), today)
                .forEach(x -> byMonitor.computeIfAbsent(x.getMonitorId(), k -> new ArrayList<>()).add(x));
        return byMonitor.entrySet().stream().map(e -> {
            List<EcomMonitorSnapshot> list = e.getValue();
            return new OperViews.MonitorSparkline(e.getKey(),
                    list.stream().map(EcomMonitorSnapshot::getSnapshotDate).toList(),
                    list.stream().map(EcomMonitorSnapshot::getRankMain).toList(),
                    list.stream().map(EcomMonitorSnapshot::getRankSub).toList(),
                    list.stream().map(EcomMonitorSnapshot::getRating).toList(),
                    list.stream().map(EcomMonitorSnapshot::getReviewCount).toList());
        }).toList();
    }

    // ---------------------------------------------------------------- 维护

    @Transactional
    public Long create(EcomMonitor m) {
        m.setId(null);
        normalize(m, null);
        m.setCreateBy(SecurityUtils.getUserIdOrNull());
        monitorMapper.insert(m);
        return m.getId();
    }

    /** 修改监控设置（平台数据——标题、排名等——由快照维护，不在这里修改） */
    @Transactional
    public void update(Long id, EcomMonitor m) {
        EcomMonitor old = require(id);
        normalize(m, id);
        EcomMonitor u = new EcomMonitor();
        u.setId(id);
        u.setMonitorType(m.getMonitorType());
        u.setPlatform(m.getPlatform());
        u.setMarketplace(m.getMarketplace());
        u.setItemId(m.getItemId());
        u.setStoreId(m.getStoreId());
        u.setListingId(m.getListingId());
        u.setSellerSku(m.getSellerSku());
        u.setBenchmarkId(m.getBenchmarkId());
        u.setBrand(m.getBrand());
        u.setAutoFetch(m.getAutoFetch());
        u.setRankDropPercent(m.getRankDropPercent());
        u.setRatingDrop(m.getRatingDrop());
        u.setNotifyUserId(m.getNotifyUserId());
        u.setRemark(m.getRemark());
        monitorMapper.updateById(u);
        // 允许清空的字段
        monitorMapper.update(Wrappers.<EcomMonitor>lambdaUpdate().set(EcomMonitor::getStoreId, m.getStoreId())
                .set(EcomMonitor::getListingId, m.getListingId()).set(EcomMonitor::getSellerSku, m.getSellerSku())
                .set(EcomMonitor::getBenchmarkId, m.getBenchmarkId()).set(EcomMonitor::getNotifyUserId, m.getNotifyUserId())
                .set(EcomMonitor::getRemark, m.getRemark()).eq(EcomMonitor::getId, id));
        if (!java.util.Objects.equals(old.getStoreId(), m.getStoreId())) {
            alertMapper.update(Wrappers.<EcomMonitorAlert>lambdaUpdate().set(EcomMonitorAlert::getStoreId, m.getStoreId())
                    .eq(EcomMonitorAlert::getMonitorId, id));
        }
    }

    private void normalize(EcomMonitor m, Long id) {
        m.setItemId(m.getItemId().trim());
        if (m.getPlatform() == EcomPlatform.AMAZON) {
            m.setItemId(m.getItemId().toUpperCase(Locale.ROOT));
        }
        m.setMarketplace(m.getMarketplace().trim());
        dictLookup.check(EcomConstants.MARKETPLACE_DICT, m.getMarketplace(), "ecom.label.marketplace");
        if (m.getStoreId() != null) {
            EcomStore store = storeService.get(m.getStoreId());
            if (store.getPlatform() != m.getPlatform()) {
                throw new BizException("ecom.error.oper.monitorStorePlatform", store.getName());
            }
        } else if (m.getMonitorType() == EcomMonitorType.OWN) {
            throw new BizException("ecom.error.oper.monitorStoreRequired");
        }
        if (m.getListingId() != null) {
            EcomListing l = listingService.get(m.getListingId());
            if (!l.getStoreId().equals(m.getStoreId())) {
                throw new BizException("ecom.error.oper.monitorListingStore");
            }
            m.setSellerSku(l.getSellerSku());
        }
        m.setSellerSku(StringUtils.hasText(m.getSellerSku()) ? m.getSellerSku().trim() : null);
        if (m.getMonitorType() == EcomMonitorType.OWN) {
            m.setBenchmarkId(null);
        } else if (m.getBenchmarkId() != null) {
            EcomMonitor b = require(m.getBenchmarkId());
            if (b.getMonitorType() != EcomMonitorType.OWN) {
                throw new BizException("ecom.error.oper.benchmarkNotOwn");
            }
        }
        if (m.getNotifyUserId() != null) {
            userLookup.requireActive(m.getNotifyUserId(), "ecom.error.oper.userNotFound");
        }
        if (m.getRankDropPercent() == null) m.setRankDropPercent(DEFAULT_RANK_DROP);
        if (m.getRatingDrop() == null) m.setRatingDrop(DEFAULT_RATING_DROP);
        if (m.getAutoFetch() == null) m.setAutoFetch(1);
        if (m.getStatus() == null) m.setStatus(EnableStatus.ENABLED);
        boolean exists = EcomStoreScope.unrestricted(() -> monitorMapper.exists(Wrappers.<EcomMonitor>lambdaQuery()
                .eq(EcomMonitor::getPlatform, m.getPlatform()).eq(EcomMonitor::getMarketplace, m.getMarketplace())
                .eq(EcomMonitor::getItemId, m.getItemId()).ne(id != null, EcomMonitor::getId, id)));
        if (exists) {
            throw new BizException("ecom.error.oper.monitorExists", m.getItemId());
        }
    }

    public void changeStatus(Long id, EnableStatus status) {
        require(id);
        monitorMapper.update(Wrappers.<EcomMonitor>lambdaUpdate().set(EcomMonitor::getStatus, status).eq(EcomMonitor::getId, id));
    }

    /** 删除监控及其快照、预警 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            require(id);
            snapshotMapper.delete(Wrappers.<EcomMonitorSnapshot>lambdaQuery().eq(EcomMonitorSnapshot::getMonitorId, id));
            alertMapper.delete(Wrappers.<EcomMonitorAlert>lambdaQuery().eq(EcomMonitorAlert::getMonitorId, id));
            monitorMapper.update(Wrappers.<EcomMonitor>lambdaUpdate().set(EcomMonitor::getBenchmarkId, null).eq(EcomMonitor::getBenchmarkId, id));
            monitorMapper.deleteById(id);
        }
    }

    // ---------------------------------------------------------------- 快照

    /** 手工录入一天的快照 */
    @Transactional
    public List<EcomMonitorAlert> record(Long id, OperRequests.SnapshotRequest req) {
        EcomMonitor m = require(id);
        EcomMonitorSnapshot s = new EcomMonitorSnapshot();
        s.setSnapshotDate(req.snapshotDate());
        s.setCategory(blankToNull(req.category()));
        s.setRankMain(req.rankMain());
        s.setSubCategory(blankToNull(req.subCategory()));
        s.setRankSub(req.rankSub());
        s.setRating(req.rating());
        s.setReviewCount(req.reviewCount());
        s.setSellerCount(req.sellerCount());
        s.setPrice(req.price());
        s.setTitle(blankToNull(req.title()));
        s.setMainImage(blankToNull(req.mainImage()));
        s.setBullets(blankToNull(req.bullets()));
        s.setListingStatus(req.listingStatus());
        s.setSource(EcomMonitorSource.MANUAL);
        return saveSnapshot(m, s);
    }

    /** 导入快照（监控按 平台 + 站点 + 商品 ID 定位，必须已建立） */
    @Transactional
    public void importRow(SnapshotImportRow row) {
        EcomPlatform platform;
        try {
            platform = EcomPlatform.valueOf(row.getPlatform().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BizException("ecom.error.oper.platformInvalid", row.getPlatform());
        }
        String itemId = row.getItemId().trim();
        EcomMonitor m = monitorMapper.selectOne(Wrappers.<EcomMonitor>lambdaQuery().eq(EcomMonitor::getPlatform, platform)
                .eq(EcomMonitor::getMarketplace, row.getMarketplace().trim())
                .eq(EcomMonitor::getItemId, platform == EcomPlatform.AMAZON ? itemId.toUpperCase(Locale.ROOT) : itemId));
        if (m == null) {
            throw new BizException("ecom.error.oper.monitorNotFoundFor", itemId);
        }
        EcomMonitorSnapshot s = new EcomMonitorSnapshot();
        s.setSnapshotDate(row.getSnapshotDate());
        s.setCategory(blankToNull(row.getCategory()));
        s.setRankMain(row.getRankMain());
        s.setSubCategory(blankToNull(row.getSubCategory()));
        s.setRankSub(row.getRankSub());
        s.setRating(row.getRating());
        s.setReviewCount(row.getReviewCount());
        s.setSellerCount(row.getSellerCount());
        s.setPrice(row.getPrice());
        s.setTitle(blankToNull(row.getTitle()));
        s.setMainImage(blankToNull(row.getMainImage()));
        s.setListingStatus(parseStatus(row.getListingStatus()));
        s.setSource(EcomMonitorSource.IMPORT);
        saveSnapshot(m, s);
    }

    static ListingStatus parseStatus(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String v = s.trim();
        if ("在售".equals(v)) { // i18n-ignore 导入文件中的中文取值
            return ListingStatus.ACTIVE;
        }
        if ("下架".equals(v)) { // i18n-ignore 导入文件中的中文取值
            return ListingStatus.INACTIVE;
        }
        try {
            ListingStatus st = ListingStatus.valueOf(v.toUpperCase(Locale.ROOT));
            return st == ListingStatus.ACTIVE ? ListingStatus.ACTIVE : ListingStatus.INACTIVE;
        } catch (IllegalArgumentException e) {
            throw new BizException("ecom.error.oper.listingStatusInvalid", s);
        }
    }

    /**
     * 保存快照（同一天覆盖），与上一次快照比较生成预警；是最新一天时更新监控的最新数据（没有取到的指标保留原值），
     * 最近两天的新预警发站内消息。返回本次新增的预警
     */
    List<EcomMonitorAlert> saveSnapshot(EcomMonitor m, EcomMonitorSnapshot s) {
        s.setMonitorId(m.getId());
        snapshotMapper.delete(Wrappers.<EcomMonitorSnapshot>lambdaQuery().eq(EcomMonitorSnapshot::getMonitorId, m.getId())
                .eq(EcomMonitorSnapshot::getSnapshotDate, s.getSnapshotDate()));
        snapshotMapper.insert(s);
        EcomMonitorSnapshot prev = Queries.first(snapshotMapper, Wrappers.<EcomMonitorSnapshot>lambdaQuery()
                .eq(EcomMonitorSnapshot::getMonitorId, m.getId()).lt(EcomMonitorSnapshot::getSnapshotDate, s.getSnapshotDate())
                .orderByDesc(EcomMonitorSnapshot::getSnapshotDate));
        List<EcomMonitorAlert> created = saveAlerts(m, s.getSnapshotDate(),
                MonitorAlertRules.evaluate(prev, s, m.getRankDropPercent(), m.getRatingDrop()));
        boolean latest = m.getLastSnapshotDate() == null || !s.getSnapshotDate().isBefore(m.getLastSnapshotDate());
        if (latest) {
            applyLatest(m, s);
            if (!created.isEmpty() && !s.getSnapshotDate().isBefore(LocalDate.now().minusDays(NOTIFY_DAYS))) {
                notify(m, created);
            }
        }
        return created;
    }

    /**
     * 批量写入历史快照（按日期升序，已有日期的跳过）：逐日与前一天比较生成预警，最后用最新一天更新监控；不发送预警消息。
     * 用于演示数据与大批量历史导入，返回生成的预警
     */
    @Transactional
    public List<EcomMonitorAlert> saveHistory(Long monitorId, List<EcomMonitorSnapshot> history) {
        EcomMonitor m = require(monitorId);
        Set<LocalDate> existing = snapshotMapper.selectList(Wrappers.<EcomMonitorSnapshot>lambdaQuery().select(EcomMonitorSnapshot::getSnapshotDate)
                .eq(EcomMonitorSnapshot::getMonitorId, monitorId)).stream().map(EcomMonitorSnapshot::getSnapshotDate).collect(Collectors.toSet());
        List<EcomMonitorSnapshot> list = history.stream().filter(s -> !existing.contains(s.getSnapshotDate()))
                .sorted(java.util.Comparator.comparing(EcomMonitorSnapshot::getSnapshotDate)).toList();
        if (list.isEmpty()) {
            return List.of();
        }
        list.forEach(s -> s.setMonitorId(monitorId));
        for (int i = 0; i < list.size(); i += 200) {
            snapshotMapper.insertBatch(list.subList(i, Math.min(list.size(), i + 200)));
        }
        EcomMonitorSnapshot prev = Queries.first(snapshotMapper, Wrappers.<EcomMonitorSnapshot>lambdaQuery()
                .eq(EcomMonitorSnapshot::getMonitorId, monitorId).lt(EcomMonitorSnapshot::getSnapshotDate, list.get(0).getSnapshotDate())
                .orderByDesc(EcomMonitorSnapshot::getSnapshotDate));
        List<EcomMonitorAlert> created = new ArrayList<>();
        for (EcomMonitorSnapshot s : list) {
            created.addAll(saveAlerts(m, s.getSnapshotDate(), MonitorAlertRules.evaluate(prev, s, m.getRankDropPercent(), m.getRatingDrop())));
            prev = s;
        }
        EcomMonitorSnapshot last = list.get(list.size() - 1);
        if (m.getLastSnapshotDate() == null || !last.getSnapshotDate().isBefore(m.getLastSnapshotDate())) {
            applyLatest(m, last);
        }
        return created;
    }

    /** 写入预警：同一天重新取数时，未处理的重新生成，已处理 / 已忽略的同类型不再生成 */
    private List<EcomMonitorAlert> saveAlerts(EcomMonitor m, LocalDate date, List<MonitorAlertRules.Hit> hits) {
        alertMapper.delete(Wrappers.<EcomMonitorAlert>lambdaQuery().eq(EcomMonitorAlert::getMonitorId, m.getId())
                .eq(EcomMonitorAlert::getAlertDate, date).eq(EcomMonitorAlert::getStatus, EcomMonitorAlertStatus.NEW));
        Set<EcomMonitorAlertType> handled = alertMapper.selectList(Wrappers.<EcomMonitorAlert>lambdaQuery()
                        .select(EcomMonitorAlert::getAlertType).eq(EcomMonitorAlert::getMonitorId, m.getId()).eq(EcomMonitorAlert::getAlertDate, date))
                .stream().map(EcomMonitorAlert::getAlertType).collect(Collectors.toSet());
        List<EcomMonitorAlert> out = new ArrayList<>();
        for (MonitorAlertRules.Hit h : hits) {
            if (handled.contains(h.type())) {
                continue;
            }
            EcomMonitorAlert a = new EcomMonitorAlert();
            a.setMonitorId(m.getId());
            a.setStoreId(m.getStoreId());
            a.setAlertDate(date);
            a.setAlertType(h.type());
            a.setOldValue(h.oldValue());
            a.setNewValue(h.newValue());
            a.setStatus(EcomMonitorAlertStatus.NEW);
            alertMapper.insert(a);
            out.add(a);
        }
        return out;
    }

    private void applyLatest(EcomMonitor m, EcomMonitorSnapshot s) {
        var u = Wrappers.<EcomMonitor>lambdaUpdate().set(EcomMonitor::getLastSnapshotDate, s.getSnapshotDate())
                .set(s.getCategory() != null, EcomMonitor::getCategory, s.getCategory())
                .set(s.getRankMain() != null, EcomMonitor::getRankMain, s.getRankMain())
                .set(s.getSubCategory() != null, EcomMonitor::getSubCategory, s.getSubCategory())
                .set(s.getRankSub() != null, EcomMonitor::getRankSub, s.getRankSub())
                .set(s.getRating() != null, EcomMonitor::getRating, s.getRating())
                .set(s.getReviewCount() != null, EcomMonitor::getReviewCount, s.getReviewCount())
                .set(s.getSellerCount() != null, EcomMonitor::getSellerCount, s.getSellerCount())
                .set(s.getPrice() != null, EcomMonitor::getPrice, s.getPrice())
                .set(s.getTitle() != null, EcomMonitor::getTitle, s.getTitle())
                .set(s.getMainImage() != null, EcomMonitor::getMainImage, s.getMainImage())
                .set(s.getListingStatus() != null, EcomMonitor::getListingStatus, s.getListingStatus())
                .eq(EcomMonitor::getId, m.getId());
        monitorMapper.update(u);
        m.setLastSnapshotDate(s.getSnapshotDate());
    }

    /** 预警消息：通知人 &gt; 店铺负责人 &gt; 创建人，都没有时发给有处理预警权限的用户；同一监控同一天只提醒一次 */
    private void notify(EcomMonitor m, List<EcomMonitorAlert> alerts) {
        Long to = m.getNotifyUserId();
        if (to == null && m.getStoreId() != null) {
            to = storeService.get(m.getStoreId()).getOwnerId();
        }
        if (to == null) {
            to = m.getCreateBy();
        }
        Map<EcomMonitorAlertType, String> names = new LinkedHashMap<>();
        alerts.forEach(a -> names.put(a.getAlertType(), ALERT_NAMES.get(a.getAlertType())));
        String title = "商品监控预警：" + m.getItemId() + " " + String.join("、", names.values()); // i18n-ignore 站内消息存库
        String body = (m.getMonitorType() == EcomMonitorType.COMPETITOR ? "竞品 " : "") // i18n-ignore 站内消息存库
                + m.getPlatform().name() + " " + m.getMarketplace() + " " + m.getItemId()
                + (m.getTitle() == null ? "" : " " + MonitorAlertRules.cut(m.getTitle()));
        String link = "/ecom/asin-monitor?tab=alert&monitorId=" + m.getId();
        MessageSender.Message message = MessageSender.Message.of(MessageCategory.ALERT, title, body, link)
                .dedup("ecom-monitor:" + m.getId() + ":" + alerts.get(0).getAlertDate());
        if (to != null) {
            messageSender.send(List.of(to), message);
        } else {
            messageSender.sendToPermission(ALERT_PERM, message);
        }
    }

    /** 预警类型的中文名（站内消息存库） */
    private static final Map<EcomMonitorAlertType, String> ALERT_NAMES = Map.of(
            EcomMonitorAlertType.RANK_DROP, "排名大跌", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.RATING_DROP, "评分下降", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.NEW_SELLER, "新增跟卖", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.TITLE_CHANGED, "标题被改", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.IMAGE_CHANGED, "主图被改", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.BULLETS_CHANGED, "五点被改", // i18n-ignore 站内消息存库
            EcomMonitorAlertType.DELISTED, "被下架"); // i18n-ignore 站内消息存库

    // ---------------------------------------------------------------- 接口取数

    /** 平台是否支持接口取数 */
    public boolean supportsFetch(EcomPlatform platform) {
        return connectors.containsKey(platform);
    }

    /** 已实现接口取数的平台 */
    public List<EcomPlatform> fetchPlatforms() {
        return List.copyOf(connectors.keySet());
    }

    /** 手工对一个或多个监控接口取数（不支持的平台、没有授权店铺的记为失败） */
    public OperViews.FetchOutcome fetch(List<Long> ids) {
        List<EcomMonitor> list = ids.stream().map(this::require).toList();
        return fetchList(list);
    }

    /** 定时任务：全部监控中、开启自动取数、平台支持的监控 */
    public OperViews.FetchOutcome fetchAll() {
        List<EcomMonitor> list = monitorMapper.selectList(Wrappers.<EcomMonitor>lambdaQuery().eq(EcomMonitor::getStatus, EnableStatus.ENABLED)
                .eq(EcomMonitor::getAutoFetch, 1).in(EcomMonitor::getPlatform, connectors.isEmpty() ? List.of(EcomPlatform.OTHER) : connectors.keySet()));
        return fetchList(list);
    }

    private OperViews.FetchOutcome fetchList(List<EcomMonitor> list) {
        int ok = 0;
        int failed = 0;
        int alerts = 0;
        List<String> errors = new ArrayList<>();
        Map<String, Optional<EcomStore>> storeCache = new HashMap<>();
        for (EcomMonitor m : list) {
            try {
                Integer n = transactionTemplate.execute(st -> fetchOne(m, storeCache).size());
                alerts += n == null ? 0 : n;
                ok++;
            } catch (RuntimeException e) {
                failed++;
                String msg = e instanceof BizException ? e.getMessage() : String.valueOf(e);
                errors.add(m.getItemId() + ": " + msg);
                log.warn("商品监控取数失败 {}: {}", m.getItemId(), msg); // i18n-ignore 日志
                transactionTemplate.executeWithoutResult(st -> monitorMapper.update(Wrappers.<EcomMonitor>lambdaUpdate()
                        .set(EcomMonitor::getLastFetchTime, LocalDateTime.now()).set(EcomMonitor::getLastFetchMessage, MonitorAlertRules.cut(msg))
                        .eq(EcomMonitor::getId, m.getId())));
            }
        }
        return new OperViews.FetchOutcome(list.size(), ok, failed, alerts, errors.isEmpty() ? null : String.join("; ", errors.subList(0, Math.min(10, errors.size()))));
    }

    private List<EcomMonitorAlert> fetchOne(EcomMonitor m, Map<String, Optional<EcomStore>> storeCache) {
        MonitorConnector connector = connectors.get(m.getPlatform());
        if (connector == null) {
            throw new BizException("ecom.error.oper.fetchNotSupported", m.getPlatform().name());
        }
        EcomStore store = credentialStore(m, storeCache);
        if (store == null) {
            throw new BizException("ecom.error.oper.fetchNoStore", m.getPlatform().name(), m.getMarketplace());
        }
        ConnectorContext ctx = syncService.requireContext(store);
        channelRegistry.find(store.getPlatform()).ifPresent(c -> c.ensureToken(ctx));
        MonitorData d = connector.fetch(ctx, m.getItemId());
        EcomMonitorSnapshot s = new EcomMonitorSnapshot();
        s.setSnapshotDate(LocalDate.now());
        s.setCategory(d.category());
        s.setRankMain(d.rankMain());
        s.setSubCategory(d.subCategory());
        s.setRankSub(d.rankSub());
        s.setRating(d.rating());
        s.setReviewCount(d.reviewCount());
        s.setSellerCount(d.sellerCount());
        s.setPrice(d.price());
        s.setTitle(MonitorAlertRules.cut(d.title()));
        s.setMainImage(MonitorAlertRules.cut(d.mainImage()));
        s.setBullets(d.bullets() == null || d.bullets().length() <= 4000 ? d.bullets() : d.bullets().substring(0, 4000));
        s.setListingStatus(d.listingStatus());
        s.setSource(EcomMonitorSource.API);
        // 当天已有手工录入的评分 / 评论数（接口取不到）时保留
        EcomMonitorSnapshot same = snapshotMapper.selectOne(Wrappers.<EcomMonitorSnapshot>lambdaQuery()
                .eq(EcomMonitorSnapshot::getMonitorId, m.getId()).eq(EcomMonitorSnapshot::getSnapshotDate, s.getSnapshotDate()));
        if (same != null) {
            if (s.getRating() == null) s.setRating(same.getRating());
            if (s.getReviewCount() == null) s.setReviewCount(same.getReviewCount());
        }
        List<EcomMonitorAlert> created = saveSnapshot(m, s);
        monitorMapper.update(Wrappers.<EcomMonitor>lambdaUpdate().set(EcomMonitor::getLastFetchTime, LocalDateTime.now())
                .set(EcomMonitor::getLastFetchMessage, null)
                .set(d.brand() != null && m.getBrand() == null, EcomMonitor::getBrand, d.brand())
                .set(d.currency() != null, EcomMonitor::getCurrency, d.currency())
                .eq(EcomMonitor::getId, m.getId()));
        return created;
    }

    /** 取数用的店铺授权：监控指定的店铺（有授权、同平台），否则同平台同站点任一有授权的启用店铺 */
    private EcomStore credentialStore(EcomMonitor m, Map<String, Optional<EcomStore>> cache) {
        if (m.getStoreId() != null) {
            EcomStore s = storeService.get(m.getStoreId());
            if (s.getPlatform() == m.getPlatform() && StringUtils.hasText(s.getCredentials())) {
                return s;
            }
        }
        return cache.computeIfAbsent(m.getPlatform() + "|" + m.getMarketplace(), k -> storeService.syncable().stream()
                .filter(s -> s.getPlatform() == m.getPlatform() && m.getMarketplace().equalsIgnoreCase(s.getMarketplace())).findFirst()).orElse(null);
    }

    // ---------------------------------------------------------------- 预警

    public PageResult<EcomMonitorAlert> alerts(long current, long size, AlertQuery q) {
        Page<EcomMonitorAlert> page = Page.of(current, size);
        alertMapper.selectPageList(page, q);
        return PageResult.of(page);
    }

    /** 未处理预警数（按类型），条件同列表（不含状态） */
    public List<OperViews.AlertCount> alertCounts(AlertQuery q) {
        return alertMapper.countNewByType(q).stream()
                .map(r -> new OperViews.AlertCount(String.valueOf(r.get("alertType")), ((Number) r.get("cnt")).longValue())).toList();
    }

    /** 处理 / 忽略预警 */
    @Transactional
    public void handle(OperRequests.AlertHandleRequest req) {
        if (req.status() == EcomMonitorAlertStatus.NEW) {
            throw new BizException("ecom.error.oper.alertStatusInvalid");
        }
        for (Long id : req.ids()) {
            EcomMonitorAlert a = alertMapper.selectById(id);
            if (a == null) {
                throw new BizException("ecom.error.oper.alertNotFound");
            }
            a.setStatus(req.status());
            a.setHandlerId(SecurityUtils.getUserIdOrNull());
            a.setHandleTime(LocalDateTime.now());
            a.setHandleNote(blankToNull(req.note()));
            alertMapper.updateById(a);
        }
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
