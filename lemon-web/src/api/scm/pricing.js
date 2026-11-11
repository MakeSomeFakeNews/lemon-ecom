import request from '@/utils/request'

const list = (v) => (Array.isArray(v) ? v.join(',') : v)

/**
 * 开单默认价（含税、基本单位、本位币）
 * params: { kind: 'SALES' | 'PURCHASE', partnerId, materialIds: [id] | '1,2', qtys?: [数量]（与 materialIds 一一对应）, date? }
 * → 按行返回 [{ materialId, price, taxRate, source }]（与 materialIds 一一对应，同一物料多行按各行数量分别匹配起订量 / 阶梯价），source 见 @/constants/enums 的 PriceSource（没有价格时 price、source 为空，
 * taxRate 为空表示来源没有税率，保持原税率）。
 * 优先级：销售 客户执行价 → 客户价格等级价 → 零售价 → 最近售价；采购 供货价 → 最近进价 → 参考成本价
 */
export const defaultPrices = (params) =>
  request.get('/scm/pricing/default', {
    params: {
      ...params,
      kind: params.kind ? String(params.kind).toUpperCase() : params.kind,
      materialIds: list(params.materialIds),
      qtys: params.qtys ? list(params.qtys.map((q) => (q == null || q === '' ? 1 : q))) : undefined
    }
  })

/**
 * 价格参考：params { materialId, partnerId?, kind?（partnerId 是客户 SALES 还是供应商 PURCHASE，指定时只筛选该方向的分区）,
 *   keyword?（往来单位编码 / 名称）, start?, end? }
 * → { sales, purchases, purchaseQty, supplierCompare, customerCompare }，没有权限的分区不返回
 */
export const priceReference = (params) => request.get('/scm/pricing/reference', { params })

/** 客户执行价：[{ materialId, price, taxRate, remark, materialCode, materialName, spec, unit }] */
export const customerPrices = (customerId) => request.get(`/scm/customer/${customerId}/prices`)
export const saveCustomerPrices = (customerId, rows) => request.put(`/scm/customer/${customerId}/prices`, rows)
/** 导出执行价：params { customerId?（为空导出全部客户）, keyword?（客户编码 / 名称） } → [{ customerCode, customerName, materialCode, ..., price, taxRate, remark }] */
export const exportCustomerPrices = (params) => request.get('/scm/customer-price/export', { params })
/** Excel 导入：{ rows: [{ rowNum, data: { customerCode, materialCode, price, taxRate, remark } }] }，按 客户 × 物料 新增或更新 */
export const importCustomerPrices = (data) => request.post('/scm/customer-price/import', data)

/** 价格列设置：[{ level（0 零售价，1-8 批发价）, name, status }]，登录即可读取 */
export const priceLevels = () => request.get('/wms/price-level')
export const savePriceLevels = (rows) => request.put('/wms/price-level', rows)
