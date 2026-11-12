package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.ReturnType;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomReturnMapper extends BaseMapper<EcomReturn> {

    IPage<EcomReturn> selectPageList(IPage<EcomReturn> page, @Param("storeId") Long storeId, @Param("returnType") ReturnType returnType,
                                     @Param("status") EcomReturnStatus status, @Param("keyword") String keyword);

    /** 各状态的单据数（条件同列表，不含状态） */
    List<Map<String, Object>> countByStatus(@Param("storeId") Long storeId, @Param("returnType") ReturnType returnType, @Param("keyword") String keyword);

    EcomReturn selectDetail(@Param("id") Long id);
}
