package com.lemon.erp.modules.ecom.analytics.mapper;

import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.model.SalesDimension;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据分析的只读统计（EcomAnalyticsMapper.xml）：时间参数 begin / end 为 [begin, end) 的下单时间
 */
@Mapper
public interface EcomAnalyticsMapper {

    List<AnalyticsRows.SalesCell> selectSales(@Param("dimension") SalesDimension dimension, @Param("begin") LocalDateTime begin,
                                              @Param("end") LocalDateTime end, @Param("storeId") Long storeId,
                                              @Param("platform") EcomPlatform platform, @Param("keyword") String keyword,
                                              @Param("byDay") boolean byDay);

    List<AnalyticsRows.SalesCell> selectSkuDaily(@Param("storeId") Long storeId, @Param("sellerSku") String sellerSku,
                                                 @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    List<AnalyticsRows.ListingDay> selectListingDaily(@Param("listingIds") Collection<Long> listingIds, @Param("begin") LocalDateTime begin,
                                                      @Param("end") LocalDateTime end);

    List<AnalyticsRows.ListingRef> selectActiveListings(@Param("storeId") Long storeId, @Param("listingIds") Collection<Long> listingIds,
                                                        @Param("afterId") long afterId, @Param("limit") int limit);

    List<Map<String, Object>> selectFreeStock(@Param("materialIds") Collection<Long> materialIds, @Param("warehouseIds") Collection<Long> warehouseIds);

    List<AnalyticsRows.SkuSales> selectSkuSales(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end,
                                                @Param("storeId") Long storeId, @Param("keyword") String keyword);

    List<AnalyticsRows.SkuReturn> selectSkuReturns(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end,
                                                   @Param("storeId") Long storeId, @Param("keyword") String keyword);

    Map<String, Object> selectMovingCounts(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    List<AnalyticsRows.MaterialStock> selectSlowMaterials(@Param("begin") LocalDateTime begin, @Param("storeId") Long storeId,
                                                         @Param("keyword") String keyword);

    List<AnalyticsRows.AgeLot> selectStockBatches(@Param("materialIds") Collection<Long> materialIds);

    List<AnalyticsRows.AgeLot> selectInboundFlows(@Param("materialIds") Collection<Long> materialIds, @Param("since") LocalDateTime since);

    List<AnalyticsRows.MaterialPeriod> selectMaterialPeriod(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end,
                                                           @Param("storeId") Long storeId, @Param("keyword") String keyword);

    List<AnalyticsRows.FlowSum> selectFlowSums(@Param("materialIds") Collection<Long> materialIds, @Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    List<AnalyticsRows.StockoutStat> selectStockout(@Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate,
                                                    @Param("storeId") Long storeId, @Param("listingIds") Collection<Long> listingIds,
                                                    @Param("keyword") String keyword);

    List<AnalyticsRows.ListingDay> selectStockoutDays(@Param("listingIds") Collection<Long> listingIds, @Param("from") LocalDate from,
                                                      @Param("to") LocalDate to);

    List<AnalyticsRows.FirstSale> selectFirstSales(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end,
                                                   @Param("storeId") Long storeId, @Param("keyword") String keyword);

    List<AnalyticsRows.SalesCell> selectSkusDaily(@Param("storeIds") Collection<Long> storeIds, @Param("skus") Collection<String> skus,
                                                  @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    List<AnalyticsRows.ListingInfo> selectListingInfo(@Param("listingIds") Collection<Long> listingIds);

    List<Long> selectListingIdsByKeyword(@Param("keyword") String keyword, @Param("storeId") Long storeId, @Param("limit") int limit);
}
