package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface EcomAdDailyMapper extends BaseMapper<EcomAdDaily> {

    IPage<EcomAdDaily> selectPageList(IPage<EcomAdDaily> page, @Param("storeId") Long storeId, @Param("beginDate") LocalDate beginDate,
                                      @Param("endDate") LocalDate endDate, @Param("keyword") String keyword);

    /**
     * 广告汇总
     *
     * @param groupBy campaign 按活动 / sku 按平台 SKU / store 按店铺
     */
    List<EcomReports.AdRow> selectSummary(@Param("storeId") Long storeId, @Param("beginDate") LocalDate beginDate,
                                          @Param("endDate") LocalDate endDate, @Param("groupBy") String groupBy);
}
