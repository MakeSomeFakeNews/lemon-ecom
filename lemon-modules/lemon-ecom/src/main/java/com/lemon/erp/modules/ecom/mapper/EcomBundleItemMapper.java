package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomBundleItem;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomBundleItemMapper extends BaseMapper<EcomBundleItem> {

    /** 若干组合商品的组成（含物料编码、名称），按商品、顺序排序 */
    List<EcomBundleItem> selectByListingIds(@Param("listingIds") Collection<Long> listingIds);

    int insertBatch(@Param("items") List<EcomBundleItem> items);
}
