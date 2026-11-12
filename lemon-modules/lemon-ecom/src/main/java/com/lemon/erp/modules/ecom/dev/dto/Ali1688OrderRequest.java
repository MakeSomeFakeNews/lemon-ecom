package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 采购订单一键下单到 1688
 *
 * @param accountId 下单账号，为空取默认账号
 * @param message   买家留言
 */
public record Ali1688OrderRequest(@NotNull(message = "{ecom.valid.dev.scmOrderRequired}") Long scmOrderId, Long accountId,
                                  @Size(max = 500, message = "{ecom.valid.dev.messageTooLong}") String message) {
}
