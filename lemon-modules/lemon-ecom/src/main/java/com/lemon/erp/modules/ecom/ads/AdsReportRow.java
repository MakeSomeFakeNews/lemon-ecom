package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告报告的一行（统一格式）：广告接口拉取的报告与文件导入的报告（{@code EcomAdReportParser}）都转为它再写入。
 * 活动 / 广告组 / 投放既可以用平台 ID 也可以用名称关联（文件导入通常只有名称）；金额为店铺币种
 */
@Data
public class AdsReportRow {

    /** 文件中的行号（导入出错时提示），接口拉取的为 0 */
    private int rowNum;
    private LocalDate date;
    private String campaignPlatformId;
    private String campaignName;
    private EcomAdStatus campaignStatus;
    private BigDecimal campaignBudget;
    private String adGroupPlatformId;
    private String adGroupName;
    private String targetPlatformId;
    /** 关键词文字或投放表达式 */
    private String targetText;
    private EcomAdTargetType targetType;
    private EcomAdMatchType matchType;
    /** 客户搜索词（搜索词报告） */
    private String searchTerm;
    /** 推广的平台 SKU（推广商品报告） */
    private String sellerSku;
    private String asin;
    private long impressions;
    private long clicks;
    private BigDecimal spend = BigDecimal.ZERO;
    private BigDecimal sales = BigDecimal.ZERO;
    private int orders;
    private int units;
    private String currency;
}
