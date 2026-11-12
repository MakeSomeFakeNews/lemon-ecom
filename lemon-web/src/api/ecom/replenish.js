import request from '@/utils/request'

/** 平台仓补货建议（不分页）：storeId、keyword、level，日均加权 w7 / w14 / w30 / w60（全部为空取默认）→ ReplenishRow[] */
export const replenishRows = (params) => request.get('/ecom/replenish', { params })
/** 本地仓采购建议（物料，汇总全部店铺）：keyword、level、w7 / w14 / w30 / w60 → PurchaseSuggestRow[] */
export const purchaseSuggestions = (params) => request.get('/ecom/replenish/purchase', { params })
/** 物料的在途构成：materialId、storeId（只看某店铺的货件）→ { purchases, shipments } */
export const replenishTransit = (params) => request.get('/ecom/replenish/transit', { params })
/** 补货设置：scope（ReplenishScope）+ listingId / materialId，没有设置时为空 */
export const getReplenishSetting = (params) => request.get('/ecom/replenish/setting', { params })
/** 保存补货设置（全部项为空时删除，返回空） */
export const saveReplenishSetting = (data) => request.put('/ecom/replenish/setting', data)
/**
 * 由补货建议生成发货计划（发货计划模块提供）：{ storeId, toWarehouseId, items: [{ listingId, qty }] }
 */
export const shipmentPlanFromReplenish = (data) => request.post('/ecom/shipment-plan/from-replenish', data)
