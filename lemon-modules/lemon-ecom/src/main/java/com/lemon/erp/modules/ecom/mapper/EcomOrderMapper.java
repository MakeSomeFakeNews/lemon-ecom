package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomOrderMapper extends BaseMapper<EcomOrder> {

    IPage<EcomOrder> selectPageList(IPage<EcomOrder> page, @Param("q") OrderQuery query);

    EcomOrder selectDetail(@Param("id") Long id);

    /** 店铺中若干平台订单号对应的订单（判断重复导入） */
    List<EcomOrder> selectByStoreOrderNos(@Param("storeId") Long storeId, @Param("orderNos") Collection<String> orderNos);
}
