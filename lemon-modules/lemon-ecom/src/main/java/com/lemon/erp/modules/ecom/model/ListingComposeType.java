package com.lemon.erp.modules.ecom.model;

/**
 * 在线商品的组成方式（字典 ecom_compose_type）：单品 = 一个物料 × 多件装数量；组合 = 多个物料各若干（ecom_bundle_item）
 */
public enum ListingComposeType {
    /** 单品（含多件装） */
    SINGLE,
    /** 组合（捆绑 / 套装） */
    BUNDLE
}
