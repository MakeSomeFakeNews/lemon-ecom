package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 包裹查询条件
 *
 * @see com.lemon.erp.modules.ecom.mapper.EcomPackageMapper#selectPageList
 */
@Data
public class PackageQuery {
    private Long storeId;
    private Long warehouseId;
    private Long channelId;
    private EcomPackageStatus status;
    /** 包裹号 / 运单号 / 平台订单号 / 收件人 */
    private String keyword;
    /** 创建日期 */
    private LocalDate beginDate;
    private LocalDate endDate;

    public LocalDateTime getBeginTime() {
        return beginDate == null ? null : beginDate.atStartOfDay();
    }

    public LocalDateTime getEndExclusive() {
        return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
    }
}
