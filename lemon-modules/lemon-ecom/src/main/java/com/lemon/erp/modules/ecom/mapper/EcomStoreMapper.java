package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.apache.ibatis.annotations.Param;

public interface EcomStoreMapper extends BaseMapper<EcomStore> {

    IPage<EcomStore> selectPageList(IPage<EcomStore> page, @Param("keyword") String keyword, @Param("platform") EcomPlatform platform,
                                    @Param("status") EnableStatus status);

    EcomStore selectDetail(@Param("id") Long id);
}
