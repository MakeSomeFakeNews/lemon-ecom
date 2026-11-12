package com.lemon.erp.modules.ecom.cs.model;

/**
 * 客服与评价的常量
 */
public final class CsConstants {

    private CsConstants() {
    }

    /** 低分评价：评分不超过该值时自动生成待处理任务并提醒店铺负责人 */
    public static final int LOW_RATING = 3;
    /** 平台没有声明回复时限时的默认值（小时） */
    public static final int DEFAULT_REPLY_HOURS = 24;
    /** 业务字典：模板语言 */
    public static final String LANGUAGE_DICT = "ecom_cs_language";
    /** 同步日志的触发方式 */
    public static final String JOB = "JOB";
    public static final String MANUAL = "MANUAL";
    /** 第一次同步消息往前取的天数 */
    public static final int FIRST_SYNC_DAYS = 7;
    /** 每次同步往前重叠的分钟数，避免漏消息 */
    public static final int OVERLAP_MINUTES = 30;
    /** 亚马逊请求评价的时间窗口：发货后第 5 天到第 30 天 */
    public static final int REQUEST_MIN_DAYS = 5;
    public static final int REQUEST_MAX_DAYS = 30;
}
