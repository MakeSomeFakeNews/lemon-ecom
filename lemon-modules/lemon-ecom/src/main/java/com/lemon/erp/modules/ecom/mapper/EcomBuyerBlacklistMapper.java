package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomBuyerBlacklist;
import com.lemon.erp.modules.ecom.model.BlacklistType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomBuyerBlacklistMapper extends BaseMapper<EcomBuyerBlacklist> {

    IPage<EcomBuyerBlacklist> selectPageList(IPage<EcomBuyerBlacklist> page, @Param("matchType") BlacklistType matchType,
                                             @Param("status") EnableStatus status, @Param("keyword") String keyword);

    /** 对店铺生效的启用条目（店铺为空的全部店铺生效） */
    List<EcomBuyerBlacklist> selectEnabledForStore(@Param("storeId") Long storeId);

    /** 命中次数加 1 */
    int hit(@Param("id") Long id);
}
