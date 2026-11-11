package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.EcomReports;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 电商看板与利润分析的统计（只读，金额为本位币；取消的订单不计）
 */
public interface EcomReportMapper {

    /** 一段时间的订单数、销售额、利润、退款、费用、成本 */
    Map<String, Object> selectTotals(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** 待处理、异常、逾期未发货订单数 */
    Map<String, Object> selectOrderCounts(@Param("now") LocalDateTime now);

    long countInTransitInbounds();

    List<EcomReports.TrendRow> selectTrend(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    List<EcomReports.StoreRow> selectByStore(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    List<EcomReports.SkuRow> selectBySku(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId,
                                         @Param("keyword") String keyword);

    List<EcomReports.CountryRow> selectByCountry(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** 结算明细按类型汇总（本位币，按结算单汇率折算） */
    List<EcomReports.FeeRow> selectFees(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 广告花费（本位币，按店铺币种当月汇率折算）：storeId、sellerSku → spend */
    List<Map<String, Object>> selectAdSpendBySku(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 广告花费按店铺 */
    List<Map<String, Object>> selectAdSpendByStore(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);
}
