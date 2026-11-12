package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaInventory;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EcomFbaInventoryMapper extends BaseMapper<EcomFbaInventory> {

    /** 库存健康：快照 + 店铺、映射物料、售价、尺寸，以及 since 之后的订单件数 */
    List<FbaViews.HealthRow> selectHealthRows(@Param("storeId") Long storeId, @Param("keyword") String keyword, @Param("since") LocalDateTime since);

    /** 店铺平台仓配送的在线商品的头程签收记录（推算库龄），按签收日期倒序 */
    List<FbaViews.ReceiptLot> selectReceiptLots(@Param("storeId") Long storeId);

    /** 店铺平台仓配送的在线商品：平台 SKU、平台库存快照的可售 / 预留 / 不可售、系统平台仓库存（件） */
    List<FbaViews.StockBase> selectStockBase(@Param("storeId") Long storeId);
}
