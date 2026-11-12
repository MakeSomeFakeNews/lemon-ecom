package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemoval;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;
import org.apache.ibatis.annotations.Param;

public interface EcomFbaRemovalMapper extends BaseMapper<EcomFbaRemoval> {

    IPage<EcomFbaRemoval> selectPageList(IPage<EcomFbaRemoval> page, @Param("storeId") Long storeId, @Param("removalType") FbaRemovalType removalType,
                                         @Param("status") FbaRemovalStatus status, @Param("keyword") String keyword);

    EcomFbaRemoval selectDetail(@Param("id") Long id);

    /** 行锁（收货、弃置出库时防止并发重复处理） */
    EcomFbaRemoval selectForUpdate(@Param("id") Long id);
}
