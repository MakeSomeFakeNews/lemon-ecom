package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 在线商品的 SPU 视图：店铺 + 父商品 ID 为一组（没有父商品 ID 的商品自成一组）
 */
@Data
public class SpuGroup {
    private Long storeId;
    private String storeName;
    private EcomPlatform platform;
    /** 父商品 ID，没有时为空（单个商品） */
    private String parentItemId;
    /** 没有父商品时为该商品 ID */
    private Long listingId;
    private String title;
    private String imageUrl;
    private Integer variantCount;
    private Integer mappedCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
}
