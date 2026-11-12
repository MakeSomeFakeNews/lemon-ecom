package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 海外仓服务商账号：服务商、授权（CredentialCipher 加密，只写不读）、环境与自定义地址
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_account")
public class EcomOwhAccount extends BaseEntity {

    @NotBlank(message = "{ecom.valid.owh.codeRequired}")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{ecom.valid.owh.codePattern}")
    private String code;
    @NotBlank(message = "{ecom.valid.owh.nameRequired}")
    @Size(max = 64, message = "{ecom.valid.owh.nameTooLong}")
    private String name;
    @NotNull(message = "{ecom.valid.owh.providerRequired}")
    private OwhProvider provider;
    /** 加密后的授权 JSON，不返回前端 */
    @JsonIgnore
    private String credentials;
    /** 测试环境 1是 */
    private Integer sandbox;
    @Size(max = 255, message = "{ecom.valid.owh.apiUrlTooLong}")
    private String apiBaseUrl;
    @Size(max = 8, message = "{ecom.valid.owh.currencyTooLong}")
    private String currency;
    private EnableStatus status;
    /** 自动同步 1是：定时任务推送 / 同步该账号（演示、测试账号关闭） */
    private Integer autoSync;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime stockSyncTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime feeSyncTime;
    private String lastError;
    @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}")
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 提交的授权（键值），只写；空值不修改已保存的 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> auth;
    /** 是否已保存授权 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean hasCredentials;
    /** 映射的仓库数 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer warehouseCount;
}
