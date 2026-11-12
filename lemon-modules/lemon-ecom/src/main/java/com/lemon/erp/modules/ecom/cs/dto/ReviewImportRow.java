package com.lemon.erp.modules.ecom.cs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 商品评价导入的一行（常见评价报告的列）：店铺编码、评价 ID（可空，按内容生成）、平台 SKU / 平台商品 ID（ASIN 等，至少一个）、评分、标题、内容、
 * 评价人、评价日期、平台订单号、卖家回复。同一 店铺 + 评价 ID 重复导入时更新
 */
@Data
public class ReviewImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @Size(max = 128, message = "{ecom.valid.cs.reviewIdTooLong}")
    private String reviewId;

    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;

    @NotNull(message = "{ecom.valid.cs.ratingRequired}")
    @Min(value = 1, message = "{ecom.valid.cs.ratingRange}")
    @Max(value = 5, message = "{ecom.valid.cs.ratingRange}")
    private Integer rating;

    @Size(max = 255, message = "{ecom.valid.cs.titleTooLong}")
    private String title;

    @Size(max = 4000, message = "{ecom.valid.cs.contentTooLong}")
    private String content;

    @Size(max = 128, message = "{ecom.valid.cs.reviewerTooLong}")
    private String reviewer;

    @NotNull(message = "{ecom.valid.cs.reviewDateRequired}")
    private LocalDate reviewDate;

    @Size(max = 64, message = "{ecom.valid.cs.orderNoTooLong}")
    private String platformOrderNo;

    @Size(max = 2000, message = "{ecom.valid.cs.replyTooLong}")
    private String replyContent;
}
