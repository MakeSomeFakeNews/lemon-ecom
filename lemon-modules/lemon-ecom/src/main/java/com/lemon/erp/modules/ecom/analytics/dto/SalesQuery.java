package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.analytics.model.SalesDimension;
import com.lemon.erp.modules.ecom.analytics.model.SalesGrain;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 销量统计的查询条件：维度（默认 SKU）、粒度（默认按日）、日期范围（含首尾）、店铺、平台、关键字（SKU / 标题 / 物料）
 */
@Data
public class SalesQuery {

    private SalesDimension dimension;
    private SalesGrain grain;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate beginDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Long storeId;
    private EcomPlatform platform;
    private String keyword;
}
