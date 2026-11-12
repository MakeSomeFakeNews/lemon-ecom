package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 商品监控快照
 */
public interface EcomMonitorSnapshotMapper extends BaseMapper<EcomMonitorSnapshot> {

    /** 趋势：日期范围内的快照（不含五点描述大字段），按日期升序 */
    List<EcomMonitorSnapshot> selectTrend(@Param("monitorId") Long monitorId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 批量写入（单条多值 INSERT，演示数据与导入用） */
    int insertBatch(@Param("items") List<EcomMonitorSnapshot> items);
}
