package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomInboundBoxItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomInboundBoxItemMapper extends BaseMapper<EcomInboundBoxItem> {

    int insertBatch(@Param("items") List<EcomInboundBoxItem> items);
}
