package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhReturnItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhReturnItemMapper extends BaseMapper<EcomOwhReturnItem> {

    List<EcomOwhReturnItem> selectByReturn(@Param("returnId") Long returnId);

    void insertBatch(@Param("list") List<EcomOwhReturnItem> list);
}
