package com.lemon.erp.modules.ecom.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecast;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface EcomForecastMapper extends BaseMapper<EcomForecast> {

    int insertBatch(@Param("items") List<EcomForecast> items);

    /** 删除这些 SKU 从某天起的预测（重新生成前） */
    int deleteFrom(@Param("listingIds") Collection<Long> listingIds, @Param("date") LocalDate date);

    /** 删除早于某天的预测（已过去的日期） */
    int deleteBefore(@Param("date") LocalDate date);

    /** 删除某次全量生成之前生成、之后没有再生成的预测（已停售的 SKU） */
    int deleteGeneratedBefore(@Param("time") LocalDateTime time);

    /** [from, to) 的预测，按 SKU、日期排序 */
    List<EcomForecast> selectRange(@Param("listingIds") Collection<Long> listingIds, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
