package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.modules.ecom.model.ListingStatus;

import java.math.BigDecimal;

/**
 * 平台接口取到的商品监控数据：取不到的项为 null（如亚马逊 SP-API 不提供评分、评论数，需要手工录入或导入）
 *
 * @param bullets 五点描述，每点一行
 */
public record MonitorData(String title, String brand, String mainImage, String bullets, String category, Integer rankMain,
                          String subCategory, Integer rankSub, BigDecimal rating, Integer reviewCount, Integer sellerCount,
                          BigDecimal price, String currency, ListingStatus listingStatus) {

    /** 商品在平台上不存在 / 已删除：只有下架状态 */
    public static MonitorData delisted() {
        return new MonitorData(null, null, null, null, null, null, null, null, null, null, null, null, null, ListingStatus.INACTIVE);
    }
}
