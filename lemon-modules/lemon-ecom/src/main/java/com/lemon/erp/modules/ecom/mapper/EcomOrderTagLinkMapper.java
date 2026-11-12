package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderTagLink;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderTagLinkMapper extends BaseMapper<EcomOrderTagLink> {

    int insertBatch(@Param("items") List<EcomOrderTagLink> items);
}
