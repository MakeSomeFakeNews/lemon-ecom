/** 在线商品页面共用的小函数 */

/** 映射物料的显示名：编码 名称（规格） */
export const materialLabel = (r) =>
  r?.materialId ? `${r.materialCode || ''} ${r.materialName || ''}${r.materialSpec ? `（${r.materialSpec}）` : ''}`.trim() : undefined

/** 组合商品的组成：编码 × 用量 + …（列表、详情显示） */
export const bundleText = (r) =>
  (r?.bundleItems || []).map((b) => `${b.materialCode || b.materialId} × ${Number(b.qty)}`).join(' + ')
