package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.model.SyncType;
import org.apache.ibatis.annotations.Param;

public interface EcomSyncLogMapper extends BaseMapper<EcomSyncLog> {

    IPage<EcomSyncLog> selectPageList(IPage<EcomSyncLog> page, @Param("storeId") Long storeId, @Param("syncType") SyncType syncType,
                                      @Param("success") Integer success);
}
