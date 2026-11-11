import { t } from '@/locales'

/** 参考时效文字：3~7 天、7 天、- */
export function daysText(r) {
  const min = r?.daysMin
  const max = r?.daysMax
  if (min == null && max == null) return '-'
  if (min != null && max != null && min !== max) return t('ecom.logistics.daysRange', { min, max })
  return t('ecom.logistics.daysN', { n: min ?? max })
}
