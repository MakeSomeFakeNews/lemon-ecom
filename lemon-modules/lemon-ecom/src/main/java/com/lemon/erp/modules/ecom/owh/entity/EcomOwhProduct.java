package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 海外仓物流产品（从服务商拉取）；可对应一条物流渠道，审单规则指定该渠道的包裹推送时使用此产品
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_product")
public class EcomOwhProduct extends BaseEntity {

    private Long accountId;
    private String productCode;
    private String productName;
    private String owhCode;
    private String carrierName;
    private Long channelId;
    private EnableStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;

    @TableField(exist = false)
    private String channelName;
}
