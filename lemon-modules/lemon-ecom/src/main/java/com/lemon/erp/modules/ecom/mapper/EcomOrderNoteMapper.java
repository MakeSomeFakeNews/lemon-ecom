package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderNote;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderNoteMapper extends BaseMapper<EcomOrderNote> {

    /** 订单的备注历史（新的在前，带备注人名称） */
    List<EcomOrderNote> selectByOrderId(@Param("orderId") Long orderId);
}
