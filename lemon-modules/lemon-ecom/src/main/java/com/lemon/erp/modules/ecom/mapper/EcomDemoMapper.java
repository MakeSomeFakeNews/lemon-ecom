package com.lemon.erp.modules.ecom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 演示数据用的只读查询（只读关联库存表，用于回推历史库存快照）
 */
@Mapper
public interface EcomDemoMapper {

    /**
     * 各仓库各物料按业务日期的库存变动（在途调拨的调入按收货日期）：warehouseId、materialId、bizDate、qty
     */
    List<Map<String, Object>> selectDailyStockChange(@Param("warehouseIds") Collection<Long> warehouseIds);

    /** 各月自发货订单的尾程运费（本位币，按发货月）：ym（yyyyMM）、amount */
    List<Map<String, Object>> selectMonthlySellerShipping(@Param("storeIds") Collection<Long> storeIds);

    /** 各月头程货件的运费、关税与杂费（本位币，按发运月）：ym、amount */
    List<Map<String, Object>> selectMonthlyInboundCost(@Param("storeIds") Collection<Long> storeIds);

    /** 各月各币种的广告花费（店铺币种）：ym、currency、amount */
    List<Map<String, Object>> selectMonthlyAdSpend(@Param("storeIds") Collection<Long> storeIds);
}
