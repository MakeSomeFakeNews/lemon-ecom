package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlan;
import com.lemon.erp.modules.ecom.model.ShipmentPlanStatus;
import org.apache.ibatis.annotations.Param;

public interface EcomShipmentPlanMapper extends BaseMapper<EcomShipmentPlan> {

    IPage<EcomShipmentPlan> selectPageList(IPage<EcomShipmentPlan> page, @Param("storeId") Long storeId, @Param("status") ShipmentPlanStatus status,
                                           @Param("keyword") String keyword);

    EcomShipmentPlan selectDetail(@Param("id") Long id);
}
