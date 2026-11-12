package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.AdsViews;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 广告分析的只读查询：结构指标（活动 = 活动报告，没有时取广告日报；广告组 = 投放报告合计，没有时取广告日报；投放 = 投放报告）、
 * 趋势、搜索词、SKU 广告花费
 */
public interface EcomAdsAnalysisMapper {

    /** 按活动汇总，结果 id 为活动 ID */
    List<Map<String, Object>> selectCampaignMetrics(@Param("storeIds") Collection<Long> storeIds, @Param("campaignId") Long campaignId,
                                                    @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 按广告组汇总，结果 id 为广告组 ID */
    List<Map<String, Object>> selectAdGroupMetrics(@Param("storeIds") Collection<Long> storeIds, @Param("campaignId") Long campaignId,
                                                   @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 按投放汇总，结果 id 为投放 ID */
    List<Map<String, Object>> selectTargetMetrics(@Param("storeIds") Collection<Long> storeIds, @Param("campaignId") Long campaignId,
                                                  @Param("adGroupId") Long adGroupId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 广告日报按店铺汇总（概览） */
    List<Map<String, Object>> selectStoreTotals(@Param("storeId") Long storeId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 广告日报按店铺 × 日期汇总（趋势） */
    List<Map<String, Object>> selectDaily(@Param("storeId") Long storeId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 广告日报按店铺 × SKU 汇总（TACOS） */
    List<Map<String, Object>> selectSkuTotals(@Param("storeId") Long storeId, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                              @Param("keyword") String keyword);

    /** 搜索词按 活动 × 广告组 × 投放词 × 搜索词 汇总 */
    List<AdsViews.SearchTermRow> selectSearchTerms(@Param("storeId") Long storeId, @Param("campaignId") Long campaignId,
                                                   @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                                   @Param("keyword") String keyword, @Param("limit") int limit);

    /** 已有的否定关键词（广告组 ID + 小写文字），用于标记搜索词已否定 */
    List<Map<String, Object>> selectNegatives(@Param("storeId") Long storeId);

    long countCampaigns(@Param("storeId") Long storeId, @Param("enabledOnly") boolean enabledOnly);

    long countPendingLogs(@Param("storeId") Long storeId);
}
