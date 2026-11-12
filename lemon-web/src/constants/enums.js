/**
 * 后端枚举常量 —— 由 scripts/gen-enums.mjs 生成，请勿手工修改；后端新增或修改枚举后运行 npm run gen:enums
 *
 * 来源：lemon-common、lemon-framework、lemon-modules 中文件名与枚举同名的顶层 Java 枚举，键和值都是枚举常量名。
 * 命名：Java 简单类名；多个模块有同名枚举时加模块前缀（如 WmsDocStatus、MesDocStatus）。
 * 用法：import { WorkOrderStatus } from '@/constants/enums'；record.status === WorkOrderStatus.DRAFT
 */

/** com.lemon.erp.modules.fin.model.AccountCategory（字典 fin_account_category） */
export const AccountCategory = Object.freeze({
  ASSET: 'ASSET',
  LIABILITY: 'LIABILITY',
  EQUITY: 'EQUITY',
  COST: 'COST',
  PROFIT: 'PROFIT', // 损益类
})

/** com.lemon.erp.modules.fin.model.AccountDirection */
export const AccountDirection = Object.freeze({
  DEBIT: 'DEBIT',
  CREDIT: 'CREDIT',
})

/** com.lemon.erp.modules.fin.model.AccountRole */
export const AccountRole = Object.freeze({
  CASH: 'CASH', // 库存现金
  BANK: 'BANK', // 银行存款
  RECEIVABLE: 'RECEIVABLE',
  PAYABLE: 'PAYABLE',
  ADVANCE_RECEIVED: 'ADVANCE_RECEIVED',
  PREPAYMENT: 'PREPAYMENT',
  NOTES_RECEIVABLE: 'NOTES_RECEIVABLE',
  NOTES_PAYABLE: 'NOTES_PAYABLE',
  OTHER_RECEIVABLE: 'OTHER_RECEIVABLE',
  OTHER_PAYABLE: 'OTHER_PAYABLE',
  FREIGHT_ADVANCE: 'FREIGHT_ADVANCE', // 代垫运费
  INVENTORY: 'INVENTORY', // 默认
  OUTSOURCE_MATERIAL: 'OUTSOURCE_MATERIAL',
  CONTRACT_COST: 'CONTRACT_COST',
  PENDING_LOSS: 'PENDING_LOSS',
  OTHER_IN_OFFSET: 'OTHER_IN_OFFSET', // 其他入库
  OTHER_OUT_OFFSET: 'OTHER_OUT_OFFSET', // 其他出库、报废出库
  REVENUE: 'REVENUE',
  COST_OF_SALES: 'COST_OF_SALES',
  OTHER_REVENUE: 'OTHER_REVENUE',
  CONTRACT_LIABILITY: 'CONTRACT_LIABILITY',
  SELLING_EXPENSE: 'SELLING_EXPENSE',
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  PURCHASE_FREIGHT: 'PURCHASE_FREIGHT', // 采购运费
  FINANCE_EXPENSE: 'FINANCE_EXPENSE',
  NON_OPERATING_INCOME: 'NON_OPERATING_INCOME',
  NON_OPERATING_EXPENSE: 'NON_OPERATING_EXPENSE',
  RECEIPT_DISCOUNT: 'RECEIPT_DISCOUNT', // 收款折让 / 抹零
  PAYMENT_DISCOUNT: 'PAYMENT_DISCOUNT', // 付款折让 / 抹零
  OFFSET_AR: 'OFFSET_AR', // 应收冲账
  OFFSET_AP: 'OFFSET_AP', // 应付冲账
  PURCHASE_REBATE: 'PURCHASE_REBATE', // 采购返利
  INPUT_VAT: 'INPUT_VAT',
  OUTPUT_VAT: 'OUTPUT_VAT',
  UNPAID_VAT: 'UNPAID_VAT',
  CITY_TAX: 'CITY_TAX',
  EDU_SURCHARGE: 'EDU_SURCHARGE',
  LOCAL_EDU_SURCHARGE: 'LOCAL_EDU_SURCHARGE',
  TAX_SURCHARGE: 'TAX_SURCHARGE',
  INCOME_TAX: 'INCOME_TAX',
  FIXED_ASSET: 'FIXED_ASSET',
  DEPRECIATION: 'DEPRECIATION',
  ASSET_DISPOSAL: 'ASSET_DISPOSAL',
  PROFIT_THIS_YEAR: 'PROFIT_THIS_YEAR',
  PROFIT_DISTRIBUTION: 'PROFIT_DISTRIBUTION',
})

/** com.lemon.erp.modules.fin.model.AdjustItem */
export const AdjustItem = Object.freeze({
  DISCOUNT: 'DISCOUNT', // 整单优惠
  FREIGHT: 'FREIGHT', // 本公司承担的运费
  SETTLE_DISCOUNT: 'SETTLE_DISCOUNT', // 收款折让 / 抹零
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
})

/** com.lemon.erp.modules.fin.model.AdjustMode */
export const AdjustMode = Object.freeze({
  BOTTOM: 'BOTTOM', // 汇总到底部
  ALLOCATE: 'ALLOCATE', // 分摊到各行
})

/** com.lemon.erp.framework.ai.AiCleanseField */
export const AiCleanseField = Object.freeze({
  NAME: 'NAME', // 名称
  SPEC: 'SPEC', // 规格型号
  CATEGORY: 'CATEGORY', // 分类
  UNIT: 'UNIT', // 计量单位
  CONTACT: 'CONTACT', // 联系人
  ADDRESS: 'ADDRESS', // 地址
})

/** com.lemon.erp.modules.ai.model.AiCleanseKind */
export const AiCleanseKind = Object.freeze({
  DUPLICATE: 'DUPLICATE', // 疑似重复
  NORMALIZE: 'NORMALIZE', // 规范化
  MISSING: 'MISSING', // 缺失字段提示
})

/** com.lemon.erp.modules.ai.model.AiCleansePhase */
export const AiCleansePhase = Object.freeze({
  LOADING: 'LOADING', // 读取记录、程序分组
  DUPLICATE: 'DUPLICATE', // 模型判断疑似重复
  NORMALIZE: 'NORMALIZE', // 模型给出规范化建议
  DONE: 'DONE', // 结束
})

