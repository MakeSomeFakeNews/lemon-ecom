package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import org.apache.ibatis.annotations.Param;

public interface EcomLogisticsChannelMapper extends BaseMapper<EcomLogisticsChannel> {

    IPage<EcomLogisticsChannel> selectPageList(IPage<EcomLogisticsChannel> page, @Param("keyword") String keyword,
                                               @Param("status") EnableStatus status, @Param("transportMode") String transportMode);

    EcomLogisticsChannel selectDetail(@Param("id") Long id);
}
