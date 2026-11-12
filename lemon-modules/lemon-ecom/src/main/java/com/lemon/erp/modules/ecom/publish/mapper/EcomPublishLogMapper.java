package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 刊登日志 */
public interface EcomPublishLogMapper extends BaseMapper<EcomPublishLog> {

    List<EcomPublishLog> selectByItem(@Param("itemId") Long itemId);
}
