package com.lemon.erp.modules.ecom.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 违禁词库 */
public interface EcomBannedWordMapper extends BaseMapper<EcomBannedWord> {

    int insertBatch(@Param("items") List<EcomBannedWord> items);
}
