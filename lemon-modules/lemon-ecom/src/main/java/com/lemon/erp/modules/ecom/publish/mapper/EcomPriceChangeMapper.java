package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceChange;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 改价 / 改库存历史 */
public interface EcomPriceChangeMapper extends BaseMapper<EcomPriceChange> {

    int insertBatch(@Param("items") List<EcomPriceChange> items);

    Page<EcomPriceChange> selectPageList(Page<EcomPriceChange> page, @Param("storeId") Long storeId, @Param("listingId") Long listingId,
                                         @Param("keyword") String keyword, @Param("changeType") PriceChangeType changeType,
                                         @Param("source") PriceChangeSource source, @Param("status") PriceChangeStatus status,
                                         @Param("batchNo") String batchNo, @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
