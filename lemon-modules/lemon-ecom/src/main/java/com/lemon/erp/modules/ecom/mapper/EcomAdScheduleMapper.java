package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomAdScheduleMapper extends BaseMapper<EcomAdSchedule> {

    /**
     * 分时策略列表（带活动、店铺名称）。不能命名为 selectList：XML 中同名语句会覆盖 BaseMapper.selectList(Wrapper)，
     * 定时任务按条件查询时报 Parameter 'storeId' not found
     */
    List<EcomAdSchedule> selectByStore(@Param("storeId") Long storeId);
}
