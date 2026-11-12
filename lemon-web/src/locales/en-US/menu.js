// 菜单名称英文翻译：键为 sys_menu.id（与 sql/init.sql 一致），中文直接使用数据库中的名称
export default {
  // 导航重组：一级模块与新分组
  19: 'Products', // 商品管理
  20: 'Orders', // 订单管理
  23: 'Operations', // 运营管理
  22: 'Logistics', // 物流管理
  990: 'Platform Settlement', // 平台结算
  991: 'Publishing & Pricing', // 刊登定价
  992: 'Stores', // 店铺管理
  993: 'After-sales & Service', // 售后客服
  994: 'Order Settings', // 订单设置
  995: '1688 Sourcing', // 1688 采购
  996: 'Data Analytics', // 数据分析
  997: 'FBA', // FBA
  998: 'Advertising', // 广告管理
  490: 'Sales Statistics', // 销量统计
  491: 'Sales Forecast', // 销量预测
  492: 'Product Analysis', // 商品分析
  4900: 'Export Sales Statistics', // 销量统计导出
  4910: 'Generate Forecasts', // 生成预测
  4911: 'Forecast Settings & Methods', // 预测参数与方法
  4912: 'Maintain Promotions / Holidays', // 促销节假日维护
  4920: 'Export Product Analysis', // 商品分析导出
  470: 'Fee Mapping', // 费用映射
  471: 'Tax', // 税务
  472: 'Payout Reconciliation', // 回款对账
  4700: 'Add Fee Mapping', // 费用映射新增
  4701: 'Edit Fee Mapping', // 费用映射修改
  4702: 'Delete Fee Mapping', // 费用映射删除
  4710: 'Add Tax Rate', // 税率新增
  4711: 'Edit Tax Rate', // 税率修改
  4712: 'Delete Tax Rate', // 税率删除
  4713: 'Recalculate Order Tax', // 重算订单税额
  4720: 'Import Payout Transactions', // 导入到账流水
  4721: 'Record / Match Payout', // 登记到账 / 匹配
  4722: 'Undo / Delete Payout', // 撤销 / 删除到账
  4730: 'Reallocate Fees', // 重新分摊费用
  480: 'Ad Rules', // 广告规则
  4800: 'Maintain Ad Structure', // 广告结构维护
  4801: 'Ads Authorization', // 广告授权
  4802: 'Sync Ads', // 广告同步
  4803: 'Maintain Dayparting', // 分时策略维护
  4810: 'Add Rule', // 规则新增
  4811: 'Edit Rule', // 规则修改
  4812: 'Delete Rule', // 规则删除
  4813: 'Run Rules / Handle Suggestions', // 规则执行 / 处理建议
  485: 'Buyer Messages', // 买家消息
  486: 'Reviews', // 评价管理
  487: 'Reply Templates', // 回复模板
  488: 'After-Sales Tickets', // 售后工单
  4850: 'Reply to Messages', // 回复消息
  4851: 'Handle Conversations (Owner / Tags / Close / Link Order)', // 会话处理（负责人 / 标签 / 关闭 / 关联订单）
  4852: 'Sync Messages', // 同步消息
  4860: 'Import Reviews', // 导入评价
  4861: 'Reply to Reviews', // 回复评价
  4862: 'Handle Reviews', // 处理评价
  4863: 'Sync Reviews', // 同步评价
  4864: 'Request Reviews', // 请求评价
  4865: 'Delete Reviews', // 删除评价
  4870: 'Add Template', // 模板新增
  4871: 'Edit Template', // 模板修改
  4872: 'Delete Template', // 模板删除
  4880: 'Create After-Sales', // 转售后
  // 商品体系（店铺与商品 981 下的 SKU 配对，库存与物流 983 下的发货计划，头程货件 425 的装箱按钮）
  450: 'SKU Pairing', // SKU 配对
  451: 'Shipment Plans', // 发货计划
  4500: 'Confirm / Unpair SKUs', // 确认 / 解除配对
  4501: 'Import Pairings', // 导入配对
  4510: 'Add Plan', // 计划新增
  4511: 'Edit Plan', // 计划修改
  4512: 'Delete Plan', // 计划删除
  4513: 'Confirm / Cancel Plan', // 计划确认 / 取消
  4514: 'Generate Shipments', // 生成货件
  4520: 'Pack Boxes', // 货件装箱
  // 补货建议 → 采购计划（库存与物流 983 下的页面，补货建议 427 的按钮）
  460: 'Purchase Plans', // 采购计划
  4600: 'Add Purchase Plan', // 采购计划新增
  4601: 'Edit Purchase Plan', // 采购计划修改
  4602: 'Delete Purchase Plan', // 采购计划删除
  4603: 'Approve Purchase Plan', // 采购计划审核
  4604: 'Generate Purchase Orders', // 生成采购订单
  4605: 'Replenishment Settings', // 补货设置
  440: 'Channels', // 渠道接入
  4400: 'Authorize Store', // 店铺授权
  4401: 'Sync Listings', // 同步商品
  4402: 'Push Inventory', // 回传库存
  // 订单作业（订单与售后 982 下的页面，平台订单 423 的按钮）
  432: 'Order Rules', // 订单规则
  433: 'Packages', // 包裹打单
  434: 'Scan to Ship', // 扫描发货
  4320: 'Add Rule', // 规则新增
  4321: 'Edit Rule', // 规则修改
  4322: 'Delete Rule', // 规则删除
  4323: 'Review Orders', // 订单审核
  4324: 'Merge / Split Orders', // 合并拆分
  4330: 'Get Tracking Numbers', // 申请运单
  4331: 'Ship Packages', // 包裹发货
  981: 'Product Catalog', // 商品资料
  982: 'Order Processing', // 订单处理
  983: 'First-Mile Logistics', // 头程物流
  420: 'E-commerce Dashboard', // 电商看板
  421: 'Stores', // 平台店铺
  422: 'Listings', // 在线商品
  423: 'Platform Orders', // 平台订单
  424: 'Returns & Refunds', // 退货退款
  425: 'Inbound Shipments', // 头程货件
  426: 'Platform Inventory', // 平台仓库存
  427: 'Replenishment', // 补货建议
  428: 'Logistics Channels', // 物流渠道
  429: 'Platform Settlements', // 平台结算
  430: 'Advertising Data', // 广告数据
  431: 'Profit Analysis', // 利润分析
  4210: 'Add Store', // 店铺新增
  4211: 'Edit Store', // 店铺修改
  4212: 'Delete Store', // 店铺删除
  4213: 'Sync Orders', // 同步订单
  4220: 'Add Listing', // 在线商品新增
  4221: 'Edit Listing', // 在线商品修改
  4222: 'Delete Listing', // 在线商品删除
  4223: 'Import Listings', // 在线商品导入
  4230: 'Import Orders', // 订单导入
  4231: 'Edit Order', // 订单修改
  4232: 'Ship Out Orders', // 订单出库
  4233: 'Ship Orders', // 订单发货
  4234: 'Cancel Order', // 订单取消
  4240: 'Add Return', // 退货退款新增
  4241: 'Receive Return', // 退货入库
  4242: 'Record Refund', // 登记退款
  4243: 'Delete Return', // 退货退款删除
  4250: 'Add Shipment', // 货件新增
  4251: 'Edit Shipment', // 货件修改
  4252: 'Delete Shipment', // 货件删除
  4253: 'Dispatch Shipment', // 货件发运
  4254: 'Receive Shipment', // 货件签收
  4260: 'Import Platform Inventory', // 平台库存导入
  4280: 'Add Channel', // 渠道新增
  4281: 'Edit Channel', // 渠道修改
  4282: 'Delete Channel', // 渠道删除
  4290: 'Import Settlements', // 结算导入
  4291: 'Confirm Settlement', // 结算确认
  4292: 'Delete Settlement', // 结算删除
  4300: 'Import Ads', // 广告导入
  4301: 'Delete Ads', // 广告删除
  1: 'System', // 系统管理
  2: 'Monitor', // 系统监控
  3: 'Workflow', // 流程中心
  4: 'Warehouse', // 仓库管理
  5: 'Supply Chain', // 供应链
  7: 'Finance', // 财务管理
  13: 'Supplier Portal', // 供应商门户
  15: 'Mobile', // 移动作业
  16: 'AI', // 智能
  100: 'Users', // 用户管理
  101: 'Roles', // 角色管理
  102: 'Menus', // 菜单管理
  103: 'Dictionaries', // 字典管理
  104: 'Departments', // 部门管理
  105: 'Tenants', // 租户管理
  106: 'Tenant Plans', // 租户套餐
  107: 'Numbering Rules', // 编码规则
  108: 'Print Templates', // 打印模板
  109: 'System Parameters', // 参数设置
  110: 'Online Users', // 在线用户
  111: 'Operation Logs', // 操作日志
  112: 'Login Logs', // 登录日志
  113: 'Server Monitor', // 服务监控
  114: 'Cache Monitor', // 缓存监控
  115: 'Scheduled Jobs', // 定时任务
  120: 'Start a Request', // 发起申请
  121: 'My To-dos', // 我的待办
  122: 'My Processed', // 我的已办
  123: 'My Requests', // 我的申请
  124: 'CC to Me', // 抄送我的
  125: 'Process Models', // 流程模型
  126: 'Process Instances', // 流程实例
  130: 'Inbound Orders', // 入库单
  131: 'Outbound Orders', // 出库单
  132: 'Transfer Orders', // 调拨单
  133: 'Stock Counts', // 盘点单
  134: 'Stock Inquiry', // 库存查询
  135: 'Stock Ledger', // 库存流水
  136: 'Batches & Serial Nos.', // 批次与序列号
  137: 'Local Products', // 本地商品
  138: 'Warehouses', // 仓库管理
  139: 'Locations', // 库位管理
  140: 'Purchase Orders', // 采购订单
  141: 'Purchase Receipts', // 采购入库
  142: 'Purchase Returns', // 采购退货
  143: 'Sales Orders', // 销售订单
  144: 'Sales Deliveries', // 销售出库
  145: 'Sales Returns', // 销售退货
  146: 'Shipments', // 物流发运
  147: 'Suppliers', // 供应商
  148: 'Supplier Evaluation', // 供应商评估
  149: 'Customers', // 客户
  180: 'Vouchers', // 凭证管理
  181: 'Ledgers', // 账簿查询
  182: 'Financial Statements', // 财务报表
  183: 'Receivables', // 应收管理
  184: 'Payables', // 应付管理
  185: 'Receipts', // 收款单
  186: 'Payments', // 付款单
  187: 'Fixed Assets', // 固定资产
  188: 'Cost Accounting', // 成本核算
  189: 'Budgets', // 预算管理
  190: 'Chart of Accounts', // 会计科目
  191: 'Period Close', // 期末结账
  192: 'Opening Balances', // 期初余额
  193: 'Bills & Notes', // 票据管理
  194: 'Account Transfers', // 账户调拨
  195: 'Cash Plans', // 资金计划
  266: 'Stock Holds', // 库存冻结
  267: 'Purchase Requisitions', // 请购单
  268: 'RFQ & Comparison', // 询价比价
  269: 'Sales Quotations', // 销售报价
  270: 'Sales Price Lists', // 销售价目表
  271: 'Delivery Notices', // 发货通知
  272: 'Backorders', // 欠交清单
  273: 'Stock Reservations', // 库存预留
  274: 'Safety Stock Replenishment', // 安全库存补货
  276: 'Invoices', // 发票管理
  277: 'Advances', // 预收预付
  278: 'Bank Reconciliation', // 银行对账
  279: 'Expense Claims', // 费用报销
  280: 'Employee Loans', // 员工借款
  281: 'Business Analysis', // 经营分析
  286: 'Currencies & Exchange Rates', // 币种汇率
  287: 'Consignment Settlement', // 寄售结算
  296: 'Tendering', // 招标采购
  297: 'Purchase Contracts', // 采购合同
  298: 'Advance Shipping Notices', // 送货通知
  299: 'Supplier Statements', // 供应商对账
  300: 'My Orders', // 我的订单
  301: 'Shipping Notices', // 送货通知
  302: 'Statements', // 对账单
  303: 'Tenders & Bids', // 招标投标
  304: 'Quality Claims', // 质量索赔
  307: 'Asset Counts', // 资产盘点
  308: 'Low-value Consumables', // 低值易耗品
  309: 'Standard Cost', // 标准成本
  312: 'Promotions', // 促销活动
  313: 'Sales Rebates', // 销售返利
  314: 'Channel Inventory', // 渠道库存
  315: 'Wave Picking', // 波次拣货
  330: 'Scan to Receive', // 扫码收货
  331: 'Put-away', // 上架
  332: 'Picking', // 拣货
  333: 'Stock Count', // 盘点
  340: 'Voucher Account Settings', // 凭证科目设置
  341: 'Cashier', // 出纳管理
  342: 'Variant Attributes', // 规格属性
  343: 'Assembly & Disassembly', // 组装拆卸
  344: 'Sales Commissions', // 销售提成
  345: 'Custom Fields', // 自定义字段
  346: 'Business Parameters', // 业务参数
  347: 'Inventory Movement Summary', // 收发存汇总
  348: 'Partner Statements', // 往来对账单
  350: 'Integrations', // 第三方集成
  352: 'Business Dictionaries', // 业务字典
  353: 'Notices', // 通知公告
  901: 'Replenishment & Purchasing', // 补货采购
  902: 'Wholesale Sales', // 批发销售
  903: 'Business Partners', // 往来单位
  904: 'Sourcing', // 采购寻源
  911: 'Stock Operations', // 库存作业
  912: 'Stock Inquiry', // 库存查询
  913: 'Stock Control', // 库存控制
  914: 'Master Data', // 基础资料
  931: 'General Ledger', // 总账
  932: 'Receivables & Payables', // 应收应付
  933: 'Treasury & Expenses', // 资金与费用
  934: 'Assets & Costs', // 资产与成本
  935: 'Budget & Analysis', // 预算与分析
  936: 'Closing & Settings', // 期末与设置
  961: 'My Tasks', // 我的事务
  962: 'Process Management', // 流程管理
  966: 'Sales & Purchase Reports', // 进销存报表
  370: 'Sales Report', // 销售报表
  371: 'Purchase Report', // 进货报表
  372: 'Business Overview', // 经营状况
  373: 'Asset Overview', // 资产状况
  374: 'Product Analysis', // 商品分析
  375: 'Customer Analysis', // 客户分析
  377: 'Offsets', // 冲账单
  3770: 'Add Offset', // 冲账单新增
  3771: 'Edit Offset', // 冲账单修改
  3772: 'Delete Offset', // 冲账单删除
  3773: 'Review Offset', // 冲账单复核
  391: 'Other Income & Expenses', // 其他收支
  3910: 'Add Income / Expense Document', // 收支单新增
  3911: 'Edit Income / Expense Document', // 收支单修改
  3912: 'Delete Income / Expense Document', // 收支单删除
  3913: 'Confirm Income / Expense Document', // 收支单确认
  3914: 'Maintain Income / Expense Categories', // 收支类别维护
  392: 'Purchase Rebates', // 采购返利
  3920: 'Maintain Purchase Rebate Agreements', // 采购返利协议维护
  3921: 'Settle Purchase Rebates', // 采购返利结算
  1000: 'Add User', // 用户新增
  1001: 'Edit User', // 用户修改
  1002: 'Delete User', // 用户删除
  1003: 'Reset Password', // 重置密码
  1010: 'Add Role', // 角色新增
  1011: 'Edit Role', // 角色修改
  1012: 'Delete Role', // 角色删除
  1013: 'Data Permission', // 数据权限
  1014: 'Assign Users', // 分配用户
  1020: 'Add Menu', // 菜单新增
  1021: 'Edit Menu', // 菜单修改
  1022: 'Delete Menu', // 菜单删除
  1030: 'Add Dictionary', // 字典新增
  1031: 'Edit Dictionary', // 字典修改
  1032: 'Delete Dictionary', // 字典删除
  1040: 'Add Department', // 部门新增
  1041: 'Edit Department', // 部门修改
  1042: 'Delete Department', // 部门删除
  1050: 'Add Tenant', // 租户新增
  1051: 'Edit Tenant', // 租户修改
  1052: 'Delete Tenant', // 租户删除
  1060: 'Add Plan', // 套餐新增
  1061: 'Edit Plan', // 套餐修改
  1062: 'Delete Plan', // 套餐删除
  1070: 'Edit Rule', // 规则修改
  1080: 'Edit Template', // 模板修改
  1090: 'Edit Parameter', // 参数修改
  1100: 'Force Logout', // 强制退出
  1110: 'Delete Log', // 日志删除
  1120: 'Delete Log', // 日志删除
  1140: 'Clear Cache', // 清除缓存
  1150: 'Edit Job', // 任务修改
  1151: 'Run Now', // 立即执行
  1250: 'Add Model', // 模型新增
  1251: 'Edit Model', // 模型修改
  1252: 'Deploy Model', // 模型发布
  1253: 'Delete Model', // 模型删除
  1260: 'Terminate Process', // 终止流程
  1300: 'Add Inbound Order', // 入库单新增
  1301: 'Edit Inbound Order', // 入库单修改
  1302: 'Delete Inbound Order', // 入库单删除
  1303: 'Confirm Inbound', // 入库确认
  1304: 'Reverse Inbound', // 入库冲销
  1310: 'Add Outbound Order', // 出库单新增
  1311: 'Edit Outbound Order', // 出库单修改
  1312: 'Delete Outbound Order', // 出库单删除
  1313: 'Confirm Outbound', // 出库确认
  1314: 'Reverse Outbound', // 出库冲销
  1320: 'Add Transfer Order', // 调拨单新增
  1321: 'Edit Transfer Order', // 调拨单修改
  1322: 'Delete Transfer Order', // 调拨单删除
  1323: 'Confirm Transfer', // 调拨确认
  1324: 'Reverse Transfer', // 调拨冲销
  1325: 'Receive Transfer', // 调拨收货
  1330: 'Add Stock Count', // 盘点单新增
  1331: 'Edit Stock Count', // 盘点单修改
  1332: 'Delete Stock Count', // 盘点单删除
  1333: 'Confirm Stock Count', // 盘点确认
  1370: 'Add Material', // 物料新增
  1371: 'Edit Material', // 物料修改
  1372: 'Delete Material', // 物料删除
  1374: 'Maintain Material Categories', // 物料分类维护
  1380: 'Add Warehouse', // 仓库新增
  1381: 'Edit Warehouse', // 仓库修改
  1382: 'Delete Warehouse', // 仓库删除
  1390: 'Add Location', // 库位新增
  1391: 'Edit Location', // 库位修改
  1392: 'Delete Location', // 库位删除
  1400: 'Add Purchase Order', // 采购订单新增
  1401: 'Edit Purchase Order', // 采购订单修改
  1402: 'Delete Purchase Order', // 采购订单删除
  1403: 'Approve Purchase Order', // 采购订单审核
  1404: 'Close Purchase Order', // 采购订单关闭
  1405: 'Change Purchase Order', // 采购订单变更
  1410: 'Purchase Receipt', // 采购入库
  1411: 'Post Purchase Receipt', // 采购入库审核
  1420: 'Purchase Return', // 采购退货
  1421: 'Post Purchase Return', // 采购退货审核
  1430: 'Add Sales Order', // 销售订单新增
  1431: 'Edit Sales Order', // 销售订单修改
  1432: 'Delete Sales Order', // 销售订单删除
  1433: 'Approve Sales Order', // 销售订单审核
  1434: 'Close Sales Order', // 销售订单关闭
  1435: 'Reserve Stock', // 预留库存
  1436: 'Change Sales Order', // 销售订单变更
  1440: 'Sales Delivery', // 销售出库
  1441: 'Post Sales Delivery', // 销售出库审核
  1450: 'Sales Return', // 销售退货
  1451: 'Post Sales Return', // 销售退货审核
  1460: 'Add Shipment', // 发运单新增
  1461: 'Edit Shipment', // 发运单修改
  1462: 'Delete Shipment', // 发运单删除
  1463: 'Ship & Track', // 发货与跟踪
  1470: 'Add Supplier', // 供应商新增
  1471: 'Edit Supplier', // 供应商修改
  1472: 'Delete Supplier', // 供应商删除
  1474: 'Merge Suppliers', // 供应商合并
  1473: 'Portal Accounts', // 门户账号
  1480: 'Add Evaluation', // 评估新增
  1481: 'Delete Evaluation', // 评估删除
  1490: 'Add Customer', // 客户新增
  1491: 'Edit Customer', // 客户修改
  1492: 'Delete Customer', // 客户删除
  1494: 'Merge Customers', // 客户合并
  1493: 'Portal Accounts', // 门户账号
  1800: 'Add Voucher', // 凭证新增
  1801: 'Edit Voucher', // 凭证修改
  1802: 'Delete Voucher', // 凭证删除
  1803: 'Approve Voucher', // 凭证审核
  1804: 'Post Voucher', // 凭证记账
  1805: 'Reversal', // 红字冲销
  1830: 'Add Receivable', // 应收单新增
  1831: 'Delete Receivable', // 应收单删除
  1832: 'Generate AR Voucher', // 应收生成凭证
  1834: 'Write Off Bad Debt', // 坏账核销
  1843: 'Write Off Payable', // 应付坏账
  1835: 'AR/AP Netting', // 应收应付对冲
  1836: 'Delete Netting', // 删除对冲单
  1840: 'Add Payable', // 应付单新增
  1841: 'Delete Payable', // 应付单删除
  1842: 'Generate AP Voucher', // 应付生成凭证
  1850: 'Add Receipt', // 收款单新增
  1851: 'Delete Receipt', // 收款单删除
  1852: 'Generate Receipt Voucher', // 收款生成凭证
  1853: 'Confirm Receipt', // 收款单确认
  1860: 'Add Payment', // 付款单新增
  1861: 'Delete Payment', // 付款单删除
  1862: 'Generate Payment Voucher', // 付款生成凭证
  1863: 'Confirm Payment', // 付款单确认
  1870: 'Add Asset', // 资产新增
  1871: 'Edit Asset', // 资产修改
  1872: 'Delete Asset', // 资产删除
  1873: 'Run Depreciation', // 计提折旧
  1874: 'Assign & Transfer', // 领用调拨
  1880: 'Run Cost Accounting', // 成本核算
  1890: 'Prepare Budget', // 预算编制
  1900: 'Add Account', // 科目新增
  1901: 'Edit Account', // 科目修改
  1902: 'Delete Account', // 科目删除
  1910: 'Close Period', // 结账
  1912: 'VAT Carry-forward', // 增值税结转
  1920: 'Edit Opening Balances', // 期初修改
  1930: 'Note Operations', // 票据操作
  1940: 'Record Transfer', // 调拨登记
  1950: 'Maintain Plan Items', // 计划项维护
  2660: 'Release Hold', // 冻结放行
  2670: 'Add Requisition', // 请购单新增
  2671: 'Edit Requisition', // 请购单修改
  2672: 'Delete Requisition', // 请购单删除
  2673: 'Create Purchase Order', // 生成采购订单
  2680: 'Add RFQ', // 询价单新增
  2681: 'Edit RFQ', // 询价单修改
  2682: 'Delete RFQ', // 询价单删除
  2690: 'Add Quotation', // 报价单新增
  2691: 'Edit Quotation', // 报价单修改
  2692: 'Delete Quotation', // 报价单删除
  2693: 'Convert to Sales Order', // 转销售订单
  2700: 'Maintain Price Lists', // 价目表维护
  2710: 'Add Delivery Notice', // 发货通知新增
  2711: 'Pick', // 拣货
  2713: 'Pack', // 装箱
  2730: 'Release Reservation', // 释放预留
  2760: 'Register Invoice', // 发票登记
  2770: 'Record Advances', // 预收预付登记
  2780: 'Import & Match', // 导入与勾对
  2790: 'Add Expense Claim', // 报销新增
  2791: 'Pay Expense Claim', // 报销付款
  2800: 'Add Loan', // 借款新增
  2801: 'Loan Payment & Repayment', // 借款付款与还款
  2860: 'Rates & FX Revaluation', // 汇率维护与期末调汇
  2870: 'Consignment Settlement', // 寄售结算
  2960: 'Add Tender', // 招标新增
  2961: 'Edit Tender', // 招标修改
  2962: 'Delete Tender', // 招标删除
  2963: 'Open & Award Bids', // 开标定标
  2970: 'Add Contract', // 合同新增
  2971: 'Edit Contract', // 合同修改
  2972: 'Delete Contract', // 合同删除
  2973: 'Submit Contract', // 合同提交
  2974: 'Close Contract', // 合同关闭
  2980: 'Receive ASN', // 送货收货
  2990: 'Generate Statement', // 对账单生成
  2991: 'Send Statement', // 对账单发送
  2992: 'Delete Statement', // 对账单删除
  3000: 'Confirm Order', // 订单确认
  3010: 'Create Shipping Notice', // 发起送货
  3020: 'Confirm Statement', // 对账确认
  3030: 'Submit Bid', // 提交投标
  3070: 'Add Count', // 盘点新增
  3071: 'Enter Count Results', // 盘点录入
  3072: 'Delete Count', // 盘点删除
  3080: 'Add Requisition', // 领用新增
  3081: 'Issue Consumables', // 领用出库
  3082: 'Delete Requisition', // 领用删除
  3090: 'Maintain Standard Cost', // 标准成本维护
  3120: 'Maintain Promotions', // 促销维护
  3130: 'Maintain Rebate Agreements', // 返利协议维护
  3131: 'Settle Rebates', // 返利结算
  3150: 'Maintain Waves', // 波次维护
  3151: 'Complete Picking', // 完成拣货
  3400: 'Edit Account Settings', // 科目设置修改
  3420: 'Maintain Variant Attributes', // 规格属性维护
  3430: 'Maintain Assembly Orders', // 组装拆卸维护
  3431: 'Confirm Assembly', // 组装拆卸确认
  3440: 'Maintain Commission Rules', // 提成规则维护
  3441: 'Calculate & Confirm Commissions', // 提成计算与确认
  3450: 'Maintain Custom Fields', // 自定义字段维护
  3460: 'Edit Business Parameters', // 业务参数修改
  3500: 'Edit Integration Settings', // 集成配置修改
  3501: 'Sync & Bind Contacts', // 通讯录同步与绑定
  3520: 'Maintain Business Dictionaries', // 业务字典维护
  3530: 'Add Notices', // 公告新增
  3531: 'Edit Notices', // 公告修改
  3532: 'Delete Notices', // 公告删除
  3533: 'Publish & Withdraw Notices', // 公告发布与撤回
  360: 'AI Q&A', // AI 问数
  361: 'AI Settings', // AI 设置
  3610: 'Edit AI Settings', // AI 设置修改
  362: 'Smart Entry', // 智能录入
  363: 'Data Cleansing', // 数据整理
  3630: 'Apply Cleansing Suggestions', // 采纳整理建议
  364: 'AI Reports', // 智能报告
  3640: 'Generate Reports', // 生成报告
  365: 'Item Prices', // 商品价格
  368: 'Quick Lookup', // 商品速查
  3650: 'Maintain Item Prices', // 商品价格维护
  3651: 'Price Columns', // 价格列设置
  3652: 'View Cost Amounts', // 查看成本金额
  5700: 'Generate Demo Data', // 生成演示数据（电商看板按钮）
  // ===== 第四轮：刊登与定价（pub） =====
  500: 'Listing Center', // 刊登中心
  501: 'Listing Templates', // 刊登模板
  502: 'Price Templates', // 价格模板
  503: 'Price Changes', // 改价记录
  504: 'Repricer', // 自动调价
  505: 'Competitor Prices', // 竞品价格
  5000: 'Add Listing', // 刊登新增
  5001: 'Edit Listing', // 刊登修改
  5002: 'Delete Listing', // 刊登删除
  5003: 'Publish / Retry / Take Offline', // 发布 / 重试 / 下架
  5010: 'Add Template', // 模板新增
  5011: 'Edit Template', // 模板修改
  5012: 'Delete Template', // 模板删除
  5020: 'Add Template', // 模板新增
  5021: 'Edit Template', // 模板修改
  5022: 'Delete Template', // 模板删除
  5030: 'Bulk Price / Stock Change', // 批量改价 / 改库存
  5040: 'Add Rule', // 规则新增
  5041: 'Edit Rule', // 规则修改
  5042: 'Delete Rule', // 规则删除
  5043: 'Run / Apply Suggestions', // 执行 / 应用建议
  5050: 'Enter / Import / Fetch', // 录入 / 导入 / 拉取
  5051: 'Delete', // 删除
  510: 'FBA Inventory Health', // FBA 库存健康
  511: 'Removal Orders', // 移除订单
  512: 'FBA Claims', // FBA 索赔
  513: 'FBA Fee Audit', // FBA 费用核查
  514: 'Amazon Reports', // 亚马逊报告
  5100: 'Sync / Rebuild Inventory Health', // 同步 / 推算库存健康
  5101: 'Maintain Storage Fee Rates', // 维护仓储费费率
  5110: 'Add Removal Order', // 移除订单新增
  5111: 'Edit Removal Order', // 移除订单修改
  5112: 'Delete Removal Order', // 移除订单删除
  5113: 'Receive / Dispose Removal', // 移除收货 / 弃置出库
  5114: 'Sync Removal Orders', // 同步移除订单
  5120: 'Detect / Add Claims', // 识别 / 新增索赔
  5121: 'Process Claims', // 索赔处理
  5122: 'Delete Claims', // 索赔删除
  5123: 'Sync Reimbursements / Ledger', // 同步赔偿 / 流水
  5130: 'Maintain Dimensions & Fulfillment Fee Rates', // 维护尺寸与配送费费率
  5140: 'Create / Upload Reports', // 创建 / 上传报告
  5141: 'Delete Reports', // 报告删除
  // ===== 第四轮：订单增强（ord） =====
  530: 'Order Tags', // 订单标签
  531: 'Buyer Blacklist', // 买家黑名单
  532: 'Logistics Tracking', // 物流跟踪
  533: 'Remote Areas', // 偏远地区
  5300: 'Add Tag', // 标签新增
  5301: 'Edit Tag', // 标签修改
  5302: 'Delete Tag', // 标签删除
  5310: 'Add Blacklist Entry', // 黑名单新增
  5311: 'Edit Blacklist Entry', // 黑名单修改
  5312: 'Delete Blacklist Entry', // 黑名单删除
  5320: 'Refresh Tracking', // 拉取轨迹
  5321: 'Tracking Settings', // 跟踪设置
  5330: 'Add Remote Area', // 偏远地区新增
  5331: 'Edit Remote Area', // 偏远地区修改
  5332: 'Delete Remote Area', // 偏远地区删除
  5340: 'Create Manual Order', // 手工建单
  5341: 'Create Resend', // 创建补发单
  5342: 'Tag / Note Orders', // 订单打标签 / 备注
  // 运营监控与团队（电商 17 下的「运营」分组 989）
  989: 'Monitoring', // 运营监控
  540: 'ASIN Monitor', // ASIN 监控
  541: 'Promotions', // 促销管理
  542: 'Operation Targets', // 运营目标
  543: 'Operator Performance', // 运营绩效
  544: 'Store Access', // 店铺授权
  5400: 'Add Monitor', // 新增监控
  5401: 'Edit Monitor', // 修改监控
  5402: 'Delete Monitor', // 删除监控
  5403: 'Fetch via API', // 接口取数
  5404: 'Enter / Import Snapshots', // 录入 / 导入快照
  5405: 'Handle Alerts', // 处理预警
  5410: 'Add Promotion', // 新增促销
  5411: 'Edit Promotion', // 修改促销
  5412: 'Delete Promotion', // 删除促销
  5413: 'Activate / Cancel', // 生效 / 取消
  5414: 'Push to Platform', // 推送平台
  5420: 'Set Targets', // 设置目标
  5440: 'Edit Store Access', // 修改授权
  980: 'Product Development', // 产品开发
  550: 'Development Projects', // 开发项目
  551: 'Development Board', // 开发看板
  552: 'Profit Calculator', // 利润试算
  553: '1688 Accounts', // 1688 账号
  554: '1688 Sources', // 1688 货源
  555: '1688 Purchase Orders', // 1688 采购单
  5500: 'Add Development Project', // 开发项目新增
  5501: 'Edit Development Project (Samples, Status, Submit)', // 开发项目修改（含样品、状态流转、提交审批）
  5502: 'Delete Development Project', // 开发项目删除
  5503: 'Approve Development Project', // 开发项目审批
  5504: 'Create Material', // 生成物料
  5520: 'Save Calculation Schemes', // 保存试算方案
  5521: 'Maintain Commission Rates', // 佣金费率维护
  5530: 'Maintain and Authorize Accounts', // 账号维护与授权
  5540: 'Maintain Source Links', // 货源关联维护
  5541: 'Sync Prices and Stock', // 同步价格库存
  5550: 'Order on 1688', // 下单到 1688
  5551: 'Sync Orders and Tracking', // 同步订单与物流
  5552: 'Cancel 1688 Orders', // 取消 1688 订单
  5600: 'AI Draft / Translate', // AI 草拟 / 翻译（买家消息）
  5601: 'AI Review Reply / Translate', // AI 草拟回复 / 翻译（评价管理）
  988: 'Overseas Warehouse', // 海外仓
  520: 'Overseas Accounts', // 海外仓账号
  521: 'Overseas Stock', // 海外仓库存
  522: 'Overseas Inbound', // 海外仓入库
  523: 'Overseas Outbound', // 海外仓出库
  524: 'Overseas Fees', // 海外仓费用
  5200: 'Add Account', // 账号新增
  5201: 'Edit Account', // 账号修改
  5202: 'Delete Account', // 账号删除
  5203: 'Warehouse Mapping & Logistics Products', // 仓库映射与物流产品
  5210: 'Sync Stock', // 同步库存
  5211: 'Adjust Stock to Overseas', // 按海外仓调整库存
  5220: 'Create Inbound', // 新建入库单
  5221: 'Push Inbound', // 推送入库单
  5222: 'Sync Receipt', // 同步收货
  5223: 'Cancel Inbound', // 取消入库单
  5230: 'Push to Overseas', // 推送海外仓
  5231: 'Sync Outbound Status', // 同步出库状态
  5232: 'Cancel / Intercept', // 取消 / 截单
  5240: 'Sync Fees', // 同步费用
  525: 'Overseas Returns', // 海外仓退件
  526: 'Overseas Transfers', // 海外仓调拨
  5250: 'Create Return', // 新建退件单
  5251: 'Push Return', // 推送退件单
  5252: 'Sync Returns', // 同步退件
  5253: 'Cancel Return', // 取消退件单
  5260: 'Create Transfer', // 新建调拨单
  5261: 'Submit Transfer', // 提交调拨
  5262: 'Sync Transfers', // 同步调拨
  5263: 'Receive Transfer', // 确认收货
  5264: 'Cancel Transfer', // 取消调拨单
  527: 'Receiving Differences', // 收货差异
  5270: 'Handle Differences', // 处理差异
  // AI 运营助手（运营管理 23 下的「智能运营」分组 985）
  985: 'AI Operations', // 智能运营
  580: 'Daily Ops Report', // 运营日报
  581: 'Banned Words', // 违禁词库
  5800: 'Generate Report', // 生成日报
  5801: 'Delete Report', // 删除日报
  5802: 'Subscriptions', // 订阅设置
  5810: 'Add Banned Word', // 新增违禁词
  5811: 'Edit Banned Word', // 修改违禁词
  5812: 'Delete Banned Word', // 删除违禁词
  5820: 'AI Listing Writer', // AI 写 Listing（刊登中心 500 下的按钮）

  // 自动化规则中心（auto 线）；985 为运营管理下的「智能运营」分组，与 AI 运营助手线共用（谁先合并谁写入，不要重复）
  590: 'Automation Rules', // 自动化规则
  591: 'Execution Log', // 执行日志
  592: 'To-do Reminders', // 待办提醒
  5900: 'Add Rule', // 新增规则
  5901: 'Edit Rule', // 修改规则
  5902: 'Delete Rule', // 删除规则
  5903: 'Test Run', // 试运行
  5910: 'Retry', // 重试
}
