package com.lemon.erp.modules.ecom.model;

/**
 * 买家黑名单的匹配方式（字典 ecom_blacklist_type）。匹配前都做规范化（小写、去空白，电话只保留数字，地址去标点），
 * 命中的订单自动挂起
 */
public enum BlacklistType {
    /** 买家 ID：与订单的买家（平台买家账号 / 用户名）比较 */
    BUYER,
    /** 买家邮箱 */
    EMAIL,
    /** 收件地址：与 地址 + 邮编 比较（包含即命中） */
    ADDRESS,
    /** 收件电话（后 7 位以上相同即命中） */
    PHONE
}
