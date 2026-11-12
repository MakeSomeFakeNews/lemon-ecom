package com.lemon.erp.modules.ecom.assistant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 运营日报的店铺范围（一行一个店铺，店铺数据权限据此判断日报是否可见） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ops_report_store")
public class EcomOpsReportStore extends BaseEntity {

    private Long reportId;
    private Long storeId;
}
