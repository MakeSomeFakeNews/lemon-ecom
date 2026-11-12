package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomSettlementItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomSettlementItemMapper extends BaseMapper<EcomSettlementItem> {

    List<EcomSettlementItem> selectBySettlementId(@Param("settlementId") Long settlementId);

    int insertBatch(@Param("items") List<EcomSettlementItem> items);

    /** 结算单中挂订单的明细按 平台订单号 + 类型 汇总：platformOrderNo、itemType、amount */
    List<Map<String, Object>> selectOrderFees(@Param("settlementId") Long settlementId);

    /** 不挂订单（没有订单号或订单不在系统中）的明细 */
    List<EcomSettlementItem> selectUnlinked(@Param("settlementId") Long settlementId);
}
