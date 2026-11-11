package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomInboundItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EcomInboundItemMapper extends BaseMapper<EcomInboundItem> {

    List<EcomInboundItem> selectByInboundId(@Param("inboundId") Long inboundId);

    int insertBatch(@Param("items") List<EcomInboundItem> items);

    /** 店铺某物料最近一次已发运 / 已签收货件的单件头程成本，没有为 null */
    BigDecimal selectLatestUnitLanded(@Param("storeId") Long storeId, @Param("materialId") Long materialId);

    /** 店铺在途（已发运未签收）货件的物料数量：materialId → qty */
    List<Map<String, Object>> selectInTransit(@Param("storeId") Long storeId);
}
