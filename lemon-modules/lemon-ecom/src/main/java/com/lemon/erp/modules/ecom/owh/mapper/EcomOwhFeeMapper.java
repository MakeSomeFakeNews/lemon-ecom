package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFee;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EcomOwhFeeMapper extends BaseMapper<EcomOwhFee> {

    IPage<EcomOwhFee> selectPageList(IPage<EcomOwhFee> page, @Param("accountId") Long accountId, @Param("feeType") OwhFeeType feeType,
                                    @Param("status") OwhFeeStatus status, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                    @Param("keyword") String keyword);

    /** 按 月份 × 费用类型 汇总本位币金额：[{ period, feeType, amount, cnt }] */
    List<Map<String, Object>> selectSummary(@Param("accountId") Long accountId, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 出库单的费用合计（本位币）：[{ outboundId, amount }] */
    List<Map<String, Object>> sumByOutbound(@Param("ids") java.util.Collection<Long> outboundIds);
}
