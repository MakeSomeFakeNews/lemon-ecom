package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhStock;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomOwhStockMapper extends BaseMapper<EcomOwhStock> {

    /**
     * 库存列表：带 ERP 账面库存与差异
     *
     * @param diffOnly 只看有差异的
     */
    IPage<EcomOwhStock> selectPageList(IPage<EcomOwhStock> page, @Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId,
                                      @Param("keyword") String keyword, @Param("diffOnly") boolean diffOnly);

    /** 汇总：SKU 数、有差异的 SKU 数、海外仓可用 / 在途 / 锁定合计、ERP 账面合计 */
    Map<String, Object> selectSummary(@Param("accountId") Long accountId, @Param("warehouseId") Long warehouseId);

    /** 指定行（带 ERP 账面库存），调整用 */
    List<EcomOwhStock> selectWithErp(@Param("ids") Collection<Long> ids);

    /** 按编码查物料 ID：[{ code, id }] */
    List<Map<String, Object>> selectMaterialIds(@Param("codes") Collection<String> codes);

    /** 系统仓库中各物料的 ERP 账面库存（大于 0 的）：[{ materialId, code, qty }] */
    List<Map<String, Object>> selectErpStock(@Param("warehouseId") Long warehouseId);

    /** 按 ID 查物料：[{ id, code, name }] */
    List<Map<String, Object>> selectMaterials(@Param("ids") Collection<Long> ids);

    void insertBatch(@Param("list") List<EcomOwhStock> list);
}
