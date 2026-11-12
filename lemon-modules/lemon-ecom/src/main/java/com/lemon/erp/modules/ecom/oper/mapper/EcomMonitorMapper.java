package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.oper.dto.MonitorQuery;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitor;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 商品监控
 */
public interface EcomMonitorMapper extends BaseMapper<EcomMonitor> {

    IPage<EcomMonitor> selectPageList(IPage<EcomMonitor> page, @Param("q") MonitorQuery q);

    EcomMonitor selectDetail(@Param("id") Long id);

    /** 每个监控在最近一次快照之前的那一次快照（计算变化），没有的不返回 */
    List<EcomMonitorSnapshot> selectPreviousSnapshots(@Param("ids") Collection<Long> ids);
}
