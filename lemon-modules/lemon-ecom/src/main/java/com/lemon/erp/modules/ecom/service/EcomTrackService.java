package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.carrier.CarrierRegistry;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackEvent;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackInfo;
import com.lemon.erp.modules.ecom.carrier.SeventeenTrackClient;
import com.lemon.erp.modules.ecom.carrier.TrackStatusNormalizer;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomOrderSetting;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageTrack;
import com.lemon.erp.modules.ecom.mapper.EcomOrderEnhanceMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageTrackMapper;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.EcomTrackAlert;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import com.lemon.erp.modules.ecom.model.TrackSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流跟踪：拉取已发货包裹的轨迹（先用包裹对接的物流商接口，没有对接或查不到时用 17TRACK），状态归一（{@link TrackStatusNormalizer}），
 * 保存时间线并按订单作业设置的时限判断预警：
 * <ul>
 *   <li>超时未上网：发货后超过设定小时数仍没有轨迹</li>
 *   <li>运输超时：上网后超过设定天数未签收，或轨迹超过设定天数没有更新</li>
 *   <li>轨迹异常：最新状态为异常或退回</li>
 * </ul>
 * 定时任务（{@link EcomTrackJob}）每 2 小时拉取发货 60 天内、未签收 / 退回、4 小时内没有拉取过的包裹；接口调用在事务之外，每个包裹一个事务，
 * 一个失败不影响其他。没有轨迹来源（未对接物流商且没有 17TRACK 密钥）的包裹也按发货时间判断「超时未上网」
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomTrackService {

    /** 定时任务每次最多拉取的包裹数 */
    static final int BATCH_LIMIT = 200;
    /** 同一包裹两次拉取的最短间隔（小时） */
    static final int PULL_INTERVAL_HOURS = 4;
    /** 只跟踪发货多少天内的包裹 */
    static final int TRACK_DAYS = 60;
    private static final int TEXT_MAX = 500;

    private final EcomPackageMapper packageMapper;
    private final EcomPackageTrackMapper trackMapper;
    private final EcomOrderEnhanceMapper enhanceMapper;
    private final EcomLogisticsService logisticsService;
    private final CarrierRegistry carrierRegistry;
    private final SeventeenTrackClient seventeenTrack;
    private final EcomOrderSettingService settingService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomPackage> page(long current, long size, OrderEnhanceViews.TrackQuery query) {
        Page<EcomPackage> page = Page.of(current, size);
        enhanceMapper.selectTrackPage(page, query);
        return PageResult.of(page);
    }

    /** 各轨迹状态、预警的包裹数：{ "S:IN_TRANSIT": n, "A:NO_SCAN": n } */
    public Map<String, Long> counts(OrderEnhanceViews.TrackQuery query) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : enhanceMapper.countTrack(query)) {
            out.put(String.valueOf(row.get("k")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    /** 包裹的轨迹时间线 */
    public OrderEnhanceViews.PackageTracks tracks(Long packageId) {
        EcomPackage p = packageMapper.selectDetail(packageId);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        p.setLabelFile(null);
        return new OrderEnhanceViews.PackageTracks(p, events(packageId));
    }

    /** 订单已发货包裹的轨迹（订单详情） */
    public List<OrderEnhanceViews.PackageTracks> orderTracks(Long orderId) {
        List<OrderEnhanceViews.PackageTracks> out = new ArrayList<>();
        for (EcomPackage p : packageMapper.selectByOrderId(orderId)) {
            if (p.getStatus() == EcomPackageStatus.SHIPPED) {
                out.add(new OrderEnhanceViews.PackageTracks(p, events(p.getId())));
            }
        }
        return out;
    }

    private List<EcomPackageTrack> events(Long packageId) {
        return trackMapper.selectList(Wrappers.<EcomPackageTrack>lambdaQuery().eq(EcomPackageTrack::getPackageId, packageId)
                .orderByAsc(EcomPackageTrack::getSort).orderByAsc(EcomPackageTrack::getId));
    }

    // ---------------------------------------------------------------- 拉取

    /** 手工拉取一个包裹（失败时提示原因） */
    public OrderEnhanceViews.PackageTracks pull(Long packageId) {
        pullOne(packageId, true);
        return tracks(packageId);
    }

    /** 批量拉取：返回 { 包裹号: 失败原因 } */
    public Map<String, String> pull(List<Long> ids) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Long id : ids.stream().distinct().toList()) {
            try {
                pullOne(id, true);
            } catch (BizException e) {
                EcomPackage p = packageMapper.selectById(id);
                out.put(p == null ? String.valueOf(id) : p.getPackageNo(), e.getMessage());
            }
        }
        return out;
    }

    /**
     * 定时拉取（当前租户）：设置中关闭了定时拉取时跳过
     *
     * @return [拉取的包裹数, 失败数]
     */
    public int[] pullDue() {
        EcomOrderSetting s = settingService.current();
        if (s.getTrackEnabled() != null && s.getTrackEnabled() == 0) {
            return new int[]{0, 0};
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = enhanceMapper.selectDueTrackIds(now.minusHours(PULL_INTERVAL_HOURS), now.minusDays(TRACK_DAYS), BATCH_LIMIT);
        int failed = 0;
        for (Long id : ids) {
            try {
                if (!pullOne(id, false)) {
                    failed++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("Pulling tracking failed for package {}: {}", id, e.getMessage());
            }
        }
        return new int[]{ids.size(), failed};
    }

    /**
     * 拉取一个包裹的轨迹并保存（接口调用在事务之外）
     *
     * @param strict 失败时抛出异常（手工拉取），否则记录在包裹上
     * @return 是否成功取到轨迹
     */
    boolean pullOne(Long id, boolean strict) {
        EcomPackage p = packageMapper.selectById(id);
        if (p == null) {
            throw new BizException("ecom.error.ops.packageNotFound");
        }
        if (p.getStatus() != EcomPackageStatus.SHIPPED || !StringUtils.hasText(p.getTrackingNo())) {
            throw new BizException("ecom.error.ord.trackNotShipped", p.getPackageNo());
        }
        EcomOrderSetting setting = settingService.current();
        String key = settingService.trackKey(setting);
        CarrierTrackInfo info = null;
        TrackSource source = null;
        String error = null;
        if (p.getCarrier() != null && p.getChannelId() != null) {
            try {
                EcomLogisticsChannel channel = logisticsService.get(p.getChannelId());
                info = carrierRegistry.require(p.getCarrier()).trackInfo(logisticsService.carrierContext(channel), p.getTrackingNo());
                source = TrackSource.CARRIER;
            } catch (BizException e) {
                error = e.getMessage();
            }
        }
        if ((info == null || info.events().isEmpty()) && StringUtils.hasText(key)) {
            try {
                CarrierTrackInfo t = seventeenTrack.track(setting.getTrackApiUrl(), key, p.getTrackingNo());
                if (info == null || !t.events().isEmpty()) {
                    info = t;
                    source = TrackSource.TRACK17;
                }
                error = null;
            } catch (BizException e) {
                error = error == null ? e.getMessage() : error + "; " + e.getMessage();
            }
        }
        if (info == null && error == null) {
            error = Messages.get("ecom.error.ord.trackNoSource");
        }
        if (strict && info == null) {
            saveResult(id, null, null, error, setting);
            throw new BizException("ecom.error.ord.trackFailed", p.getPackageNo(), error);
        }
        saveResult(id, info, source, error, setting);
        return info != null;
    }

    /** 写入外部取得的轨迹（物流商推送、演示数据），与拉取后的保存相同：替换时间线、汇总状态并判断预警 */
    public void saveTracks(Long packageId, CarrierTrackInfo info, TrackSource source) {
        saveResult(packageId, info, source, null, settingService.current());
    }

    private void saveResult(Long id, CarrierTrackInfo info, TrackSource source, String error, EcomOrderSetting setting) {
        transactionTemplate.executeWithoutResult(s -> save(id, info, source, error, setting, LocalDateTime.now()));
    }

    /** 保存轨迹（整体替换）、汇总状态与预警（调用方事务中） */
    void save(Long id, CarrierTrackInfo info, TrackSource source, String error, EcomOrderSetting setting, LocalDateTime now) {
        EcomPackage p = packageMapper.selectById(id);
        var u = Wrappers.<EcomPackage>lambdaUpdate().eq(EcomPackage::getId, id).set(EcomPackage::getTrackPullTime, now)
                .set(EcomPackage::getTrackError, cut(error));
        EcomTrackStatus status = p.getTrackStatus();
        LocalDateTime first = p.getTrackFirstTime(), last = p.getTrackLastTime(), delivered = p.getTrackDeliveredTime();
        if (info != null) {
            List<EcomPackageTrack> rows = toRows(id, info.events());
            trackMapper.delete(Wrappers.<EcomPackageTrack>lambdaQuery().eq(EcomPackageTrack::getPackageId, id));
            if (!rows.isEmpty()) {
                trackMapper.insertBatch(rows);
            }
            status = info.status();
            first = rows.stream().map(EcomPackageTrack::getEventTime).filter(t -> t != null).min(Comparator.naturalOrder()).orElse(null);
            last = rows.stream().map(EcomPackageTrack::getEventTime).filter(t -> t != null).max(Comparator.naturalOrder()).orElse(null);
            delivered = status == EcomTrackStatus.DELIVERED ? rows.stream().filter(r -> r.getStatus() == EcomTrackStatus.DELIVERED)
                    .map(EcomPackageTrack::getEventTime).filter(t -> t != null).findFirst().orElse(last) : null;
            u.set(EcomPackage::getTrackStatus, status).set(EcomPackage::getTrackSource, source).set(EcomPackage::getTrackFirstTime, first)
                    .set(EcomPackage::getTrackLastTime, last).set(EcomPackage::getTrackDeliveredTime, delivered)
                    .set(EcomPackage::getTrackLastEvent, rows.isEmpty() ? null : cut(lastText(rows.get(0))));
        }
        EcomTrackAlert alert = alertOf(p.getShipTime(), status, first, last, setting, now);
        u.set(EcomPackage::getTrackAlert, alert);
        packageMapper.update(u);
    }

    /** 轨迹按时间新的在前（时间解析不了的保持原顺序排在后面），每条按描述归一状态；第一条（最新）的状态与整体状态一致 */
    static List<EcomPackageTrack> toRows(Long packageId, List<CarrierTrackEvent> events) {
        List<EcomPackageTrack> rows = new ArrayList<>();
        for (CarrierTrackEvent e : events) {
            EcomPackageTrack t = new EcomPackageTrack();
            t.setPackageId(packageId);
            t.setEventTime(TrackStatusNormalizer.parseTime(e.time()));
            t.setTimeText(e.time() == null ? null : e.time().length() <= 32 ? e.time() : e.time().substring(0, 32));
            t.setLocation(e.location() == null ? null : e.location().length() <= 128 ? e.location() : e.location().substring(0, 128));
            t.setDescription(cut(e.description()));
            t.setStatus(TrackStatusNormalizer.fromText(e.description()));
            rows.add(t);
        }
        rows.sort(Comparator.comparing(EcomPackageTrack::getEventTime, Comparator.nullsLast(Comparator.reverseOrder())));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setSort(i);
        }
        return rows;
    }

    private static String lastText(EcomPackageTrack t) {
        return StringUtils.hasText(t.getLocation()) ? t.getLocation() + " " + t.getDescription() : t.getDescription();
    }

    /**
     * 预警：异常 / 退回 → 轨迹异常；已签收没有预警；没有轨迹且发货超过设定小时 → 超时未上网；
     * 上网超过设定天数未签收或轨迹超过设定天数没有更新 → 运输超时
     */
    public static EcomTrackAlert alertOf(LocalDateTime shipTime, EcomTrackStatus status, LocalDateTime first, LocalDateTime last,
                                         EcomOrderSetting s, LocalDateTime now) {
        if (status == EcomTrackStatus.EXCEPTION || status == EcomTrackStatus.RETURNED) {
            return EcomTrackAlert.EXCEPTION;
        }
        if (status == EcomTrackStatus.DELIVERED) {
            return null;
        }
        int noScan = s.getNoScanHours() == null ? 72 : s.getNoScanHours();
        int transit = s.getTransitDays() == null ? 20 : s.getTransitDays();
        int stale = s.getStaleDays() == null ? 7 : s.getStaleDays();
        if (first == null || status == null || status == EcomTrackStatus.NOT_FOUND) {
            return shipTime != null && shipTime.plusHours(noScan).isBefore(now) ? EcomTrackAlert.NO_SCAN : null;
        }
        if (first.plusDays(transit).isBefore(now) || last != null && last.plusDays(stale).isBefore(now)) {
            return EcomTrackAlert.TRANSIT_TIMEOUT;
        }
        return null;
    }

    private static String cut(String s) {
        return s == null || s.length() <= TEXT_MAX ? s : s.substring(0, TEXT_MAX);
    }
}
