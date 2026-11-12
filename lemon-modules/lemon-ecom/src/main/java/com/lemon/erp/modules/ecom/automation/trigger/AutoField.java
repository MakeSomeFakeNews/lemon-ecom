package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.model.AutoFieldType;

/**
 * 触发器的一个可用字段
 *
 * @param key    字段名（事件 fields 的键，消息模板中写 ${key}）
 * @param type   类型
 * @param label  名称的消息键
 * @param dict   ENUM 字段的字典类型（前端据此显示可选值）
 * @param sample 样例值（没有真实事件时试运行使用）
 */
public record AutoField(String key, AutoFieldType type, String label, String dict, Object sample) {

    public static AutoField number(String key, Object sample) {
        return new AutoField(key, AutoFieldType.NUMBER, "ecom.label.auto.field." + key, null, sample);
    }

    public static AutoField text(String key, Object sample) {
        return new AutoField(key, AutoFieldType.TEXT, "ecom.label.auto.field." + key, null, sample);
    }

    public static AutoField enumOf(String key, String dict, Object sample) {
        return new AutoField(key, AutoFieldType.ENUM, "ecom.label.auto.field." + key, dict, sample);
    }

    public static AutoField bool(String key, Object sample) {
        return new AutoField(key, AutoFieldType.BOOLEAN, "ecom.label.auto.field." + key, null, sample);
    }

    /** 店铺相关的公共字段：店铺名称、平台、站点 */
    public static AutoField[] store() {
        return new AutoField[]{text("storeName", "DEMO Amazon US"), enumOf("platform", "ecom_platform", "AMAZON"), text("site", "US")};
    }
}
