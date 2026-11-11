package com.lemon.erp.modules.ecom.model;

/**
 * 补货建议的紧急程度：预计断货日期在补货周期内为紧急，在补货周期 + 安全天数内为预警
 */
public enum ReplenishLevel {
    /** 库存充足 */
    OK,
    /** 预警 */
    WARN,
    /** 紧急 */
    URGENT
}
