package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomAdScheduleMapper extends BaseMapper<EcomAdSchedule> {

    List<EcomAdSchedule> selectList(@Param("storeId") Long storeId);
}
