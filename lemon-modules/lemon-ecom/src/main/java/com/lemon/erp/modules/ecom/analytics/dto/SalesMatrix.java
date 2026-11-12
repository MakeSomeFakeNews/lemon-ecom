package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.analytics.model.SalesDimension;
import com.lemon.erp.modules.ecom.analytics.model.SalesGrain;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 销量统计矩阵：行是维度对象，列是时间段（periods），每格为件数（qty[i]）与销售额（amount[i]，本位币）；
 * 行合计、环比（上一个同样长度的区间）、同比（去年同期）及增长率（%，上期为 0 时为空）；total 为各列合计
 */
@Data
public class SalesMatrix {

    private SalesDimension dimension;
    private SalesGrain grain;
    private LocalDate beginDate;
    private LocalDate endDate;
    /** 环比区间 */
    private LocalDate prevBegin;
    private LocalDate prevEnd;
    /** 同比区间 */
    private LocalDate yoyBegin;
    private LocalDate yoyEnd;
    private List<Period> periods = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private Row total;

    /** 时间段：key 为日（yyyy-MM-dd）、周（周一的日期）或月（yyyy-MM），begin / end 为在查询范围内的首尾日期 */
    public record Period(String key, LocalDate begin, LocalDate end) {
    }

    @Data
    public static class Row {
        /** 行标识：SKU 为「店铺 ID|SKU」，SPU 为「店铺 ID|父商品」，其他为 ID / 平台 / 国家 */
        private String key;
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String sellerSku;
        private Long listingId;
        private String title;
        private String spu;
        private Integer skuCount;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String country;
        private List<BigDecimal> qty = new ArrayList<>();
        private List<BigDecimal> amount = new ArrayList<>();
        private long totalOrders;
        private BigDecimal totalQty = BigDecimal.ZERO;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal prevQty = BigDecimal.ZERO;
        private BigDecimal prevAmount = BigDecimal.ZERO;
        private BigDecimal yoyQty = BigDecimal.ZERO;
        private BigDecimal yoyAmount = BigDecimal.ZERO;
        /** 环比增长率（%） */
        private BigDecimal momQtyPercent;
        private BigDecimal momAmountPercent;
        /** 同比增长率（%） */
        private BigDecimal yoyQtyPercent;
        private BigDecimal yoyAmountPercent;
    }
}
