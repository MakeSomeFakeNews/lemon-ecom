package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishVariant;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 刊登变体 */
public interface EcomPublishVariantMapper extends BaseMapper<EcomPublishVariant> {

    int insertBatch(@Param("items") List<EcomPublishVariant> items);

    /** 刊登记录的变体（带物料编码、名称），按排序 */
    List<EcomPublishVariant> selectByItems(@Param("itemIds") Collection<Long> itemIds);
}
