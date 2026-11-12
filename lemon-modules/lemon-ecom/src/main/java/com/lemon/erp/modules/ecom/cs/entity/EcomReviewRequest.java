package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 请求评价记录：每张订单一条（亚马逊每张订单只能请求一次；不符合条件、失败的可以再次请求）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_review_request")
public class EcomReviewRequest extends BaseEntity {

    private Long storeId;
    private Long orderId;
    private String platformOrderNo;
    private CsRequestStatus status;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
}
