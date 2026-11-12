package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dto.MaterialPairing;
import com.lemon.erp.modules.ecom.dto.SpuGroup;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 商品体系的查询：SPU 视图、SKU 配对中心（只读关联物料、店铺）
 */
public interface EcomCatalogMapper {

    /** SPU 视图：店铺 + 父商品 ID 分组（没有父商品 ID 的商品自成一组） */
    IPage<SpuGroup> selectSpuPage(IPage<SpuGroup> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                  @Param("mapped") Boolean mapped);

    /** 物料及其在线商品数（单品映射）、作为组件的组合商品数、店铺数、平台 */
    IPage<MaterialPairing> selectMaterialPage(IPage<MaterialPairing> page, @Param("keyword") String keyword, @Param("pairedOnly") Boolean pairedOnly);

    /** 映射到该物料的单品在线商品 */
    List<EcomListing> selectListingsOfMaterial(@Param("materialId") Long materialId);

    /** 含有该物料的组合商品（packQty 为组合中的用量） */
    List<EcomListing> selectBundlesOfMaterial(@Param("materialId") Long materialId);

    /** 未配对的在线商品（单品且没有映射物料、组合且没有组成） */
    IPage<EcomListing> selectUnpaired(IPage<EcomListing> page, @Param("storeId") Long storeId, @Param("keyword") String keyword);

    /** 按编码找物料（只取启用的非款式物料）：[{ id, code, name, spec }] */
    List<java.util.Map<String, Object>> selectMaterialsByCodes(@Param("codes") Collection<String> codes);

    /** 有异常（SKU 未匹配）的订单 ID，可按店铺 */
    List<Long> selectExceptionOrderIds(@Param("storeId") Long storeId, @Param("limit") int limit);
}
