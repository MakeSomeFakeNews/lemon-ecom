package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;

import java.time.LocalDate;
import java.util.Set;

/**
 * 亚马逊报告的处理器（Spring Bean）：报告下载 / 上传并解析后，按报告类型交给对应的处理器写入业务数据
 */
public interface AmzReportHandler {

    /** 处理的报告类型 */
    Set<AmzReportType> types();

    /**
     * 写入报告数据（实现方自己管理事务）
     *
     * @param snapshotDate 快照日期（库存类报告没有日期列时使用，一般为今天）
     * @return 写入 / 更新的条数
     */
    int apply(EcomStore store, AmzReportType type, TsvReport report, LocalDate snapshotDate);
}
