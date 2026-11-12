package com.lemon.erp.modules.ecom.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 违禁词：平台 / 站点为空表示全部适用 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_banned_word")
public class EcomBannedWord extends BaseEntity {

    private String word;
    /** 类型（业务字典 ecom_banned_word_type） */
    private String wordType;
    private BannedWordLevel level;
    private BannedWordMatch matchMode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private EcomPlatform platform;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String marketplace;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String suggestion;
    private Integer builtin;
    private EnableStatus status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    private Long createBy;
}
