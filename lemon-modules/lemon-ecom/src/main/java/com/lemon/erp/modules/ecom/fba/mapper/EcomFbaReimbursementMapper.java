package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaReimbursement;
import org.apache.ibatis.annotations.Param;

public interface EcomFbaReimbursementMapper extends BaseMapper<EcomFbaReimbursement> {

    IPage<EcomFbaReimbursement> selectPageList(IPage<EcomFbaReimbursement> page, @Param("storeId") Long storeId, @Param("matched") Boolean matched,
                                               @Param("keyword") String keyword);
}
