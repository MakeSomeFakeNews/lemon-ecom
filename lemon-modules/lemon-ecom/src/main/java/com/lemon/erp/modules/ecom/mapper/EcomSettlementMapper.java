package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.model.SettlementStatus;
import org.apache.ibatis.annotations.Param;

public interface EcomSettlementMapper extends BaseMapper<EcomSettlement> {

    IPage<EcomSettlement> selectPageList(IPage<EcomSettlement> page, @Param("storeId") Long storeId, @Param("status") SettlementStatus status,
                                         @Param("keyword") String keyword);

    EcomSettlement selectDetail(@Param("id") Long id);
}
