package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.oper.dto.AlertQuery;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorAlert;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商品监控预警
 */
public interface EcomMonitorAlertMapper extends BaseMapper<EcomMonitorAlert> {

    IPage<EcomMonitorAlert> selectPageList(IPage<EcomMonitorAlert> page, @Param("q") AlertQuery q);

    /** 未处理预警数：monitorId、cnt */
    List<Map<String, Object>> countNewByMonitor(@Param("ids") Collection<Long> ids);

    /** 未处理预警数：alertType、cnt */
    List<Map<String, Object>> countNewByType(@Param("q") AlertQuery q);
}
