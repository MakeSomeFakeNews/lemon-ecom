package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface EcomAdGroupMapper extends BaseMapper<EcomAdGroup> {

    List<EcomAdGroup> selectByCampaign(@Param("campaignId") Long campaignId);

    /** 规则 / 报表的对象：未归档活动下未归档的广告组 */
    List<AdsViews.RuleEntity> selectEntities(@Param("storeIds") Collection<Long> storeIds, @Param("keyword") String keyword,
                                             @Param("adType") EcomAdType adType, @Param("campaignId") Long campaignId,
                                             @Param("includeArchived") boolean includeArchived);
}
