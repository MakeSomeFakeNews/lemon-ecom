package com.lemon.erp.modules.ecom.analytics.model;

/**
 * ABC 分类（系统字典 ecom_abc_class）：按销售额或毛利从高到低累计，累计占比（不含本行）未达 A 阈值为 A，未达 B 阈值为 B，其余为 C
 */
public enum AbcClass {
    A,
    B,
    C
}
