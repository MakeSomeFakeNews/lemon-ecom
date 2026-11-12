package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomOrderTagMapper extends BaseMapper<EcomOrderTag> {

    /** 标签列表（含使用的订单数），按排序、ID */
    List<EcomOrderTag> selectListWithCount(@Param("keyword") String keyword, @Param("status") EnableStatus status);

    /** 若干订单的标签（带 orderId、autoTagged） */
    List<EcomOrderTag> selectByOrderIds(@Param("orderIds") Collection<Long> orderIds);
}