/** com.lemon.erp.modules.ai.model.AiCleanseTaskStatus */
export const AiCleanseTaskStatus = Object.freeze({
  RUNNING: 'RUNNING', // 进行中
  COMPLETED: 'COMPLETED', // 已完成
  FAILED: 'FAILED', // 失败
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ai.model.AiFeature */
export const AiFeature = Object.freeze({
  CHAT: 'CHAT', // AI 问数
  EXTRACT: 'EXTRACT', // 智能录入
  CLEANSE: 'CLEANSE', // 数据整理
  REPORT: 'REPORT', // 智能报告
  TEST: 'TEST', // 「AI 设置」中的测试连接
})

/** com.lemon.erp.modules.ai.model.AiMessageRole */
export const AiMessageRole = Object.freeze({
  USER: 'USER', // 用户的提问
  ASSISTANT: 'ASSISTANT', // 模型的回答
  TOOL: 'TOOL', // 工具的返回结果
})

/** com.lemon.erp.modules.ai.model.AiProvider */
export const AiProvider = Object.freeze({
  DEEPSEEK: 'DEEPSEEK', // DeepSeek
  QWEN: 'QWEN', // 通义千问
  ZHIPU: 'ZHIPU', // 智谱 GLM
  MOONSHOT: 'MOONSHOT', // Kimi
  DOUBAO: 'DOUBAO', // 豆包
  CUSTOM: 'CUSTOM', // 其他兼容 OpenAI 协议的服务
})

/** com.lemon.erp.modules.ai.model.AiReportPeriodType */
export const AiReportPeriodType = Object.freeze({
  WEEK: 'WEEK',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.ai.model.AiReportStatus */
export const AiReportStatus = Object.freeze({
  GENERATING: 'GENERATING', // 正在生成
  DONE: 'DONE', // 已完成
  FAILED: 'FAILED', // 生成失败
})

/** com.lemon.erp.modules.ai.model.AiSuggestionStatus */
export const AiSuggestionStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  APPLIED: 'APPLIED', // 已采纳
  IGNORED: 'IGNORED', // 已忽略
})

/** com.lemon.erp.modules.workflow.model.ApprovalAction（字典 wf_task_action） */
export const ApprovalAction = Object.freeze({
  START: 'START', // 发起
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
  TRANSFER: 'TRANSFER',
  CANCEL: 'CANCEL', // 发起人撤回
  TERMINATE: 'TERMINATE', // 管理员终止
  CC: 'CC', // 抄送
  PENDING: 'PENDING', // 待审批
})

/** com.lemon.erp.modules.workflow.model.ApproveMode */
export const ApproveMode = Object.freeze({
  SEQUENTIAL: 'SEQUENTIAL', // 依次审批
  AND: 'AND', // 会签
  OR: 'OR', // 或签
})

/** com.lemon.erp.framework.workflow.ApproveStatus（字典 sys_approve_status） */
export const ApproveStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVED: 'APPROVED', // 已生效
  APPROVING: 'APPROVING', // 审批中
  REJECTED: 'REJECTED', // 已拒绝
  CANCELED: 'CANCELED', // 已撤回
  DONE: 'DONE', // 已生效
})

/** com.lemon.erp.modules.wms.model.AssemblyKind（字典 wms_assembly_kind） */
export const AssemblyKind = Object.freeze({
  ASSEMBLE: 'ASSEMBLE', // 组装
  DISASSEMBLE: 'DISASSEMBLE', // 拆卸
})

/** com.lemon.erp.modules.fin.model.AssetCheckResult（字典 fin_asset_check_result） */
export const AssetCheckResult = Object.freeze({
  PENDING: 'PENDING',
  NORMAL: 'NORMAL', // 相符
  MOVED: 'MOVED', // 位置变化
  LOSS: 'LOSS', // 盘亏
  GAIN: 'GAIN', // 盘盈
})

/** com.lemon.erp.modules.fin.model.AssetCheckStatus（字典 fin_asset_check_status） */
export const AssetCheckStatus = Object.freeze({
  CHECKING: 'CHECKING',
  DONE: 'DONE',
})

/** com.lemon.erp.modules.fin.model.AssetMoveType（字典 fin_asset_move_type） */
export const AssetMoveType = Object.freeze({
  ASSIGN: 'ASSIGN', // 领用
  RETURN: 'RETURN', // 归还
  TRANSFER: 'TRANSFER', // 调拨
})

/** com.lemon.erp.modules.fin.model.AssetStatus（字典 fin_asset_status） */
export const AssetStatus = Object.freeze({
  IN_USE: 'IN_USE',
  IDLE: 'IDLE',
  DISPOSED: 'DISPOSED',
})

/** com.lemon.erp.modules.workflow.model.AssigneeType */
export const AssigneeType = Object.freeze({
  USER: 'USER',
  ROLE: 'ROLE',
  DEPT_LEADER: 'DEPT_LEADER', // 发起人的部门主管
  INITIATOR: 'INITIATOR',
})

/** com.lemon.erp.modules.ecom.connector.AuthType */
export const AuthType = Object.freeze({
  OAUTH: 'OAUTH', // 卖家在平台授权页面同意后回调换取令牌
  KEYS: 'KEYS', // 直接填写平台后台生成的密钥 / 令牌
})

/** com.lemon.erp.modules.fin.model.AuxItem */
export const AuxItem = Object.freeze({
  DEPT: 'DEPT',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  PROJECT: 'PROJECT',
  MATERIAL: 'MATERIAL',
})

/** com.lemon.erp.modules.fin.model.BillKind */
export const BillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.fin.model.BillSource（字典 fin_bill_source） */
export const BillSource = Object.freeze({
  SALES_OUT: 'SALES_OUT', // 销售出库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  PURCHASE_IN: 'PURCHASE_IN', // 采购入库
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  MANUAL: 'MANUAL', // 期初 / 手工
  PROJECT: 'PROJECT', // 项目开票
  SHIPMENT: 'SHIPMENT', // 运费
  TICKET: 'TICKET', // 服务收费
  ASSET: 'ASSET', // 固定资产
  NOTE: 'NOTE', // 票据拒付
  QUALITY_CLAIM: 'QUALITY_CLAIM', // 质量索赔
  REBATE: 'REBATE', // 销售返利
  OFFSET: 'OFFSET', // 冲账单
  ORDER_ADJUST: 'ORDER_ADJUST', // 订单尾差调整
  PURCHASE_FREIGHT: 'PURCHASE_FREIGHT', // 采购运费
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  SALES_RETURN_FREIGHT: 'SALES_RETURN_FREIGHT', // 销售退货运费
  PURCHASE_RETURN_FREIGHT: 'PURCHASE_RETURN_FREIGHT', // 采购退货运费
})

