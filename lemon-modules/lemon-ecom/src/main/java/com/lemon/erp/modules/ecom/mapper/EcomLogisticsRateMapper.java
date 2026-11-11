package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsRate;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface EcomLogisticsRateMapper extends BaseMapper<EcomLogisticsRate> {

    int insertBatch(@Param("items") List<EcomLogisticsRate> items);

    /** 启用渠道中匹配目的国（或通用 *）和重量段的费率，带渠道信息 */
    List<EcomLogisticsRate> selectCandidates(@Param("country") String country, @Param("weight") BigDecimal weight);
}
