package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsRecordSource;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品评价：店铺 × 评价 ID 唯一（导入时没有评价 ID 的按内容生成）。低分（不超过 3 星）的新评价为待处理，处理人默认店铺负责人
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_review")
public class EcomReview extends BaseEntity {

    private Long storeId;
    private EcomPlatform platform;
    private String reviewId;
    private String platformItemId;
    private String sellerSku;
    private Long listingId;
    private Integer rating;
    private String title;
    private String content;
    private String reviewer;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reviewDate;
    private Long orderId;
    private String platformOrderNo;
    private Integer replied;
    private String replyContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;
    private CsReviewStatus handleStatus;
    private Long handlerId;
    private String handleNote;
    private CsRecordSource source;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String handlerName;
    @TableField(exist = false)
    private String listingTitle;
}
