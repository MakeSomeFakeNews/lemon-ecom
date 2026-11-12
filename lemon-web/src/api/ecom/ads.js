import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 广告结构
/** 活动分页：params { current, size, storeId, keyword, status, adType } */
export const pageCampaigns = (params) => request.get('/ecom/ads/campaigns', { params })
export const getCampaign = (id) => request.get(`/ecom/ads/campaigns/${id}`)
export const addCampaign = (data) => request.post('/ecom/ads/campaigns', data)
export const updateCampaign = (id, data) => request.put(`/ecom/ads/campaigns/${id}`, data)
export const deleteCampaigns = (id) => request.delete(`/ecom/ads/campaigns/${ids(id)}`)
export const listGroups = (campaignId) => request.get(`/ecom/ads/campaigns/${campaignId}/groups`)
export const addGroup = (data) => request.post('/ecom/ads/groups', data)
export const updateGroup = (id, data) => request.put(`/ecom/ads/groups/${id}`, data)
export const deleteGroup = (id) => request.delete(`/ecom/ads/groups/${id}`)
/** 投放分页：params { current, size, campaignId, adGroupId, keyword, status, targetType } */
export const pageTargets = (params) => request.get('/ecom/ads/targets', { params })
export const addTarget = (data) => request.post('/ecom/ads/targets', data)
export const updateTarget = (id, data) => request.put(`/ecom/ads/targets/${id}`, data)
export const deleteTargets = (id) => request.delete(`/ecom/ads/targets/${ids(id)}`)
/** 修改竞价：level = TARGET / AD_GROUP，返回是否已同步到平台 */
export const changeBid = (level, id, value) => request.put(`/ecom/ads/${level}/${id}/bid`, { value })
export const changeBudget = (id, value) => request.put(`/ecom/ads/campaigns/${id}/budget`, { value })
/** 暂停 / 启用：level = CAMPAIGN / AD_GROUP / TARGET */
export const changeAdStatus = (level, id, status) => request.put(`/ecom/ads/${level}/${id}/status`, null, { params: { status } })

// ---------------------------------------------------------------- 报告导入与分析
/** 导入广告后台报告：{ storeId, reportType?, adType?, headers, rows, firstRowNum } */
export const importAdReport = (data) => request.post('/ecom/ads/report/import', data, { timeout: 120000 })
export const adsOverview = (params) => request.get('/ecom/ads/overview', { params })
export const adsTrend = (params) => request.get('/ecom/ads/trend', { params })
/** 层级报表：params { level, storeId, campaignId, adGroupId, beginDate, endDate, keyword } */
export const adsHierarchy = (params) => request.get('/ecom/ads/hierarchy', { params })
export const adsSearchTerms = (params) => request.get('/ecom/ads/search-terms', { params })
/** 否定搜索词：{ matchType, items: [{ adGroupId, text }] } */
export const negateSearchTerms = (data) => request.post('/ecom/ads/search-terms/negate', data)
export const adsTacos = (params) => request.get('/ecom/ads/tacos', { params })

// ---------------------------------------------------------------- 授权与同步
export const adsConnectors = () => request.get('/ecom/ads/auth/connectors')
export const adsAuthStores = () => request.get('/ecom/ads/auth/stores')
/** 保存授权：{ credentials, sandbox, baseUrl, profileId, sync } */
export const saveAdsAuth = (storeId, data) => request.put(`/ecom/ads/auth/${storeId}`, data)
export const clearAdsAuth = (storeId) => request.delete(`/ecom/ads/auth/${storeId}`)
export const testAdsAuth = (storeId) => request.post(`/ecom/ads/auth/${storeId}/test`, null, { timeout: 60000 })
export const adsProfiles = (storeId) => request.get(`/ecom/ads/auth/${storeId}/profiles`, { timeout: 60000 })
/** Login with Amazon 授权页地址（先保存 clientId / clientSecret / 区域） */
export const adsAuthorizeUrl = (storeId) => request.post(`/ecom/ads/oauth/${storeId}/authorize-url`)
/** 店铺的广告授权状态（授权窗口打开后轮询） */
export const adsOAuthStatus = (storeId) => request.get(`/ecom/ads/oauth/${storeId}/status`)
/** 选择广告配置文件 */
export const selectAdsProfile = (storeId, profileId) => request.put(`/ecom/ads/oauth/${storeId}/profile`, { profileId })
export const syncAds = (storeId, days) => request.post(`/ecom/ads/sync/${storeId}`, null, { params: { days } })
export const syncAdsStructure = (storeId) => request.post(`/ecom/ads/sync/${storeId}/structure`, null, { timeout: 120000 })

// ---------------------------------------------------------------- 分时策略
export const listSchedules = (params) => request.get('/ecom/ads/schedules', { params })
export const getSchedule = (campaignId) => request.get(`/ecom/ads/schedules/campaign/${campaignId}`)
export const saveSchedule = (campaignId, data) => request.put(`/ecom/ads/schedules/campaign/${campaignId}`, data, { timeout: 60000 })
export const deleteSchedule = (campaignId) => request.delete(`/ecom/ads/schedules/campaign/${campaignId}`, { timeout: 60000 })
export const applySchedule = (campaignId) => request.post(`/ecom/ads/schedules/campaign/${campaignId}/apply`, null, { timeout: 60000 })
