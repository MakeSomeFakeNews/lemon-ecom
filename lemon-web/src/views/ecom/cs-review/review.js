import { t } from '@/locales'

/** 评分筛选：差评（不超过 3 星）、中评（3 星）、好评（4–5 星） */
export const ratingFilters = () => [
  { value: 'low', label: t('ecomCs.review.ratingLow'), params: { maxRating: 3 } },
  { value: 'mid', label: t('ecomCs.review.ratingMid'), params: { minRating: 3, maxRating: 3 } },
  { value: 'high', label: t('ecomCs.review.ratingHigh'), params: { minRating: 4 } }
]

/** 评分筛选的值 → 查询参数 */
export const ratingParams = (value) => ratingFilters().find((f) => f.value === value)?.params || {}

/** 差评（不超过 3 星，与后端 CsConstants.LOW_RATING 一致） */
export const LOW_RATING = 3
