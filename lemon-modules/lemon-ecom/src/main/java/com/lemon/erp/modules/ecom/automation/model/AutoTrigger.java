package com.lemon.erp.modules.ecom.automation.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 自动化触发器（字典 ecom_auto_trigger）。每个触发器的可用字段、关联对象、取最近事件的方式由
 * {@code automation.trigger} 包中的 {@code AutoTriggerSource} 实现声明；新增触发器：在这里加一个枚举值（分类、产生方式、关联对象），
 * 再在某个 AutoTriggerSource 中声明字段并产生事件，字典 ecom_auto_trigger 和语言包同步增加
 */
public enum AutoTrigger {
    /** 新订单 */
    ORDER_NEW(AutoCategory.ORDER, AutoScanMode.REALTIME, AutoRef.ORDER, AutoRef.STORE),
    /** 订单异常（SKU 未匹配等） */
    ORDER_EXCEPTION(AutoCategory.ORDER, AutoScanMode.REALTIME, AutoRef.ORDER, AutoRef.STORE),
    /** 订单预估亏损 */
    ORDER_LOSS(AutoCategory.ORDER, AutoScanMode.REALTIME, AutoRef.ORDER, AutoRef.STORE),
    /** 断货风险（可售天数不足，每日扫描） */
    STOCKOUT_RISK(AutoCategory.STOCK, AutoScanMode.DAILY, AutoRef.LISTING, AutoRef.MATERIAL, AutoRef.STORE),
    /** 本地库存低于安全库存（每日扫描） */
    STOCK_LOW(AutoCategory.STOCK, AutoScanMode.DAILY, AutoRef.MATERIAL),
    /** 新差评 */
    REVIEW_NEGATIVE(AutoCategory.CS, AutoScanMode.REALTIME, AutoRef.REVIEW, AutoRef.LISTING, AutoRef.STORE),
    /** 买家消息超时未回复 */
    MESSAGE_TIMEOUT(AutoCategory.CS, AutoScanMode.FREQUENT, AutoRef.THREAD, AutoRef.STORE),
    /** 商品被跟卖 */
    LISTING_HIJACKED(AutoCategory.LISTING, AutoScanMode.REALTIME, AutoRef.MONITOR, AutoRef.LISTING, AutoRef.STORE),
    /** 排名大跌 */
    LISTING_RANK_DROP(AutoCategory.LISTING, AutoScanMode.REALTIME, AutoRef.MONITOR, AutoRef.LISTING, AutoRef.STORE),
    /** 商品被下架 */
    LISTING_DELISTED(AutoCategory.LISTING, AutoScanMode.REALTIME, AutoRef.MONITOR, AutoRef.LISTING, AutoRef.STORE),
    /** 店铺授权失效 */
    STORE_AUTH_INVALID(AutoCategory.STORE, AutoScanMode.REALTIME, AutoRef.STORE),
    /** 渠道同步失败 */
    SYNC_FAILED(AutoCategory.STORE, AutoScanMode.REALTIME, AutoRef.STORE),
    /** 头程货件签收 */
    INBOUND_RECEIVED(AutoCategory.LOGISTICS, AutoScanMode.REALTIME, AutoRef.INBOUND, AutoRef.STORE),
    /** 头程货件超期未签收（每日扫描） */
    INBOUND_OVERDUE(AutoCategory.LOGISTICS, AutoScanMode.DAILY, AutoRef.INBOUND, AutoRef.STORE),
    /** 广告 ACOS 超过阈值（每日扫描，近 7 天） */
    AD_ACOS_HIGH(AutoCategory.ADS, AutoScanMode.DAILY, AutoRef.CAMPAIGN, AutoRef.STORE),
    /** 广告预算耗尽 */
    AD_BUDGET_EXHAUSTED(AutoCategory.ADS, AutoScanMode.FREQUENT, AutoRef.CAMPAIGN, AutoRef.STORE),
    /** FBA 索赔临近截止（每日扫描） */
    FBA_CLAIM_DEADLINE(AutoCategory.FBA, AutoScanMode.DAILY, AutoRef.CLAIM, AutoRef.STORE),
    /** 退货率异常（每日扫描，近 30 天） */
    RETURN_RATE_HIGH(AutoCategory.ORDER, AutoScanMode.DAILY, AutoRef.LISTING, AutoRef.MATERIAL, AutoRef.STORE),
    /** 价格被竞品低于（每日扫描） */
    COMPETITOR_PRICE_LOWER(AutoCategory.PRICE, AutoScanMode.DAILY, AutoRef.LISTING, AutoRef.STORE);

    private final AutoCategory category;
    private final AutoScanMode mode;
    private final Set<AutoRef> refs;

    AutoTrigger(AutoCategory category, AutoScanMode mode, AutoRef first, AutoRef... rest) {
        this.category = category;
        this.mode = mode;
        this.refs = EnumSet.of(first, rest);
    }

    public AutoCategory category() {
        return category;
    }

    public AutoScanMode mode() {
        return mode;
    }

    /** 事件可能关联的对象（实际事件中可能为空，如评价没有关联订单） */
    public Set<AutoRef> refs() {
        return refs;
    }

    public boolean scan() {
        return mode != AutoScanMode.REALTIME;
    }

    /** 名称的消息键 */
    public String label() {
        return "ecom.enum.auto.trigger." + name();
    }
}
