package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhReturn;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomOwhReturnMapper extends BaseMapper<EcomOwhReturn> {

    IPage<EcomOwhReturn> selectPageList(IPage<EcomOwhReturn> page, @Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                        @Param("status") OwhReturnStatus status, @Param("keyword") String keyword);

    List<Map<String, Object>> countByStatus(@Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    EcomOwhReturn selectDetail(@Param("id") Long id);

    EcomOwhReturn selectForUpdate(@Param("id") Long id);

    /**
     * 可生成退件单的 ERP 退货退款单：退货退款、待处理、退回仓库是海外仓（已映射）、还没有有效退件单
     * [{ id, docNo, platformOrderNo, storeName, warehouseName, createTime }]
     */
    List<Map<String, Object>> selectSources(@Param("keyword") String keyword);

    /** 需要自动同步的退件单 ID（账号开启自动同步） */
    List<Long> selectSyncingIds();
}
