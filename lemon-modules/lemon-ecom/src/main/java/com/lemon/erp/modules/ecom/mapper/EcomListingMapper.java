package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomListingMapper extends BaseMapper<EcomListing> {

    /**
     * @param mapped true 只看已映射物料的，false 只看未映射的，null 全部
     */
    IPage<EcomListing> selectPageList(IPage<EcomListing> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                      @Param("status") ListingStatus status, @Param("fulfillment") FulfillmentType fulfillment,
                                      @Param("mapped") Boolean mapped, @Param("materialId") Long materialId);

    EcomListing selectDetail(@Param("id") Long id);

    /** 店铺中若干平台 SKU 的在线商品（含物料信息） */
    List<EcomListing> selectByStoreSkus(@Param("storeId") Long storeId, @Param("skus") Collection<String> skus);
}
