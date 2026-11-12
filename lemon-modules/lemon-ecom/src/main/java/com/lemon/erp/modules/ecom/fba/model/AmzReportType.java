package com.lemon.erp.modules.ecom.fba.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FBA 运营使用的亚马逊报告（字典 ecom_amz_report_type），对应 Reports API 2021-06-30 的 reportType
 */
@Getter
@AllArgsConstructor
public enum AmzReportType {
    /** 库存规划（库龄分段、最近 30 天发货、预估仓储费），不带日期范围 */
    FBA_INVENTORY_PLANNING("GET_FBA_INVENTORY_PLANNING_DATA", false),
    /** FBA 库存（可售、在途、预留、不可售、调查中），不带日期范围 */
    FBA_MYI_INVENTORY("GET_FBA_MYI_UNSUPPRESSED_INVENTORY_DATA", false),
    /** 库存分类账明细（丢失、损坏、找到、退货入库等事件） */
    LEDGER_DETAIL("GET_LEDGER_DETAIL_VIEW_DATA", true),
    /** 赔偿 */
    REIMBURSEMENTS("GET_FBA_REIMBURSEMENTS_DATA", true),
    /** 移除订单详情 */
    REMOVAL_ORDERS("GET_FBA_FULFILLMENT_REMOVAL_ORDER_DETAIL_DATA", true),
    /** 预估 FBA 费用（商品尺寸重量、尺寸分段、预估配送费），不带日期范围 */
    ESTIMATED_FEES("GET_FBA_ESTIMATED_FBA_FEES_TXT_DATA", false);

    /** 亚马逊的报告类型 */
    private final String amazonType;
    /** 是否按日期范围（dataStartTime / dataEndTime）生成 */
    private final boolean dated;
}
