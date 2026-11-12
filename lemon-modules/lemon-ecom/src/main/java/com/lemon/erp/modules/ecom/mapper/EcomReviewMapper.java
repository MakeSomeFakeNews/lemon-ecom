package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.cs.dto.ReviewQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 商品评价（客服）：列表与统计
 */
public interface EcomReviewMapper extends BaseMapper<EcomReview> {

    IPage<EcomReview> selectPageList(IPage<EcomReview> page, @Param("q") ReviewQuery q, @Param("low") int low);

    EcomReview selectDetail(@Param("id") Long id);

    /** 概况：total、ratingSum、lowCount、pendingCount、repliedCount */
    Map<String, Object> selectSummary(@Param("q") ReviewQuery q, @Param("low") int low);

    /** 按 店铺 + SKU：storeId、storeName、sellerSku、title、cnt、ratingSum、lowCount、lastDate，按差评数、评价数倒序 */
    List<Map<String, Object>> selectSkuStats(@Param("q") ReviewQuery q, @Param("low") int low);

    /** 按天：reviewDate、cnt、ratingSum、lowCount */
    List<Map<String, Object>> selectTrend(@Param("q") ReviewQuery q, @Param("low") int low);
}
