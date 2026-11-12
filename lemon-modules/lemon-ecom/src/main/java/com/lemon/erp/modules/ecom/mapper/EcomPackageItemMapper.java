package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomPackageItemMapper extends BaseMapper<EcomPackageItem> {

    /** 包裹明细（带平台订单号、SKU、标题、物料），按包裹、订单、行排序 */
    List<EcomPackageItem> selectByPackageIds(@Param("packageIds") Collection<Long> packageIds);

    /** 订单各行已分配到未取消包裹的件数：[{ orderItemId, qty, locked（其中已打单 / 已发货的） }] */
    List<Map<String, Object>> selectAllocated(@Param("orderId") Long orderId);

    int insertBatch(@Param("items") List<EcomPackageItem> items);
}
