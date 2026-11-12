package com.lemon.erp.modules.ecom.publish.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 刊登中的命名约定：父 SKU、由物料规格文字推出的变体属性
 */
final class PublishSupportNames {

    /** 父 SKU 后缀（多变体时，亚马逊父体 / eBay 商品组 / 沃尔玛变体组） */
    static final String PARENT_SUFFIX = "-P";

    private PublishSupportNames() {
    }

    static String parentSku(String code) {
        String base = code.length() > 60 ? code.substring(0, 60) : code;
        return base + PARENT_SUFFIX;
    }

    /**
     * 物料规格文字（如「红色 / XL」）→ 变体属性 {Option1: 红色, Option2: XL}（之后可在页面改为 Color、Size 等平台属性名）
     */
    static Map<String, String> variantAttrs(String variantText) {
        Map<String, String> out = new LinkedHashMap<>();
        if (variantText == null || variantText.isBlank()) {
            return out;
        }
        String[] parts = variantText.split("\\s*/\\s*");
        for (int i = 0; i < parts.length && i < 5; i++) {
            if (!parts[i].isBlank()) {
                out.put("Option" + (i + 1), parts[i].trim());
            }
        }
        return out;
    }
}
