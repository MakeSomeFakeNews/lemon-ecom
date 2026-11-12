package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomAdTargetMapper extends BaseMapper<EcomAdTarget> {

    IPage<EcomAdTarget> selectPageList(IPage<EcomAdTarget> page, @Param("campaignId") Long campaignId, @Param("adGroupId") Long adGroupId,
                                       @Param("keyword") String keyword, @Param("status") EcomAdStatus status,
                                       @Param("targetType") EcomAdTargetType targetType);

    /** 规则 / 报表的对象：未归档的投放（不含否定关键词） */
    List<AdsViews.RuleEntity> selectEntities(@Param("storeIds") Collection<Long> storeIds, @Param("keyword") String keyword,
                                             @Param("adType") EcomAdType adType, @Param("campaignId") Long campaignId,
                                             @Param("adGroupId") Long adGroupId, @Param("includeArchived") boolean includeArchived);
}
