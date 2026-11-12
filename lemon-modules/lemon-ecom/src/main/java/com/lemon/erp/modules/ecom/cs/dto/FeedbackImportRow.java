package com.lemon.erp.modules.ecom.cs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 店铺反馈导入的一行（亚马逊 Feedback Manager 报告的列）：店铺编码、日期、评分、留言、卖家回复、平台订单号、评价人邮箱；
 * 反馈 ID 可空（按订单号 + 日期生成）。同一 店铺 + 反馈 ID 重复导入时更新
 */
@Data
public class FeedbackImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @Size(max = 128, message = "{ecom.valid.cs.reviewIdTooLong}")
    private String feedbackId;

    @NotNull(message = "{ecom.valid.cs.reviewDateRequired}")
    private LocalDate feedbackDate;

    @NotNull(message = "{ecom.valid.cs.ratingRequired}")
    @Min(value = 1, message = "{ecom.valid.cs.ratingRange}")
    @Max(value = 5, message = "{ecom.valid.cs.ratingRange}")
    private Integer rating;

    @Size(max = 2000, message = "{ecom.valid.cs.contentTooLong}")
    private String comments;

    @Size(max = 2000, message = "{ecom.valid.cs.replyTooLong}")
    private String response;

    @Size(max = 64, message = "{ecom.valid.cs.orderNoTooLong}")
    private String platformOrderNo;

    @Size(max = 128, message = "{ecom.valid.cs.reviewerTooLong}")
    private String rater;
}
