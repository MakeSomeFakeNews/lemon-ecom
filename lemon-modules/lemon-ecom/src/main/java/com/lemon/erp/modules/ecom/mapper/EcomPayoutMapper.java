package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomPayout;
import com.lemon.erp.modules.ecom.model.PayoutStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EcomPayoutMapper extends BaseMapper<EcomPayout> {

    IPage<EcomPayout> selectPageList(IPage<EcomPayout> page, @Param("status") PayoutStatus status, @Param("storeId") Long storeId,
                                     @Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("keyword") String keyword);

    EcomPayout selectDetail(@Param("id") Long id);

    /** 未到账的应到账记录（匹配候选）：带店铺编码 / 名称、结算编号 */
    List<EcomPayout> selectPending(@Param("currency") String currency, @Param("storeId") Long storeId);

    /** 各状态的记录数与金额合计：status、cnt、expectedBase、amountBase */
    List<Map<String, Object>> selectStatusSummary(@Param("storeId") Long storeId);
}
