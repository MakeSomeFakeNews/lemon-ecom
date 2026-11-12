package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface EcomOwhInboundMapper extends BaseMapper<EcomOwhInbound> {

    IPage<EcomOwhInbound> selectPageList(IPage<EcomOwhInbound> page, @Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                        @Param("status") OwhInboundStatus status, @Param("keyword") String keyword);

    EcomOwhInbound selectDetail(@Param("id") Long id);

    EcomOwhInbound selectForUpdate(@Param("id") Long id);

    /** 各状态数量 */
    List<Map<String, Object>> countByStatus(@Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    /**
     * 可生成入库单的来源：调入仓是海外仓（已映射）、未取消、还没有有效入库单的头程货件和在途调拨单
     * [{ sourceType, sourceId, sourceNo, warehouseId, warehouseName, totalQty, bizDate, status }]
     */
    List<Map<String, Object>> selectSources(@Param("keyword") String keyword);

    /** 在途调拨单（只读）：{ id, docNo, status, toWarehouseId, bizDate } */
    Map<String, Object> selectTransfer(@Param("id") Long id);

    /** 调拨单明细（只读）：[{ materialId, qty }] */
    List<Map<String, Object>> selectTransferItems(@Param("id") Long id);
}
