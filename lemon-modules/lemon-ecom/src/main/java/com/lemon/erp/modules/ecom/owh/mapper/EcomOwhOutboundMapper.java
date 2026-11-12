package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomOwhOutboundMapper extends BaseMapper<EcomOwhOutbound> {

    IPage<EcomOwhOutbound> selectPageList(IPage<EcomOwhOutbound> page, @Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                         @Param("storeId") Long storeId, @Param("status") OwhOutboundStatus status, @Param("keyword") String keyword);

    /** 各状态数量 */
    List<Map<String, Object>> countByStatus(@Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                            @Param("storeId") Long storeId, @Param("keyword") String keyword);

    /** 待推送：发货仓是海外仓（已映射、启用）、待打单、没有有效出库单的包裹 */
    IPage<EcomPackage> selectPending(IPage<EcomPackage> page, @Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                    @Param("storeId") Long storeId, @Param("keyword") String keyword);

    long countPending(@Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId, @Param("storeId") Long storeId,
                      @Param("keyword") String keyword);

    /** 待推送（从未推送过）且仓库映射开启了自动推送的包裹 ID */
    List<Long> selectAutoPushPackageIds(@Param("limit") int limit);

    EcomOwhOutbound selectDetail(@Param("id") Long id);

    EcomOwhOutbound selectForUpdate(@Param("id") Long id);

    /** 包裹最近的一张出库单（按 ID 倒序） */
    List<EcomOwhOutbound> selectByPackageIds(@Param("ids") Collection<Long> ids);

    /** 订单的包裹的出库单 */
    List<EcomOwhOutbound> selectByOrderId(@Param("orderId") Long orderId);

    /** 需要同步的出库单 ID */
    List<Long> selectSyncingIds(@Param("statuses") Collection<OwhOutboundStatus> statuses);

    /** 包裹的平台订单号：[{ packageId, orderNo }] */
    List<Map<String, Object>> selectOrderNos(@Param("ids") Collection<Long> packageIds);
}
