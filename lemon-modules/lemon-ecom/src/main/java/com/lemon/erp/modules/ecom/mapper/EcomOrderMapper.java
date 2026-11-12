package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.OrderQuery;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomOrderMapper extends BaseMapper<EcomOrder> {

    IPage<EcomOrder> selectPageList(IPage<EcomOrder> page, @Param("q") OrderQuery query);

    EcomOrder selectDetail(@Param("id") Long id);

    /** 店铺中若干平台订单号对应的订单（判断重复导入） */
    /** 各处理状态的订单数：[{ localStatus, cnt }] */
    List<Map<String, Object>> countByLocalStatus(@Param("q") OrderQuery query);

    /** 锁定若干订单（合并、拆分时防止并发修改） */
    List<EcomOrder> selectByIdsForUpdate(@Param("ids") Collection<Long> ids);

    List<EcomOrder> selectByStoreOrderNos(@Param("storeId") Long storeId, @Param("orderNos") Collection<String> orderNos);
}
