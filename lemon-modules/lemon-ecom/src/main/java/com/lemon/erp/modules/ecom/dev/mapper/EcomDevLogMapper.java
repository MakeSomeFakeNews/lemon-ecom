package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomDevLogMapper extends BaseMapper<EcomDevLog> {

    /** 项目的流转记录（按时间正序），带操作人 */
    List<EcomDevLog> selectByProject(@Param("projectId") Long projectId);
}