/** com.lemon.erp.modules.fin.model.BillStatus（字典 fin_bill_status） */
export const BillStatus = Object.freeze({
  UNSETTLED: 'UNSETTLED', // 未核销
  PARTIAL: 'PARTIAL', // 部分核销
  SETTLED: 'SETTLED', // 已核销
})

/** com.lemon.erp.modules.fin.model.BudgetControl（字典 fin_budget_control） */
export const BudgetControl = Object.freeze({
  NONE: 'NONE', // 不控制
  WARN: 'WARN', // 超预算提示
  BLOCK: 'BLOCK', // 超预算禁止
})

/** com.lemon.erp.framework.setting.CaptchaMode */
export const CaptchaMode = Object.freeze({
  OFF: 'OFF',
  ALWAYS: 'ALWAYS',
  AFTER_FAILURE: 'AFTER_FAILURE',
})

/** com.lemon.erp.framework.setting.CaptchaType */
export const CaptchaType = Object.freeze({
  MATH: 'MATH',
  CHAR: 'CHAR',
})

/** com.lemon.erp.modules.fin.model.CheckLevel */
export const CheckLevel = Object.freeze({
  OK: 'OK', // 通过
  WARN: 'WARN', // 提示
  ERROR: 'ERROR', // 错误
})

/** com.lemon.erp.modules.workflow.model.ConditionOperator */
export const ConditionOperator = Object.freeze({
  EQ: 'EQ',
  NE: 'NE',
  GT: 'GT',
  GE: 'GE',
  LT: 'LT',
  LE: 'LE',
  IN: 'IN', // 属于其中之一
  CONTAINS: 'CONTAINS', // 文本包含
})

/** com.lemon.erp.modules.ecom.connector.ConnectorCapability */
export const ConnectorCapability = Object.freeze({
  ORDERS: 'ORDERS', // 拉取订单
  SHIP_CONFIRM: 'SHIP_CONFIRM', // 回传发货
  INVENTORY_PUSH: 'INVENTORY_PUSH', // 回传可售库存
  LISTINGS: 'LISTINGS', // 拉取在线商品
  PLATFORM_STOCK: 'PLATFORM_STOCK', // 拉取平台仓库存
})

/** com.lemon.erp.modules.fin.model.ConsumableStatus（字典 fin_consumable_status） */
export const ConsumableStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED', // 已生成部门领用出库单
})

/** com.lemon.erp.modules.scm.model.ContractStatus（字典 scm_contract_status） */
export const ContractStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  ACTIVE: 'ACTIVE', // 生效
  CLOSED: 'CLOSED',
})

/** com.lemon.erp.modules.scm.model.ContractType（字典 scm_contract_type） */
export const ContractType = Object.freeze({
  FRAMEWORK: 'FRAMEWORK', // 框架协议
  FIXED: 'FIXED', // 固定数量合同
})

/** com.lemon.erp.modules.wms.model.CostMethod（字典 wms_cost_method） */
export const CostMethod = Object.freeze({
  WEIGHTED: 'WEIGHTED', // 月末一次加权平均
  MOVING: 'MOVING', // 移动加权平均
  FIFO: 'FIFO', // 先进先出
})

/** com.lemon.erp.modules.fin.model.CostRunKind */
export const CostRunKind = Object.freeze({
  RUN: 'RUN', // 月度成本核算
  OPENING: 'OPENING', // 存货期初
})

/** com.lemon.erp.modules.scm.model.CreditControl */
export const CreditControl = Object.freeze({
  ALL: 'ALL', // 审核和出库时都控制
  SHIPMENT: 'SHIPMENT', // 只在出库时控制
  NONE: 'NONE', // 不控制
})

/** com.lemon.erp.modules.system.model.CustomFieldType（字典 sys_custom_field_type） */
export const CustomFieldType = Object.freeze({
  TEXT: 'TEXT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 数字
  DATE: 'DATE', // 日期
  SELECT: 'SELECT', // 下拉
  BOOL: 'BOOL', // 是否
})

/** com.lemon.erp.modules.dashboard.model.DashboardBillKind */
export const DashboardBillKind = Object.freeze({
  AR: 'AR', // 应收
  AP: 'AP', // 应付
})

/** com.lemon.erp.modules.dashboard.model.DashboardOrderKind */
export const DashboardOrderKind = Object.freeze({
  SALES: 'SALES', // 销售订单
  PURCHASE: 'PURCHASE', // 采购订单
})

/** com.lemon.erp.framework.security.datascope.DataScopeType（字典 sys_data_scope） */
export const DataScopeType = Object.freeze({
  ALL: 'ALL',
  CUSTOM: 'CUSTOM',
  DEPT: 'DEPT',
  DEPT_AND_CHILD: 'DEPT_AND_CHILD',
  SELF: 'SELF',
})

/** com.lemon.erp.modules.system.model.DictKind */
export const DictKind = Object.freeze({
  SYSTEM: 'SYSTEM', // 系统字典
  BUSINESS: 'BUSINESS', // 业务字典
})

/** com.lemon.erp.modules.wms.model.DocStatus（字典 wms_doc_status） */
export const DocStatus = Object.freeze({
  DRAFT: 'DRAFT',
  TRANSIT: 'TRANSIT', // 在途
  APPROVING: 'APPROVING', // 审批中
  DONE: 'DONE',
})

/** com.lemon.erp.modules.ecom.model.EcomCarrier（字典 ecom_carrier） */
export const EcomCarrier = Object.freeze({
  FOURPX: 'FOURPX', // 递四方 4PX
  YUNEXPRESS: 'YUNEXPRESS', // 云途物流 YunExpress
})

