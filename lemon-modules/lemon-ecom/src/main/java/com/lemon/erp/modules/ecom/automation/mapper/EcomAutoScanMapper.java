package com.lemon.erp.modules.ecom.automation.mapper;

import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 自动化触发器取数（只读）：各触发器构造事件、扫描、取最近事件时使用。返回的 Map 键为驼峰别名
 */
public interface EcomAutoScanMapper {

    // ---------------------------------------------------------------- 订单

    Map<String, Object> selectOrder(@Param("id") Long id);

    List<Long> selectRecentOrderIds(@Param("exception") boolean exception, @Param("loss") boolean loss, @Param("limit") int limit);

    List<String> selectOrderSkus(@Param("orderId") Long orderId);

    List<String> selectOrderTags(@Param("orderId") Long orderId);

    // ---------------------------------------------------------------- 评价、买家消息

    List<Map<String, Object>> selectReviews(@Param("ids") Collection<Long> ids, @Param("maxRating") Integer maxRating, @Param("limit") int limit);

    List<Map<String, Object>> selectOverdueThreads(@Param("now") LocalDateTime now, @Param("limit") int limit);

    // ---------------------------------------------------------------- 商品监控

    List<Map<String, Object>> selectAlerts(@Param("ids") Collection<Long> ids, @Param("types") Collection<String> types, @Param("limit") int limit);

    // ---------------------------------------------------------------- 店铺与同步

    List<Map<String, Object>> selectFailedSyncLogs(@Param("id") Long id, @Param("auth") boolean auth, @Param("limit") int limit);

    // ---------------------------------------------------------------- 头程

    List<Map<String, Object>> selectInbounds(@Param("ids") Collection<Long> ids, @Param("overdueBefore") LocalDate overdueBefore,
                                             @Param("received") boolean received, @Param("limit") int limit);

    // ---------------------------------------------------------------- 广告

    List<Map<String, Object>> selectCampaignMetrics(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Map<String, Object>> selectCampaignBudgets(@Param("since") LocalDate since, @Param("minUsage") java.math.BigDecimal minUsage);

    // ---------------------------------------------------------------- FBA、退货、竞品

    List<Map<String, Object>> selectClaimsDue(@Param("today") LocalDate today, @Param("until") LocalDate until);

    List<Map<String, Object>> selectSkuOrdered(@Param("since") LocalDateTime since);

    List<Map<String, Object>> selectSkuReturned(@Param("since") LocalDateTime since);

    List<Map<String, Object>> selectCompetitorLower(@Param("since") LocalDateTime since);

    // ---------------------------------------------------------------- 接收人

    /** 角色下正常状态的内部用户 */
    List<Long> selectRoleUsers(@Param("roleIds") Collection<Long> roleIds);

    List<AutoViews.RoleOption> selectRoles();

    /** 用户昵称（试运行描述接收人） */
    List<Map<String, Object>> selectUserNames(@Param("ids") Collection<Long> ids);
}
