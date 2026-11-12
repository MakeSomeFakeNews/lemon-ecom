package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;

import java.math.BigDecimal;

/**
 * 对广告接口的一项修改：竞价（广告组默认竞价 / 投放竞价）、日预算（活动）、状态，为空的项不修改
 *
 * @param localId    本地记录 ID（结果按它对应）
 * @param targetType 投放的类型（亚马逊关键词与商品投放是两个接口），其他层级为空
 */
public record AdsChange(Long localId, EcomAdLevel level, String platformId, EcomAdTargetType targetType, BigDecimal bid, BigDecimal budget,
                        EcomAdStatus status) {

    public static AdsChange bid(Long localId, EcomAdLevel level, String platformId, EcomAdTargetType targetType, BigDecimal bid) {
        return new AdsChange(localId, level, platformId, targetType, bid, null, null);
    }
}
