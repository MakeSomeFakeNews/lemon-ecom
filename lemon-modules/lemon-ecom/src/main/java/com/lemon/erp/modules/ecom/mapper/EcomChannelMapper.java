package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.ChannelHubViews.LogStat;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 渠道同步中心：库存回传的候选商品与回传水位、同步日志的统计
 */
public interface EcomChannelMapper {

    /** 店铺中需要回传库存的在线商品：自发货、在售、已映射物料（id、平台 SKU、平台商品 ID、物料、多件装、上次回传数量） */
    List<EcomListing> selectPushCandidates(@Param("storeId") Long storeId);

    /** 记录回传水位：items 的 id 与 lastPushedQty */
    int updatePushed(@Param("items") Collection<EcomListing> items, @Param("time") LocalDateTime time);

    /** 各店铺各类同步最近一次的日志，storeIds 为空表示全部店铺 */
    List<LogStat> selectLatestLogs(@Param("storeIds") Collection<Long> storeIds);

    /** since 之后各店铺各类同步的失败次数 */
    List<LogStat> selectFailCounts(@Param("since") LocalDateTime since);
}
