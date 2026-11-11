package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台订单查询条件
 *
 * @see com.lemon.erp.modules.ecom.mapper.EcomOrderMapper#selectPageList
 */
@Data
public class OrderQuery {
    private Long storeId;
    private EcomPlatform platform;
    private EcomOrderStatus status;
    private EcomLocalStatus localStatus;
    private FulfillmentType fulfillment;
    /** 平台订单号 / 买家 / 运单号 / 平台 SKU */
    private String keyword;
    private String country;
    private LocalDate beginDate;
    private LocalDate endDate;
    /** 只看承诺发货时间已过且未发货的 */
    private Boolean overdue;

    /** 下单时间下限（含），由 beginDate 换算，mapper XML 使用 */
    public LocalDateTime getBeginTime() {
        return beginDate == null ? null : beginDate.atStartOfDay();
    }

    /** 下单时间上限（不含），由 endDate 换算，避免在 SQL 中做方言不同的日期加减 */
    public LocalDateTime getEndExclusive() {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
