package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhSku;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhSkuMapper extends BaseMapper<EcomOwhSku> {

    /** 账号的 SKU 映射（带物料编码、名称）；keyword 按服务商 SKU / 物料编码 / 名称过滤 */
    List<EcomOwhSku> selectByAccount(@Param("accountId") Long accountId, @Param("keyword") String keyword);
}
