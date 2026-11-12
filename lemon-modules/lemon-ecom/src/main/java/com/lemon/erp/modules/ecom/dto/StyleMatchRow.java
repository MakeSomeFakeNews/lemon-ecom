package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.PairingMatchType;

/**
 * 按款式批量映射的预览结果：在线商品与配对到的款式 SKU（没有配对到时 materialId 为空）
 */
public record StyleMatchRow(Long listingId, String storeName, String sellerSku, String title, String variant, Long currentMaterialId,
                            String currentMaterialCode, Long materialId, String materialCode, String materialName, String variantText,
                            PairingMatchType matchType) {
}
