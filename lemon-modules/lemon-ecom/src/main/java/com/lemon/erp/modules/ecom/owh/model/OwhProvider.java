package com.lemon.erp.modules.ecom.owh.model;

/**
 * 对接的第三方海外仓服务商（字典 ecom_owh_provider），每个对应一个 owh/connector/OwhConnector 实现
 */
public enum OwhProvider {
    /** 谷仓海外仓（Goodcang OMS 开放接口） */
    GOODCANG,
    /** 万邑通（WINIT 开放平台） */
    WINIT,
    /** 递四方海外仓（4PX 开放平台 FB4 / 订单履约接口） */
    FOURPX_FB4
}
