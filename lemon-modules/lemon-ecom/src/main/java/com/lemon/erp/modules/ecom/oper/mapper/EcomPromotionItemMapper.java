package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotionItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 促销活动的参与 SKU
 */
public interface EcomPromotionItemMapper extends BaseMapper<EcomPromotionItem> {

    int insertBatch(@Param("items") List<EcomPromotionItem> items);
}
