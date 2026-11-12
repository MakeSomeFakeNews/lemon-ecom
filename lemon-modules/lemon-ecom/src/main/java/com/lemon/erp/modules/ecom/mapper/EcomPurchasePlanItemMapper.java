package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomPurchasePlanItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomPurchasePlanItemMapper extends BaseMapper<EcomPurchasePlanItem> {

    /** 明细：带物料、供应商名称，生成的采购订单状态和本物料的已入库数量 */
    List<EcomPurchasePlanItem> selectByPlanId(@Param("planId") Long planId);

    int insertBatch(@Param("items") List<EcomPurchasePlanItem> items);
}
