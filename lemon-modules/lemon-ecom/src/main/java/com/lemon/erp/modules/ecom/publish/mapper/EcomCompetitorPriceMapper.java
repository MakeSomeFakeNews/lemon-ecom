package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ecom.publish.entity.EcomCompetitorPrice;
import com.lemon.erp.modules.ecom.publish.model.CompetitorSource;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 竞品价格快照 */
public interface EcomCompetitorPriceMapper extends BaseMapper<EcomCompetitorPrice> {

    int insertBatch(@Param("items") List<EcomCompetitorPrice> items);

    /**
     * @param latestOnly 每个在线商品只返回最近一条
     */
    Page<EcomCompetitorPrice> selectPageList(Page<EcomCompetitorPrice> page, @Param("storeId") Long storeId, @Param("listingId") Long listingId,
                                             @Param("keyword") String keyword, @Param("source") CompetitorSource source,
                                             @Param("latestOnly") boolean latestOnly);

    /** 若干在线商品各自最近的一条快照（快照时间不早于 since） */
    List<EcomCompetitorPrice> selectLatest(@Param("listingIds") Collection<Long> listingIds, @Param("since") LocalDateTime since);
}
