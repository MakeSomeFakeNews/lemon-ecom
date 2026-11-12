package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhDiff;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhDiffMapper extends BaseMapper<EcomOwhDiff> {

    IPage<EcomOwhDiff> selectPageList(IPage<EcomOwhDiff> page, @Param("sourceType") OwhDiffSource sourceType, @Param("status") OwhDiffStatus status,
                                      @Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    List<EcomOwhDiff> selectBySource(@Param("sourceType") OwhDiffSource sourceType, @Param("sourceId") Long sourceId);
}
