package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 订单增强的查询：利润预估用的历史包裹单件重量、补发单、物流跟踪列表与预警统计、待拉取轨迹的包裹、多包裹退货的发货来源
 */
public interface EcomOrderEnhanceMapper {

    /** 物料的历史单件重量：只含一种物料的已发货包裹，重量 ÷ 物料数量的平均值，[{ materialId, unitWeight }] */
    List<Map<String, Object>> selectUnitWeights(@Param("materialIds") Collection<Long> materialIds);

    /** 原订单的补发单（含出库成本与运费） */
    List<EcomOrder> selectResends(@Param("orderId") Long orderId);

    /** 物流跟踪列表：已发货的包裹 */
    IPage<EcomPackage> selectTrackPage(IPage<EcomPackage> page, @Param("q") OrderEnhanceViews.TrackQuery query);

    /** 各轨迹状态 / 预警的包裹数（条件同列表，不含状态与预警）：[{ k, cnt }]，k 为 S:状态 或 A:预警 */
    List<Map<String, Object>> countTrack(@Param("q") OrderEnhanceViews.TrackQuery query);

    /** 需要拉取轨迹的包裹 ID：已发货、有运单号、发货不超过 maxDays 天、轨迹不是终态，上次拉取早于 pulledBefore（或从未拉取） */
    List<Long> selectDueTrackIds(@Param("pulledBefore") LocalDateTime pulledBefore, @Param("shippedAfter") LocalDateTime shippedAfter,
                                 @Param("limit") int limit);

    /** 订单已发货包裹的明细（发货来源）：包裹、订单行、组件、件数、物料数量、出库单，按包裹顺序 */
    List<Map<String, Object>> selectShippedLines(@Param("orderId") Long orderId);
}
