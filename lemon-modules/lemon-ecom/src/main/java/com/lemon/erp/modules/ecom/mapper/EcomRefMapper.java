package com.lemon.erp.modules.ecom.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 主数据引用检查与往来单位合并
 */
public interface EcomRefMapper {

    /** 引用物料的单据名称消息键，没有为 null */
    String findMaterial(@Param("id") Long id);

    String findWarehouse(@Param("id") Long id);

    String findCustomer(@Param("id") Long id);

    String findSupplier(@Param("id") Long id);

    String findUser(@Param("id") Long id);

    /** 被合并客户对应的店铺数 */
    long countStoresByCustomers(@Param("ids") List<Long> ids);

    int mergeStoreCustomer(@Param("ids") List<Long> ids, @Param("targetId") Long targetId);
}