/** com.lemon.erp.modules.ecom.model.EcomLocalStatus（字典 ecom_local_status） */
export const EcomLocalStatus = Object.freeze({
  PENDING: 'PENDING', // 待审核
  ON_HOLD: 'ON_HOLD', // 已挂起
  EXCEPTION: 'EXCEPTION', // 异常
  OUT_OF_STOCK: 'OUT_OF_STOCK', // 缺货
  AUDITED: 'AUDITED', // 待打单
  LABELED: 'LABELED', // 待发货
  OUTBOUND: 'OUTBOUND', // 已出库
  PARTIAL: 'PARTIAL', // 部分发货
  SHIPPED: 'SHIPPED', // 已发货
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.ecom.model.EcomOrderStatus（字典 ecom_order_status） */
export const EcomOrderStatus = Object.freeze({
  PENDING: 'PENDING', // 待付款
  UNSHIPPED: 'UNSHIPPED', // 待发货
  SHIPPED: 'SHIPPED', // 已发货
  CANCELLED: 'CANCELLED', // 已取消
  REFUNDED: 'REFUNDED', // 已退款
})

/** com.lemon.erp.modules.ecom.model.EcomPackageStatus（字典 ecom_package_status） */
export const EcomPackageStatus = Object.freeze({
  WAIT_LABEL: 'WAIT_LABEL', // 待打单
  LABELED: 'LABELED', // 待发货
  SHIPPED: 'SHIPPED', // 已发货
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.model.EcomPlatform（字典 ecom_platform） */
export const EcomPlatform = Object.freeze({
  AMAZON: 'AMAZON', // 亚马逊
  ALIEXPRESS: 'ALIEXPRESS', // 速卖通
  EBAY: 'EBAY', // eBay
  SHOPEE: 'SHOPEE', // Shopee
  LAZADA: 'LAZADA', // Lazada
  SHOPIFY: 'SHOPIFY', // Shopify 独立站
  TIKTOK: 'TIKTOK', // TikTok Shop
  TEMU: 'TEMU', // Temu
  WALMART: 'WALMART', // 沃尔玛
  OZON: 'OZON', // Ozon
  MERCADO_LIBRE: 'MERCADO_LIBRE', // 美客多 Mercado Libre
  OTHER: 'OTHER', // 其他
})

/** com.lemon.erp.modules.ecom.model.EcomReturnStatus（字典 ecom_return_status） */
export const EcomReturnStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  RECEIVED: 'RECEIVED', // 已入库
  REFUNDED: 'REFUNDED', // 已退款
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.workflow.model.EmptyAction */
export const EmptyAction = Object.freeze({
  AUTO_PASS: 'AUTO_PASS', // 自动通过
  TO_ADMIN: 'TO_ADMIN', // 转交给超级管理员审批
})

/** com.lemon.erp.modules.fin.model.ExpenseClaimStatus（字典 fin_claim_status） */
export const ExpenseClaimStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 待付款
  PAID: 'PAID', // 已付款
})

/** com.lemon.erp.modules.fin.model.FeeBearer（字典 fin_fee_bearer） */
export const FeeBearer = Object.freeze({
  FROM: 'FROM',
  TO: 'TO',
})

/** com.lemon.erp.framework.workflow.FlowStatus（字典 wf_status） */
export const FlowStatus = Object.freeze({
  RUNNING: 'RUNNING', // 审批中
  APPROVED: 'APPROVED', // 审批通过
  REJECTED: 'REJECTED', // 审批拒绝
  CANCELED: 'CANCELED', // 发起人撤回或管理员终止
})

/** com.lemon.erp.modules.workflow.model.FormFieldType */
export const FormFieldType = Object.freeze({
  INPUT: 'INPUT',
  TEXTAREA: 'TEXTAREA',
  NUMBER: 'NUMBER', // 整数保存为 Long
  DATE: 'DATE', // yyyy-MM-dd
  SELECT: 'SELECT',
})

/** com.lemon.erp.modules.scm.model.FreightBearer（字典 scm_freight_bearer） */
export const FreightBearer = Object.freeze({
  COMPANY: 'COMPANY', // 本公司承担
  CUSTOMER: 'CUSTOMER', // 客户一方
  SUPPLIER: 'SUPPLIER', // 供应商一方
})

/** com.lemon.erp.modules.scm.model.FreightSettle（字典 scm_freight_settle） */
export const FreightSettle = Object.freeze({
  CASH: 'CASH', // 现付
  MONTHLY: 'MONTHLY', // 月结
})

/** com.lemon.erp.modules.ecom.model.FulfillmentType（字典 ecom_fulfillment） */
export const FulfillmentType = Object.freeze({
  PLATFORM: 'PLATFORM', // 平台仓配送
  SELLER: 'SELLER', // 自发货
})

/** com.lemon.erp.modules.fin.model.FundDirection（字典 fin_fund_direction） */
export const FundDirection = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
})

/** com.lemon.erp.modules.fin.model.FundSource */
export const FundSource = Object.freeze({
  AR: 'AR', // 未核销的应收单
  AP: 'AP', // 未核销的应付单
  NOTE_IN: 'NOTE_IN', // 持有中的应收票据到期
  NOTE_OUT: 'NOTE_OUT', // 未兑付的应付票据到期
  CLAIM: 'CLAIM', // 已审批待付款的报销
  PLAN: 'PLAN', // 手工计划项
})

/** com.lemon.erp.modules.ecom.jxc.model.GoodsRankBy */
export const GoodsRankBy = Object.freeze({
  QTY: 'QTY', // 净销量
  AMOUNT: 'AMOUNT', // 净销售额
  PROFIT: 'PROFIT', // 毛利
})

/** com.lemon.erp.modules.wms.model.HoldStatus（字典 wms_hold_status） */
export const HoldStatus = Object.freeze({
  PENDING: 'PENDING', // 待检
  REJECTED: 'REJECTED', // 不合格
  RELEASED: 'RELEASED', // 已放行
  RETURNED: 'RETURNED', // 已退货
})

/** com.lemon.erp.modules.system.im.ImPlatform（字典 sys_im_platform） */
export const ImPlatform = Object.freeze({
  DINGTALK: 'DINGTALK',
  WECOM: 'WECOM',
  FEISHU: 'FEISHU',
})

/** com.lemon.erp.modules.ecom.model.InboundStatus（字典 ecom_inbound_status） */
export const InboundStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  SHIPPED: 'SHIPPED', // 已发运
  RECEIVED: 'RECEIVED', // 已签收
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.fin.model.InvoiceKind */
export const InvoiceKind = Object.freeze({
  OUTPUT: 'OUTPUT', // 销项发票
  INPUT: 'INPUT', // 进项发票
})

/** com.lemon.erp.modules.fin.model.InvoiceSource */
export const InvoiceSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工登记
  XML: 'XML', // 导入电子发票 XML
  EXCEL: 'EXCEL', // Excel 批量导入
})

/** com.lemon.erp.modules.fin.model.InvoiceStatus */
export const InvoiceStatus = Object.freeze({
  VALID: 'VALID', // 有效
  VOID: 'VOID', // 已作废
})

