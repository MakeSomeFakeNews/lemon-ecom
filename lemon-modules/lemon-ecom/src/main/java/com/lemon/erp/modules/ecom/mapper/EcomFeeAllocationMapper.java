package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomFeeAllocation;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EcomFeeAllocationMapper extends BaseMapper<EcomFeeAllocation> {

    int insertBatch(@Param("items") List<EcomFeeAllocation> items);

    /** 结算单的分摊结果（带结算明细的说明、原始类型） */
    List<EcomFeeAllocation> selectBySettlement(@Param("settlementId") Long settlementId);

    /**
     * 店铺在一段下单时间内各平台 SKU 的销量与本位币销售额（取消的订单不计）：sellerSku、qty、sales
     */
    List<Map<String, Object>> selectSkuBasis(@Param("storeId") Long storeId, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
