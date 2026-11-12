package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 店铺反馈（亚马逊 Seller Feedback 等）：店铺 × 反馈 ID 唯一，低分（不超过 3 星）为待处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_feedback")
public class EcomFeedback extends BaseEntity {

    private Long storeId;
    private EcomPlatform platform;
    private String feedbackId;
    private Integer rating;
    private String comments;
    private String rater;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate feedbackDate;
    private Long orderId;
    private String platformOrderNo;
    private String response;
    private CsReviewStatus handleStatus;
    private Long handlerId;
    private String handleNote;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String handlerName;
}
