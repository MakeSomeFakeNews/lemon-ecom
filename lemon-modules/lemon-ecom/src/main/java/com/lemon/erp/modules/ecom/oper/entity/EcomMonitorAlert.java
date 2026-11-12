package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品监控预警：快照与上一次快照比较得到，同一监控同一天同一类型一条
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_monitor_alert")
public class EcomMonitorAlert extends BaseEntity {

    private Long monitorId;
    private Long storeId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate alertDate;
    private EcomMonitorAlertType alertType;
    private String oldValue;
    private String newValue;
    private EcomMonitorAlertStatus status;
    private Long handlerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    private String handleNote;

    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String marketplace;
    @TableField(exist = false)
    private String itemId;
    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private EcomMonitorType monitorType;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String handlerName;
}
