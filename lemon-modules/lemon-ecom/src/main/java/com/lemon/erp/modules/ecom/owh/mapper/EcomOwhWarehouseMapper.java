package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhWarehouseMapper extends BaseMapper<EcomOwhWarehouse> {

    /** 映射列表（带账号、系统仓库名称）；accountId 为空时全部 */
    List<EcomOwhWarehouse> selectMappings(@Param("accountId") Long accountId);

    EcomOwhWarehouse selectDetail(@Param("id") Long id);

    /** 系统仓库的映射（带账号） */
    EcomOwhWarehouse selectByWarehouseId(@Param("warehouseId") Long warehouseId);
}
