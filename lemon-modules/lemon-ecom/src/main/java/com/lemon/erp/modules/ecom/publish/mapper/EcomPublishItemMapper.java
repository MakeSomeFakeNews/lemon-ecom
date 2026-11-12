package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 刊登记录 */
public interface EcomPublishItemMapper extends BaseMapper<EcomPublishItem> {

    Page<EcomPublishItem> selectPageList(Page<EcomPublishItem> page, @Param("storeId") Long storeId, @Param("platform") EcomPlatform platform,
                                         @Param("status") PublishStatus status, @Param("keyword") String keyword, @Param("draftId") Long draftId);

    /** 各状态的数量（店铺、平台、关键字筛选与列表相同） */
    List<PublishViews.StatusCount> countByStatus(@Param("storeId") Long storeId, @Param("platform") EcomPlatform platform, @Param("keyword") String keyword);

    /** 带店铺、物料信息的刊登记录 */
    List<EcomPublishItem> selectDetails(@Param("ids") Collection<Long> ids);
}
