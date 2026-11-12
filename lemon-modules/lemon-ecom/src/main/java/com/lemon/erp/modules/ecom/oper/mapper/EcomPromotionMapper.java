package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.oper.dto.PromotionQuery;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 促销活动与效果统计
 */
public interface EcomPromotionMapper extends BaseMapper<EcomPromotion> {

    IPage<EcomPromotion> selectPageList(IPage<EcomPromotion> page, @Param("q") PromotionQuery q);

    EcomPromotion selectDetail(@Param("id") Long id);

    /** 促销日历：与 [begin, end) 有交集、未取消的活动 */
    List<EcomPromotion> selectCalendar(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** 参与 SKU 按天的订单数、件数、销售额（本位币）、利润（按行金额分摊订单利润，本位币）：day、orders、qty、sales、profit */
    List<Map<String, Object>> selectSkuDaily(@Param("storeId") Long storeId, @Param("skus") Collection<String> skus,
                                             @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /** 参与 SKU 在时段内按 SKU 的件数与销售额：sellerSku、title、qty、sales */
    List<Map<String, Object>> selectSkuTotals(@Param("storeId") Long storeId, @Param("skus") Collection<String> skus,
                                              @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
