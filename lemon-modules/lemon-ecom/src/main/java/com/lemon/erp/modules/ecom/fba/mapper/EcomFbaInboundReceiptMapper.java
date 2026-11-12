package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaInboundReceipt;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomFbaInboundReceiptMapper extends BaseMapper<EcomFbaInboundReceipt> {

    /** 货件的明细（对账、按接口同步实收数量） */
    List<FbaViews.ReceiptRow> selectInboundItems(@Param("inboundId") Long inboundId);

    /** 一条货件明细 */
    FbaViews.ReceiptRow selectInboundItem(@Param("itemId") Long itemId);
}
