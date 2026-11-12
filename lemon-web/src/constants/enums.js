/**
 * 后端枚举常量 —— 由 scripts/gen-enums.mjs 生成，请勿手工修改；后端新增或修改枚举后运行 npm run gen:enums
 *
 * 来源：lemon-common、lemon-framework、lemon-modules 中文件名与枚举同名的顶层 Java 枚举，键和值都是枚举常量名。
 * 命名：Java 简单类名；多个模块有同名枚举时加模块前缀（如 WmsDocStatus、MesDocStatus）。
 * 用法：import { WorkOrderStatus } from '@/constants/enums'；record.status === WorkOrderStatus.DRAFT
 */

/** com.lemon.erp.modules.ecom.analytics.model.AbcClass（字典 ecom_abc_class） */
export const AbcClass = Object.freeze({
  A: 'A',
  B: 'B',
  C: 'C',
})

/** com.lemon.erp.modules.ecom.analytics.model.AbcMetric */
export const AbcMetric = Object.freeze({
  SALES: 'SALES', // 销售额
  PROFIT: 'PROFIT', // 毛利
})

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
  OTHER_RECEIVABLE: 'OTHER_RECEIVABLE',
  OTHER_PAYABLE: 'OTHER_PAYABLE',
  FREIGHT_ADVANCE: 'FREIGHT_ADVANCE', // 代垫运费
  INVENTORY: 'INVENTORY', // 默认
  PENDING_LOSS: 'PENDING_LOSS',
  OTHER_IN_OFFSET: 'OTHER_IN_OFFSET', // 其他入库
  OTHER_OUT_OFFSET: 'OTHER_OUT_OFFSET', // 其他出库、报废出库
  REVENUE: 'REVENUE',
  COST_OF_SALES: 'COST_OF_SALES',
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
  PLATFORM_FEE: 'PLATFORM_FEE', // 电商平台费用
  INPUT_VAT: 'INPUT_VAT',
  OUTPUT_VAT: 'OUTPUT_VAT',
  UNPAID_VAT: 'UNPAID_VAT',
  CITY_TAX: 'CITY_TAX',
  EDU_SURCHARGE: 'EDU_SURCHARGE',
  LOCAL_EDU_SURCHARGE: 'LOCAL_EDU_SURCHARGE',
  TAX_SURCHARGE: 'TAX_SURCHARGE',
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

