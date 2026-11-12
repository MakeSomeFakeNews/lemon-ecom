package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import org.apache.ibatis.annotations.Param;

/** 刊登模板 */
public interface EcomPublishTemplateMapper extends BaseMapper<EcomPublishTemplate> {

    Page<EcomPublishTemplate> selectPageList(Page<EcomPublishTemplate> page, @Param("keyword") String keyword, @Param("platform") EcomPlatform platform,
                                             @Param("status") EnableStatus status);
}
