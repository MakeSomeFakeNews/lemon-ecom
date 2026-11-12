package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomInboundMapper extends BaseMapper<EcomInbound> {

    IPage<EcomInbound> selectPageList(IPage<EcomInbound> page, @Param("storeId") Long storeId, @Param("status") InboundStatus status,
                                      @Param("keyword") String keyword);

    EcomInbound selectDetail(@Param("id") Long id);

    /** 各状态的条数（status 传 null，条件与列表相同、不含状态）：[{ status, cnt }] */
    List<Map<String, Object>> countByStatus(@Param("storeId") Long storeId, @Param("status") InboundStatus status, @Param("keyword") String keyword);
}
