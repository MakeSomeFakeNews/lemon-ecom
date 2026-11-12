package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomReturnItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomReturnItemMapper extends BaseMapper<EcomReturnItem> {

    List<EcomReturnItem> selectByReturnId(@Param("returnId") Long returnId);

    int insertBatch(@Param("items") List<EcomReturnItem> items);

    /** 订单各行已登记（未关闭）的退货数量：orderItemId → qty */
    List<Map<String, Object>> selectReturnedQty(@Param("orderId") Long orderId);

    /** 订单已入库（生成了销售退货）的退货数量：[{ orderItemId, packageId, qty }]，不含 excludeId */
    List<Map<String, Object>> selectReceivedQty(@Param("orderId") Long orderId, @Param("excludeId") Long excludeId);
}
