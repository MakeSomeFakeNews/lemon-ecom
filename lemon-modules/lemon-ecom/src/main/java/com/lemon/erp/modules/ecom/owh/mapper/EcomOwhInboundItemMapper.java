package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInboundItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhInboundItemMapper extends BaseMapper<EcomOwhInboundItem> {

    List<EcomOwhInboundItem> selectByInbound(@Param("inboundId") Long inboundId);

    void insertBatch(@Param("list") List<EcomOwhInboundItem> list);
}
