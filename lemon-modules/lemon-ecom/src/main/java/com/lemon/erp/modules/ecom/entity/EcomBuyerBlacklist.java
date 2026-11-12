package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.BlacklistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 买家黑名单：按买家 ID / 邮箱 / 地址 / 电话匹配（规范化后比较），命中的订单在审单时自动挂起
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_buyer_blacklist")
public class EcomBuyerBlacklist extends BaseEntity {

    @NotNull(message = "{ecom.valid.ord.blacklistTypeRequired}")
    private BlacklistType matchType;
    @NotBlank(message = "{ecom.valid.ord.blacklistValueRequired}")
    @Size(max = 255, message = "{ecom.valid.ord.blacklistValueTooLong}")
    private String matchValue;
    /** 规范化后的匹配值（服务端生成） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String matchKey;
    /** 只对该店铺生效，为空全部店铺 */
    private Long storeId;
    @Size(max = 255, message = "{ecom.valid.ord.blacklistReasonTooLong}")
    private String reason;
    private EnableStatus status;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer hitCount;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHitTime;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
}
