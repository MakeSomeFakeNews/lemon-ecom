package com.lemon.erp.modules.ecom.oper.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 运营目标与绩效的统计（只读，按店铺汇总）
 */
public interface EcomOperReportMapper {

    /** 店铺（含停用）：storeId、code、name、platform、marketplace、currency、ownerId、ownerName、status */
    List<Map<String, Object>> selectStores();

    /** 运营人员（有负责店铺的用户）：userId、nickname */
    List<Map<String, Object>> selectOwners();

    /** 按店铺的订单数、销售额、利润（本位币，不含已取消）：storeId、orders、sales、profit */
    List<Map<String, Object>> selectOrderTotals(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /** 按店铺的广告花费与广告销售额（店铺币种）：storeId、spend、adSales */
    List<Map<String, Object>> selectAdTotals(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 按店铺的退货退款单数（不含已关闭）：storeId、cnt */
    List<Map<String, Object>> selectReturnCounts(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /** 按店铺的差评数（不超过 maxRating 星）：storeId、cnt */
    List<Map<String, Object>> selectBadReviewCounts(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("maxRating") int maxRating);

    /** 店铺销量最高的平台 SKU（不含已取消）：sellerSku、platformItemId、title、qty，按件数倒序取前 limit 个（演示数据用） */
    List<Map<String, Object>> selectTopSkus(@Param("storeId") Long storeId, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end,
                                            @Param("limit") int limit);
}
