package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemovalItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomFbaRemovalItemMapper extends BaseMapper<EcomFbaRemovalItem> {

    List<EcomFbaRemovalItem> selectByRemovalId(@Param("removalId") Long removalId);
}
