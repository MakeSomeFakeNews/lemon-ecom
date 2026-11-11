import request from '@/utils/request'

/** 补货建议（再订货点）：{ keyword, onlyNeeded }，返回数组（不分页） */
export const listReorder = (params) => request.get('/scm/reorder', { params })
