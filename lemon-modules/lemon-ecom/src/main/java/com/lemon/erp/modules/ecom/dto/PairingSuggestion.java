package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.PairingMatchType;

/**
 * 未配对平台 SKU 的推荐物料
 *
 * @param score 0–100，编码相同 100、去掉前后缀后相同 90、标题相似按相似度
 */
public record PairingSuggestion(Long materialId, String materialCode, String materialName, String materialSpec, PairingMatchType matchType,
                                int score) {
}