/** com.lemon.erp.modules.ecom.ads.AdsCapability */
export const AdsCapability = Object.freeze({
  PROFILES: 'PROFILES', // 列出授权账号下的广告配置文件
  STRUCTURE: 'STRUCTURE', // 拉取广告活动 / 广告组 / 投放结构
  REPORTS: 'REPORTS', // 拉取广告报告
  UPDATE_BID: 'UPDATE_BID', // 修改竞价
  UPDATE_BUDGET: 'UPDATE_BUDGET', // 修改活动日预算
  UPDATE_STATE: 'UPDATE_STATE', // 暂停 / 启用
  NEGATIVE_KEYWORDS: 'NEGATIVE_KEYWORDS', // 添加否定关键词
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
  WRITE: 'WRITE', // AI 写作
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

/** com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus（字典 ecom_1688_order_status） */
export const Ali1688OrderStatus = Object.freeze({
  WAIT_BUYER_PAY: 'WAIT_BUYER_PAY', // 待付款
  WAIT_SELLER_SEND: 'WAIT_SELLER_SEND', // 待发货
  WAIT_BUYER_RECEIVE: 'WAIT_BUYER_RECEIVE', // 待收货
  CONFIRM_GOODS: 'CONFIRM_GOODS', // 已收货
  SUCCESS: 'SUCCESS', // 交易成功
  CANCEL: 'CANCEL', // 已取消
  TERMINATED: 'TERMINATED', // 交易终止
})

/** com.lemon.erp.modules.ecom.fba.model.AmzReportStatus（字典 ecom_amz_report_status） */
export const AmzReportStatus = Object.freeze({
  SUBMITTED: 'SUBMITTED', // 已提交
  PROCESSING: 'PROCESSING', // 生成中
  DONE: 'DONE', // 已下载并处理
  FAILED: 'FAILED', // 失败
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.fba.model.AmzReportType（字典 ecom_amz_report_type） */
export const AmzReportType = Object.freeze({
  FBA_INVENTORY_PLANNING: 'FBA_INVENTORY_PLANNING', // 库存规划
  FBA_MYI_INVENTORY: 'FBA_MYI_INVENTORY', // FBA 库存
  LEDGER_DETAIL: 'LEDGER_DETAIL', // 库存分类账明细
  REIMBURSEMENTS: 'REIMBURSEMENTS', // 赔偿
  REMOVAL_ORDERS: 'REMOVAL_ORDERS', // 移除订单详情
  ESTIMATED_FEES: 'ESTIMATED_FEES', // 预估 FBA 费用
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

/** com.lemon.erp.modules.ecom.automation.model.AutoActionStatus（字典 ecom_auto_action_status） */
export const AutoActionStatus = Object.freeze({
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
  SKIPPED: 'SKIPPED', // 跳过
  SUGGESTED: 'SUGGESTED', // 仅建议
})

/** com.lemon.erp.modules.ecom.automation.model.AutoActionType（字典 ecom_auto_action） */
export const AutoActionType = Object.freeze({
  NOTIFY: 'NOTIFY', // 站内消息
  IM_ROBOT: 'IM_ROBOT', // 钉钉 / 企业微信 / 飞书群机器人
  WEBHOOK: 'WEBHOOK', // Webhook
  TODO: 'TODO', // 创建待办提醒
  ORDER_HOLD: 'ORDER_HOLD', // 订单挂起
  ORDER_TAG: 'ORDER_TAG', // 订单打标签
  ORDER_ASSIGN: 'ORDER_ASSIGN', // 指定发货仓 / 物流渠道
  LISTING_REPRICE: 'LISTING_REPRICE', // 在线商品调价
  PURCHASE_PLAN: 'PURCHASE_PLAN', // 生成采购计划草稿
  CS_TICKET: 'CS_TICKET', // 建售后工单
  AD_PAUSE: 'AD_PAUSE', // 暂停广告活动
  AD_ADJUST: 'AD_ADJUST', // 调整广告竞价 / 预算
})

/** com.lemon.erp.modules.ecom.automation.model.AutoAdTarget */
export const AutoAdTarget = Object.freeze({
  BID: 'BID',
  BUDGET: 'BUDGET',
})

/** com.lemon.erp.modules.ecom.automation.model.AutoCategory（字典 ecom_auto_category） */
export const AutoCategory = Object.freeze({
  ORDER: 'ORDER', // 订单
  STOCK: 'STOCK', // 库存与补货
  CS: 'CS', // 客服与评价
  LISTING: 'LISTING', // 商品监控
  STORE: 'STORE', // 店铺与渠道
  LOGISTICS: 'LOGISTICS', // 头程物流
  ADS: 'ADS', // 广告
  FBA: 'FBA', // FBA
  PRICE: 'PRICE', // 价格
})

/** com.lemon.erp.modules.ecom.automation.model.AutoFieldType */
export const AutoFieldType = Object.freeze({
  NUMBER: 'NUMBER', // 数字
  TEXT: 'TEXT', // 文本
  ENUM: 'ENUM', // 枚举
  BOOLEAN: 'BOOLEAN', // 是 / 否
})

/** com.lemon.erp.modules.ecom.automation.model.AutoLogic */
export const AutoLogic = Object.freeze({
  AND: 'AND',
  OR: 'OR',
})

/** com.lemon.erp.modules.ecom.automation.model.AutoOperator */
export const AutoOperator = Object.freeze({
  EQ: 'EQ',
  NE: 'NE',
  GT: 'GT',
  GE: 'GE',
  LT: 'LT',
  LE: 'LE',
  IN: 'IN', // 属于其中之一
  NOT_IN: 'NOT_IN',
  CONTAINS: 'CONTAINS', // 文本包含
  NOT_CONTAINS: 'NOT_CONTAINS',
  EMPTY: 'EMPTY', // 为空
  NOT_EMPTY: 'NOT_EMPTY',
})

/** com.lemon.erp.modules.ecom.automation.model.AutoRef */
export const AutoRef = Object.freeze({
  ORDER: 'ORDER', // 平台订单
  LISTING: 'LISTING', // 在线商品
  MATERIAL: 'MATERIAL', // 本地物料
  CAMPAIGN: 'CAMPAIGN', // 广告活动
  REVIEW: 'REVIEW', // 商品评价
  THREAD: 'THREAD', // 买家消息会话
  STORE: 'STORE', // 店铺
  INBOUND: 'INBOUND', // 头程货件
  CLAIM: 'CLAIM', // FBA 索赔
  MONITOR: 'MONITOR', // 商品监控
})

/** com.lemon.erp.modules.ecom.automation.model.AutoResult（字典 ecom_auto_result） */
export const AutoResult = Object.freeze({
  SUCCESS: 'SUCCESS', // 动作全部成功
  PARTIAL: 'PARTIAL', // 部分动作失败
  FAILED: 'FAILED', // 动作全部失败
  NOT_MATCHED: 'NOT_MATCHED', // 条件不满足
  DEBOUNCED: 'DEBOUNCED', // 防抖跳过
  LIMITED: 'LIMITED', // 达到每日执行上限
})

/** com.lemon.erp.modules.ecom.automation.model.AutoScanMode */
export const AutoScanMode = Object.freeze({
  REALTIME: 'REALTIME',
  FREQUENT: 'FREQUENT',
  DAILY: 'DAILY',
})

/** com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus（字典 ecom_auto_todo_status） */
export const AutoTodoStatus = Object.freeze({
  OPEN: 'OPEN', // 待处理
  DONE: 'DONE', // 已完成
})

/** com.lemon.erp.modules.ecom.automation.model.AutoTrigger（字典 ecom_auto_trigger） */
export const AutoTrigger = Object.freeze({
  ORDER_NEW: 'ORDER_NEW', // 新订单
  ORDER_EXCEPTION: 'ORDER_EXCEPTION', // 订单异常
  ORDER_LOSS: 'ORDER_LOSS', // 订单预估亏损
  STOCKOUT_RISK: 'STOCKOUT_RISK', // 断货风险
  STOCK_LOW: 'STOCK_LOW', // 本地库存低于安全库存
  REVIEW_NEGATIVE: 'REVIEW_NEGATIVE', // 新差评
  MESSAGE_TIMEOUT: 'MESSAGE_TIMEOUT', // 买家消息超时未回复
  LISTING_HIJACKED: 'LISTING_HIJACKED', // 商品被跟卖
  LISTING_RANK_DROP: 'LISTING_RANK_DROP', // 排名大跌
  LISTING_DELISTED: 'LISTING_DELISTED', // 商品被下架
  STORE_AUTH_INVALID: 'STORE_AUTH_INVALID', // 店铺授权失效
  SYNC_FAILED: 'SYNC_FAILED', // 渠道同步失败
  INBOUND_RECEIVED: 'INBOUND_RECEIVED', // 头程货件签收
  INBOUND_OVERDUE: 'INBOUND_OVERDUE', // 头程货件超期未签收
  AD_ACOS_HIGH: 'AD_ACOS_HIGH', // 广告 ACOS 超过阈值
  AD_BUDGET_EXHAUSTED: 'AD_BUDGET_EXHAUSTED', // 广告预算耗尽
  FBA_CLAIM_DEADLINE: 'FBA_CLAIM_DEADLINE', // FBA 索赔临近截止
  RETURN_RATE_HIGH: 'RETURN_RATE_HIGH', // 退货率异常
  COMPETITOR_PRICE_LOWER: 'COMPETITOR_PRICE_LOWER', // 价格被竞品低于
})

/** com.lemon.erp.modules.fin.model.AuxItem */
export const AuxItem = Object.freeze({
  DEPT: 'DEPT',
  CUSTOMER: 'CUSTOMER',
  SUPPLIER: 'SUPPLIER',
  MATERIAL: 'MATERIAL',
})

/** com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel（字典 ecom_banned_word_level） */
export const BannedWordLevel = Object.freeze({
  BLOCK: 'BLOCK', // 阻止发布
  WARN: 'WARN', // 只提醒
})

/** com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch（字典 ecom_banned_word_match） */
export const BannedWordMatch = Object.freeze({
  WORD: 'WORD', // 整词
  CONTAINS: 'CONTAINS', // 包含即命中
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
  ASSET: 'ASSET', // 固定资产
  OFFSET: 'OFFSET', // 冲账单
  ORDER_ADJUST: 'ORDER_ADJUST', // 订单尾差调整
  PURCHASE_FREIGHT: 'PURCHASE_FREIGHT', // 采购运费
  SALES_FREIGHT: 'SALES_FREIGHT', // 销售运费
  SALES_RETURN_FREIGHT: 'SALES_RETURN_FREIGHT', // 销售退货运费
  PURCHASE_RETURN_FREIGHT: 'PURCHASE_RETURN_FREIGHT', // 采购退货运费
  PLATFORM_FEE: 'PLATFORM_FEE', // 电商平台费用
})

/** com.lemon.erp.modules.fin.model.BillStatus（字典 fin_bill_status） */
export const BillStatus = Object.freeze({
  UNSETTLED: 'UNSETTLED', // 未核销
  PARTIAL: 'PARTIAL', // 部分核销
  SETTLED: 'SETTLED', // 已核销
})

/** com.lemon.erp.modules.ecom.model.BlacklistType（字典 ecom_blacklist_type） */
export const BlacklistType = Object.freeze({
  BUYER: 'BUYER', // 买家 ID
  EMAIL: 'EMAIL', // 买家邮箱
  ADDRESS: 'ADDRESS', // 收件地址
  PHONE: 'PHONE', // 收件电话
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

/** com.lemon.erp.modules.ecom.publish.model.CompetitorSource（字典 ecom_competitor_source） */
export const CompetitorSource = Object.freeze({
  API: 'API', // 平台接口
  MANUAL: 'MANUAL', // 手工录入
  IMPORT: 'IMPORT', // Excel 导入
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

/** com.lemon.erp.modules.ecom.cs.connector.CsCapability */
export const CsCapability = Object.freeze({
  MESSAGES: 'MESSAGES', // 拉取买家消息会话与消息
  SEND_MESSAGE: 'SEND_MESSAGE', // 发送回复
  REVIEWS: 'REVIEWS', // 拉取商品评价
  REVIEW_REPLY: 'REVIEW_REPLY', // 回复商品评价
  REVIEW_REQUEST: 'REVIEW_REQUEST', // 请求买家留评
})

/** com.lemon.erp.modules.ecom.cs.model.CsDirection */
export const CsDirection = Object.freeze({
  BUYER: 'BUYER', // 买家发来的
  SELLER: 'SELLER', // 卖家
})

/** com.lemon.erp.modules.ecom.cs.model.CsRecordSource */
export const CsRecordSource = Object.freeze({
  IMPORT: 'IMPORT', // Excel 导入
  SYNC: 'SYNC', // 平台接口同步
})

/** com.lemon.erp.modules.ecom.cs.model.CsRequestStatus */
export const CsRequestStatus = Object.freeze({
  SENT: 'SENT', // 已发送
  NOT_ELIGIBLE: 'NOT_ELIGIBLE', // 平台判定不符合条件
  FAILED: 'FAILED', // 调用失败
})

/** com.lemon.erp.modules.ecom.cs.model.CsReviewStatus（字典 ecom_cs_review_status） */
export const CsReviewStatus = Object.freeze({
  NONE: 'NONE', // 无需处理
  PENDING: 'PENDING', // 待处理
  PROCESSING: 'PROCESSING', // 处理中
  RESOLVED: 'RESOLVED', // 已处理
})

/** com.lemon.erp.modules.ecom.cs.model.CsSendStatus（字典 ecom_cs_send_status） */
export const CsSendStatus = Object.freeze({
  SENDING: 'SENDING', // 发送中
  SENT: 'SENT', // 已发送
  FAILED: 'FAILED', // 发送失败
})

/** com.lemon.erp.modules.ecom.cs.model.CsSourceType */
export const CsSourceType = Object.freeze({
  THREAD: 'THREAD', // 买家消息会话
  REVIEW: 'REVIEW', // 商品评价
  FEEDBACK: 'FEEDBACK', // 店铺反馈
})

/** com.lemon.erp.modules.ecom.cs.model.CsThreadStatus（字典 ecom_cs_thread_status） */
export const CsThreadStatus = Object.freeze({
  PENDING: 'PENDING', // 待回复
  REPLIED: 'REPLIED', // 已回复
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.ecom.cs.model.CsTicketType（字典 ecom_cs_ticket_type） */
export const CsTicketType = Object.freeze({
  RETURN_REFUND: 'RETURN_REFUND', // 退货退款
  REFUND_ONLY: 'REFUND_ONLY', // 仅退款
  RESEND: 'RESEND', // 补发
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

/** com.lemon.erp.modules.ecom.dev.model.DevFbaSizeTier */
export const DevFbaSizeTier = Object.freeze({
  SMALL_STANDARD: 'SMALL_STANDARD', // 小号标准尺寸
  LARGE_STANDARD: 'LARGE_STANDARD', // 大号标准尺寸
  LARGE_BULKY: 'LARGE_BULKY', // 大件
  EXTRA_LARGE: 'EXTRA_LARGE', // 超大件
})

/** com.lemon.erp.modules.ecom.dev.model.DevHeadPricing */
export const DevHeadPricing = Object.freeze({
  CHANNEL: 'CHANNEL', // 物流渠道费率
  PER_KG: 'PER_KG', // 每公斤单价 × 计费重
  PER_CBM: 'PER_CBM', // 每立方米单价 × 体积
})

/** com.lemon.erp.modules.ecom.dev.model.DevLogAction */
export const DevLogAction = Object.freeze({
  CREATE: 'CREATE', // 创建
  MOVE: 'MOVE', // 手工流转
  SUBMIT: 'SUBMIT', // 提交审批
  WITHDRAW: 'WITHDRAW', // 撤回审批
  APPROVE: 'APPROVE', // 审批通过
  REJECT: 'REJECT', // 审批驳回
  ABANDON: 'ABANDON', // 放弃
  MATERIAL: 'MATERIAL', // 生成物料
})

/** com.lemon.erp.modules.ecom.dev.model.DevProjectStatus（字典 ecom_dev_status） */
export const DevProjectStatus = Object.freeze({
  PENDING_RESEARCH: 'PENDING_RESEARCH', // 待调研
  RESEARCHING: 'RESEARCHING', // 调研中
  SAMPLING: 'SAMPLING', // 打样中
  PENDING_APPROVAL: 'PENDING_APPROVAL', // 待审批
  APPROVED: 'APPROVED', // 已通过
  REJECTED: 'REJECTED', // 已驳回
  ABANDONED: 'ABANDONED', // 已放弃
})

/** com.lemon.erp.modules.ecom.dev.model.DevSampleResult（字典 ecom_dev_sample_result） */
export const DevSampleResult = Object.freeze({
  PENDING: 'PENDING', // 待评估
  PASSED: 'PASSED', // 合格
  FAILED: 'FAILED', // 不合格
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

/** com.lemon.erp.modules.ecom.oper.model.EcomAchievementDimension */
export const EcomAchievementDimension = Object.freeze({
  STORE: 'STORE', // 按店铺
  OWNER: 'OWNER', // 按负责人
  PLATFORM: 'PLATFORM', // 按平台
})

/** com.lemon.erp.modules.ecom.model.EcomAdAction（字典 ecom_ad_rule_action） */
export const EcomAdAction = Object.freeze({
  BID_PERCENT: 'BID_PERCENT', // 竞价按百分比调整
  BID_SET: 'BID_SET', // 竞价调整到固定值
  BUDGET_PERCENT: 'BUDGET_PERCENT', // 日预算按百分比调整
  BUDGET_SET: 'BUDGET_SET', // 日预算调整到固定值
  PAUSE: 'PAUSE', // 暂停
  ENABLE: 'ENABLE', // 启用
  NOTIFY: 'NOTIFY', // 仅提醒
})

/** com.lemon.erp.modules.ecom.model.EcomAdDailyLevel */
export const EcomAdDailyLevel = Object.freeze({
  PRODUCT: 'PRODUCT', // 活动 × 推广商品
  CAMPAIGN: 'CAMPAIGN', // 只有活动级数据
})

/** com.lemon.erp.modules.ecom.model.EcomAdLevel */
export const EcomAdLevel = Object.freeze({
  CAMPAIGN: 'CAMPAIGN', // 广告活动
  AD_GROUP: 'AD_GROUP', // 广告组
  TARGET: 'TARGET', // 投放
})

/** com.lemon.erp.modules.ecom.model.EcomAdLogSource */
export const EcomAdLogSource = Object.freeze({
  RULE: 'RULE', // 广告规则
  MANUAL: 'MANUAL', // 页面上手工修改竞价 / 预算 / 状态
})

/** com.lemon.erp.modules.ecom.model.EcomAdLogStatus（字典 ecom_ad_log_status） */
export const EcomAdLogStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理的建议
  APPLIED: 'APPLIED', // 已通过广告接口执行
  MANUAL: 'MANUAL', // 已手工执行
  FAILED: 'FAILED', // 执行失败
  NOTIFIED: 'NOTIFIED', // 仅提醒
  IGNORED: 'IGNORED', // 已忽略
})

/** com.lemon.erp.modules.ecom.model.EcomAdMatchType（字典 ecom_ad_match_type） */
export const EcomAdMatchType = Object.freeze({
  EXACT: 'EXACT', // 精准
  PHRASE: 'PHRASE', // 词组
  BROAD: 'BROAD', // 广泛
  NEGATIVE_EXACT: 'NEGATIVE_EXACT', // 否定精准
  NEGATIVE_PHRASE: 'NEGATIVE_PHRASE', // 否定词组
})

/** com.lemon.erp.modules.ecom.model.EcomAdMetric */
export const EcomAdMetric = Object.freeze({
  SPEND: 'SPEND', // 花费
  SALES: 'SALES', // 广告销售额
  CLICKS: 'CLICKS', // 点击
  IMPRESSIONS: 'IMPRESSIONS', // 曝光
  ORDERS: 'ORDERS', // 广告订单
  ACOS: 'ACOS', // ACOS
  ROAS: 'ROAS', // ROAS
  CTR: 'CTR', // 点击率
  CPC: 'CPC', // 单次点击成本
  CVR: 'CVR', // 转化率
})

/** com.lemon.erp.modules.ecom.model.EcomAdOperator */
export const EcomAdOperator = Object.freeze({
  GT: 'GT', // 大于
  GTE: 'GTE', // 大于等于
  LT: 'LT', // 小于
  LTE: 'LTE', // 小于等于
  EQ: 'EQ', // 等于
})

/** com.lemon.erp.modules.ecom.model.EcomAdReportType */
export const EcomAdReportType = Object.freeze({
  CAMPAIGN: 'CAMPAIGN', // 广告活动报告
  PRODUCT: 'PRODUCT', // 推广商品报告
  TARGETING: 'TARGETING', // 投放报告
  SEARCH_TERM: 'SEARCH_TERM', // 搜索词报告
})

/** com.lemon.erp.modules.ecom.model.EcomAdRunMode */
export const EcomAdRunMode = Object.freeze({
  MANUAL: 'MANUAL', // 手动
  DAILY: 'DAILY', // 每天在设定的整点执行一次
})

/** com.lemon.erp.modules.ecom.model.EcomAdStatus（字典 ecom_ad_status） */
export const EcomAdStatus = Object.freeze({
  ENABLED: 'ENABLED', // 启用
  PAUSED: 'PAUSED', // 暂停
  ARCHIVED: 'ARCHIVED', // 已归档
})

/** com.lemon.erp.modules.ecom.model.EcomAdTargetType */
export const EcomAdTargetType = Object.freeze({
  KEYWORD: 'KEYWORD', // 关键词投放
  PRODUCT: 'PRODUCT', // 商品 / 类目投放
  AUTO: 'AUTO', // 自动投放
  NEGATIVE: 'NEGATIVE', // 否定关键词
})

/** com.lemon.erp.modules.ecom.model.EcomAdType（字典 ecom_ad_type） */
export const EcomAdType = Object.freeze({
  SP: 'SP', // 商品推广
  SB: 'SB', // 品牌推广
  SD: 'SD', // 展示型推广
  GENERIC: 'GENERIC', // 平台通用
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

/** com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus（字典 ecom_monitor_alert_status） */
export const EcomMonitorAlertStatus = Object.freeze({
  NEW: 'NEW', // 未处理
  HANDLED: 'HANDLED', // 已处理
  IGNORED: 'IGNORED', // 已忽略
})

/** com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertType（字典 ecom_monitor_alert_type） */
export const EcomMonitorAlertType = Object.freeze({
  RANK_DROP: 'RANK_DROP', // 大类排名大跌
  RATING_DROP: 'RATING_DROP', // 评分下降
  NEW_SELLER: 'NEW_SELLER', // 新增跟卖
  TITLE_CHANGED: 'TITLE_CHANGED', // 标题被改
  IMAGE_CHANGED: 'IMAGE_CHANGED', // 主图被改
  BULLETS_CHANGED: 'BULLETS_CHANGED', // 五点描述被改
  DELISTED: 'DELISTED', // 被下架
})

/** com.lemon.erp.modules.ecom.oper.model.EcomMonitorSource（字典 ecom_monitor_source） */
export const EcomMonitorSource = Object.freeze({
  API: 'API', // 平台接口
  MANUAL: 'MANUAL', // 手工录入
  IMPORT: 'IMPORT', // Excel 导入
})

/** com.lemon.erp.modules.ecom.oper.model.EcomMonitorType（字典 ecom_monitor_type） */
export const EcomMonitorType = Object.freeze({
  OWN: 'OWN', // 自己的商品
  COMPETITOR: 'COMPETITOR', // 竞品
})

/** com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType（字典 ecom_ops_target_type） */
export const EcomOpsTargetType = Object.freeze({
  STORE: 'STORE', // 店铺
  USER: 'USER', // 运营人员
})

/** com.lemon.erp.modules.ecom.model.EcomOrderLogAction */
export const EcomOrderLogAction = Object.freeze({
  CREATE: 'CREATE', // 订单写入系统
  PLATFORM_STATUS: 'PLATFORM_STATUS', // 平台状态变化
  RULE: 'RULE', // 命中审单规则
  TAG: 'TAG', // 打标签
  UNTAG: 'UNTAG', // 去标签
  AUDIT: 'AUDIT', // 审核通过
  EXCEPTION: 'EXCEPTION', // 异常
  OUT_OF_STOCK: 'OUT_OF_STOCK', // 缺货
  RECHECK: 'RECHECK', // 重新检查库存
  REMATCH: 'REMATCH', // 重新匹配 SKU
  HOLD: 'HOLD', // 挂起
  UNHOLD: 'UNHOLD', // 取消挂起
  EDIT: 'EDIT', // 修改订单信息 / 收件地址
  ASSIGN: 'ASSIGN', // 指定发货仓 / 物流渠道
  PACKAGE: 'PACKAGE', // 包裹变化
  LABEL: 'LABEL', // 申请运单号 / 填写运单号
  PRINT: 'PRINT', // 打印面单
  OUTBOUND: 'OUTBOUND', // 出库
  SHIP: 'SHIP', // 发货
  SHIP_CONFIRM: 'SHIP_CONFIRM', // 回传平台发货结果
  CANCEL: 'CANCEL', // 取消订单
  RETURN: 'RETURN', // 退货退款
  REFUND: 'REFUND', // 退款
  RESEND: 'RESEND', // 补发
  NOTE: 'NOTE', // 内部备注
})

/** com.lemon.erp.modules.ecom.model.EcomOrderStatus（字典 ecom_order_status） */
export const EcomOrderStatus = Object.freeze({
  PENDING: 'PENDING', // 待付款
  UNSHIPPED: 'UNSHIPPED', // 待发货
  SHIPPED: 'SHIPPED', // 已发货
  CANCELLED: 'CANCELLED', // 已取消
  REFUNDED: 'REFUNDED', // 已退款
})

/** com.lemon.erp.modules.ecom.model.EcomOrderType（字典 ecom_order_type） */
export const EcomOrderType = Object.freeze({
  PLATFORM: 'PLATFORM', // 平台订单
  MANUAL: 'MANUAL', // 手工订单
  RESEND: 'RESEND', // 补发 / 重发单
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

/** com.lemon.erp.modules.ecom.oper.model.EcomPromoPhase（字典 ecom_promo_phase） */
export const EcomPromoPhase = Object.freeze({
  NOT_STARTED: 'NOT_STARTED', // 未开始
  ONGOING: 'ONGOING', // 进行中
  ENDED: 'ENDED', // 已结束
})

/** com.lemon.erp.modules.ecom.oper.model.EcomPromoPushStatus（字典 ecom_promo_push_status） */
export const EcomPromoPushStatus = Object.freeze({
  NONE: 'NONE', // 未推送
  PUSHED: 'PUSHED', // 已推送
  FAILED: 'FAILED', // 推送失败
})

/** com.lemon.erp.modules.ecom.oper.model.EcomPromoStatus（字典 ecom_promo_status） */
export const EcomPromoStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  ACTIVE: 'ACTIVE', // 已生效
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.model.EcomReturnStatus（字典 ecom_return_status） */
export const EcomReturnStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  RECEIVED: 'RECEIVED', // 已入库
  REFUNDED: 'REFUNDED', // 已退款
  CLOSED: 'CLOSED', // 已关闭
})

/** com.lemon.erp.modules.ecom.model.EcomTrackAlert（字典 ecom_track_alert） */
export const EcomTrackAlert = Object.freeze({
  NO_SCAN: 'NO_SCAN', // 超时未上网
  TRANSIT_TIMEOUT: 'TRANSIT_TIMEOUT', // 运输超时
  EXCEPTION: 'EXCEPTION', // 轨迹异常
})

/** com.lemon.erp.modules.ecom.model.EcomTrackStatus（字典 ecom_track_status） */
export const EcomTrackStatus = Object.freeze({
  NOT_FOUND: 'NOT_FOUND', // 暂无轨迹
  PICKED_UP: 'PICKED_UP', // 已揽收
  IN_TRANSIT: 'IN_TRANSIT', // 运输中
  PICKUP: 'PICKUP', // 到达待取
  DELIVERED: 'DELIVERED', // 已签收
  EXCEPTION: 'EXCEPTION', // 异常
  RETURNED: 'RETURNED', // 退回
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

/** com.lemon.erp.modules.ecom.fba.model.FbaAgeSource */
export const FbaAgeSource = Object.freeze({
  REPORT: 'REPORT', // 亚马逊库存规划报告
  FIFO: 'FIFO', // 按头程货件签收先进先出推算
})

/** com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus（字典 ecom_fba_claim_status） */
export const FbaClaimStatus = Object.freeze({
  PENDING: 'PENDING', // 待提交
  SUBMITTED: 'SUBMITTED', // 已提交
  PAID: 'PAID', // 已赔付
  REJECTED: 'REJECTED', // 已驳回
  IGNORED: 'IGNORED', // 已忽略
})

/** com.lemon.erp.modules.ecom.fba.model.FbaClaimType（字典 ecom_fba_claim_type） */
export const FbaClaimType = Object.freeze({
  LOST: 'LOST', // 仓库丢失
  DAMAGED: 'DAMAGED', // 仓库损坏
  INBOUND_SHORTAGE: 'INBOUND_SHORTAGE', // 入库短少
  RETURN_NOT_RECEIVED: 'RETURN_NOT_RECEIVED', // 退款后超过 45 天未退回库存
  FEE_OVERCHARGE: 'FEE_OVERCHARGE', // 多收配送费
  OTHER: 'OTHER', // 其他
})

/** com.lemon.erp.modules.ecom.fba.model.FbaDataSource */
export const FbaDataSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工维护
  REPORT: 'REPORT', // 亚马逊报告
  API: 'API', // 亚马逊接口
})

/** com.lemon.erp.modules.ecom.fba.model.FbaDisposition（字典 ecom_fba_disposition） */
export const FbaDisposition = Object.freeze({
  SELLABLE: 'SELLABLE', // 可售
  UNSELLABLE: 'UNSELLABLE', // 不可售
})

/** com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice（字典 ecom_fba_advice） */
export const FbaHealthAdvice = Object.freeze({
  HEALTHY: 'HEALTHY', // 健康
  PROMOTE: 'PROMOTE', // 冗余
  PRICE_DOWN: 'PRICE_DOWN', // 冗余且已超龄
  REMOVE: 'REMOVE', // 长期滞销或超过一年
})

/** com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus（字典 ecom_fba_removal_status） */
export const FbaRemovalStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  SUBMITTED: 'SUBMITTED', // 已提交
  PROCESSING: 'PROCESSING', // 处理中
  COMPLETED: 'COMPLETED', // 亚马逊已完成
  RECEIVED: 'RECEIVED', // 本地已收货
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.fba.model.FbaRemovalType（字典 ecom_fba_removal_type） */
export const FbaRemovalType = Object.freeze({
  RETURN: 'RETURN', // 退回本地仓
  DISPOSAL: 'DISPOSAL', // 弃置
})

/** com.lemon.erp.modules.ecom.fba.model.FbaSizeCategory（字典 ecom_fba_size_category） */
export const FbaSizeCategory = Object.freeze({
  STANDARD: 'STANDARD', // 标准件
  OVERSIZE: 'OVERSIZE', // 大件
})

/** com.lemon.erp.modules.ecom.fba.model.FbaStorageFeeType（字典 ecom_fba_storage_fee_type） */
export const FbaStorageFeeType = Object.freeze({
  MONTHLY: 'MONTHLY', // 月度仓储费
  AGED: 'AGED', // 超龄库存附加费
})

/** com.lemon.erp.modules.ecom.fba.model.FbaUnitSystem */
export const FbaUnitSystem = Object.freeze({
  IMPERIAL: 'IMPERIAL', // 英寸 / 磅
  METRIC: 'METRIC', // 厘米 / 千克
})

/** com.lemon.erp.modules.ecom.fba.model.FbaVolumeUnit */
export const FbaVolumeUnit = Object.freeze({
  CUFT: 'CUFT', // 立方英尺
  CBM: 'CBM', // 立方米
})

/** com.lemon.erp.modules.ecom.model.FeeAllocMethod（字典 ecom_fee_alloc_method） */
export const FeeAllocMethod = Object.freeze({
  SKU: 'SKU', // 结算报告带 SKU
  QTY: 'QTY', // 按结算周期内各 SKU 的销量
  SALES: 'SALES', // 按结算周期内各 SKU 的本位币销售额
  STORE: 'STORE', // 结算周期内店铺没有销量
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

/** com.lemon.erp.modules.ecom.analytics.model.ForecastMethod（字典 ecom_forecast_method） */
export const ForecastMethod = Object.freeze({
  MA: 'MA', // 移动平均
  WMA: 'WMA', // 加权移动平均
  HOLT: 'HOLT', // 指数平滑
  SEASONAL: 'SEASONAL', // 星期季节性
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

/** com.lemon.erp.framework.message.ImChannel */
export const ImChannel = Object.freeze({
  DINGTALK: 'DINGTALK', // 钉钉
  WECOM: 'WECOM', // 企业微信
  FEISHU: 'FEISHU', // 飞书
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

/** com.lemon.erp.modules.ecom.assistant.model.OpsAbnormalType（字典 ecom_ops_abnormal_type） */
export const OpsAbnormalType = Object.freeze({
  SALES_DROP: 'SALES_DROP', // 销量骤降
  PROFIT_DROP: 'PROFIT_DROP', // 利润下滑
  RETURN_RATE_UP: 'RETURN_RATE_UP', // 退货率升高
})

/** com.lemon.erp.modules.ecom.assistant.model.OpsAdAlertType（字典 ecom_ops_ad_alert_type） */
export const OpsAdAlertType = Object.freeze({
  OVER_BUDGET: 'OVER_BUDGET', // 当天花费达到或超过日预算
  ACOS_HIGH: 'ACOS_HIGH', // ACOS 明显高于近 7 天
  NO_ORDER: 'NO_ORDER', // 花费较多但没有订单
})

/** com.lemon.erp.modules.ecom.assistant.model.OpsPushChannel（字典 ecom_ops_push_channel） */
export const OpsPushChannel = Object.freeze({
  SITE: 'SITE',
  DINGTALK: 'DINGTALK',
  WECOM: 'WECOM',
  FEISHU: 'FEISHU',
})

/** com.lemon.erp.modules.ecom.assistant.model.OpsReportAiStatus（字典 ecom_ops_report_ai_status） */
export const OpsReportAiStatus = Object.freeze({
  NONE: 'NONE', // 未生成
  SUCCESS: 'SUCCESS', // 已生成
  FAILED: 'FAILED', // 调用模型失败
})

/** com.lemon.erp.modules.ecom.assistant.model.OpsReportSource（字典 ecom_ops_report_source） */
export const OpsReportSource = Object.freeze({
  SCHEDULE: 'SCHEDULE', // 定时任务生成
  MANUAL: 'MANUAL', // 手动「立即生成」
})

/** com.lemon.erp.modules.ecom.assistant.model.OpsTodoKind */
export const OpsTodoKind = Object.freeze({
  ORDER_OVERDUE: 'ORDER_OVERDUE',
  ORDER_EXCEPTION: 'ORDER_EXCEPTION',
  ORDER_PENDING: 'ORDER_PENDING',
  RETURN_PENDING: 'RETURN_PENDING',
  REVIEW_PENDING: 'REVIEW_PENDING',
  CLAIM_EXPIRING: 'CLAIM_EXPIRING',
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
  CONSIGN: 'CONSIGN', // 寄售结算
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

/** com.lemon.erp.modules.ecom.owh.model.OwhCapability */
export const OwhCapability = Object.freeze({
  WAREHOUSES: 'WAREHOUSES', // 仓库列表
  PRODUCTS: 'PRODUCTS', // 物流产品列表
  INVENTORY: 'INVENTORY', // 库存查询
  INBOUND: 'INBOUND', // 入库单创建 / 查询
  OUTBOUND: 'OUTBOUND', // 出库单创建 / 查询
  CANCEL: 'CANCEL', // 出库单取消 / 截单
  FEES: 'FEES', // 费用查询
  RETURN: 'RETURN', // 退件单创建 / 查询
  TRANSFER: 'TRANSFER', // 调拨出库
  CANCEL_INBOUND: 'CANCEL_INBOUND', // 已推送的入库单可接口取消
  CANCEL_RETURN: 'CANCEL_RETURN', // 已推送的退件单可接口取消
})

/** com.lemon.erp.modules.ecom.owh.model.OwhDiffSource（字典 ecom_owh_diff_source） */
export const OwhDiffSource = Object.freeze({
  INBOUND: 'INBOUND', // 海外仓入库单
  RETURN: 'RETURN', // 海外仓退件单
  TRANSFER: 'TRANSFER', // 海外仓调拨单
})

/** com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus（字典 ecom_owh_diff_status） */
export const OwhDiffStatus = Object.freeze({
  PENDING: 'PENDING', // 待处理
  LOSS: 'LOSS', // 确认损耗
  FOUND: 'FOUND', // 找回补收
  ACCEPTED: 'ACCEPTED', // 确认溢收
})

/** com.lemon.erp.modules.ecom.owh.model.OwhFeeStatus（字典 ecom_owh_fee_status） */
export const OwhFeeStatus = Object.freeze({
  PENDING: 'PENDING', // 待入账
  POSTED: 'POSTED', // 已入账
  UNALLOCATED: 'UNALLOCATED', // 无法归属
})

/** com.lemon.erp.modules.ecom.owh.model.OwhFeeType（字典 ecom_owh_fee_type） */
export const OwhFeeType = Object.freeze({
  STORAGE: 'STORAGE', // 仓租
  OPERATION: 'OPERATION', // 操作费
  FREIGHT: 'FREIGHT', // 尾程运费
  INBOUND: 'INBOUND', // 入库费
  OTHER: 'OTHER', // 其他
})

/** com.lemon.erp.modules.ecom.owh.model.OwhInboundSource（字典 ecom_owh_inbound_source） */
export const OwhInboundSource = Object.freeze({
  INBOUND: 'INBOUND', // 头程货件
  TRANSFER: 'TRANSFER', // 在途调拨单
})

/** com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus（字典 ecom_owh_inbound_status） */
export const OwhInboundStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  PUSHED: 'PUSHED', // 已推送
  RECEIVING: 'RECEIVING', // 收货中
  RECEIVED: 'RECEIVED', // 已收货
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus（字典 ecom_owh_outbound_status） */
export const OwhOutboundStatus = Object.freeze({
  FAILED: 'FAILED', // 推送失败
  PUSHED: 'PUSHED', // 已推送
  INTERCEPTING: 'INTERCEPTING', // 截单中
  OUTBOUND: 'OUTBOUND', // 已出库
  SHIPPED: 'SHIPPED', // 已发货
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.owh.model.OwhProvider（字典 ecom_owh_provider） */
export const OwhProvider = Object.freeze({
  GOODCANG: 'GOODCANG', // 谷仓海外仓
  WINIT: 'WINIT', // 万邑通
  FOURPX_FB4: 'FOURPX_FB4', // 递四方海外仓
})

/** com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus（字典 ecom_owh_return_status） */
export const OwhReturnStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  PUSHED: 'PUSHED', // 已推送
  RECEIVED: 'RECEIVED', // 已收货
  CANCELLED: 'CANCELLED', // 已取消
})

/** com.lemon.erp.modules.ecom.owh.model.OwhTransferStatus（字典 ecom_owh_transfer_status） */
export const OwhTransferStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  OUT_PUSHED: 'OUT_PUSHED', // 调出仓处理中
  IN_TRANSIT: 'IN_TRANSIT', // 在途
  RECEIVED: 'RECEIVED', // 已收货
  CANCELLED: 'CANCELLED', // 已取消
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

/** com.lemon.erp.modules.ecom.model.PayoutSource */
export const PayoutSource = Object.freeze({
  SETTLEMENT: 'SETTLEMENT', // 结算单导入时生成的应到账记录
  IMPORT: 'IMPORT', // 导入的银行 / 收款平台流水
  MANUAL: 'MANUAL', // 手工登记到账
})

/** com.lemon.erp.modules.ecom.model.PayoutStatus（字典 ecom_payout_status） */
export const PayoutStatus = Object.freeze({
  PENDING: 'PENDING', // 未到账
  RECEIVED: 'RECEIVED', // 已到账
  DIFF: 'DIFF', // 有差异
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

/** com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode（字典 ecom_price_adjust_mode） */
export const PriceAdjustMode = Object.freeze({
  FIXED: 'FIXED', // 改为固定值
  PERCENT: 'PERCENT', // 按百分比加减
  AMOUNT: 'AMOUNT', // 按金额加减
  FORMULA: 'FORMULA', // 按价格模板的定价公式计算
})

/** com.lemon.erp.modules.ecom.publish.model.PriceChangeSource（字典 ecom_price_change_source） */
export const PriceChangeSource = Object.freeze({
  MANUAL: 'MANUAL', // 手工修改单个
  BATCH: 'BATCH', // 批量修改
  REPRICER: 'REPRICER', // 自动调价
  PUBLISH: 'PUBLISH', // 刊登
  AUTOMATION: 'AUTOMATION', // 自动化规则
})

/** com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus（字典 ecom_price_change_status） */
export const PriceChangeStatus = Object.freeze({
  SUCCESS: 'SUCCESS', // 已推送到平台
  FAILED: 'FAILED', // 推送失败
  LOCAL: 'LOCAL', // 只修改了本地
})

/** com.lemon.erp.modules.ecom.publish.model.PriceChangeType（字典 ecom_price_change_type） */
export const PriceChangeType = Object.freeze({
  PRICE: 'PRICE', // 价格
  STOCK: 'STOCK', // 库存
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

/** com.lemon.erp.modules.ecom.model.ProfitBasis */
export const ProfitBasis = Object.freeze({
  ORDER: 'ORDER', // 订单口径
  SETTLEMENT: 'SETTLEMENT', // 结算口径
})

/** com.lemon.erp.modules.ecom.model.ProfitDimension */
export const ProfitDimension = Object.freeze({
  STORE: 'STORE',
  PLATFORM: 'PLATFORM',
  MARKETPLACE: 'MARKETPLACE',
  SKU: 'SKU',
  SPU: 'SPU',
  MATERIAL: 'MATERIAL',
  OWNER: 'OWNER',
  COUNTRY: 'COUNTRY',
  MONTH: 'MONTH',
})

/** com.lemon.erp.modules.scm.model.PromoType（字典 scm_promo_type） */
export const PromoType = Object.freeze({
  DISCOUNT: 'DISCOUNT',
  REDUCE: 'REDUCE',
  GIFT: 'GIFT',
})

/** com.lemon.erp.modules.ecom.publish.model.PublishAction（字典 ecom_publish_action） */
export const PublishAction = Object.freeze({
  SUBMIT: 'SUBMIT', // 提交发布 / 重试
  PUBLISH: 'PUBLISH', // 调用平台发布
  CHECK: 'CHECK', // 查询平台异步处理结果
  OFFLINE: 'OFFLINE', // 下架
})

/** com.lemon.erp.modules.ecom.publish.model.PublishBarcodeType */
export const PublishBarcodeType = Object.freeze({
  UPC: 'UPC', // UPC
  EAN: 'EAN', // EAN
  GTIN: 'GTIN', // GTIN-14
  JAN: 'JAN', // JAN
  ISBN: 'ISBN', // ISBN
})

/** com.lemon.erp.modules.ecom.publish.connector.PublishCapability */
export const PublishCapability = Object.freeze({
  CATEGORIES: 'CATEGORIES', // 查询平台类目
  ATTRIBUTES: 'ATTRIBUTES', // 查询类目的属性
  PUBLISH: 'PUBLISH', // 发布商品
  OFFLINE: 'OFFLINE', // 下架
  PRICE: 'PRICE', // 推送价格
  COMPETITIVE_PRICE: 'COMPETITIVE_PRICE', // 拉取竞品价格
})

/** com.lemon.erp.modules.ecom.publish.model.PublishStatus（字典 ecom_publish_status） */
export const PublishStatus = Object.freeze({
  DRAFT: 'DRAFT', // 草稿
  PENDING: 'PENDING', // 待发布
  PUBLISHING: 'PUBLISHING', // 发布中
  SUCCESS: 'SUCCESS', // 成功
  FAILED: 'FAILED', // 失败
  OFFLINE: 'OFFLINE', // 已下架
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

/** com.lemon.erp.modules.ecom.publish.model.RepriceFloorMode（字典 ecom_reprice_floor_mode） */
export const RepriceFloorMode = Object.freeze({
  FIXED: 'FIXED', // 固定最低价
  COST_MARGIN: 'COST_MARGIN', // 按成本 + 最低利润率推算
})

/** com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus（字典 ecom_reprice_log_status） */
export const RepriceLogStatus = Object.freeze({
  SUGGESTED: 'SUGGESTED', // 仅建议
  PUSHED: 'PUSHED', // 已推送到平台
  FAILED: 'FAILED', // 推送失败
  LOCAL: 'LOCAL', // 平台不支持改价
  SKIPPED: 'SKIPPED', // 跳过
  IGNORED: 'IGNORED', // 建议已忽略
})

/** com.lemon.erp.modules.ecom.publish.model.RepriceStrategy（字典 ecom_reprice_strategy） */
export const RepriceStrategy = Object.freeze({
  MATCH_LOWEST: 'MATCH_LOWEST', // 跟随竞品最低价
  BEAT_LOWEST: 'BEAT_LOWEST', // 低于竞品最低价 X
  MATCH_BUYBOX: 'MATCH_BUYBOX', // 跟随购物车价
  TARGET_MARGIN: 'TARGET_MARGIN', // 按价格模板与目标利润率计算
  BY_STOCK: 'BY_STOCK', // 按库存 / 销量
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

/** com.lemon.erp.modules.ecom.analytics.model.SalesDimension */
export const SalesDimension = Object.freeze({
  SKU: 'SKU', // 店铺 × 平台 SKU
  SPU: 'SPU', // 店铺 × 父商品
  MATERIAL: 'MATERIAL', // 本地物料
  STORE: 'STORE', // 店铺
  PLATFORM: 'PLATFORM', // 平台
  COUNTRY: 'COUNTRY', // 收件国家 / 地区
})

/** com.lemon.erp.modules.ecom.analytics.model.SalesGrain */
export const SalesGrain = Object.freeze({
  DAY: 'DAY', // 按日
  WEEK: 'WEEK', // 按周
  MONTH: 'MONTH', // 按月
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
  STORAGE: 'STORAGE', // 月度仓储费
  LONG_STORAGE: 'LONG_STORAGE', // 长期仓储费
  REMOVAL: 'REMOVAL', // 移除 / 弃置费
  RETURN_FEE: 'RETURN_FEE', // 退货处理费
  INBOUND_FEE: 'INBOUND_FEE', // 入库配置费
  SUBSCRIPTION: 'SUBSCRIPTION', // 订阅费
  PROMOTION: 'PROMOTION', // 促销费
  COUPON: 'COUPON', // 优惠券
  DEAL: 'DEAL', // 秒杀
  AD: 'AD', // 广告费
  REIMBURSEMENT: 'REIMBURSEMENT', // 赔偿
  RESERVE: 'RESERVE', // 预留 / 释放
  MARKETPLACE_TAX: 'MARKETPLACE_TAX', // 平台代收代缴税
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
  SALES_RETURN: 'SALES_RETURN', // 销售退货
  OTHER: 'OTHER', // 其他入库
  CONSIGN_IN: 'CONSIGN_IN', // 寄售入库
  OPENING: 'OPENING', // 期初入库
  ASSEMBLY: 'ASSEMBLY', // 组装拆卸
})

/** com.lemon.erp.modules.wms.model.StockOutType（字典 wms_out_type） */
export const StockOutType = Object.freeze({
  SALES: 'SALES', // 销售出库
  PURCHASE_RETURN: 'PURCHASE_RETURN', // 采购退货
  OTHER: 'OTHER', // 其他出库
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
  MESSAGES: 'MESSAGES', // 买家消息
  REVIEWS: 'REVIEWS', // 评价
  REVIEW_REQUEST: 'REVIEW_REQUEST', // 请求评价
})

/** com.lemon.erp.modules.ecom.model.TaxType（字典 ecom_tax_type） */
export const TaxType = Object.freeze({
  VAT: 'VAT', // 增值税
  GST: 'GST', // 商品及服务税
  SALES_TAX: 'SALES_TAX', // 销售税
})

/** com.lemon.erp.modules.ecom.model.TrackSource */
export const TrackSource = Object.freeze({
  CARRIER: 'CARRIER', // 物流商接口
  TRACK17: 'TRACK17', // 17TRACK 通用查询
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
  EXPENSE: 'EXPENSE',
  ADVANCE: 'ADVANCE', // 预收预付及冲抵
  BAD_DEBT: 'BAD_DEBT', // 坏账核销
  REVERSAL: 'REVERSAL', // 红字冲销
  TAX: 'TAX', // 增值税结转、附加税计提
  CLAIM: 'CLAIM', // 费用报销
  LOAN: 'LOAN', // 员工借款、还款
  FX: 'FX', // 期末调汇
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
