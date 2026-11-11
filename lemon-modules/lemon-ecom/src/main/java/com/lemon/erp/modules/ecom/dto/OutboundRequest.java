package com.lemon.erp.modules.ecom.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单出库（生成销售出库）：可指定仓库（为空按配送方式取店铺的平台仓 / 默认发货仓）和业务日期（为空取当天）
 *
 * @param ids 批量出库的订单
 */
public record OutboundRequest(List<Long> ids, Long warehouseId, LocalDate bizDate) {
}