/** com.lemon.erp.modules.fin.model.InvoiceType（字典 fin_invoice_type） */
export const InvoiceType = Object.freeze({
  SPECIAL: 'SPECIAL', // 增值税专用发票
  NORMAL: 'NORMAL', // 普通发票
  E_SPECIAL: 'E_SPECIAL', // 数电票
  E_NORMAL: 'E_NORMAL', // 数电票
})

/** com.lemon.erp.modules.fin.model.IoCategoryCode */
export const IoCategoryCode = Object.freeze({
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  FREIGHT_ADVANCE: 'FREIGHT_ADVANCE', // 代垫运费
})

/** com.lemon.erp.modules.fin.model.IoPartnerType（字典 fin_io_partner_type） */
export const IoPartnerType = Object.freeze({
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  OTHER: 'OTHER',
})

/** com.lemon.erp.modules.fin.model.IoStatus（字典 fin_io_status） */
export const IoStatus = Object.freeze({
  DRAFT: 'DRAFT',
  CONFIRMED: 'CONFIRMED',
})

/** com.lemon.erp.modules.system.model.JobRunStatus */
export const JobRunStatus = Object.freeze({
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
})

/** com.lemon.erp.modules.system.model.JobTrigger */
export const JobTrigger = Object.freeze({
  SCHEDULE: 'SCHEDULE', // 按 Cron 表达式定时触发
  MANUAL: 'MANUAL', // 手动立即执行
})

/** com.lemon.erp.modules.ecom.jxc.model.JxcDimension */
export const JxcDimension = Object.freeze({
  CATEGORY: 'CATEGORY', // 物料分类
  BRAND: 'BRAND', // 品牌
  REGION: 'REGION', // 客户区域
  CUSTOMER: 'CUSTOMER', // 客户
  SALESMAN: 'SALESMAN', // 业务员
  SUPPLIER: 'SUPPLIER', // 供应商
  MATERIAL: 'MATERIAL', // 商品
  DAY: 'DAY', // 日
  MONTH: 'MONTH', // 月
})

/** com.lemon.erp.modules.fin.model.LedgerRowType */
export const LedgerRowType = Object.freeze({
  OPEN: 'OPEN', // 期初余额
  ENTRY: 'ENTRY', // 凭证分录
  DAY: 'DAY', // 本日合计
  TOTAL: 'TOTAL', // 本期合计
})

/** com.lemon.erp.modules.ecom.model.ListingComposeType（字典 ecom_compose_type） */
export const ListingComposeType = Object.freeze({
  SINGLE: 'SINGLE', // 单品
  BUNDLE: 'BUNDLE', // 组合
})

/** com.lemon.erp.modules.ecom.model.ListingStatus（字典 ecom_listing_status） */
export const ListingStatus = Object.freeze({
  ACTIVE: 'ACTIVE', // 在售
  INACTIVE: 'INACTIVE', // 下架
  INCOMPLETE: 'INCOMPLETE', // 信息不完整
  DELETED: 'DELETED', // 已删除
})

/** com.lemon.erp.modules.fin.model.LoanRepayMethod */
export const LoanRepayMethod = Object.freeze({
  CASH: 'CASH', // 现金 / 银行还款
  CLAIM: 'CLAIM', // 报销冲抵
})

/** com.lemon.erp.modules.fin.model.LoanStatus（字典 fin_loan_status） */
export const LoanStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 待付款
  PAID: 'PAID', // 未还清
  CLEARED: 'CLEARED', // 已还清
})

/** com.lemon.erp.modules.wms.model.LocationType（字典 wms_location_type） */
export const LocationType = Object.freeze({
  STORAGE: 'STORAGE',
  PICK: 'PICK',
  RECEIVE: 'RECEIVE',
  SHIP: 'SHIP',
  DEFECT: 'DEFECT',
})

/** com.lemon.erp.modules.fin.model.MarginGroup */
export const MarginGroup = Object.freeze({
  MATERIAL: 'MATERIAL',
  CUSTOMER: 'CUSTOMER',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.system.model.MenuType（字典 sys_menu_type） */
export const MenuType = Object.freeze({
  M: 'M', // 目录
  C: 'C', // 菜单
  F: 'F', // 按钮
})

/** com.lemon.erp.framework.message.MessageCategory（字典 sys_message_category） */
export const MessageCategory = Object.freeze({
  APPROVAL: 'APPROVAL', // 审批
  REMIND: 'REMIND', // 提醒
  ALERT: 'ALERT', // 预警
  SYSTEM: 'SYSTEM', // 系统
  NOTICE: 'NOTICE', // 公告
})

/** com.lemon.erp.modules.workflow.model.NodeStatus */
export const NodeStatus = Object.freeze({
  DONE: 'DONE', // 已完成
  RUNNING: 'RUNNING', // 进行中
  REJECTED: 'REJECTED', // 已拒绝
})

/** com.lemon.erp.modules.workflow.model.NodeType */
export const NodeType = Object.freeze({
  START: 'START', // 发起人
  APPROVER: 'APPROVER', // 审批人
  CC: 'CC', // 抄送人
  CONDITION: 'CONDITION', // 条件分支
  PARALLEL: 'PARALLEL', // 并行分支
})

/** com.lemon.erp.modules.system.model.NoticeScope（字典 sys_notice_scope） */
export const NoticeScope = Object.freeze({
  ALL: 'ALL', // 全员
  DEPT: 'DEPT', // 指定部门
  ROLE: 'ROLE', // 指定角色
})

/** com.lemon.erp.modules.system.model.NoticeStatus（字典 sys_notice_status） */
export const NoticeStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  PUBLISHED: 'PUBLISHED', // 已发布
  WITHDRAWN: 'WITHDRAWN', // 已撤回
})

/** com.lemon.erp.modules.fin.model.OffsetStatus（字典 fin_offset_status） */
export const OffsetStatus = Object.freeze({
  DRAFT: 'DRAFT', // 待复核
  REVIEWED: 'REVIEWED', // 已复核
})

/** com.lemon.erp.framework.log.OperType（字典 sys_oper_type） */
export const OperType = Object.freeze({
  OTHER: 'OTHER',
  INSERT: 'INSERT',
  UPDATE: 'UPDATE',
  DELETE: 'DELETE',
  GRANT: 'GRANT',
  EXPORT: 'EXPORT',
  FORCE: 'FORCE',
  CLEAN: 'CLEAN',
  IMPORT: 'IMPORT',
})

