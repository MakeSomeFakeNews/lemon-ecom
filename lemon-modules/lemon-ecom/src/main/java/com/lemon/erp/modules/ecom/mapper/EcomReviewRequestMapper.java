package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.entity.EcomReviewRequest;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请求评价记录（客服）
 */
public interface EcomReviewRequestMapper extends BaseMapper<EcomReviewRequest> {

    /**
     * 已发货的订单（发货时间在 [shipFrom, shipTo) 内，排除已取消 / 已退款）与请求记录
     *
     * @param pendingOnly 只列还没有成功请求过的
     */
    IPage<CsViews.RequestCandidate> selectCandidates(IPage<CsViews.RequestCandidate> page, @Param("platform") EcomPlatform platform,
                                                     @Param("storeId") Long storeId, @Param("shipFrom") LocalDateTime shipFrom,
                                                     @Param("shipTo") LocalDateTime shipTo, @Param("keyword") String keyword,
                                                     @Param("pendingOnly") boolean pendingOnly, @Param("status") CsRequestStatus status);

    /** 定时请求：店铺中发货时间在窗口内、还没有请求过（或之前失败）的订单 ID */
    List<Long> selectDueOrderIds(@Param("storeId") Long storeId, @Param("shipFrom") LocalDateTime shipFrom, @Param("shipTo") LocalDateTime shipTo,
                                 @Param("limit") int limit);
}
