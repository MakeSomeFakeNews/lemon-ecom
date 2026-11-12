package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFeeAlloc;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EcomOwhFeeAllocMapper extends BaseMapper<EcomOwhFeeAlloc> {

    /**
     * 物料在期间内从该仓库发出的销量，按 店铺 × 平台 SKU：[{ storeId, sellerSku, qty }]
     */
    List<Map<String, Object>> selectShippedQty(@Param("warehouseId") Long warehouseId, @Param("materialId") Long materialId,
                                               @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 映射到该物料的在线商品：[{ storeId, sellerSku }] */
    List<Map<String, Object>> selectListings(@Param("materialId") Long materialId);

    /** 费用的入账明细（带店铺名称、平台订单号） */
    List<EcomOwhFeeAlloc> selectByFee(@Param("feeId") Long feeId);

    void insertBatch(@Param("list") List<EcomOwhFeeAlloc> list);
}