/** com.lemon.erp.modules.scm.model.OrderChangeAction */
export const OrderChangeAction = Object.freeze({
  ADD: 'ADD', // 新增明细
  MODIFY: 'MODIFY', // 修改数量 / 单价
  REMOVE: 'REMOVE', // 删除明细
})

/** com.lemon.erp.modules.scm.model.OrderChangeStatus（字典 scm_change_status） */
export const OrderChangeStatus = Object.freeze({
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已生效
  REJECTED: 'REJECTED', // 已驳回
})

/** com.lemon.erp.modules.scm.model.OrderKind */
export const OrderKind = Object.freeze({
  PURCHASE: 'PURCHASE',
  SALES: 'SALES',
})

/** com.lemon.erp.modules.scm.model.OrderSource（字典 scm_order_source） */
export const OrderSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工
  MRP: 'MRP', // MRP
  KIT: 'KIT', // 生产缺料
  CRM: 'CRM', // 商机
  REQUISITION: 'REQUISITION', // 请购单
  INQUIRY: 'INQUIRY', // 询价单
  QUOTATION: 'QUOTATION', // 销售报价
  CONSIGN: 'CONSIGN', // 寄售结算
  TENDER: 'TENDER', // 招标
  DEALER: 'DEALER', // 经销商订货
  IMPORT: 'IMPORT', // 外部导入
  DIRECT: 'DIRECT', // 直接开单
  PURCHASE_PLAN: 'PURCHASE_PLAN', // 电商采购计划
})

/** com.lemon.erp.modules.scm.model.OrderStatus（字典 scm_order_status） */
export const OrderStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVING: 'APPROVING', // 审批中
  APPROVED: 'APPROVED', // 已审核
  PARTIAL: 'PARTIAL', // 部分入库 / 出库
  COMPLETED: 'COMPLETED', // 已完成
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.ecom.model.PairingMatchType */
export const PairingMatchType = Object.freeze({
  EXACT: 'EXACT', // 平台 SKU 与物料编码相同
  STRIPPED: 'STRIPPED', // 去掉前后缀后与物料编码相同
  TITLE: 'TITLE', // 标题与物料名称相似
  ATTRIBUTE: 'ATTRIBUTE', // 变体属性与 SKU 规格相同
})

/** com.lemon.erp.modules.fin.model.PartnerLedgerType */
export const PartnerLedgerType = Object.freeze({
  BILL: 'BILL',
  SETTLEMENT: 'SETTLEMENT',
  ADVANCE: 'ADVANCE',
  NETTING: 'NETTING', // 应收应付对冲
})

/** com.lemon.erp.framework.ref.PartnerType */
export const PartnerType = Object.freeze({
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
})

/** com.lemon.erp.framework.setting.PasswordComplexity */
export const PasswordComplexity = Object.freeze({
  LETTER_DIGIT: 'LETTER_DIGIT',
  MIXED_CASE_DIGIT: 'MIXED_CASE_DIGIT',
  LETTER_DIGIT_SYMBOL: 'LETTER_DIGIT_SYMBOL',
})

/** com.lemon.erp.modules.fin.model.PeriodStatus */
export const PeriodStatus = Object.freeze({
  OPEN: 'OPEN', // 未结账
  CLOSED: 'CLOSED', // 已结账
})

/** com.lemon.erp.modules.fin.model.PlanUnit */
export const PlanUnit = Object.freeze({
  WEEK: 'WEEK',
  MONTH: 'MONTH',
})

/** com.lemon.erp.framework.portal.PortalKind */
export const PortalKind = Object.freeze({
  SUPPLIER: 'SUPPLIER',
  DEALER: 'DEALER',
})

/** com.lemon.erp.modules.scm.model.PriceControlMode（字典 scm_price_control_mode） */
export const PriceControlMode = Object.freeze({
  WARN: 'WARN', // 提示
  BLOCK: 'BLOCK', // 禁止保存
})

/** com.lemon.erp.modules.scm.model.PriceSource */
export const PriceSource = Object.freeze({
  CUSTOMER_PRICE: 'CUSTOMER_PRICE',
  PRICE_LIST: 'PRICE_LIST',
  PRICE_LEVEL: 'PRICE_LEVEL',
  RETAIL: 'RETAIL',
  CONTRACT_PRICE: 'CONTRACT_PRICE',
  SUPPLIER_PRICE: 'SUPPLIER_PRICE',
  LAST_PRICE: 'LAST_PRICE',
  REF_COST: 'REF_COST',
})

/** com.lemon.erp.modules.scm.model.PromoType（字典 scm_promo_type） */
export const PromoType = Object.freeze({
  DISCOUNT: 'DISCOUNT',
  REDUCE: 'REDUCE',
  GIFT: 'GIFT',
})

/** com.lemon.erp.modules.scm.model.PurchaseFreightCost */
export const PurchaseFreightCost = Object.freeze({
  INVENTORY: 'INVENTORY', // 计入存货成本
  EXPENSE: 'EXPENSE', // 计入当期费用
})

/** com.lemon.erp.modules.ecom.model.PurchasePlanStatus（字典 ecom_purchase_plan_status） */
export const PurchasePlanStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  APPROVED: 'APPROVED', // 已审核
  ORDERED: 'ORDERED', // 已下单
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.scm.model.QuickPrice */
export const QuickPrice = Object.freeze({
  REF_COST: 'REF_COST', // 参考进货价
  AVG_COST: 'AVG_COST', // 库存均价
  LAST_PURCHASE: 'LAST_PURCHASE', // 最新进价
  LAST_SALES: 'LAST_SALES', // 最新售价
  RETAIL: 'RETAIL',
  WHOLESALE1: 'WHOLESALE1',
  WHOLESALE2: 'WHOLESALE2',
  WHOLESALE3: 'WHOLESALE3',
  WHOLESALE4: 'WHOLESALE4',
  WHOLESALE5: 'WHOLESALE5',
  WHOLESALE6: 'WHOLESALE6',
  WHOLESALE7: 'WHOLESALE7',
  WHOLESALE8: 'WHOLESALE8',
  NONE: 'NONE', // 不显示价格
})

/** com.lemon.erp.modules.scm.model.RebateCalcMode（字典 scm_rebate_calc） */
export const RebateCalcMode = Object.freeze({
  TIER: 'TIER',
  ITEM: 'ITEM',
})

