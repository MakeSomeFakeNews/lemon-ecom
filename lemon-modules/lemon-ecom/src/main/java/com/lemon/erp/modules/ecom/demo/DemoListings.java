package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.model.FulfillmentType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 演示在线商品的内存视图（订单计划、订单写入用）
 */
final class DemoListings {

    private DemoListings() {
    }

    /**
     * @param id             在线商品 ID
     * @param storeId        店铺 ID
     * @param sku            平台 SKU
     * @param platformItemId 平台商品 ID（ASIN 等）
     * @param title          标题
     * @param price          售价（店铺币种）
     * @param fulfillment    配送方式
     * @param materialId     映射物料（组合与未配对为空）
     * @param packQty        多件装数量
     * @param components     组合商品的组成：物料 ID → 每件用量（单品为空）
     * @param weight         单件毛重（kg）
     * @param popularity     销量权重
     * @param productCode    主物料编码（组合取第一个组件）
     */
    record Item(Long id, Long storeId, String sku, String platformItemId, String title, BigDecimal price, FulfillmentType fulfillment,
                Long materialId, BigDecimal packQty, Map<Long, BigDecimal> components, double weight, int popularity, String productCode) {

        boolean mapped() {
            return materialId != null || components != null;
        }

        /** 每件对应的物料数量 */
        Map<Long, BigDecimal> perUnit() {
            return components != null ? components : materialId == null ? Map.of() : Map.of(materialId, packQty);
        }
    }
}
