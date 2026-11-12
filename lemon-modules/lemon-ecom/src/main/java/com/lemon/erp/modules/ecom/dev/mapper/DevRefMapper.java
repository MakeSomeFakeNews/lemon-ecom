package com.lemon.erp.modules.ecom.dev.mapper;

import com.lemon.erp.framework.ref.MergeImpact;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品开发与 1688 采购对物料、供应商、用户的引用检查与供应商合并
 */
public interface DevRefMapper {

    String findMaterial(@Param("id") Long id);

    String findSupplier(@Param("id") Long id);

    String findUser(@Param("id") Long id);

    List<MergeImpact> supplierImpacts(@Param("ids") List<Long> ids);

    int mergeSampleSupplier(@Param("ids") List<Long> ids, @Param("targetId") Long targetId);

    int mergeOfferSupplier(@Param("ids") List<Long> ids, @Param("targetId") Long targetId);
}
