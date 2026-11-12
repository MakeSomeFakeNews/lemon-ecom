package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 1688 开放平台采购账号：AppKey / AppSecret（加密）、OAuth 授权得到的令牌（加密）、默认收货地址。
 * 密钥和令牌不返回前端，只返回是否已设置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_1688_account")
public class Ecom1688Account extends BaseEntity {

    private String name;
    private String appKey;
    @JsonIgnore
    private String appSecret;
    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tokenExpire;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refreshExpire;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String memberId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String loginId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime authorizedTime;
    private String apiUrl;
    private String receiverName;
    private String receiverMobile;
    private String province;
    private String city;
    private String district;
    private String town;
    private String address;
    private String postCode;
    private Integer isDefault;
    private EnableStatus status;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastMessage;
    private String remark;

    /** 已设置 AppSecret */
    @TableField(exist = false)
    private Boolean secretSet;
    /** 已完成授权（有访问令牌） */
    @TableField(exist = false)
    private Boolean authorized;
}
