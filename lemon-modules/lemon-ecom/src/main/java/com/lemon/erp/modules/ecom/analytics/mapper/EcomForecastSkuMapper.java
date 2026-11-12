package com.lemon.erp.modules.ecom.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSku;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface EcomForecastSkuMapper extends BaseMapper<EcomForecastSku> {

    int insertBatch(@Param("items") List<EcomForecastSku> items);

    int deleteByListings(@Param("listingIds") Collection<Long> listingIds);

    int deleteGeneratedBefore(@Param("time") LocalDateTime time);

    /** 预测列表：在售 SKU 的预测汇总（含店铺、SKU、标题、物料） */
    IPage<EcomForecastSku> selectRows(Page<EcomForecastSku> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                      @Param("method") ForecastMethod method, @Param("listingIds") Collection<Long> listingIds);
}
