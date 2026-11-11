package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import org.apache.ibatis.annotations.Param;

public interface EcomReturnMapper extends BaseMapper<EcomReturn> {

    IPage<EcomReturn> selectPageList(IPage<EcomReturn> page, @Param("storeId") Long storeId, @Param("status") EcomReturnStatus status,
                                     @Param("keyword") String keyword);

    EcomReturn selectDetail(@Param("id") Long id);
}
