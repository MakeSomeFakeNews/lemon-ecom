package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderItemMapper extends BaseMapper<EcomOrderItem> {

    List<EcomOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /** 列表展示用的商品行（SKU、标题、数量、图片），按订单分组显示 */
    List<EcomOrderItem> selectBriefByOrderIds(@Param("orderIds") List<Long> orderIds);

    int insertBatch(@Param("items") List<EcomOrderItem> items);
}
