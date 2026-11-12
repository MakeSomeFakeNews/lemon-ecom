package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOrderRuleMapper extends BaseMapper<EcomOrderRule> {

    IPage<EcomOrderRule> selectPageList(IPage<EcomOrderRule> page, @Param("keyword") String keyword, @Param("status") EnableStatus status);

    /** 启用的规则，按优先级、ID 排序 */
    List<EcomOrderRule> selectEnabled();
}
