package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 快捷回复模板：名称 × 语言唯一，内容中的变量（{buyerName}、{orderNo}、{trackingNo}、{carrier}、{productName}、{storeName}）回复时按会话的订单替换
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_reply_template")
public class EcomReplyTemplate extends BaseEntity {

    @NotBlank(message = "{ecom.valid.cs.templateNameRequired}")
    @Size(max = 64, message = "{ecom.valid.cs.templateNameTooLong}")
    private String name;
    /** 语言（业务字典 ecom_cs_language） */
    @NotBlank(message = "{ecom.valid.cs.languageRequired}")
    @Size(max = 16)
    private String language;
    /** 适用平台，为空不限 */
    private EcomPlatform platform;
    @Size(max = 64, message = "{ecom.valid.cs.sceneTooLong}")
    private String scene;
    @NotBlank(message = "{ecom.valid.cs.templateContentRequired}")
    @Size(max = 2000, message = "{ecom.valid.cs.templateContentTooLong}")
    private String content;
    private Integer sort;
    private EnableStatus status;
    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
}
