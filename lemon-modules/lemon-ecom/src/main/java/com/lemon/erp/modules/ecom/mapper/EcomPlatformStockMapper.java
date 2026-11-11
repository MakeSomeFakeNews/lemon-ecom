package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomPlatformStock;
import org.apache.ibatis.annotations.Param;

public interface EcomPlatformStockMapper extends BaseMapper<EcomPlatformStock> {

    /**
     * 平台库存快照与系统平台仓库存对比
     *
     * @param diffOnly true 只看有差异的
     */
    IPage<EcomPlatformStock> selectPageList(IPage<EcomPlatformStock> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                            @Param("diffOnly") Boolean diffOnly);
}
