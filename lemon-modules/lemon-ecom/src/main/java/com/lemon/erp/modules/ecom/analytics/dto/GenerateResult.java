package com.lemon.erp.modules.ecom.analytics.dto;

/**
 * 批量生成（快照 / 预测）的结果：处理的 SKU 数、失败的 SKU 数、第一个失败原因
 */
public record GenerateResult(int total, int failed, String firstError) {
}
