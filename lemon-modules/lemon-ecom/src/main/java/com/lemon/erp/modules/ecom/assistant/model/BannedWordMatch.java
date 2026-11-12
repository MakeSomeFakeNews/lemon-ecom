package com.lemon.erp.modules.ecom.assistant.model;

/** 违禁词的匹配方式（字典 ecom_banned_word_match） */
public enum BannedWordMatch {
    /** 整词：前后不是字母或数字（中日韩文字按包含处理） */
    WORD,
    /** 包含即命中 */
    CONTAINS
}