/** com.lemon.erp.modules.scm.model.RebateKind（字典 scm_rebate_kind） */
export const RebateKind = Object.freeze({
  SALES: 'SALES',
  PURCHASE: 'PURCHASE',
})

/** com.lemon.erp.modules.scm.model.RebateSettle（字典 scm_rebate_settle） */
export const RebateSettle = Object.freeze({
  CREDIT_NOTE: 'CREDIT_NOTE',
  NEXT_ORDER: 'NEXT_ORDER',
})

/** com.lemon.erp.modules.scm.model.RebateStatus（字典 scm_rebate_status） */
export const RebateStatus = Object.freeze({
  ACTIVE: 'ACTIVE',
  SETTLED: 'SETTLED',
})

/** com.lemon.erp.framework.ref.RefType */
export const RefType = Object.freeze({
  MATERIAL: 'MATERIAL',
  WAREHOUSE: 'WAREHOUSE',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  USER: 'USER',
  DEPT: 'DEPT',
  MATERIAL_CATEGORY: 'MATERIAL_CATEGORY', // 物料分类
})

/** com.lemon.erp.modules.ecom.model.ReplenishLevel */
export const ReplenishLevel = Object.freeze({
  OK: 'OK', // 库存充足
  WARN: 'WARN', // 预警
  URGENT: 'URGENT', // 紧急
})

/** com.lemon.erp.modules.ecom.model.ReplenishScope */
export const ReplenishScope = Object.freeze({
  LISTING: 'LISTING', // 在线商品
  MATERIAL: 'MATERIAL', // 物料
})

/** com.lemon.erp.modules.wms.model.ReservationSource */
export const ReservationSource = Object.freeze({
  SALES: 'SALES', // 销售订单
})

/** com.lemon.erp.modules.ecom.model.ReturnType（字典 ecom_return_type） */
export const ReturnType = Object.freeze({
  REFUND_ONLY: 'REFUND_ONLY', // 仅退款
  RETURN_REFUND: 'RETURN_REFUND', // 退货退款
})

/** com.lemon.erp.modules.scm.model.RfmMode */
export const RfmMode = Object.freeze({
  THRESHOLD: 'THRESHOLD', // 固定阈值
  AVERAGE: 'AVERAGE', // 平均分
})

/** com.lemon.erp.modules.scm.model.RfmSegment */
export const RfmSegment = Object.freeze({
  IMPORTANT_VALUE: 'IMPORTANT_VALUE', // 重要价值
  IMPORTANT_DEVELOP: 'IMPORTANT_DEVELOP', // 重要发展
  IMPORTANT_KEEP: 'IMPORTANT_KEEP', // 重要保持
  IMPORTANT_RETAIN: 'IMPORTANT_RETAIN', // 重要挽留
  GENERAL_VALUE: 'GENERAL_VALUE', // 一般价值
  GENERAL_DEVELOP: 'GENERAL_DEVELOP', // 一般发展
  GENERAL_KEEP: 'GENERAL_KEEP', // 一般保持
  GENERAL_RETAIN: 'GENERAL_RETAIN', // 一般挽留
})

/** com.lemon.erp.modules.scm.model.SalesChannel（字典 scm_channel） */
export const SalesChannel = Object.freeze({
  DIRECT: 'DIRECT',
  DEALER: 'DEALER',
})

/** com.lemon.erp.modules.wms.model.SerialStatus（字典 wms_serial_status） */
export const SerialStatus = Object.freeze({
  IN_STOCK: 'IN_STOCK',
  OUT: 'OUT',
})

/** com.lemon.erp.framework.setting.Setting */
export const Setting = Object.freeze({
  SYSTEM_NAME: 'SYSTEM_NAME',
  SHOW_DEMO_ACCOUNT: 'SHOW_DEMO_ACCOUNT',
  WATERMARK: 'WATERMARK',
  CAPTCHA_MODE: 'CAPTCHA_MODE',
  CAPTCHA_TYPE: 'CAPTCHA_TYPE',
  LOGIN_MAX_RETRY: 'LOGIN_MAX_RETRY',
  LOGIN_LOCK_MINUTES: 'LOGIN_LOCK_MINUTES',
  SESSION_EXPIRE_HOURS: 'SESSION_EXPIRE_HOURS',
  SESSION_SINGLE: 'SESSION_SINGLE',
  PASSWORD_MIN_LENGTH: 'PASSWORD_MIN_LENGTH',
  PASSWORD_COMPLEXITY: 'PASSWORD_COMPLEXITY',
  PASSWORD_EXPIRE_DAYS: 'PASSWORD_EXPIRE_DAYS',
  USER_INITIAL_PASSWORD: 'USER_INITIAL_PASSWORD',
  ATTACHMENT_MAX_MB: 'ATTACHMENT_MAX_MB',
})

/** com.lemon.erp.framework.setting.SettingType */
export const SettingType = Object.freeze({
  TEXT: 'TEXT',
  NUMBER: 'NUMBER',
  BOOL: 'BOOL',
  SELECT: 'SELECT',
  DECIMAL: 'DECIMAL', // 小数
  TABLE: 'TABLE', // 表格
})

/** com.lemon.erp.modules.fin.model.SettleKind */
export const SettleKind = Object.freeze({
  RECEIPT: 'RECEIPT', // 收款
  PAYMENT: 'PAYMENT', // 付款
})

/** com.lemon.erp.modules.ecom.model.SettlementItemType（字典 ecom_settlement_item_type） */
export const SettlementItemType = Object.freeze({
  ORDER: 'ORDER', // 订单收入
  REFUND: 'REFUND', // 退款
  COMMISSION: 'COMMISSION', // 平台佣金
  FULFILLMENT: 'FULFILLMENT', // 配送费
  STORAGE: 'STORAGE', // 仓储费
  AD: 'AD', // 广告费
  OTHER: 'OTHER', // 其他费用
  ADJUSTMENT: 'ADJUSTMENT', // 调整
})

/** com.lemon.erp.modules.ecom.model.SettlementStatus（字典 ecom_settlement_status） */
export const SettlementStatus = Object.freeze({
  IMPORTED: 'IMPORTED', // 已导入
  CONFIRMED: 'CONFIRMED', // 已确认
})

/** com.lemon.erp.modules.ecom.model.ShipmentPlanStatus（字典 ecom_shipment_plan_status） */
export const ShipmentPlanStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  CONFIRMED: 'CONFIRMED', // 已确认
  GENERATED: 'GENERATED', // 已生成货件
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.connector.ShipmentResultStatus */
export const ShipmentResultStatus = Object.freeze({
  CONFIRMED: 'CONFIRMED', // 已把承运商、运单号回传平台
  PLATFORM_MANAGED: 'PLATFORM_MANAGED', // 平台物流托管
  SKIPPED: 'SKIPPED', // 跳过
})

