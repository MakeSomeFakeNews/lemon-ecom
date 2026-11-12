package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomPurchasePlan;
import com.lemon.erp.modules.ecom.model.PurchasePlanStatus;
import org.apache.ibatis.annotations.Param;

public interface EcomPurchasePlanMapper extends BaseMapper<EcomPurchasePlan> {

    /**
     * 分页：带已下单数量、已到货数量、明细行数、生成的采购订单号
     *
     * @param keyword 计划单号、采购订单号、物料编码 / 名称
     */
    IPage<EcomPurchasePlan> selectPageList(IPage<EcomPurchasePlan> page, @Param("status") PurchasePlanStatus status,
                                           @Param("keyword") String keyword);

    EcomPurchasePlan selectDetail(@Param("id") Long id);
}
