package com.lemon.erp.modules.ecom.dto;

import java.math.BigDecimal;

/**
 * 在线商品的一个组成物料：单品为 映射的物料 × 多件装数量，组合为各组件 × 用量
 *
 * @param qty 一件平台 SKU 包含的物料数量
 */
public record BundleComponent(Long materialId, BigDecimal qty) {
}
