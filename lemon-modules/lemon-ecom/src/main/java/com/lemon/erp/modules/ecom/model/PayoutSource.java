package com.lemon.erp.modules.ecom.model;

/**
 * 回款记录的来源
 */
public enum PayoutSource {
    /** 结算单导入时生成的应到账记录 */
    SETTLEMENT,
    /** 导入的银行 / 收款平台流水（未匹配时单独一条，匹配后并入结算单的记录） */
    IMPORT,
    /** 手工登记到账 */
    MANUAL
}
