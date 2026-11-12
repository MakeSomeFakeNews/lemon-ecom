package com.lemon.erp.modules.ecom.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 在售 SKU 的每日库存快照（定时任务每天记录）：平台仓、本地仓的可售数（平台 SKU 件数，组合商品为能凑齐的套数），
 * 按配送方式取可售数（平台仓配送取平台仓，自发货取本地仓），可售数不大于 0 记为断货
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_stock_snapshot")
public class EcomStockSnapshot extends BaseEntity {

    private LocalDate snapshotDate;
    private Long listingId;
    private Long storeId;
    private FulfillmentType fulfillment;
    /** 平台仓可售（店铺平台仓的空闲库存 ÷ 多件装） */
    private BigDecimal platformQty;
    /** 本地仓可售（店铺默认发货仓的空闲库存 ÷ 多件装） */
    private BigDecimal localQty;
    /** 按配送方式的可售数 */
    private BigDecimal sellableQty;
}
