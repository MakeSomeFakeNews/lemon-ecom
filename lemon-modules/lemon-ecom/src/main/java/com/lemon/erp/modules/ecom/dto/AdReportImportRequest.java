package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 导入广告后台下载的报告文件（前端读取 xlsx / csv 后原样提交表头与各行文字）：英文表头按名称识别，
 * 报告类型为空时按表头自动判断（有 Customer Search Term 为搜索词报告、有 Advertised SKU / ASIN 为推广商品报告、
 * 有 Targeting / Keyword 为投放报告，否则为活动报告）
 */
@Data
public class AdReportImportRequest {

    @NotNull(message = "{ecom.valid.ads.storeRequired}")
    private Long storeId;

    private EcomAdReportType reportType;

    /** 新建活动的广告类型，为空按平台（亚马逊为 SP） */
    private EcomAdType adType;

    @NotEmpty(message = "{ecom.valid.ads.headersRequired}")
    @Size(max = 100, message = "{ecom.valid.ads.headersRequired}")
    private List<String> headers;

    @NotEmpty(message = "{ecom.valid.ads.rowsRequired}")
    @Size(max = 50000, message = "{ecom.valid.ads.tooManyRows}")
    private List<List<String>> rows;

    /** 第一行数据在文件中的行号（提示出错的行），默认 2 */
    private Integer firstRowNum;
}
