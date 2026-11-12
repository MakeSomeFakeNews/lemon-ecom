package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevSample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomDevSampleMapper extends BaseMapper<EcomDevSample> {

    /** 项目的样品：带供应商档案名称、创建人 */
    List<EcomDevSample> selectByProject(@Param("projectId") Long projectId);
}
