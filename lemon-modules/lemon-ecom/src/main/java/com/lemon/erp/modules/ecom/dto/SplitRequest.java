package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拆分订单：每个元素是一个新包裹包含的订单行与件数；没有分配的件数留待之后（补货后「重新检查库存」生成包裹）
 */
public record SplitRequest(
        @NotEmpty(message = "{ecom.valid.ops.splitPackagesRequired}") @Size(max = 20, message = "{ecom.valid.ops.splitTooManyPackages}")
        List<@Valid @NotEmpty(message = "{ecom.valid.ops.splitPackageEmpty}") List<@Valid @NotNull Line>> packages) {

    public record Line(@NotNull(message = "{ecom.valid.ops.splitLineRequired}") Long orderItemId,
                       @NotNull(message = "{ecom.valid.ops.splitQtyRequired}")
                       @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.ops.splitQtyPositive}") BigDecimal qty) {
    }
}
