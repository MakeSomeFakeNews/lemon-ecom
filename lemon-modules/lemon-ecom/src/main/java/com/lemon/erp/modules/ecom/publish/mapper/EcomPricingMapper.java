package com.lemon.erp.modules.ecom.publish.mapper;

import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 定价用到的统计：在线商品的近期销量（未取消订单的件数） */
@Mapper
public interface EcomPricingMapper {

    List<PublishViews.ListingSales> selectSales(@Param("listingIds") Collection<Long> listingIds, @Param("since") LocalDateTime since);
}
