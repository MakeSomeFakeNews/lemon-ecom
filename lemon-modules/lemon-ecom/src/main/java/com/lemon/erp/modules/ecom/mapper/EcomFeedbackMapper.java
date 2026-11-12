package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.cs.dto.ReviewQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomFeedback;
import org.apache.ibatis.annotations.Param;

/**
 * 店铺反馈（客服）
 */
public interface EcomFeedbackMapper extends BaseMapper<EcomFeedback> {

    IPage<EcomFeedback> selectPageList(IPage<EcomFeedback> page, @Param("q") ReviewQuery q);
}
