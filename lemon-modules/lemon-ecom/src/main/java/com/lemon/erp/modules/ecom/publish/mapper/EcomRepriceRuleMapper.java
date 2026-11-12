package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceRule;
import org.apache.ibatis.annotations.Param;

/** 自动调价规则 */
public interface EcomRepriceRuleMapper extends BaseMapper<EcomRepriceRule> {

    Page<EcomRepriceRule> selectPageList(Page<EcomRepriceRule> page, @Param("keyword") String keyword, @Param("status") EnableStatus status);
}
