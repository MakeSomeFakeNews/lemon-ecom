package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomSettlementItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomSettlementItemMapper extends BaseMapper<EcomSettlementItem> {

    List<EcomSettlementItem> selectBySettlementId(@Param("settlementId") Long settlementId);

    int insertBatch(@Param("items") List<EcomSettlementItem> items);

    /** 结算单中按平台订单号汇总的费用：platformOrderNo、commission、fulfillment、refund、other */
    List<Map<String, Object>> selectOrderFees(@Param("settlementId") Long settlementId);
}
