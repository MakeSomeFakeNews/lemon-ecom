package com.lemon.erp.modules.ecom.oper.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorType;
import lombok.Data;

/**
 * 商品监控列表的条件：关键字匹配商品 ID、标题、品牌、平台 SKU；alertOnly 只看有未处理预警的
 */
@Data
public class MonitorQuery {
    private Long storeId;
    private EcomPlatform platform;
    private String marketplace;
    private EcomMonitorType monitorType;
    private EnableStatus status;
    private String keyword;
    private Boolean alertOnly;
}
