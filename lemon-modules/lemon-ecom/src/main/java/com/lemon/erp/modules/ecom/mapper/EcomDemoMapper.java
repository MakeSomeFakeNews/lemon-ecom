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
}
