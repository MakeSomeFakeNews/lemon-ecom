package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dev.entity.EcomProfitScheme;
import org.apache.ibatis.annotations.Param;

public interface EcomProfitSchemeMapper extends BaseMapper<EcomProfitScheme> {

    /** 分页（不含试算参数大字段），带项目名称、创建人 */
    IPage<EcomProfitScheme> selectPageList(IPage<EcomProfitScheme> page, @Param("projectId") Long projectId, @Param("keyword") String keyword);
}
