package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单作业设置（每个租户一行，第一次读取时按默认值建立）：审核前地址校验开关、17TRACK API 密钥（加密）、物流跟踪的时限
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_setting")
public class EcomOrderSetting extends BaseEntity {

    /** 审核前校验地址 1是 0否 */
    private Integer addressCheck;
    /** 17TRACK API 密钥（加密保存），不返回前端 */
    @JsonIgnore
    private String trackKey;
    /** 17TRACK 自定义接口地址，为空用默认地址 */
    private String trackApiUrl;
    /** 定时拉取已发货包裹的轨迹 1是 0否 */
    private Integer trackEnabled;
    /** 发货后超过多少小时没有轨迹为超时未上网 */
    private Integer noScanHours;
    /** 上网后超过多少天未签收为运输超时 */
    private Integer transitDays;
    /** 轨迹超过多少天没有更新为运输超时（停滞） */
    private Integer staleDays;

    /** 是否已填写 17TRACK 密钥 */
    @TableField(exist = false)
    private Boolean hasTrackKey;
}
