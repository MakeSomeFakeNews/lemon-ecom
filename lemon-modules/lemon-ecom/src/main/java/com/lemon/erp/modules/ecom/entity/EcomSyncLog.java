package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.SyncType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 渠道同步日志：每个店铺每次拉单、回传发货、回传库存、拉取商品、授权的结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_sync_log")
public class EcomSyncLog extends BaseEntity {

    private Long storeId;
    private SyncType syncType;
    /** JOB / MANUAL */
    private String triggerBy;
    private Integer success;
    private Integer fetched;
    private Integer created;
    private Integer updated;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @TableField(exist = false)
    private String storeName;
}
