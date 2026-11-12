package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhOutboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhWarehouseMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 包裹打单的海外仓拦截（EcomPackageService 调用）：发货仓是海外仓的包裹不在本地向物流商申请运单号（推送海外仓出库单）；
 * 已推送海外仓（处理中 / 截单中 / 已出库）的包裹要先在「海外仓出库」中取消出库单才能取消包裹
 */
@Component
@RequiredArgsConstructor
public class OwhPackageGuard {

    private final EcomOwhWarehouseMapper mappingMapper;
    private final EcomOwhOutboundMapper outboundMapper;

    /** 是否海外仓（已映射）的包裹 */
    public boolean isOverseas(EcomPackage p) {
        return p != null && p.getWarehouseId() != null && mappingMapper.exists(Wrappers.<EcomOwhWarehouse>lambdaQuery()
                .eq(EcomOwhWarehouse::getWarehouseId, p.getWarehouseId()));
    }

    /** 本地申请运单号前：海外仓的包裹提示推送海外仓 */
    public void checkLocalLabel(EcomPackage p) {
        if (isOverseas(p)) {
            throw new BizException("ecom.error.owh.packageIsOverseas", p.getPackageNo());
        }
    }

    /** 取消包裹前：有处理中的海外仓出库单时提示先取消出库单 */
    public void checkCancel(EcomPackage p) {
        if (p != null && outboundMapper.exists(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getPackageId, p.getId())
                .in(EcomOwhOutbound::getStatus, OwhOutboundStatus.PUSHED, OwhOutboundStatus.INTERCEPTING, OwhOutboundStatus.OUTBOUND))) {
            throw new BizException("ecom.error.owh.packagePushed", p.getPackageNo());
        }
    }
}
