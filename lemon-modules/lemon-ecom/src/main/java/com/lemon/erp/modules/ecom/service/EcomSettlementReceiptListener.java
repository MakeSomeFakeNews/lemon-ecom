package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.fin.event.FinSettlementDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 平台结算单生成的收款单在财务「收款单」中删除时，解除结算单与收款单的关联、删除未使用的平台费用应收（与删除同一事务）
 */
@Component
@RequiredArgsConstructor
public class EcomSettlementReceiptListener {

    private final EcomSettlementService settlementService;

    @EventListener
    public void onReceiptDeleted(FinSettlementDeletedEvent e) {
        settlementService.onReceiptDeleted(e.settlementId(), e.settleNo());
    }
}
