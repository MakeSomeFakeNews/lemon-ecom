package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.mapper.EcomInboundMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransfer;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhTransferMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.wms.service.TransferReceiveOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 跨境电商生成 / 关联的在途调拨单不能在仓库「调拨单」页面直接收货：
 * 头程货件（签收时更新货件状态与已收数量，触发签收后的处理）、海外仓调拨（按实收收货并登记差异）、
 * 海外仓入库（来源为在途调拨单，服务商收货完成后按实收转可用并登记差异）
 */
@Component
@RequiredArgsConstructor
public class EcomTransferReceiveOwner implements TransferReceiveOwner {

    private final EcomInboundMapper inboundMapper;
    private final EcomOwhTransferMapper owhTransferMapper;
    private final EcomOwhInboundMapper owhInboundMapper;

    @Override
    public Map<Long, String> owners(Collection<Long> docIds) {
        Map<Long, String> owners = new HashMap<>();
        if (docIds == null || docIds.isEmpty()) {
            return owners;
        }
        inboundMapper.selectList(Wrappers.<EcomInbound>lambdaQuery()
                        .select(EcomInbound::getTransferDocId, EcomInbound::getDocNo).in(EcomInbound::getTransferDocId, docIds))
                .forEach(i -> owners.putIfAbsent(i.getTransferDocId(), Messages.get("ecom.text.fixa.ownerInbound", i.getDocNo())));
        owhTransferMapper.selectList(Wrappers.<EcomOwhTransfer>lambdaQuery()
                        .select(EcomOwhTransfer::getTransferDocId, EcomOwhTransfer::getTransferNo).in(EcomOwhTransfer::getTransferDocId, docIds))
                .forEach(t -> owners.putIfAbsent(t.getTransferDocId(), Messages.get("ecom.text.fixa.ownerOwhTransfer", t.getTransferNo())));
        owhInboundMapper.selectList(Wrappers.<EcomOwhInbound>lambdaQuery()
                        .select(EcomOwhInbound::getSourceId, EcomOwhInbound::getInboundNo)
                        .eq(EcomOwhInbound::getSourceType, OwhInboundSource.TRANSFER).in(EcomOwhInbound::getSourceId, docIds)
                        .ne(EcomOwhInbound::getStatus, OwhInboundStatus.CANCELLED))
                .forEach(i -> owners.putIfAbsent(i.getSourceId(), Messages.get("ecom.text.fixa.ownerOwhInbound", i.getInboundNo())));
        return owners;
    }
}
