package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhProduct;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhProductMapper extends BaseMapper<EcomOwhProduct> {

    /** 账号的物流产品（带物流渠道名称） */
    List<EcomOwhProduct> selectByAccount(@Param("accountId") Long accountId);
}
