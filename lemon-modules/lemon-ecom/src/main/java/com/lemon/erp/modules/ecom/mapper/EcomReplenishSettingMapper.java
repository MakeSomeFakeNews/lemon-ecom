package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomReplenishSetting;
import org.apache.ibatis.annotations.Param;

public interface EcomReplenishSettingMapper extends BaseMapper<EcomReplenishSetting> {

    /** 设置详情（带在线商品 / 物料的显示名） */
    EcomReplenishSetting selectDetail(@Param("id") Long id);
}
