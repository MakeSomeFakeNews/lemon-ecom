package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 售后工单（客服）
 */
public interface EcomCsTicketMapper extends BaseMapper<EcomCsTicket> {

    IPage<EcomCsTicket> selectPageList(IPage<EcomCsTicket> page, @Param("storeId") Long storeId, @Param("ticketType") CsTicketType ticketType,
                                       @Param("sourceType") CsSourceType sourceType, @Param("keyword") String keyword);

    List<EcomCsTicket> selectBySource(@Param("sourceType") CsSourceType sourceType, @Param("sourceId") Long sourceId);
}
