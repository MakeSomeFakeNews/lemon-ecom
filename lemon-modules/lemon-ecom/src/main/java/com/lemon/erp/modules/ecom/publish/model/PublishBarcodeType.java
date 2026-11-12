package com.lemon.erp.modules.ecom.publish.model;

/**
 * 刊登的商品条码类型（UPC 豁免另用 upcExempt 标记）
 */
public enum PublishBarcodeType {
    /** UPC（12 位） */
    UPC,
    /** EAN（13 位） */
    EAN,
    /** GTIN-14 */
    GTIN,
    /** JAN（日本） */
    JAN,
    /** ISBN（图书） */
    ISBN
}
