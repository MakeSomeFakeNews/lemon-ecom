package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.dto.ReplenishTransit;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomReplenishMapper {

    /**
     * 补货建议的基础数据：已映射物料的在售在线商品，带最近 7 / 14 / 30 / 60 天销量、平台仓 / 本地仓系统库存、
     * 头程在途数量与最早预计到达日期（件数均已按多件装折算）
     */
    List<ReplenishRow> selectRows(@Param("storeId") Long storeId, @Param("keyword") String keyword,
                                  @Param("since7") LocalDateTime since7, @Param("since14") LocalDateTime since14,
                                  @Param("since30") LocalDateTime since30, @Param("since60") LocalDateTime since60);

    /** 物料在指定仓库的库存：materialId、warehouseId → qty */
    List<Map<String, Object>> selectStock(@Param("materialIds") Collection<Long> materialIds, @Param("warehouseIds") Collection<Long> warehouseIds);

    /** 各店铺的头程在途：storeId、materialId → qty、eta */
    List<Map<String, Object>> selectStoreInboundTransit(@Param("materialIds") Collection<Long> materialIds);

    /** 本地仓库存：materialId → qty */
    List<Map<String, Object>> selectLocalStock(@Param("materialIds") Collection<Long> materialIds);

    /** 采购在途：materialId → qty、eta */
    List<Map<String, Object>> selectPurchaseTransit(@Param("materialIds") Collection<Long> materialIds);

    /** 头程在途（全部店铺）：materialId → qty、eta */
    List<Map<String, Object>> selectInboundTransit(@Param("materialIds") Collection<Long> materialIds);

    List<ReplenishTransit.Purchase> selectPurchaseTransitDetail(@Param("materialId") Long materialId);

    List<ReplenishTransit.Shipment> selectInboundTransitDetail(@Param("materialId") Long materialId, @Param("storeId") Long storeId);
}
