package com.lemon.erp.modules.ecom.model;

/**
 * 店铺的渠道授权状态（不存库，按授权信息与最近一次授权日志计算）
 */
public enum StoreAuthStatus {
    /** 未授权：没有授权信息，或 OAuth 平台还没有完成授权、必填的密钥没有填写 */
    NONE,
    /** 已授权 */
    AUTHORIZED,
    /** 需重新授权：最近一次刷新令牌 / 测试授权失败 */
    REAUTH
}
