/** 业务正确性修复：在途调拨的收货归属、结算单补生成收款单、供应商对账内部登记 */
export default {
  transfer: {
    receiveLabel: '收货方式',
    receiveAt: '由{source}生成，请到该单据中收货'
  },
  settlement: {
    createReceipt: '生成收款单',
    receiptTitle: '结算单 {no} 生成收款单',
    receiptTip: '结算单已确认但没有收款单（收款单已在财务中删除，或确认时没有选择收款账户）：按结算净额重新生成收款单，平台费用红字应收冲抵应收后核销店铺客户的应收。',
    accountRequired: '请选择收款账户',
    receiptCreated: '已生成收款单'
  },
  statement: {
    export: '导出对账单',
    register: '登记对账结果',
    registeredBy: '（{name} 登记）',
    sentTip: '对账单已发送给供应商，收到供应商的回复后点「登记对账结果」：确认（可同时登记发票）或有异议（填写供应商的意见）。',
    result: '供应商回复',
    confirmed: '确认无误',
    disputed: '有异议',
    replyDate: '回复日期',
    remarkPh: '供应商的意见或说明，有异议时必填',
    remarkRequired: '请填写供应商的异议说明',
    invoiceNo: '发票号',
    invoiceAmount: '发票金额',
    invoiceDate: '开票日期',
    optional: '可不填，之后在附件中补充',
    registered: '已登记对账结果'
  }
}
