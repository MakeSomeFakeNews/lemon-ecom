package com.lemon.erp.modules.ecom.assistant.mapper;

import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * AI 运营助手的只读汇总查询（运营日报、问数工具）。storeIds 为 null 时不按店铺限制（店铺数据权限仍由拦截器自动追加）
 */
@Mapper
public interface EcomAssistantQueryMapper {

    /** 广告日报按店铺汇总（花费、广告销售额为店铺币种） */
    List<AssistantRows.AdTotal> selectAdTotals(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDate begin,
                                               @Param("end") LocalDate end);

    /** 按店铺的退货退款单数（登记时间在 [begin, end)） */
    List<Map<String, Object>> selectReturnCounts(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDateTime begin,
                                                 @Param("end") LocalDateTime end);

    /** 按店铺 + 平台 SKU 的退货件数、退款（本位币）（登记时间在 [begin, end)） */
    List<AssistantRows.SkuReturn> selectSkuReturns(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDateTime begin,
                                                   @Param("end") LocalDateTime end);

    /** 按退货原因汇总 */
    List<AssistantRows.ReasonRow> selectReturnReasons(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDateTime begin,
                                                      @Param("end") LocalDateTime end);

    /** 按 SKU + 原因 */
    List<AssistantRows.SkuReasonRow> selectSkuReturnReasons(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDateTime begin,
                                                            @Param("end") LocalDateTime end, @Param("keyword") String keyword);

    /** 评价（评价日期在 [begin, end] 内、评分不高于 maxRating），新的在前 */
    List<AssistantRows.ReviewRow> selectReviews(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDate begin,
                                                @Param("end") LocalDate end, @Param("maxRating") Integer maxRating,
                                                @Param("keyword") String keyword, @Param("limit") int limit);

    /** 按 SKU 的评价数、平均分、差评数 */
    List<AssistantRows.ReviewSkuRow> selectReviewSkus(@Param("storeIds") Collection<Long> storeIds, @Param("begin") LocalDate begin,
                                                      @Param("end") LocalDate end, @Param("keyword") String keyword);

    /** 待处理事项计数（键见 XML） */
    Map<String, Object> selectTodoCounts(@Param("storeIds") Collection<Long> storeIds, @Param("now") LocalDateTime now,
                                         @Param("claimDeadline") LocalDate claimDeadline);

    /** 订单异常与超时：kind = EXCEPTION 异常 / 挂起 / 缺货，OVERDUE 已超过承诺发货时间未发出，DUE_SOON 即将到期（dueBefore 之前） */
    List<AssistantRows.OrderIssue> selectOrderIssues(@Param("storeIds") Collection<Long> storeIds, @Param("kind") String kind,
                                                     @Param("now") LocalDateTime now, @Param("dueBefore") LocalDateTime dueBefore,
                                                     @Param("limit") int limit);

    /** 订单异常与超时按店铺汇总 */
    List<AssistantRows.OrderIssueSummary> selectOrderIssueSummary(@Param("storeIds") Collection<Long> storeIds, @Param("now") LocalDateTime now,
                                                                  @Param("dueBefore") LocalDateTime dueBefore);

    /** 广告活动基础信息（名称、日预算、状态、店铺币种） */
    List<AssistantRows.CampaignInfo> selectCampaigns(@Param("storeIds") Collection<Long> storeIds);

    /**
     * 广告搜索词报告中的高转化词：投放的商品（广告日报中带 SKU 的行所在广告组）属于指定物料（或同一款式），
     * sameCategory 时扩大到同品类的物料；按订单数、转化率排序
     */
    List<AssistantRows.KeywordRow> selectKeywordSuggestions(@Param("materialId") Long materialId, @Param("sameCategory") boolean sameCategory,
                                                            @Param("marketplace") String marketplace, @Param("begin") LocalDate begin,
                                                            @Param("limit") int limit);
}
