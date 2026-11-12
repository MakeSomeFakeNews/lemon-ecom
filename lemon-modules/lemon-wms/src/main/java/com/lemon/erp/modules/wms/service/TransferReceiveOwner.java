package com.lemon.erp.modules.wms.service;

import java.util.Collection;
import java.util.Map;

/**
 * 在途调拨单的收货归属：由业务单据（如跨境电商的头程货件、海外仓入库 / 调拨）生成或关联的在途调拨单，
 * 收货时要按业务单据分摊成本、按实收登记差异，不能在仓库「调拨单」页面直接收货，只能由业务单据调用
 * {@link WmsStockDocService#receiveBySource}。业务模块实现本接口声明自己管理的调拨单（wms 不依赖业务模块）。
 */
public interface TransferReceiveOwner {

    /**
     * @param docIds 在途调拨单 ID
     * @return 由本模块管理收货的调拨单 ID → 来源说明（如「头程货件 FBA202611120001」，已按当前语言），不归本模块管理的不返回
     */
    Map<Long, String> owners(Collection<Long> docIds);
}
