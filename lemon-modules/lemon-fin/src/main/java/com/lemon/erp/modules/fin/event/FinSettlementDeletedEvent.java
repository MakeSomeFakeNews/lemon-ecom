package com.lemon.erp.modules.fin.event;

/**
 * 收付款单已删除（与删除同一事务，核销金额已恢复、明细已删除）。由其他模块生成收付款单的业务（如电商平台结算）监听此事件
 * 解除来源单据与收付款单的关联；监听方抛出异常时整个删除回滚（用于阻止删除）
 *
 * @param settlementId 收付款单 ID
 * @param settleNo     收付款单号
 */
public record FinSettlementDeletedEvent(Long settlementId, String settleNo) {
}
