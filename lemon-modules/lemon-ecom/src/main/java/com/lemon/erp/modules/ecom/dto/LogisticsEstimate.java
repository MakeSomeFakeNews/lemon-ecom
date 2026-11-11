package com.lemon.erp.modules.ecom.dto;

import java.math.BigDecimal;

/**
 * 运费试算结果：一条渠道按目的国和计费重量匹配到的费率
 *
 * @param fee     运费（本位币）= 固定费 + 每公斤 × 重量
 * @param country 匹配到的费率国家（* 为通用）
 */
public record LogisticsEstimate(Long channelId, String channelCode, String channelName, String provider, String transportMode,
                                BigDecimal fee, Integer daysMin, Integer daysMax, String country) {
}
