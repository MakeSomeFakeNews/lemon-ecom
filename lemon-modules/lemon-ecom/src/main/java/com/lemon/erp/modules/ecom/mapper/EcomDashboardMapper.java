package com.lemon.erp.modules.ecom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 电商看板的统计（只读）：区间的订单数 / 销售额 / 订单利润、件数，按小时的订单与销售额（不含已取消订单，金额为本位币）
 */
@Mapper
public interface EcomDashboardMapper {

    Map<String, Object> selectKpi(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    BigDecimal selectUnits(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** 店铺订单的原币销售额合计（订单币种的 total_amount，演示数据校准广告规模用） */
    BigDecimal selectOrderAmount(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    List<Map<String, Object>> selectHourly(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);
}
