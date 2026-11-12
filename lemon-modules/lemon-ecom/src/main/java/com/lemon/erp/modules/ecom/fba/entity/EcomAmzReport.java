package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.fba.model.AmzReportStatus;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 亚马逊报告任务：createReport 后由定时任务轮询状态，完成后下载报告文档、解析 TSV 并交给对应的处理器写入
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_amz_report")
public class EcomAmzReport extends BaseEntity {

    private Long storeId;
    private AmzReportType reportType;
    private LocalDate dataStart;
    private LocalDate dataEnd;
    private String reportId;
    private String documentId;
    private AmzReportStatus status;
    /** 手工上传的报告文件 1是 */
    private Integer uploaded;
    private Integer rowCount;
    private Integer appliedCount;
    private Integer pollCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextPollTime;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String createByName;
}
