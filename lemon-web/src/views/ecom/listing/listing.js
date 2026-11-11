/** 在线商品页面共用的小函数 */

/** 映射物料的显示名：编码 名称（规格） */
export const materialLabel = (r) =>
  r?.materialId ? `${r.materialCode || ''} ${r.materialName || ''}${r.materialSpec ? `（${r.materialSpec}）` : ''}`.trim() : undefined
