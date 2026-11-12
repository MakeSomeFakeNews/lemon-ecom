package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomAdStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomAdStatMapper extends BaseMapper<EcomAdStat> {

    int insertBatch(@Param("items") List<EcomAdStat> items);
}
