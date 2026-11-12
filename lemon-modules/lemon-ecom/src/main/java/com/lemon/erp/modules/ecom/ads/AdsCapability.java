package com.lemon.erp.modules.ecom.ads;

/**
 * 广告接口的能力
 */
public enum AdsCapability {
    /** 列出授权账号下的广告配置文件（profile，一个站点一个） */
    PROFILES,
    /** 拉取广告活动 / 广告组 / 投放结构 */
    STRUCTURE,
    /** 拉取广告报告（日报） */
    REPORTS,
    /** 修改竞价（广告组默认竞价、投放竞价） */
    UPDATE_BID,
    /** 修改活动日预算 */
    UPDATE_BUDGET,
    /** 暂停 / 启用 */
    UPDATE_STATE,
    /** 添加否定关键词 */
    NEGATIVE_KEYWORDS
}
