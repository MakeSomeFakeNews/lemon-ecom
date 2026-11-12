package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomAdRule;
import org.apache.ibatis.annotations.Param;

public interface EcomAdRuleMapper extends BaseMapper<EcomAdRule> {

    IPage<EcomAdRule> selectPageList(IPage<EcomAdRule> page, @Param("keyword") String keyword, @Param("status") EnableStatus status);
}
