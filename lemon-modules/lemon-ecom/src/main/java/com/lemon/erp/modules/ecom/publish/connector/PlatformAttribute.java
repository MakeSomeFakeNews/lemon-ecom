package com.lemon.erp.modules.ecom.publish.connector;

import java.util.List;

/**
 * 类目属性
 *
 * @param key         属性键（刊登时作为属性名提交：亚马逊为属性名，虾皮 / TikTok 为属性 ID）
 * @param name        显示名称
 * @param required    是否必填
 * @param inputType   输入方式：TEXT 文本、NUMBER 数字、SELECT 单选、MULTI 多选（多个值用英文逗号分隔）
 * @param options     可选值（没有为空列表）
 * @param description 说明（单位、格式等），没有为 null
 */
public record PlatformAttribute(String key, String name, boolean required, String inputType, List<String> options, String description) {

    public static final String TEXT = "TEXT";
    public static final String NUMBER = "NUMBER";
    public static final String SELECT = "SELECT";
    public static final String MULTI = "MULTI";

    public PlatformAttribute {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
