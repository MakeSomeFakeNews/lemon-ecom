package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomAdCampaignMapper extends BaseMapper<EcomAdCampaign> {

    IPage<EcomAdCampaign> selectPageList(IPage<EcomAdCampaign> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                         @Param("status") EcomAdStatus status, @Param("adType") EcomAdType adType);

    EcomAdCampaign selectDetail(@Param("id") Long id);

    /** 规则 / 报表的对象：未归档的活动（店铺、名称包含、广告类型筛选，为空不限） */
    List<AdsViews.RuleEntity> selectEntities(@Param("storeIds") Collection<Long> storeIds, @Param("keyword") String keyword,
                                             @Param("adType") EcomAdType adType, @Param("campaignId") Long campaignId,
                                             @Param("includeArchived") boolean includeArchived);
}
