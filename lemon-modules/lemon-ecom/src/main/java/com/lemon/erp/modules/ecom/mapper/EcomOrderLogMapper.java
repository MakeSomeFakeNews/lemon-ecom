package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderLogMapper extends BaseMapper<EcomOrderLog> {

    /** 订单的操作日志（新的在前，带操作人名称） */
    List<EcomOrderLog> selectByOrderId(@Param("orderId") Long orderId);

    /** 批量写入（createTime 为空时取当前时间） */
    int insertBatch(@Param("logs") List<EcomOrderLog> logs);
}
