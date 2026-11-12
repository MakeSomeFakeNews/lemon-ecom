package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.PackageQuery;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomPackageMapper extends BaseMapper<EcomPackage> {

    IPage<EcomPackage> selectPageList(IPage<EcomPackage> page, @Param("q") PackageQuery query);

    /** 各状态的包裹数：[{ status, cnt }] */
    List<Map<String, Object>> countByStatus(@Param("q") PackageQuery query);

    EcomPackage selectDetail(@Param("id") Long id);

    /** 若干包裹（带店铺、仓库、渠道名称和订单号），按 ID 排序 */
    List<EcomPackage> selectDetails(@Param("ids") Collection<Long> ids);

    /** 包含某订单的包裹（含已取消） */
    List<EcomPackage> selectByOrderId(@Param("orderId") Long orderId);

    /** 按运单号或包裹号查找（扫描发货），不含已取消 */
    List<EcomPackage> selectByScanCode(@Param("code") String code);

    /** 锁定包裹（打单、发货、取消时防止重复处理） */
    EcomPackage selectForUpdate(@Param("id") Long id);

    /**
     * 仓库中未发货包裹已占用的物料数量（审核通过还没有出库）：[{ materialId, qty }]
     *
     * @param excludeIds 不计入的包裹（重新分配时自身的包裹）
     */
    List<Map<String, Object>> selectReserved(@Param("warehouseId") Long warehouseId, @Param("materialIds") Collection<Long> materialIds,
                                             @Param("excludeIds") Collection<Long> excludeIds);

    /** 拣货库位：仓库中有可用库存的库位（按区域、拣货顺序、编码排序）：[{ materialId, locationCode, zone, qty }] */
    List<Map<String, Object>> selectPickLocations(@Param("warehouseId") Long warehouseId, @Param("materialIds") Collection<Long> materialIds);
}
