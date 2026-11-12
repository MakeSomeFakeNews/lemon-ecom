import { t } from '@/locales'
import { EcomPlatform, StoreAuthStatus, SyncType } from '@/constants/enums'

/** 平台品牌色与缩写（店铺、渠道页面的平台图标） */
export const PLATFORM_BRAND = {
  [EcomPlatform.AMAZON]: { color: '#ff9900', text: 'a' },
  [EcomPlatform.ALIEXPRESS]: { color: '#e62e04', text: 'AE' },
  [EcomPlatform.EBAY]: { color: '#0064d2', text: 'eb' },
  [EcomPlatform.SHOPEE]: { color: '#ee4d2d', text: 'S' },
  [EcomPlatform.LAZADA]: { color: '#0f146d', text: 'Lz' },
  [EcomPlatform.SHOPIFY]: { color: '#5e8e3e', text: 'Sf' },
  [EcomPlatform.TIKTOK]: { color: '#161823', text: 'TT' },
  [EcomPlatform.TEMU]: { color: '#fb7701', text: 'T' },
  [EcomPlatform.WALMART]: { color: '#0071ce', text: 'W' },
  [EcomPlatform.OZON]: { color: '#005bff', text: 'O' },
  [EcomPlatform.MERCADO_LIBRE]: { color: '#2d3277', text: 'ML' }
}
export const platformBrand = (p) => PLATFORM_BRAND[p] || { color: '#8c8c8c', text: (p || '?').slice(0, 1) }

/** 相对时间：刚刚 / n 分钟前 / n 小时前 / n 天前（超过 30 天显示日期） */
export function relativeTime(time) {
  if (!time) return ''
  const ts = new Date(String(time).replace(' ', 'T')).getTime()
  if (Number.isNaN(ts)) return time
  const minutes = Math.floor((Date.now() - ts) / 60000)
  if (minutes < 1) return t('ecom.hub.ago.justNow')
  if (minutes < 60) return t('ecom.hub.ago.minutes', { n: minutes })
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return t('ecom.hub.ago.hours', { n: hours })
  const days = Math.floor(hours / 24)
  return days <= 30 ? t('ecom.hub.ago.days', { n: days }) : String(time).slice(0, 10)
}

/**
 * 店铺的同步健康度（来自渠道总览的 syncs）：
 * level = ok（最近一次都成功）/ error（有接口最近一次失败或需重新授权）/ idle（从未同步）；
 * latest 最近一次同步（不含授权检查），failed 最近一次失败的接口
 */
export function syncHealth(store, authStatus) {
  const syncs = (store?.syncs || []).filter((s) => s.supported && s.time)
  const ops = syncs.filter((s) => s.type !== SyncType.AUTH)
  const latest = ops.reduce((a, b) => (!a || b.time > a.time ? b : a), null)
  const failed = syncs.filter((s) => s.success === false).sort((a, b) => (a.time < b.time ? 1 : -1))[0] || null
  const level = failed || authStatus === StoreAuthStatus.REAUTH ? 'error' : latest ? 'ok' : 'idle'
  return { level, latest, failed, failures24h: store?.failures24h || 0, syncs }
}
