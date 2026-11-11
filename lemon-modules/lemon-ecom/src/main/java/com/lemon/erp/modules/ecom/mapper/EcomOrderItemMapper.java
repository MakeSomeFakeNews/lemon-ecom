package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderItemMapper extends BaseMapper<EcomOrderItem> {

    List<EcomOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<EcomOrderItem> items);
}
