package com.lemon.erp.modules.ecom.cs.dto;

import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 评价 / 店铺反馈列表与统计的条件
 */
@Data
public class ReviewQuery {

    private Long storeId;
    private EcomPlatform platform;
    private String sellerSku;
    /** 评分上限（如 3 = 只看差评） */
    private Integer maxRating;
    /** 评分下限 */
    private Integer minRating;
    private CsReviewStatus handleStatus;
    /** 是否已回复 1 是 0 否 */
    private Integer replied;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate beginDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    /** 内容、标题、评价人、订单号、SKU */
    private String keyword;
}
