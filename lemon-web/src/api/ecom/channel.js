import request from '@/utils/request'

/**
 * 渠道接入总览：[{ platform, supported, authType, capabilities, hasSandbox, docUrl,
 *   stores: [{ id, code, name, marketplace, status, sandbox, authStatus, authorizedTime, lastSyncTime, lastSyncMessage, orderSync, inventorySync,
 *     syncs: [{ type, supported, success, time, message, failures24h }], failures24h, problem }] }]
 */
export const channelOverview = () => request.get('/ecom/channel/overview')
/** 全部店铺的同步日志分页：storeId、syncType、success（1 / 0） */
export const pageChannelLogs = (params) => request.get('/ecom/channel/logs', { params })
