package com.lemon.erp.modules.ecom.oper.service;

import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 监控预警规则：本次快照与上一次快照比较（两边都有值的指标才比较）
 * <ul>
 *   <li>排名大跌：大类排名数值变大，且 (本次 − 上次) ÷ 上次 ≥ 阈值 %（阈值为 0 不预警）</li>
 *   <li>评分下降：上次 − 本次 ≥ 阈值（阈值为 0 不预警）</li>
 *   <li>新增跟卖：在售卖家数增加</li>
 *   <li>标题 / 主图 / 五点被改：两边都不为空且不同（五点忽略空白差异）</li>
 *   <li>被下架：上次在售、本次下架</li>
 * </ul>
 */
public final class MonitorAlertRules {

    /** 一条预警：类型、变化前、变化后 */
    public record Hit(EcomMonitorAlertType type, String oldValue, String newValue) {
    }

    private static final int VALUE_MAX = 500;

    private MonitorAlertRules() {
    }

    public static List<Hit> evaluate(EcomMonitorSnapshot prev, EcomMonitorSnapshot cur, BigDecimal rankDropPercent, BigDecimal ratingDrop) {
        List<Hit> hits = new ArrayList<>();
        if (prev == null || cur == null) {
            return hits;
        }
        if (prev.getRankMain() != null && cur.getRankMain() != null && cur.getRankMain() > prev.getRankMain()
                && positive(rankDropPercent)) {
            BigDecimal pct = BigDecimal.valueOf(cur.getRankMain() - prev.getRankMain()).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(prev.getRankMain()), 2, RoundingMode.HALF_UP);
            if (pct.compareTo(rankDropPercent) >= 0) {
                hits.add(new Hit(EcomMonitorAlertType.RANK_DROP, "#" + prev.getRankMain(), "#" + cur.getRankMain() + " (-" + pct.stripTrailingZeros().toPlainString() + "%)"));
            }
        }
        if (prev.getRating() != null && cur.getRating() != null && positive(ratingDrop)
                && prev.getRating().subtract(cur.getRating()).compareTo(ratingDrop) >= 0) {
            hits.add(new Hit(EcomMonitorAlertType.RATING_DROP, prev.getRating().stripTrailingZeros().toPlainString(),
                    cur.getRating().stripTrailingZeros().toPlainString()));
        }
        if (prev.getSellerCount() != null && cur.getSellerCount() != null && cur.getSellerCount() > prev.getSellerCount()) {
            hits.add(new Hit(EcomMonitorAlertType.NEW_SELLER, String.valueOf(prev.getSellerCount()), String.valueOf(cur.getSellerCount())));
        }
        if (changed(prev.getTitle(), cur.getTitle())) {
            hits.add(new Hit(EcomMonitorAlertType.TITLE_CHANGED, cut(prev.getTitle()), cut(cur.getTitle())));
        }
        if (changed(prev.getMainImage(), cur.getMainImage())) {
            hits.add(new Hit(EcomMonitorAlertType.IMAGE_CHANGED, cut(prev.getMainImage()), cut(cur.getMainImage())));
        }
        if (changed(normalize(prev.getBullets()), normalize(cur.getBullets()))) {
            hits.add(new Hit(EcomMonitorAlertType.BULLETS_CHANGED, cut(prev.getBullets()), cut(cur.getBullets())));
        }
        if (prev.getListingStatus() == ListingStatus.ACTIVE && cur.getListingStatus() == ListingStatus.INACTIVE) {
            hits.add(new Hit(EcomMonitorAlertType.DELISTED, ListingStatus.ACTIVE.name(), ListingStatus.INACTIVE.name()));
        }
        return hits;
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    private static boolean changed(String a, String b) {
        return a != null && b != null && !a.isBlank() && !b.isBlank() && !a.trim().equals(b.trim());
    }

    private static String normalize(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
    }

    static String cut(String s) {
        return s == null || s.length() <= VALUE_MAX ? s : s.substring(0, VALUE_MAX);
    }
}
