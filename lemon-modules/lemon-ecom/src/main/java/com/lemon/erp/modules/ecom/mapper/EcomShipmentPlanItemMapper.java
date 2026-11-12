package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlanItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomShipmentPlanItemMapper extends BaseMapper<EcomShipmentPlanItem> {

    /** 计划明细（含在线商品的标题、FNSKU、组成方式与映射物料，已生成货件的件数） */
    List<EcomShipmentPlanItem> selectByPlanId(@Param("planId") Long planId);

    int insertBatch(@Param("items") List<EcomShipmentPlanItem> items);
}
