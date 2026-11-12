package com.lemon.erp.modules.ecom.assistant.model;

/** 命中违禁词时的处理方式（字典 ecom_banned_word_level） */
public enum BannedWordLevel {
    /** 阻止发布 */
    BLOCK,
    /** 只提醒（可以继续发布） */
    WARN
}
