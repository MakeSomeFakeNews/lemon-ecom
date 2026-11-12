import { ref } from 'vue'
import { automationCatalog } from '@/api/ecom/automation'
import { AutoActionType, AutoAdTarget, AutoLogic, AutoOperator, CsTicketType, ImChannel, PriceAdjustMode } from '@/constants/enums'
import { useDictStore } from '@/store/dict'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'

/** 规则编辑目录（触发器、动作、运算符、公共变量），页面内共享一次请求；切换语言后刷新页面重新加载 */
const catalog = ref(null)
let pending = null
export function useCatalog() {
  if (!catalog.value && !pending) {
    pending = automationCatalog()
      .then((c) => (catalog.value = c))
      .finally(() => (pending = null))
  }
  return catalog
}

export const triggerOf = (c, code) => c?.triggers?.find((x) => x.code === code)
export const actionOf = (c, type) => c?.actions?.find((x) => x.type === type)
export const fieldOf = (c, trigger, key) => triggerOf(c, trigger)?.fields?.find((f) => f.key === key)

/** 模板变量：${字段} */
export const varToken = (key) => '$' + '{' + key + '}'

export const emptyCondition = () => ({ field: undefined, operator: AutoOperator.EQ, value: '' })
export const emptyGroup = () => ({ logic: AutoLogic.AND, items: [], groups: [] })
export const emptyScope = () => ({ storeIds: [], platforms: [], sites: [], skus: [], tags: [] })

/** 不需要比较值的运算符 */
export const NO_VALUE = [AutoOperator.EMPTY, AutoOperator.NOT_EMPTY]
/** 多值运算符 */
export const MULTI = [AutoOperator.IN, AutoOperator.NOT_IN]

/** 条件值的显示：枚举取字典名称，多个值用顿号 */
function valueText(field, value) {
  if (value === null || value === undefined || value === '') return ''
  if (field?.dict) {
    const store = useDictStore()
    return String(value)
      .split(/[,，]/)
      .map((v) => findDict(store.cache[field.dict], v.trim())?.label || v.trim())
      .join(t('ecomAuto.common.sep'))
  }
  if (field?.type === 'BOOLEAN') return String(value) === 'true' ? t('common.status.yes') : t('common.status.no')
  return String(value)
}

function conditionText(c, trigger, cat) {
  const f = fieldOf(cat, trigger, c.field)
  const name = f?.label || c.field || '?'
  const op = t(`ecomAuto.op.${c.operator}`)
  return NO_VALUE.includes(c.operator) ? `${name} ${op}` : `${name} ${op} ${valueText(f, c.value) || '?'}`
}

/** 条件组的文字：全部满足用「且」、任一满足用「或」连接，子组加括号 */
export function groupText(g, trigger, cat, nested = false) {
  if (!g) return ''
  const joiner = ` ${t(g.logic === AutoLogic.OR ? 'ecomAuto.editor.previewOr' : 'ecomAuto.editor.previewAnd')} `
  const parts = (g.items || []).filter((c) => c.field).map((c) => conditionText(c, trigger, cat))
  if (!nested) {
    ;(g.groups || []).forEach((sub) => {
      const s = groupText(sub, trigger, cat, true)
      if (s) parts.push(`(${s})`)
    })
  }
  return parts.join(joiner)
}

/** 范围的文字：店铺 / 平台 / 站点 / SKU / 标签；compact 时店铺超过 2 个只显示前 2 个和总数 */
export function scopeText(scope, storeName, compact = false) {
  if (!scope) return ''
  const sep = t('ecomAuto.common.sep')
  const parts = []
  if (scope.storeIds?.length) {
    const names = scope.storeIds.map((id) => storeName(id))
    parts.push(compact && names.length > 2 ? t('ecomAuto.common.storesN', { names: names.slice(0, 2).join(sep), n: names.length })
      : `${t('ecomAuto.common.store')} ${names.join(sep)}`)
  }
  if (scope.platforms?.length) parts.push(`${t('ecomAuto.common.platform')} ${scope.platforms.join(sep)}`)
  if (scope.sites?.length) parts.push(`${t('ecomAuto.common.site')} ${scope.sites.join(sep)}`)
  if (scope.skus?.length) parts.push(`${t('ecomAuto.common.sku')} ${scope.skus.join(sep)}`)
  if (scope.tags?.length) parts.push(`${t('ecomAuto.common.tags')} ${scope.tags.join(sep)}`)
  return parts.join(t('ecomAuto.common.sep'))
}

/** 规则的一句话描述：当「触发器」发生，对象为 …，且 … 时，执行 A → B */
export function ruleSentence(rule, cat, storeName) {
  const tr = triggerOf(cat, rule.trigger)
  const segs = [`${t('ecomAuto.editor.previewWhen')} ${t('ecomAuto.editor.q1')}${tr?.name || '?'}${t('ecomAuto.editor.q2')} ${t('ecomAuto.editor.previewHappen')}`]
  const scope = scopeText(rule.scope, storeName, true)
  if (scope) segs.push(`${t('ecomAuto.editor.previewIn')} ${scope}`)
  const cond = groupText(rule.condition, rule.trigger, cat)
  if (cond) segs.push(`${t('ecomAuto.editor.previewAnd')} ${cond}`)
  const actions = (rule.actions || []).map((a) => actionOf(cat, a.type)?.name || a.type)
  return `${segs.join(t('ecomAuto.editor.comma'))}${t('ecomAuto.editor.previewThen')} ${actions.length ? actions.join(' → ') : t('ecomAuto.editor.previewNoAction')}`
}

/** 动作类型的默认值（新增动作、切换类型时补齐） */
export function applyActionDefaults(a) {
  const type = a.type
  if ((type === AutoActionType.NOTIFY || type === AutoActionType.TODO) && !a.userIds?.length && !a.roleIds?.length && a.storeOwner == null) a.storeOwner = true
  if (type === AutoActionType.NOTIFY && a.site == null) a.site = true
  if (type === AutoActionType.IM_ROBOT && !a.imPlatform) a.imPlatform = ImChannel.DINGTALK
  if ((type === AutoActionType.LISTING_REPRICE || type === AutoActionType.AD_ADJUST) && !a.adjustMode) a.adjustMode = PriceAdjustMode.PERCENT
  if (type === AutoActionType.AD_ADJUST && !a.adTarget) a.adTarget = AutoAdTarget.BUDGET
  if (type === AutoActionType.CS_TICKET && !a.ticketType) a.ticketType = CsTicketType.REFUND_ONLY
  return a
}
