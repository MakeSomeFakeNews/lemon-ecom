package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    /** 多个处理状态（列表的状态标签页），与 localStatus 同时给出时都要满足 */
    private List<EcomLocalStatus> localStatuses;
    /** 排序：urgency 按发货紧急程度（承诺发货 / 计划发货时间早的在前），为空按下单时间倒序 */
    private String sort;

    /** 按发货紧急程度排序（mapper XML 使用） */
    public boolean isUrgencySort() {
        return SORT_URGENCY.equals(sort);
    }

    public static final String SORT_URGENCY = "urgency";

    /** 下单时间下限（含），由 beginDate 换算，mapper XML 使用 */
    public LocalDateTime getBeginTime() {
        return beginDate == null ? null : beginDate.atStartOfDay();
    }

    /** 下单时间上限（不含），由 endDate 换算，避免在 SQL 中做方言不同的日期加减 */
    public LocalDateTime getEndExclusive() {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
