package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomAdSearchTerm;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomAdSearchTermMapper extends BaseMapper<EcomAdSearchTerm> {

    int insertBatch(@Param("items") List<EcomAdSearchTerm> items);
}
