package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaItemDim;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface EcomFbaItemDimMapper extends BaseMapper<EcomFbaItemDim> {

    /** 平台仓配送的在线商品 + 已维护的尺寸；missingOnly 只看没有尺寸的 */
    IPage<FbaViews.DimRow> selectDimPage(IPage<FbaViews.DimRow> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                         @Param("missingOnly") Boolean missingOnly);

    /** 费用核查：结算中配送费（FULFILLMENT）按 店铺 + SKU 汇总，件数取对应订单行 */
    List<FbaViews.FeeActual> selectFeeActuals(@Param("storeId") Long storeId, @Param("beginDate") LocalDate beginDate,
                                              @Param("endDate") LocalDate endDate);

    /** 费用核查涉及的 SKU 的尺寸、物料和店铺信息 */
    List<FbaViews.AuditRow> selectAuditBase(@Param("storeId") Long storeId);
}
