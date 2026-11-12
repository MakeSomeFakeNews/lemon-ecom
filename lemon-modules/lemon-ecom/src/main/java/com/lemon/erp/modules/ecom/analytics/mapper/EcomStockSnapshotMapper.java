package com.lemon.erp.modules.ecom.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.analytics.entity.EcomStockSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Mapper
public interface EcomStockSnapshotMapper extends BaseMapper<EcomStockSnapshot> {

    int insertBatch(@Param("items") List<EcomStockSnapshot> items);

    /** 删除某天这些 SKU 的快照（重新记录前） */
    int deleteDay(@Param("date") LocalDate date, @Param("listingIds") Collection<Long> listingIds);

    /** 可售数不大于 threshold 的快照（断货日），日期在 [from, to] 之间 */
    List<EcomStockSnapshot> selectStockout(@Param("listingIds") Collection<Long> listingIds, @Param("from") LocalDate from,
                                           @Param("to") LocalDate to, @Param("threshold") java.math.BigDecimal threshold);

    /** 删除早于某天的快照（保留期之外） */
    int deleteBefore(@Param("date") LocalDate date);
}
