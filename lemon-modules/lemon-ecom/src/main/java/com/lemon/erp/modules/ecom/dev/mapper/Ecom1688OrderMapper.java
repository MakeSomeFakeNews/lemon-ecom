package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Order;
import com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface Ecom1688OrderMapper extends BaseMapper<Ecom1688Order> {

    /**
     * 分页（不含物流轨迹），带账号、采购订单号、供应商、明细行数
     *
     * @param keyword 1688 订单号、采购订单号、卖家、物流单号
     */
    IPage<Ecom1688Order> selectPageList(IPage<Ecom1688Order> page, @Param("status") Ali1688OrderStatus status, @Param("keyword") String keyword,
                                        @Param("scmOrderId") Long scmOrderId);

    Ecom1688Order selectDetail(@Param("id") Long id);

    /** 采购订单生成的 1688 采购单（不含物流轨迹） */
    List<Ecom1688Order> selectByScmOrder(@Param("scmOrderId") Long scmOrderId);
}
