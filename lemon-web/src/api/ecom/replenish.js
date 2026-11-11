import request from '@/utils/request'

/** 补货建议（不分页）：storeId、keyword、level → ReplenishRow[] */
export const replenishRows = (params) => request.get('/ecom/replenish', { params })
