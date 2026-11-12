package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomOrderItemPartMapper extends BaseMapper<EcomOrderItemPart> {

    /** 订单行展开的物料（含物料编码、名称），按行、顺序排序 */
    List<EcomOrderItemPart> selectByOrderItemIds(@Param("itemIds") Collection<Long> itemIds);

    int insertBatch(@Param("items") List<EcomOrderItemPart> items);
}
