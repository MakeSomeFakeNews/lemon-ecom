package com.lemon.erp.modules.ecom.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EcomForecastEventMapper extends BaseMapper<EcomForecastEvent> {

    /** 与 [from, to] 有重叠的事件（含店铺名称），按开始日期排序；from / to 为空不限 */
    List<EcomForecastEvent> selectOverlap(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("storeId") Long storeId);
}
