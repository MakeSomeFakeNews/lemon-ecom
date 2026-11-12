package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface Ecom1688OrderItemMapper extends BaseMapper<Ecom1688OrderItem> {

    /** 明细：带物料、1688 标题与规格（取当前关联） */
    List<Ecom1688OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<Ecom1688OrderItem> items);
}
