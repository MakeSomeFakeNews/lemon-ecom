package com.lemon.erp.modules.ecom.oper.dto;

import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 监控预警列表的条件
 */
@Data
public class AlertQuery {
    private Long storeId;
    private Long monitorId;
    private EcomMonitorAlertType alertType;
    private EcomMonitorAlertStatus status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate beginDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String keyword;
}
