import { t } from '@/locales'
import { EcomAdAction } from '@/constants/enums'

/** 规则的文字摘要：条件「ACOS（%）大于 30 且 点击 大于等于 10」、动作「竞价按百分比调整 -20%（下限 0.5）」 */
export function conditionText(rule) {
  return (rule.conditions || [])
    .map((c) => `${t(`ecomAds.enum.metric.${c.metric}`)} ${t(`ecomAds.enum.operator.${c.operator}`)} ${Number(c.value)}`)
    .join('; ')
}

export function actionText(rule, actionLabel) {
  const a = rule.action
  let s = actionLabel(a)
  if (a === EcomAdAction.BID_PERCENT || a === EcomAdAction.BUDGET_PERCENT) s += ` ${Number(rule.actionValue) > 0 ? '+' : ''}${Number(rule.actionValue)}%`
  else if (a === EcomAdAction.BID_SET || a === EcomAdAction.BUDGET_SET) s += ` ${Number(rule.actionValue)}`
  const range = []
  if (rule.minValue != null) range.push(`${t('ecomAds.rule.min')} ${Number(rule.minValue)}`)
  if (rule.maxValue != null) range.push(`${t('ecomAds.rule.max')} ${Number(rule.maxValue)}`)
  return range.length ? `${s} (${range.join(', ')})` : s
}
