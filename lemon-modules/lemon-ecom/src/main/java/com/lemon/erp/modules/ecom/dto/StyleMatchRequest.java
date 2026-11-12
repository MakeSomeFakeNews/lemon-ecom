package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.StyleMatchRule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 按款式批量映射的预览条件：款式（多规格物料）、配对规则；在线商品范围为 listingIds，没有时为店铺（+ 父商品 ID）下的商品
 *
 * @param prefix       按 SKU 编码配对时平台 SKU 要去掉的前缀（可空）
 * @param suffix       要去掉的后缀（可空）
 * @param includeMapped 是否包含已映射的在线商品（重新映射）
 */
public record StyleMatchRequest(
        @NotNull(message = "{ecom.valid.catalog.templateRequired}") Long templateId,
        @NotNull(message = "{ecom.valid.catalog.ruleRequired}") StyleMatchRule rule,
        Long storeId,
        @Size(max = 64) String parentItemId,
        @Size(max = 500, message = "{ecom.valid.ops.tooManyRecords}") List<Long> listingIds,
        @Size(max = 32) String prefix,
        @Size(max = 32) String suffix,
        Boolean includeMapped) {
}
