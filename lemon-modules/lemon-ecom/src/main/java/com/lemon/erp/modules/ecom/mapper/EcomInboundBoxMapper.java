package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomInboundBox;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomInboundBoxMapper extends BaseMapper<EcomInboundBox> {

    int insertBatch(@Param("items") List<EcomInboundBox> items);
}
