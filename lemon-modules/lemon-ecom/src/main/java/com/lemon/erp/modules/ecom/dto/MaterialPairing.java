package com.lemon.erp.modules.ecom.dto;

import lombok.Data;

/**
 * SKU 配对中心的物料行：物料与它对应的在线商品数（单品映射、作为组合商品的组件）、店铺数、平台
 */
@Data
public class MaterialPairing {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String unit;
    private Integer listingCount;
    private Integer bundleCount;
    private Integer storeCount;
    /** 涉及的平台（逗号分隔） */
    private String platforms;
}
