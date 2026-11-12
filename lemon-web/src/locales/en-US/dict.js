// 字典标签英文翻译：dict_type -> { dict_value: 英文 }（与 sql/init.sql 一致），中文直接使用数据库中的标签
export default {
  // 通用状态
  sys_common_status: {
    '1': 'Enabled', // 启用
    '0': 'Disabled', // 停用
  },
  // 显示状态
  sys_show_hide: {
    '1': 'Visible', // 显示
    '0': 'Hidden', // 隐藏
  },
  // 菜单类型
  sys_menu_type: {
    'M': 'Directory', // 目录
    'C': 'Menu', // 菜单
    'F': 'Button', // 按钮
  },
  // 操作类型
  sys_oper_type: {
    '0': 'Other', // 其他
    '1': 'Create', // 新增
    '2': 'Update', // 修改
    '3': 'Delete', // 删除
    '4': 'Grant', // 授权
    '5': 'Export', // 导出
    '6': 'Force Logout', // 强退
    '7': 'Clear', // 清空
    '8': 'Import', // 导入
  },
  // 成功失败
  sys_success_fail: {
    '1': 'Success', // 成功
    '0': 'Failed', // 失败
  },
  // 数据权限
  sys_data_scope: {
    '1': 'All Data', // 全部数据
    '2': 'Custom', // 自定义数据
    '3': 'Own Department', // 本部门数据
    '4': 'Own Department & Below', // 本部门及以下数据
    '5': 'Self Only', // 仅本人数据
  },
  // 流程分类
  wf_category: {
    'PURCHASE': 'Purchasing', // 采购
    'SALES': 'Sales', // 销售
    'INVENTORY': 'Inventory', // 库存
    'FINANCE': 'Finance', // 财务
    'OTHER': 'Other', // 其他
    'PRODUCTION': 'Production', // 生产
    'HR': 'HR', // 人事
    'PROJECT': 'Project', // 项目
    'ADMIN': 'Administration', // 行政
    'QUALITY': 'Quality', // 质量
    'ECOM': 'E-commerce', // 电商
  },
  // 流程状态
  wf_status: {
    'RUNNING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤销
  },
  // 审批动作
  wf_task_action: {
    'APPROVE': 'Approve', // 同意
    'REJECT': 'Reject', // 拒绝
    'TRANSFER': 'Transfer', // 转交
  },
  // 商品品牌
  wms_material_brand: {
    'OWN': 'Own Brand', // 自有品牌
    'OTHER': 'Other', // 其他
  },
  // 发票类型（开票类型，扩展值为税率）
  scm_invoice_kind: {
    'SPECIAL_13': 'VAT Special Invoice 13%', // 增值税专票 13%
    'SPECIAL_9': 'VAT Special Invoice 9%', // 增值税专票 9%
    'SPECIAL_6': 'VAT Special Invoice 6%', // 增值税专票 6%
    'NORMAL_13': 'VAT General Invoice 13%', // 增值税普票 13%
    'NORMAL_3': 'VAT General Invoice 3%', // 增值税普票 3%
    'NORMAL_1': 'VAT General Invoice 1%', // 增值税普票 1%
    'NONE': 'No Invoice', // 不开票
  },
  // 客户区域
  scm_region: {
    'EAST': 'East China', // 华东
    'SOUTH': 'South China', // 华南
    'NORTH': 'North China', // 华北
    'CENTRAL': 'Central China', // 华中
    'SOUTHWEST': 'Southwest China', // 西南
    'NORTHWEST': 'Northwest China', // 西北
    'NORTHEAST': 'Northeast China', // 东北
    'OVERSEAS': 'Overseas', // 海外
  },
  // 物料分类
  wms_material_category: {
    'RAW': 'Raw Material', // 原材料
    'SEMI': 'Semi-finished', // 半成品
    'FINISHED': 'Finished Goods', // 成品
    'PACKING': 'Packaging', // 包装材料
    'OTHER': 'Other', // 其他
    'LOW_VALUE': 'Low-value Consumable', // 低值易耗品
  },
  // 入库类型
  wms_in_type: {
    'PURCHASE': 'Purchase Receipt', // 采购入库
    'PRODUCTION': 'Production Receipt', // 生产入库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'OTHER': 'Other Inbound', // 其他入库
    'PRODUCTION_RETURN': 'Production Return', // 生产退料
    'OUTSOURCE_RETURN': 'Outsourcing Receipt', // 委外收回
    'SCRAP_IN': 'Scrap Receipt', // 报废入库
    'CONSIGN_IN': 'Consignment Inbound', // 寄售入库
    'OPENING': 'Opening Inbound', // 期初入库
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 出库类型
  // 入库类别（业务字典）
  wms_in_category: {
    SAMPLE_RETURN: 'Sample Return', // 样品退回
    GIFT: 'Gift Received', // 受赠入库
    USE_RETURN: 'Usage Return', // 领用退回
    OTHER: 'Other' // 其他
  },
  // 出库类别（业务字典）
  wms_out_category: {
    SAMPLE: 'Samples', // 样品出库
    GIFT: 'Giveaways', // 赠品出库
    WELFARE: 'Employee Welfare', // 福利领用
    RD: 'R&D Use', // 研发领用
    OFFICE: 'Office Use', // 办公领用
    OTHER: 'Other' // 其他
  },
  wms_out_type: {
    'SALES': 'Sales Delivery', // 销售出库
    'PRODUCTION': 'Material Issue', // 生产领料
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'OTHER': 'Other Outbound', // 其他出库
    'PROJECT': 'Project Issue', // 项目领料
    'OUTSOURCE': 'Outsourcing Issue', // 委外发料
    'SERVICE': 'Service Issue', // 服务领用
    'SCRAP': 'Scrap Outbound', // 报废出库
    'CONSIGN': 'Consignment Consumption', // 寄售消耗
    'CONSUME': 'Department Issue', // 部门领用
    'ASSEMBLY': 'Assembly/Disassembly', // 组装拆卸
  },
  // 单据状态
  wms_doc_status: {
    'DRAFT': 'Draft', // 草稿
    'DONE': 'Completed', // 已完成
    'TRANSIT': 'In Transit', // 在途
    'APPROVING': 'In Approval', // 审批中
  },
  // 库存业务类型
  wms_biz_type: {
    'IN': 'Inbound', // 入库
    'OUT': 'Outbound', // 出库
    'TRANSFER': 'Transfer', // 调拨
    'CHECK': 'Stock Count', // 盘点
  },
  // 序列号状态
  wms_serial_status: {
    'IN_STOCK': 'In Stock', // 在库
    'OUT': 'Shipped Out', // 已出库
  },
  // 供应商等级
  scm_supplier_level: {
    'A': 'A - Excellent', // A 优秀
    'B': 'B - Good', // B 良好
    'C': 'C - Qualified', // C 合格
    'D': 'D - Needs Improvement', // D 待改进
  },
  // 客户等级
  scm_customer_level: {
    'VIP': 'Key Account', // 重点客户
    'NORMAL': 'Regular', // 普通客户
    'POTENTIAL': 'Prospect', // 潜在客户
  },
  // 订单状态
  scm_order_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已审核
    'PARTIAL': 'Partially Completed', // 部分完成
    'COMPLETED': 'Completed', // 已完成
    'CLOSED': 'Closed', // 已关闭
  },
  // 出入库种类
  scm_trade_kind: {
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
  },
  // 发运状态
  scm_shipment_status: {
    'PENDING': 'To Ship', // 待发运
    'SHIPPED': 'In Transit', // 运输中
    'SIGNED': 'Delivered', // 已签收
    'EXCEPTION': 'Exception', // 异常
  },
  // 承运商
  scm_carrier: {
    'SF': 'SF Express', // 顺丰速运
    'JD': 'JD Logistics', // 京东物流
    'ZTO': 'ZTO Express', // 中通快递
    'DEPPON': 'Deppon Logistics', // 德邦物流
    'SELF': 'Pickup / Self-delivery', // 自提 / 自送
  },
  // 订单来源
  scm_order_source: {
    'PURCHASE_PLAN': 'Purchase Plan', // 采购计划
    'MANUAL': 'Manual', // 手工
    'MRP': 'MRP', // MRP
    'KIT': 'Production Shortage', // 生产缺料
    'CRM': 'Opportunity', // 商机
    'REQUISITION': 'Requisition', // 请购单
    'INQUIRY': 'RFQ', // 询价单
    'QUOTATION': 'Sales Quotation', // 销售报价
    'CONSIGN': 'Consignment Settlement', // 寄售结算
    'TENDER': 'Tender', // 招标
    'DEALER': 'Dealer Order', // 经销商订货
    'IMPORT': 'External Import', // 外部导入
    'DIRECT': 'Direct Document', // 直接开单
  },
  // 科目类别
  fin_account_category: {
    'ASSET': 'Assets', // 资产
    'LIABILITY': 'Liabilities', // 负债
    'EQUITY': 'Owners\' Equity', // 所有者权益
    'COST': 'Costs', // 成本
    'PROFIT': 'Profit & Loss', // 损益
  },
  // 凭证状态
  fin_voucher_status: {
    'DRAFT': 'Unapproved', // 未审核
    'AUDITED': 'Approved', // 已审核
    'POSTED': 'Posted', // 已记账
    'APPROVING': 'In Approval', // 审批中
  },
  // 凭证来源
  fin_voucher_source: {
    'MANUAL': 'Manual', // 手工
    'AR': 'Receivable', // 应收
    'AP': 'Payable', // 应付
    'RECEIPT': 'Receipt', // 收款
    'PAYMENT': 'Payment', // 付款
    'DEPRECIATION': 'Depreciation', // 折旧
    'ASSET': 'Asset Disposal', // 资产处置
    'COST': 'Cost Carry-forward', // 成本结转
    'CLOSE': 'P&L Carry-forward', // 结转损益
    'SALARY': 'Payroll', // 工资
    'PROJECT': 'Project', // 项目
    'EXPENSE': 'Expense', // 费用
    'ADVANCE': 'Advances', // 预收预付
    'BAD_DEBT': 'Bad Debt', // 坏账
    'REVERSAL': 'Reversal', // 红字冲销
    'TAX': 'Taxes', // 税费
    'CLAIM': 'Expense Claim', // 报销
    'LOAN': 'Loan', // 借款
    'FX': 'Exchange Gain/Loss', // 汇兑
    'NOTE': 'Notes', // 票据
    'TRANSFER': 'Account Transfer', // 账户调拨
    'INCOME_EXPENSE': 'Other Income / Expense', // 其他收支
    'NETTING': 'AR/AP Netting', // 应收应付对冲
  },
  // 核销状态
  fin_bill_status: {
    'UNSETTLED': 'Unsettled', // 未核销
    'PARTIAL': 'Partially Settled', // 部分核销
    'SETTLED': 'Settled', // 已核销
  },
  // 应收应付来源
  fin_bill_source: {
    'SALES_OUT': 'Sales Delivery', // 销售出库
    'SALES_RETURN': 'Sales Return', // 销售退货
    'PURCHASE_IN': 'Purchase Receipt', // 采购入库
    'PURCHASE_RETURN': 'Purchase Return', // 采购退货
    'MANUAL': 'Opening / Manual', // 期初 / 手工
    'PROJECT': 'Project Billing', // 项目开票
    'SHIPMENT': 'Freight', // 运费
    'TICKET': 'Service Charge', // 服务收费
    'ASSET': 'Fixed Asset', // 固定资产
    'NOTE': 'Dishonored Note', // 票据拒付
    'QUALITY_CLAIM': 'Quality Claim', // 质量索赔
    'REBATE': 'Rebate', // 返利
    'OFFSET': 'Offset', // 冲账单
    'ORDER_ADJUST': 'Order Rounding Adjustment', // 订单尾差调整
    'PURCHASE_FREIGHT': 'Purchase Freight', // 采购运费
    'SALES_FREIGHT': 'Sales Freight', // 销售运费
    'SALES_RETURN_FREIGHT': 'Sales Return Freight', // 销售退货运费
    'PURCHASE_RETURN_FREIGHT': 'Purchase Return Freight', // 采购退货运费
  },
  // 运费承担
  scm_freight_bearer: {
    'COMPANY': 'Company Pays', // 本公司承担
    'CUSTOMER': 'Customer Pays (Advanced)', // 客户承担（代垫）
    'SUPPLIER': 'Advanced by Supplier', // 供应商代垫
  },
  // 运费结算方式
  scm_freight_settle: {
    'CASH': 'Cash', // 现付
    'MONTHLY': 'Monthly', // 月结
  },
  // 冲账项目
  fin_offset_item: {
    'FREIGHT': 'Freight Paid on Behalf', // 垫付运费
    'REBATE': 'Sales Rebate', // 销售返利
    'WARRANTY': 'Warranty Compensation', // 三包赔偿
    'OTHER': 'Other', // 其他
  },
  // 冲账单状态
  fin_offset_status: {
    'DRAFT': 'Pending Review', // 待复核
    'REVIEWED': 'Reviewed', // 已复核
  },
  // 结算方式
  fin_settle_method: {
    'TRANSFER': 'Bank Transfer', // 银行转账
    'CASH': 'Cash', // 现金
    'DRAFT': 'Acceptance Bill', // 承兑汇票
    'ONLINE': 'Alipay / WeChat Pay', // 支付宝 / 微信
    'NOTE': 'Note', // 票据
  },
  // 资产类别
  fin_asset_category: {
    'BUILDING': 'Buildings', // 房屋建筑物
    'MACHINE': 'Machinery', // 机器设备
    'VEHICLE': 'Vehicles', // 运输工具
    'ELECTRONIC': 'Electronic Equipment', // 电子设备
    'FURNITURE': 'Office Furniture', // 办公家具
  },
  // 资产状态
  fin_asset_status: {
    'IN_USE': 'In Use', // 在用
    'IDLE': 'Idle', // 闲置
    'DISPOSED': 'Disposed', // 已处置
  },
  // 消息类别
  sys_message_category: {
    'APPROVAL': 'Approval', // 审批
    'REMIND': 'Reminder', // 提醒
    'ALERT': 'Alert', // 预警
    'SYSTEM': 'System', // 系统
    'NOTICE': 'Notice', // 公告
  },
  // 审批状态
  sys_approve_status: {
    'APPROVED': 'Effective', // 已生效
    'APPROVING': 'In Approval', // 审批中
    'REJECTED': 'Rejected', // 已拒绝
    'CANCELED': 'Withdrawn', // 已撤回
    'DONE': 'Effective', // 已生效
    'DRAFT': 'Draft', // 草稿
  },
  // 冻结状态
  wms_hold_status: {
    'PENDING': 'Pending Inspection', // 待检
    'REJECTED': 'Rejected', // 不合格
    'RELEASED': 'Released', // 已放行
    'RETURNED': 'Returned', // 已退货
  },
  // 请购单状态
  scm_requisition_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已批准
    'ORDERED': 'Ordered', // 已下单
    'CLOSED': 'Closed', // 已关闭
  },
  // 询价单状态
  scm_inquiry_status: {
    'QUOTING': 'Quoting', // 询价中
    'FINISHED': 'Awarded', // 已定标
    'CLOSED': 'Closed', // 已关闭
  },
  // 报价单状态
  scm_quotation_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Approved', // 已批准
    'ORDERED': 'Won', // 已成交
    'LOST': 'Expired', // 已失效
  },
  // 发货通知状态
  scm_delivery_status: {
    'PENDING': 'To Pick', // 待拣货
    'PICKED': 'Picked', // 已拣货
    'SHIPPED': 'Shipped', // 已出库
    'CANCELLED': 'Canceled', // 已取消
  },
  // 仓库类型
  wms_warehouse_type: {
    'NORMAL': 'General', // 普通仓
    'RAW': 'Raw Materials', // 原料仓
    'FINISHED': 'Finished Goods', // 成品仓
    'DEFECT': 'Defective Goods', // 不良品仓
    'CONSIGN_OUT': 'Customer Consignment', // 客户寄售仓
    'CONSIGN_IN': 'Supplier Consignment', // 供应商寄售仓
  },
  // 发票类型
  fin_invoice_type: {
    'SPECIAL': 'Special VAT Invoice', // 增值税专用发票
    'NORMAL': 'General VAT Invoice', // 普通发票
    'E_SPECIAL': 'Digital Invoice (Special)', // 数电票（专票）
    'E_NORMAL': 'Digital Invoice (General)', // 数电票（普票）
  },
  // 报销状态
  fin_claim_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'To Pay', // 待付款
    'PAID': 'Paid', // 已付款
  },
  // 借款状态
  fin_loan_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'To Pay', // 待付款
    'PAID': 'Outstanding', // 未还清
    'CLEARED': 'Repaid', // 已还清
  },
  // 预算控制
  fin_budget_control: {
    'NONE': 'No Control', // 不控制
    'WARN': 'Warn on Overrun', // 超预算提示
    'BLOCK': 'Block on Overrun', // 超预算禁止
  },
  // 变更单状态
  scm_change_status: {
    'APPROVING': 'In Approval', // 审批中
    'APPROVED': 'Effective', // 已生效
    'REJECTED': 'Rejected', // 已驳回
  },
  // 票据类型
  fin_note_type: {
    'BANK': 'Bank Acceptance Bill', // 银行承兑汇票
    'COMMERCIAL': 'Commercial Acceptance Bill', // 商业承兑汇票
  },
  // 票据状态
  fin_note_status: {
    'HELD': 'Held', // 持有
    'ENDORSING': 'Endorsement in Approval', // 背书审批中
    'ENDORSED': 'Endorsed', // 已背书
    'DISCOUNTED': 'Discounted', // 已贴现
    'COLLECTED': 'Collected', // 已托收
    'DISHONORED': 'Dishonored', // 已拒付
    'ISSUING': 'Issuance in Approval', // 开票审批中
    'ISSUED': 'Unpaid', // 未兑付
    'PAID': 'Paid', // 已兑付
    'CANCELLED': 'Voided', // 已作废
  },
  // 票据操作
  fin_note_action: {
    'RECEIVE': 'Receive', // 收票
    'ENDORSE': 'Endorse', // 背书转让
    'DISCOUNT': 'Discount', // 贴现
    'COLLECT': 'Collect', // 托收
    'DISHONOR': 'Dishonor', // 拒付
    'ISSUE': 'Issue', // 开票
    'PAY': 'Pay', // 兑付
  },
  // 采购合同类型
  scm_contract_type: {
    'FRAMEWORK': 'Framework Agreement', // 框架协议
    'FIXED': 'Fixed-quantity Contract', // 固定数量合同
  },
  // 采购合同状态
  scm_contract_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'In Approval', // 审批中
    'ACTIVE': 'Active', // 生效
    'CLOSED': 'Closed', // 已关闭
  },
  // 招标状态
  scm_tender_status: {
    'DRAFT': 'Draft', // 草稿
    'BIDDING': 'Bidding', // 投标中
    'OPENED': 'Bids Opened', // 已开标
    'AWARDED': 'Awarded', // 已定标
    'CANCELLED': 'Canceled', // 已取消
  },
  // 送货通知状态
  scm_asn_status: {
    'SUBMITTED': 'To Receive', // 待收货
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Canceled', // 已取消
  },
  // 供应商确认
  scm_supplier_confirm: {
    'ACCEPTED': 'Accepted', // 已接受
    'REJECTED': 'Rejected', // 已拒绝
  },
  // 对账单状态
  fin_statement_status: {
    'DRAFT': 'Draft', // 草稿
    'SENT': 'Pending Confirmation', // 待确认
    'CONFIRMED': 'Confirmed', // 已确认
    'DISPUTED': 'Disputed', // 有异议
  },
  // 资产变动类型
  fin_asset_move_type: {
    'ASSIGN': 'Assign', // 领用
    'RETURN': 'Return', // 归还
    'TRANSFER': 'Transfer', // 调拨
  },
  // 资产盘点状态
  fin_asset_check_status: {
    'CHECKING': 'Counting', // 盘点中
    'DONE': 'Completed', // 已完成
  },
  // 资产盘点结果
  fin_asset_check_result: {
    'PENDING': 'Not Counted', // 待盘
    'NORMAL': 'Matched', // 相符
    'MOVED': 'Location Changed', // 位置变化
    'LOSS': 'Shortage', // 盘亏
    'GAIN': 'Surplus', // 盘盈
  },
  // 易耗品领用状态
  fin_consumable_status: {
    'DRAFT': 'Draft', // 草稿
    'CONFIRMED': 'Issued', // 已出库
  },
  // 促销类型
  scm_promo_type: {
    'DISCOUNT': 'Discount', // 折扣
    'REDUCE': 'Spend & Save', // 满减
    'GIFT': 'Buy & Get Free', // 买赠
  },
  // 返利状态
  scm_rebate_status: {
    'ACTIVE': 'Active', // 执行中
    'SETTLED': 'Settled', // 已结算
  },
  // 返利兑付方式
  scm_rebate_settle: {
    'CREDIT_NOTE': 'Credit Note', // 红字应收
    'NEXT_ORDER': 'Deduct from Next Order', // 下次订单抵扣
  },
  // 返利种类
  scm_rebate_kind: {
    'SALES': 'Sales Rebate', // 销售返利
    'PURCHASE': 'Purchase Rebate', // 采购返利
  },
  // 返利计算方式
  scm_rebate_calc: {
    'TIER': 'Tiered Rate', // 阶梯比例
    'ITEM': 'Item Amounts', // 按项目填写金额
  },
  // 收支方向
  fin_fund_direction: {
    'IN': 'Income', // 收入
    'OUT': 'Expense', // 支出
  },
  // 收支单状态
  fin_io_status: {
    'DRAFT': 'Unconfirmed', // 待确认
    'CONFIRMED': 'Confirmed', // 已确认
  },
  // 关联单位类型
  fin_io_partner_type: {
    'CUSTOMER': 'Customer', // 客户
    'SUPPLIER': 'Supplier', // 供应商
    'OTHER': 'Other', // 其他
  },
  // 手续费支付账户
  fin_fee_bearer: {
    'FROM': 'Paid by Source Account', // 转出账户支付
    'TO': 'Paid by Receiving Account', // 转入账户支付
  },
  // 销售渠道
  scm_channel: {
    'DIRECT': 'Direct', // 直销
    'DEALER': 'Dealer', // 经销商
  },
  // 库位类型
  wms_location_type: {
    'STORAGE': 'Storage', // 存储位
    'PICK': 'Picking', // 拣货位
    'RECEIVE': 'Receiving Staging', // 收货暂存
    'SHIP': 'Shipping Staging', // 发货暂存
    'DEFECT': 'Defective', // 不良品位
  },
  // 波次状态
  scm_wave_status: {
    'PICKING': 'Picking', // 拣货中
    'DONE': 'Completed', // 已完成
    'CANCELLED': 'Canceled', // 已取消
  },
  // 存货计价方法
  wms_cost_method: {
    'WEIGHTED': 'Monthly Weighted Average', // 月末一次加权平均
    'MOVING': 'Moving Average', // 移动加权平均
    'FIFO': 'FIFO', // 先进先出
  },
  // 组装拆卸种类
  wms_assembly_kind: {
    'ASSEMBLE': 'Assembly', // 组装
    'DISASSEMBLE': 'Disassembly', // 拆卸
  },
  // 提成依据
  fin_commission_basis: {
    'SALES': 'Sales Amount', // 销售额
    'PROFIT': 'Gross Profit', // 毛利
    'RECEIPT': 'Collections', // 回款
  },
  // 价格控制方式
  scm_price_control_mode: {
    'WARN': 'Warn', // 提示
    'BLOCK': 'Block Saving', // 禁止保存
  },
  // 自定义字段类型
  sys_custom_field_type: {
    'TEXT': 'Text', // 文本
    'TEXTAREA': 'Multi-line Text', // 多行文本
    'NUMBER': 'Number', // 数字
    'DATE': 'Date', // 日期
    'SELECT': 'Dropdown', // 下拉选择
    'BOOL': 'Yes/No', // 是否
  },
  // 集成平台
  sys_im_platform: {
    'DINGTALK': 'DingTalk', // 钉钉
    'WECOM': 'WeCom', // 企业微信
    'FEISHU': 'Feishu (Lark)', // 飞书
  },
  // 公告状态
  sys_notice_status: {
    'DRAFT': 'Draft', // 草稿
    'PUBLISHED': 'Published', // 已发布
    'WITHDRAWN': 'Withdrawn', // 已撤回
  },
  // 公告发布范围
  sys_notice_scope: {
    'ALL': 'Everyone', // 全员
    'DEPT': 'Departments', // 指定部门
    'ROLE': 'Roles', // 指定角色
  },
  // 公告类型
  sys_notice_type: {
    'NOTICE': 'Notice', // 通知
    'ANNOUNCE': 'Announcement', // 公告
    'POLICY': 'Policy', // 制度
    'EVENT': 'Event', // 活动
  },
  // 电商平台
  ecom_platform: {
    'AMAZON': 'Amazon', // 亚马逊
    'ALIEXPRESS': 'AliExpress', // 速卖通
    'EBAY': 'eBay', // eBay
    'SHOPEE': 'Shopee', // Shopee
    'LAZADA': 'Lazada', // Lazada
    'SHOPIFY': 'Shopify', // Shopify
    'TIKTOK': 'TikTok Shop', // TikTok Shop
    'TEMU': 'Temu', // Temu
    'WALMART': 'Walmart', // 沃尔玛
    'OZON': 'Ozon', // Ozon
    'MERCADO_LIBRE': 'Mercado Libre', // 美客多
    'OTHER': 'Other', // 其他
  },
  // 配送方式
  ecom_fulfillment: {
    'PLATFORM': 'Platform Fulfilled', // 平台仓配送
    'SELLER': 'Seller Fulfilled', // 自发货
  },
  // 在线商品状态
  ecom_listing_status: {
    'ACTIVE': 'Active', // 在售
    'INACTIVE': 'Inactive', // 下架
    'INCOMPLETE': 'Incomplete', // 信息不完整
    'DELETED': 'Deleted', // 已删除
  },
  // 平台订单状态
  ecom_order_status: {
    'PENDING': 'Pending Payment', // 待付款
    'UNSHIPPED': 'Unshipped', // 待发货
    'SHIPPED': 'Shipped', // 已发货
    'CANCELLED': 'Cancelled', // 已取消
    'REFUNDED': 'Refunded', // 已退款
  },
  // 货件状态
  ecom_inbound_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVING': 'Approving', // 审批中
    'SHIPPED': 'In Transit', // 已发运
    'RECEIVED': 'Received', // 已签收
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 结算单状态
  ecom_settlement_status: {
    'IMPORTED': 'Imported', // 已导入
    'CONFIRMED': 'Confirmed', // 已确认
  },
  // 结算明细类型
  ecom_settlement_item_type: {
    'LONG_STORAGE': 'Long-term Storage Fee', // 长期仓储费
    'REMOVAL': 'Removal / Disposal Fee', // 移除 / 弃置费
    'RETURN_FEE': 'Return Processing Fee', // 退货处理费
    'INBOUND_FEE': 'Inbound Placement Fee', // 入库配置费
    'SUBSCRIPTION': 'Subscription Fee', // 订阅费
    'PROMOTION': 'Promotion Fee', // 促销费
    'COUPON': 'Coupon', // 优惠券
    'DEAL': 'Deal Fee', // 秒杀费
    'REIMBURSEMENT': 'Reimbursement', // 赔偿
    'RESERVE': 'Reserve / Release', // 预留 / 释放
    'MARKETPLACE_TAX': 'Marketplace Facilitator Tax', // 平台代收代缴税
    'ORDER': 'Order Revenue', // 订单收入
    'REFUND': 'Refund', // 退款
    'COMMISSION': 'Commission', // 平台佣金
    'FULFILLMENT': 'Fulfillment Fee', // 配送费
    'STORAGE': 'Storage Fee', // 仓储费
    'AD': 'Advertising', // 广告费
    'OTHER': 'Other Fees', // 其他费用
    'ADJUSTMENT': 'Adjustment', // 调整
  },
  // 退货退款类型
  ecom_return_type: {
    'REFUND_ONLY': 'Refund Only', // 仅退款
    'RETURN_REFUND': 'Return & Refund', // 退货退款
  },
  // 退货退款状态
  ecom_return_status: {
    'PENDING': 'Pending', // 待处理
    'RECEIVED': 'Received', // 已入库
    'REFUNDED': 'Refunded', // 已退款
    'CLOSED': 'Closed', // 已关闭
  },
  // 补货紧急程度
  ecom_replenish_level: {
    'OK': 'Sufficient', // 充足
    'WARN': 'Warning', // 预警
    'URGENT': 'Urgent', // 紧急
  },
  // 电商站点
  ecom_marketplace: {
    'US': 'United States', // 美国
    'CA': 'Canada', // 加拿大
    'MX': 'Mexico', // 墨西哥
    'BR': 'Brazil', // 巴西
    'UK': 'United Kingdom', // 英国
    'DE': 'Germany', // 德国
    'FR': 'France', // 法国
    'IT': 'Italy', // 意大利
    'ES': 'Spain', // 西班牙
    'NL': 'Netherlands', // 荷兰
    'PL': 'Poland', // 波兰
    'SE': 'Sweden', // 瑞典
    'AE': 'UAE', // 阿联酋
    'SA': 'Saudi Arabia', // 沙特
    'JP': 'Japan', // 日本
    'AU': 'Australia', // 澳大利亚
    'SG': 'Singapore', // 新加坡
    'MY': 'Malaysia', // 马来西亚
    'TH': 'Thailand', // 泰国
    'PH': 'Philippines', // 菲律宾
    'VN': 'Vietnam', // 越南
    'ID': 'Indonesia', // 印度尼西亚
    'GLOBAL': 'Global', // 全球站
  },
  // 运输方式
  ecom_transport_mode: {
    'AIR': 'Air Freight', // 空运
    'SEA': 'Sea Freight', // 海运
    'EXPRESS': 'International Express', // 国际快递
    'RAIL': 'Rail', // 铁路
    'TRUCK': 'Truck', // 卡航
    'LINE': 'Line Haul Parcel', // 专线小包
  },
  // 退货原因
  ecom_return_reason: {
    'QUALITY': 'Quality Issue', // 质量问题
    'DAMAGED': 'Damaged in Transit', // 运输破损
    'WRONG_ITEM': 'Wrong Item', // 发错货
    'NOT_AS_DESCRIBED': 'Not as Described', // 与描述不符
    'NOT_NEEDED': 'No Longer Needed', // 买家不想要
    'LATE': 'Arrived Too Late', // 送达太迟
    'OTHER': 'Other', // 其他
    'SIZE': 'Wrong Size / Spec', // 尺寸 / 规格不合适
    'NOT_RECEIVED': 'Item Not Received', // 未收到货
  },
  // 订单处理状态
  ecom_local_status: {
    'PENDING': 'To Review', // 待审核
    'ON_HOLD': 'On Hold', // 已挂起
    'EXCEPTION': 'Exception', // 异常
    'OUT_OF_STOCK': 'Out of Stock', // 缺货
    'AUDITED': 'To Label', // 待打单
    'LABELED': 'To Ship', // 待发货
    'OUTBOUND': 'Shipped Out', // 已出库
    'PARTIAL': 'Partially Shipped', // 部分发货
    'SHIPPED': 'Shipped', // 已发货
    'CLOSED': 'Closed', // 已关闭
  },
  // 包裹状态
  ecom_package_status: {
    'WAIT_LABEL': 'To Label', // 待打单
    'LABELED': 'To Ship', // 待发货
    'SHIPPED': 'Shipped', // 已发货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 对接物流商
  ecom_carrier: {
    'FOURPX': '4PX', // 递四方 4PX
    'YUNEXPRESS': 'YunExpress', // 云途物流
  },
  // 渠道同步类型
  ecom_sync_type: {
    'MESSAGES': 'Buyer Messages', // 买家消息
    'REVIEWS': 'Reviews', // 评价
    'REVIEW_REQUEST': 'Review Requests', // 请求评价
    'ORDERS': 'Fetch Orders', // 拉取订单
    'SHIP_CONFIRM': 'Confirm Shipment', // 回传发货
    'INVENTORY_PUSH': 'Push Inventory', // 回传库存
    'LISTINGS': 'Fetch Listings', // 拉取商品
    'PLATFORM_STOCK': 'Platform Stock', // 平台仓库存
    'AUTH': 'Authorization', // 授权
  },
  // 商品组成方式
  ecom_compose_type: {
    'SINGLE': 'Single', // 单品
    'BUNDLE': 'Bundle', // 组合
  },
  // 发货计划状态
  ecom_shipment_plan_status: {
    'DRAFT': 'Draft', // 草稿
    'CONFIRMED': 'Confirmed', // 已确认
    'GENERATED': 'Shipments Generated', // 已生成货件
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 采购计划状态
  ecom_purchase_plan_status: {
    'DRAFT': 'Draft', // 草稿
    'APPROVED': 'Approved', // 已审核
    'ORDERED': 'Ordered', // 已下单
    'CLOSED': 'Closed', // 已关闭
  },
  // 预测方法
  ecom_forecast_method: {
    'MA': 'Moving Average', // 移动平均
    'WMA': 'Weighted Moving Average', // 加权移动平均
    'HOLT': 'Exponential Smoothing (Holt)', // 指数平滑（Holt）
    'SEASONAL': 'Weekday Seasonality', // 星期季节性
  },
  // ABC 分类
  ecom_abc_class: {
    'A': 'Class A', // A 类
    'B': 'Class B', // B 类
    'C': 'Class C', // C 类
  },
  // 税种
  ecom_tax_type: {
    'VAT': 'VAT', // 增值税 VAT
    'GST': 'GST', // 商品及服务税 GST
    'SALES_TAX': 'Sales Tax', // 销售税 Sales Tax
  },
  // 回款状态
  ecom_payout_status: {
    'PENDING': 'Pending', // 未到账
    'RECEIVED': 'Received', // 已到账
    'DIFF': 'Difference', // 有差异
  },
  // 费用分摊方式
  ecom_fee_alloc_method: {
    'SKU': 'By SKU', // 按 SKU 直接归属
    'QTY': 'By Quantity', // 按销量
    'SALES': 'By Sales', // 按销售额
    'STORE': 'Store Level', // 留在店铺
  },
  // 收款平台（业务字典）
  ecom_payout_channel: {
    'PINGPONG': 'PingPong', // PingPong
    'LIANLIAN': 'LianLian Pay', // 连连支付
    'PAYONEER': 'Payoneer', // Payoneer
    'WORLDFIRST': 'WorldFirst', // 万里汇 WorldFirst
    'AIRWALLEX': 'Airwallex', // 空中云汇 Airwallex
    'BANK': 'Bank', // 银行
    'OTHER': 'Other', // 其他
  },
  // 结算明细类型 ecom_settlement_item_type 新增以下取值（合并到已有的 ecom_settlement_item_type 中，原有取值不变）：
  // 广告类型
  ecom_ad_type: {
    'SP': 'Sponsored Products', // 商品推广 SP
    'SB': 'Sponsored Brands', // 品牌推广 SB
    'SD': 'Sponsored Display', // 展示型推广 SD
    'GENERIC': 'Platform Generic', // 平台通用
  },
  // 广告状态
  ecom_ad_status: {
    'ENABLED': 'Enabled', // 启用
    'PAUSED': 'Paused', // 暂停
    'ARCHIVED': 'Archived', // 已归档
  },
  // 广告匹配方式
  ecom_ad_match_type: {
    'EXACT': 'Exact', // 精准
    'PHRASE': 'Phrase', // 词组
    'BROAD': 'Broad', // 广泛
    'NEGATIVE_EXACT': 'Negative Exact', // 否定精准
    'NEGATIVE_PHRASE': 'Negative Phrase', // 否定词组
  },
  // 广告规则动作
  ecom_ad_rule_action: {
    'BID_PERCENT': 'Adjust Bid by %', // 竞价按百分比调整
    'BID_SET': 'Set Bid', // 竞价调整到固定值
    'BUDGET_PERCENT': 'Adjust Budget by %', // 日预算按百分比调整
    'BUDGET_SET': 'Set Budget', // 日预算调整到固定值
    'PAUSE': 'Pause', // 暂停
    'ENABLE': 'Enable', // 启用
    'NOTIFY': 'Notify Only', // 仅提醒
  },
  // 广告执行状态
  ecom_ad_log_status: {
    'PENDING': 'Pending', // 待处理
    'APPLIED': 'Applied', // 已执行
    'MANUAL': 'Done Manually', // 已手工执行
    'FAILED': 'Failed', // 失败
    'NOTIFIED': 'Notified', // 仅提醒
    'IGNORED': 'Ignored', // 已忽略
  },
  // 客服会话状态
  ecom_cs_thread_status: {
    'PENDING': 'Awaiting Reply', // 待回复
    'REPLIED': 'Replied', // 已回复
    'CLOSED': 'Closed', // 已关闭
  },
  // 评价处理状态
  ecom_cs_review_status: {
    'NONE': 'No Action Needed', // 无需处理
    'PENDING': 'To Handle', // 待处理
    'PROCESSING': 'In Progress', // 处理中
    'RESOLVED': 'Resolved', // 已处理
  },
  // 模板语言（业务字典）
  ecom_cs_language: {
    'en': 'English', // 英语
    'de': 'German', // 德语
    'ja': 'Japanese', // 日语
    'es': 'Spanish', // 西班牙语
    'fr': 'French', // 法语
    'it': 'Italian', // 意大利语
    'zh': 'Chinese', // 中文
  },
  // 售后类型
  ecom_cs_ticket_type: {
    'RETURN_REFUND': 'Return and Refund', // 退货退款
    'REFUND_ONLY': 'Refund Only', // 仅退款
    'RESEND': 'Resend', // 补发
  },
  // 消息发送状态
  ecom_cs_send_status: {
    'SENDING': 'Sending', // 发送中
    'SENT': 'Sent', // 已发送
    'FAILED': 'Failed', // 发送失败
  },
  // 渠道同步类型（追加到已有的 ecom_sync_type 中）
  // ===== 第四轮：刊登与定价（pub） =====
  // 刊登状态
  ecom_publish_status: {
    'DRAFT': 'Draft', // 草稿
    'PENDING': 'Pending', // 待发布
    'PUBLISHING': 'Publishing', // 发布中
    'SUCCESS': 'Published', // 成功
    'FAILED': 'Failed', // 失败
    'OFFLINE': 'Offline', // 已下架
  },
  // 刊登动作
  ecom_publish_action: {
    'SUBMIT': 'Submit', // 提交发布
    'PUBLISH': 'Publish', // 发布
    'CHECK': 'Check Result', // 查询结果
    'OFFLINE': 'Take Offline', // 下架
  },
  // 改价类型
  ecom_price_change_type: {
    'PRICE': 'Price', // 价格
    'STOCK': 'Stock', // 库存
  },
  // 改价来源
  ecom_price_change_source: {
    'MANUAL': 'Manual', // 手工修改
    'BATCH': 'Bulk Change', // 批量修改
    'REPRICER': 'Repricer', // 自动调价
    'PUBLISH': 'Publishing', // 刊登
  },
  // 改价结果
  ecom_price_change_status: {
    'SUCCESS': 'Pushed', // 已推送
    'FAILED': 'Push Failed', // 推送失败
    'LOCAL': 'Local Only', // 仅本地
  },
  // 调整方式
  ecom_price_adjust_mode: {
    'FIXED': 'Fixed Value', // 固定值
    'PERCENT': 'By Percent', // 按百分比
    'AMOUNT': 'By Amount', // 按金额
    'FORMULA': 'By Formula', // 按公式
  },
  // 调价策略
  ecom_reprice_strategy: {
    'MATCH_LOWEST': 'Match Lowest', // 跟随最低价
    'BEAT_LOWEST': 'Beat Lowest', // 低于最低价
    'MATCH_BUYBOX': 'Match Buy Box', // 跟随购物车价
    'TARGET_MARGIN': 'Target Margin', // 按目标利润率
    'BY_STOCK': 'By Stock / Sales', // 按库存 / 销量
  },
  // 底价方式
  ecom_reprice_floor_mode: {
    'FIXED': 'Fixed Floor', // 固定底价
    'COST_MARGIN': 'Cost + Minimum Margin', // 成本 + 最低利润率
  },
  // 调价结果
  ecom_reprice_log_status: {
    'SUGGESTED': 'Suggested', // 建议
    'PUSHED': 'Pushed', // 已推送
    'FAILED': 'Push Failed', // 推送失败
    'LOCAL': 'Local Only', // 仅本地
    'SKIPPED': 'Skipped', // 跳过
    'IGNORED': 'Ignored', // 已忽略
  },
  // 竞品价格来源
  ecom_competitor_source: {
    'API': 'Platform API', // 平台接口
    'MANUAL': 'Manual Entry', // 手工录入
    'IMPORT': 'Import', // 导入
  },
  // 亚马逊报告类型
  ecom_amz_report_type: {
    'FBA_INVENTORY_PLANNING': 'Inventory Planning (Age)', // 库存规划（库龄）
    'FBA_MYI_INVENTORY': 'FBA Inventory', // FBA 库存
    'LEDGER_DETAIL': 'Inventory Ledger Detail', // 库存分类账明细
    'REIMBURSEMENTS': 'Reimbursements', // 赔偿
    'REMOVAL_ORDERS': 'Removal Order Detail', // 移除订单详情
    'ESTIMATED_FEES': 'Estimated FBA Fees', // 预估 FBA 费用
  },
  // 报告任务状态
  ecom_amz_report_status: {
    'SUBMITTED': 'Submitted', // 已提交
    'PROCESSING': 'Processing', // 生成中
    'DONE': 'Done', // 已完成
    'FAILED': 'Failed', // 失败
    'CANCELLED': 'Cancelled', // 已取消
  },
  // FBA 处理建议
  ecom_fba_advice: {
    'HEALTHY': 'Healthy', // 健康
    'PROMOTE': 'Off-Amazon Promotion', // 站外推广
    'PRICE_DOWN': 'Markdown', // 降价清仓
    'REMOVE': 'Remove / Dispose', // 移除 / 弃置
  },
  // 移除类型
  ecom_fba_removal_type: {
    'RETURN': 'Return', // 退回
    'DISPOSAL': 'Disposal', // 弃置
  },
  // 移除订单状态
  ecom_fba_removal_status: {
    'DRAFT': 'Draft', // 草稿
    'SUBMITTED': 'Submitted', // 已提交
    'PROCESSING': 'Processing', // 处理中
    'COMPLETED': 'Completed by Amazon', // 亚马逊已完成
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // FBA 库存状态
  ecom_fba_disposition: {
    'SELLABLE': 'Sellable', // 可售
    'UNSELLABLE': 'Unsellable', // 不可售
  },
  // 索赔类型
  ecom_fba_claim_type: {
    'LOST': 'Lost in Warehouse', // 仓库丢失
    'DAMAGED': 'Damaged in Warehouse', // 仓库损坏
    'INBOUND_SHORTAGE': 'Inbound Shortage', // 入库短少
    'RETURN_NOT_RECEIVED': 'Return Not Received', // 退货未退回
    'FEE_OVERCHARGE': 'Fulfillment Fee Overcharge', // 多收配送费
    'OTHER': 'Other', // 其他
  },
  // 索赔状态
  ecom_fba_claim_status: {
    'PENDING': 'To Submit', // 待提交
    'SUBMITTED': 'Submitted', // 已提交
    'PAID': 'Reimbursed', // 已赔付
    'REJECTED': 'Rejected', // 已驳回
    'IGNORED': 'Ignored', // 已忽略
  },
  // FBA 尺寸类别
  ecom_fba_size_category: {
    'STANDARD': 'Standard-size', // 标准件
    'OVERSIZE': 'Oversize', // 大件
  },
  // FBA 仓储费类型
  ecom_fba_storage_fee_type: {
    'MONTHLY': 'Monthly Storage Fee', // 月度仓储费
    'AGED': 'Aged Inventory Surcharge', // 超龄附加费
  },
  // ===== 第四轮：订单增强（ord） =====
  // 订单类型
  ecom_order_type: {
    'PLATFORM': 'Platform Order', // 平台订单
    'MANUAL': 'Manual Order', // 手工订单
    'RESEND': 'Resend', // 补发单
  },
  // 物流轨迹状态
  ecom_track_status: {
    'NOT_FOUND': 'No Tracking', // 暂无轨迹
    'PICKED_UP': 'Picked Up', // 已揽收
    'IN_TRANSIT': 'In Transit', // 运输中
    'PICKUP': 'Out for Delivery / Pickup', // 到达待取
    'DELIVERED': 'Delivered', // 已签收
    'EXCEPTION': 'Exception', // 异常
    'RETURNED': 'Returned', // 退回
  },
  // 物流轨迹预警
  ecom_track_alert: {
    'NO_SCAN': 'No First Scan', // 超时未上网
    'TRANSIT_TIMEOUT': 'Transit Timeout', // 运输超时
    'EXCEPTION': 'Tracking Exception', // 轨迹异常
  },
  // 黑名单匹配方式
  ecom_blacklist_type: {
    'BUYER': 'Buyer ID', // 买家 ID
    'EMAIL': 'Email', // 邮箱
    'ADDRESS': 'Address', // 地址
    'PHONE': 'Phone', // 电话
  },
  // 店铺岗位（业务字典）
  ecom_store_member_role: {
    'OPERATOR': 'Operator', // 运营
    'CS': 'Customer Service', // 客服
    'ASSISTANT': 'Assistant', // 助理
  },
  // 监控类型
  ecom_monitor_type: {
    'OWN': 'Own Product', // 自己的商品
    'COMPETITOR': 'Competitor', // 竞品
  },
  // 快照来源
  ecom_monitor_source: {
    'API': 'API', // 接口
    'MANUAL': 'Manual', // 手工
    'IMPORT': 'Import', // 导入
  },
  // 监控预警类型
  ecom_monitor_alert_type: {
    'RANK_DROP': 'Rank Drop', // 排名大跌
    'RATING_DROP': 'Rating Drop', // 评分下降
    'NEW_SELLER': 'New Hijacker', // 新增跟卖
    'TITLE_CHANGED': 'Title Changed', // 标题被改
    'IMAGE_CHANGED': 'Image Changed', // 主图被改
    'BULLETS_CHANGED': 'Bullets Changed', // 五点被改
    'DELISTED': 'Delisted', // 被下架
  },
  // 预警处理状态
  ecom_monitor_alert_status: {
    'NEW': 'Open', // 未处理
    'HANDLED': 'Handled', // 已处理
    'IGNORED': 'Ignored', // 已忽略
  },
  // 促销类型（业务字典）
  ecom_promo_type: {
    'COUPON': 'Coupon', // 优惠券
    'LIGHTNING_DEAL': 'Lightning Deal', // 秒杀 / Lightning Deal
    'PRIME_DISCOUNT': 'Prime Exclusive Discount', // Prime 专享折扣
    'SHOPEE_DISCOUNT': 'Shopee Discount', // 虾皮折扣活动
    'TIKTOK_FLASH_SALE': 'TikTok Flash Sale', // TikTok 闪购
    'FULL_REDUCTION': 'Spend & Save', // 店铺满减
    'PRICE_DISCOUNT': 'Price Markdown', // 直降
    'OTHER': 'Other', // 其他
  },
  // 促销状态
  ecom_promo_status: {
    'DRAFT': 'Draft', // 草稿
    'ACTIVE': 'Active', // 已生效
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 促销阶段
  ecom_promo_phase: {
    'NOT_STARTED': 'Upcoming', // 未开始
    'ONGOING': 'Ongoing', // 进行中
    'ENDED': 'Ended', // 已结束
  },
  // 促销推送状态
  ecom_promo_push_status: {
    'NONE': 'Not Pushed', // 未推送
    'PUSHED': 'Pushed', // 已推送
    'FAILED': 'Push Failed', // 推送失败
  },
  // 目标对象
  ecom_ops_target_type: {
    'STORE': 'Store', // 店铺
    'USER': 'Operator', // 运营人员
  },
  // 开发状态
  ecom_dev_status: {
    'PENDING_RESEARCH': 'Pending Research', // 待调研
    'RESEARCHING': 'Researching', // 调研中
    'SAMPLING': 'Sampling', // 打样中
    'PENDING_APPROVAL': 'Pending Approval', // 待审批
    'APPROVED': 'Approved', // 已通过
    'REJECTED': 'Rejected', // 已驳回
    'ABANDONED': 'Abandoned', // 已放弃
  },
  // 样品结论
  ecom_dev_sample_result: {
    'PENDING': 'Pending', // 待评估
    'PASSED': 'Passed', // 合格
    'FAILED': 'Failed', // 不合格
  },
  // 1688 订单状态
  ecom_1688_order_status: {
    'WAIT_BUYER_PAY': 'Awaiting Payment', // 待付款
    'WAIT_SELLER_SEND': 'Awaiting Shipment', // 待发货
    'WAIT_BUYER_RECEIVE': 'In Transit', // 待收货
    'CONFIRM_GOODS': 'Received', // 已收货
    'SUCCESS': 'Completed', // 交易成功
    'CANCEL': 'Cancelled', // 已取消
    'TERMINATED': 'Terminated', // 交易终止
  },
  // 产品来源
  ecom_dev_source: {
    'ALI1688': '1688 Sourcing', // 1688 选品
    'BESTSELLER': 'Marketplace Best Sellers', // 平台热卖
    'FACTORY': 'Trade Show / Factory', // 展会 / 工厂
    'DESIGN': 'Own Design', // 自主设计
    'OTHER': 'Other', // 其他
  },
  // 海外仓服务商
  ecom_owh_provider: {
    'GOODCANG': 'Goodcang', // 谷仓
    'WINIT': 'WINIT', // 万邑通
    'FOURPX_FB4': '4PX Overseas Warehouse', // 递四方海外仓
  },
  // 海外仓入库单状态
  ecom_owh_inbound_status: {
    'DRAFT': 'Draft', // 草稿
    'PUSHED': 'Pushed', // 已推送
    'RECEIVING': 'Receiving', // 收货中
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 海外仓出库单状态
  ecom_owh_outbound_status: {
    'FAILED': 'Push Failed', // 推送失败
    'PUSHED': 'Pushed', // 已推送
    'INTERCEPTING': 'Intercepting', // 截单中
    'OUTBOUND': 'Shipped by Warehouse', // 已出库
    'SHIPPED': 'Shipped', // 已发货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 海外仓费用类型
  ecom_owh_fee_type: {
    'STORAGE': 'Storage', // 仓租
    'OPERATION': 'Handling', // 操作费
    'FREIGHT': 'Last-mile Freight', // 尾程运费
    'INBOUND': 'Inbound', // 入库费
    'OTHER': 'Other', // 其他
  },
  // 海外仓入库来源
  ecom_owh_inbound_source: {
    'INBOUND': 'Shipment', // 头程货件
    'TRANSFER': 'Transfer', // 调拨单
  },
  // 海外仓费用入账状态
  ecom_owh_fee_status: {
    'PENDING': 'Pending', // 待入账
    'POSTED': 'Posted', // 已入账
    'UNALLOCATED': 'Unallocated', // 无法归属
  },
  // 海外仓退件单状态
  ecom_owh_return_status: {
    'DRAFT': 'Draft', // 草稿
    'PUSHED': 'Pushed', // 已推送
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 海外仓调拨单状态
  ecom_owh_transfer_status: {
    'DRAFT': 'Draft', // 草稿
    'OUT_PUSHED': 'Source Processing', // 调出仓处理中
    'IN_TRANSIT': 'In Transit', // 在途
    'RECEIVED': 'Received', // 已收货
    'CANCELLED': 'Cancelled', // 已取消
  },
  // 海外仓收货差异状态
  ecom_owh_diff_status: {
    'PENDING': 'Pending', // 待处理
    'LOSS': 'Loss Confirmed', // 确认损耗
    'FOUND': 'Found', // 找回补收
    'ACCEPTED': 'Overage Accepted', // 确认溢收
  },
  // 海外仓收货差异来源
  ecom_owh_diff_source: {
    'INBOUND': 'Inbound', // 入库单
    'RETURN': 'Return', // 退件单
    'TRANSFER': 'Transfer', // 调拨单
  },
}
