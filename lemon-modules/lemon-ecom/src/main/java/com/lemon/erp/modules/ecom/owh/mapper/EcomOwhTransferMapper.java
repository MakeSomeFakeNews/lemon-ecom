package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransfer;
import com.lemon.erp.modules.ecom.owh.model.OwhTransferStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomOwhTransferMapper extends BaseMapper<EcomOwhTransfer> {

    IPage<EcomOwhTransfer> selectPageList(IPage<EcomOwhTransfer> page, @Param("warehouseId") Long warehouseId,
                                          @Param("status") OwhTransferStatus status, @Param("keyword") String keyword);

    List<Map<String, Object>> countByStatus(@Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    EcomOwhTransfer selectDetail(@Param("id") Long id);

    EcomOwhTransfer selectForUpdate(@Param("id") Long id);

    /** 需要自动同步的调拨单 ID：调出仓处理中（调出海外仓的账号开启自动同步）、调入海外仓的在途单 */
    List<Long> selectSyncingIds();
}
