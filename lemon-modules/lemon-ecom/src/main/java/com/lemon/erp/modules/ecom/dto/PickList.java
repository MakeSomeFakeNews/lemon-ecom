package com.lemon.erp.modules.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拣货单（一个仓库一张）：按库位（区域 → 拣货顺序 → 编码）排序汇总物料数量，没有库位的排在最后
 *
 * @param packageNos 包含的包裹号（逗号分隔）
 */
public record PickList(Long warehouseId, String warehouseName, int packageCount, BigDecimal totalQty, String packageNos, List<Row> rows) {

    /**
     * @param locationCode 拣货库位（有多个有货库位时为第一个，other 为其余库位数）
     * @param skus         对应的平台 SKU（逗号分隔）
     * @param packageCount 涉及的包裹数
     */
    public record Row(String locationCode, int otherLocations, String materialCode, String materialName, String spec, String unit,
                      BigDecimal qty, String skus, int packageCount) {
    }
}