/** com.lemon.erp.modules.fin.model.StatementStatus（字典 fin_statement_status） */
export const StatementStatus = Object.freeze({
  DRAFT: 'DRAFT',
  SENT: 'SENT', // 已发送
  CONFIRMED: 'CONFIRMED',
  DISPUTED: 'DISPUTED', // 供应商有异议
})

/** com.lemon.erp.modules.wms.model.StockBizType（字典 wms_biz_type） */
export const StockBizType = Object.freeze({
  IN: 'IN',
  OUT: 'OUT',
  TRANSFER: 'TRANSFER',
  CHECK: 'CHECK',
})

/** com.lemon.erp.modules.wms.model.StockInType（字典 wms_in_type） */
export const StockInType = Object.freeze({
  PURCHASE: 'PURCHASE', // 采购入库
  PRODUCTION: 'PRODUCTION', // 生产入库
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  OTHER: 'OTHER', // 其他入库
  PRODUCTION_RETURN: 'PRODUCTION_RETURN', // 生产退料
  OUTSOURCE_RETURN: 'OUTSOURCE_RETURN', // 委外收回
  SCRAP_IN: 'SCRAP_IN', // 报废入库
  CONSIGN_IN: 'CONSIGN_IN', // 寄售入库
  OPENING: 'OPENING', // 期初入库
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
})

/** com.lemon.erp.modules.wms.model.StockOutType（字典 wms_out_type） */
export const StockOutType = Object.freeze({
  SALES: 'SALES', // 销售出库
  PRODUCTION: 'PRODUCTION', // 生产领料
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  OTHER: 'OTHER', // 其他出库
  PROJECT: 'PROJECT', // 项目领料
  OUTSOURCE: 'OUTSOURCE', // 委外发料
  SERVICE: 'SERVICE', // 服务领用
  SCRAP: 'SCRAP', // 报废出库
  CONSIGN: 'CONSIGN', // 寄售消耗
  CONSUME: 'CONSUME', // 部门领用
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
})

/** com.lemon.erp.modules.ecom.model.StoreAuthStatus */
export const StoreAuthStatus = Object.freeze({
  NONE: 'NONE', // 未授权
  AUTHORIZED: 'AUTHORIZED', // 已授权
  REAUTH: 'REAUTH', // 需重新授权
})

/** com.lemon.erp.modules.ecom.model.StyleMatchRule */
export const StyleMatchRule = Object.freeze({
  ATTRIBUTE: 'ATTRIBUTE', // 按变体属性
  SKU: 'SKU', // 按 SKU 编码
})

/** com.lemon.erp.modules.scm.model.SupplierConfirm（字典 scm_supplier_confirm） */
export const SupplierConfirm = Object.freeze({
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
})

/** com.lemon.erp.modules.scm.model.SupplierLevel（字典 scm_supplier_level） */
export const SupplierLevel = Object.freeze({
  A: 'A', // A 优秀
  B: 'B', // B 良好
  C: 'C', // C 合格
  D: 'D', // D 待改进
})

/** com.lemon.erp.modules.ecom.model.SyncType（字典 ecom_sync_type） */
export const SyncType = Object.freeze({
  ORDERS: 'ORDERS', // 拉取订单
  SHIP_CONFIRM: 'SHIP_CONFIRM', // 回传发货
  INVENTORY_PUSH: 'INVENTORY_PUSH', // 回传库存
  LISTINGS: 'LISTINGS', // 拉取在线商品
  PLATFORM_STOCK: 'PLATFORM_STOCK', // 平台仓库存
  AUTH: 'AUTH', // 授权
})

/** com.lemon.erp.modules.scm.model.TradeKind（字典 scm_trade_kind） */
export const TradeKind = Object.freeze({
  PURCHASE_IN: 'PURCHASE_IN',
  PURCHASE_RETURN: 'PURCHASE_RETURN',
  SALES_OUT: 'SALES_OUT',
  SALES_RETURN: 'SALES_RETURN',
})

/** com.lemon.erp.modules.fin.model.VoucherSource（字典 fin_voucher_source） */
export const VoucherSource = Object.freeze({
  MANUAL: 'MANUAL',
  AR: 'AR', // 应收单
  AP: 'AP', // 应付单
  RECEIPT: 'RECEIPT', // 收款单
  PAYMENT: 'PAYMENT', // 付款单
  DEPRECIATION: 'DEPRECIATION', // 折旧计提
  ASSET: 'ASSET', // 资产处置
  COST: 'COST', // 成本结转
  CLOSE: 'CLOSE', // 期末结转损益
  SALARY: 'SALARY', // 工资计提、发放
  PROJECT: 'PROJECT',
  EXPENSE: 'EXPENSE',
  ADVANCE: 'ADVANCE', // 预收预付及冲抵
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
  REVERSAL: 'REVERSAL', // 红字冲销
  TAX: 'TAX', // 增值税结转、附加税计提
  CLAIM: 'CLAIM', // 费用报销
  LOAN: 'LOAN', // 员工借款、还款
  FX: 'FX', // 期末调汇
  NOTE: 'NOTE', // 票据贴现、托收、拒付、兑付
  TRANSFER: 'TRANSFER', // 账户调拨
  INCOME_EXPENSE: 'INCOME_EXPENSE', // 其他收支单
  NETTING: 'NETTING', // 应收应付对冲
})

/** com.lemon.erp.modules.fin.model.VoucherStatus（字典 fin_voucher_status） */
export const VoucherStatus = Object.freeze({
  DRAFT: 'DRAFT',
  APPROVING: 'APPROVING',
  AUDITED: 'AUDITED',
  POSTED: 'POSTED',
})

/** com.lemon.erp.modules.wms.model.WarehouseType（字典 wms_warehouse_type） */
export const WarehouseType = Object.freeze({
  NORMAL: 'NORMAL', // 普通仓
  RAW: 'RAW', // 原料仓
  FINISHED: 'FINISHED', // 成品仓
  DEFECT: 'DEFECT', // 不良品仓
  CONSIGN_OUT: 'CONSIGN_OUT', // 客户寄售仓
  CONSIGN_IN: 'CONSIGN_IN', // 供应商寄售仓
})
