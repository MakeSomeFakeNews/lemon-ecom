package com.lemon.erp.modules.ecom.dev.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * 1688 采购单状态（字典 ecom_1688_order_status），由 1688 订单详情的 baseInfo.status 转换
 */
public enum Ali1688OrderStatus {
    /** 待付款（waitbuyerpay、waitsellerconfirm、waitbuyerconfirm 等付款前的状态） */
    WAIT_BUYER_PAY,
    /** 待发货（waitsellersend、waitlogisticstakein） */
    WAIT_SELLER_SEND,
    /** 待收货（waitbuyerreceive、waitbuyersign、signinsuccess、signinfailed） */
    WAIT_BUYER_RECEIVE,
    /** 已收货（confirm_goods） */
    CONFIRM_GOODS,
    /** 交易成功（success） */
    SUCCESS,
    /** 已取消（cancel） */
    CANCEL,
    /** 交易终止（terminated） */
    TERMINATED;

    /** 未结束（定时同步的范围） */
    public static final Set<Ali1688OrderStatus> OPEN = EnumSet.of(WAIT_BUYER_PAY, WAIT_SELLER_SEND, WAIT_BUYER_RECEIVE, CONFIRM_GOODS);

    /** 买家可以取消（付款前） */
    public boolean cancellable() {
        return this == WAIT_BUYER_PAY;
    }

    /** 1688 原始状态转换，未知的状态返回 null（保留原状态） */
    public static Ali1688OrderStatus fromAli(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "waitbuyerpay", "waitsellerconfirm", "waitbuyerconfirm", "waitselleract", "waitbuyerconfirmaction" -> WAIT_BUYER_PAY;
            case "waitsellersend", "waitlogisticstakein", "waitsellerpush" -> WAIT_SELLER_SEND;
            case "waitbuyerreceive", "waitbuyersign", "signinsuccess", "signinfailed" -> WAIT_BUYER_RECEIVE;
            case "confirm_goods", "confirm_goods_but_not_fund", "confirm_goods_and_has_subsidy" -> CONFIRM_GOODS;
            case "success" -> SUCCESS;
            case "cancel" -> CANCEL;
            case "terminated" -> TERMINATED;
            default -> null;
        };
    }
}
