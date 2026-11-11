package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EcomReplenishMapper {

    /**
     * 补货建议的基础数据：已映射物料的在售在线商品，带最近 N 天销量、平台仓 / 本地仓系统库存、在途数量（件数均已按多件装折算）
     *
     * @param since 销量统计起点
     */
    List<ReplenishRow> selectRows(@Param("storeId") Long storeId, @Param("keyword") String keyword, @Param("since") LocalDateTime since);
}
