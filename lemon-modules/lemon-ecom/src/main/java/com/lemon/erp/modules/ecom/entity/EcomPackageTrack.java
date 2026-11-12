package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 包裹的一条物流轨迹（每次拉取整体替换），sort 0 为最新
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_package_track")
public class EcomPackageTrack extends BaseEntity {

    private Long packageId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
    /** 发生时间原文（解析失败时显示） */
    private String timeText;
    private String location;
    private String description;
    private EcomTrackStatus status;
    private Integer sort;
}
