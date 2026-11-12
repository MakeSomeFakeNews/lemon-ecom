import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { t } from '@/locales'
import { applicableBannedWords } from '@/api/ecom/assistant'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { BannedWordLevel } from '@/constants/enums'
import { checkListing } from './bannedWords'

/**
 * 刊登编辑页的违禁词实时检查：按店铺的平台 / 站点取适用的词（违禁词库），表单的标题、五点、描述变化时在本地重新检查。
 * 返回 result（checkListing 的结果）、字段状态 / 提示（给 a-form-item / a-input 用）和发布前的确认 beforePublish()：
 * 命中阻止发布的词时提示并返回 false，只有提醒类时二次确认
 *
 * @param {import('vue').Ref<object>} form 表单（storeId、platform、title、bullets、description）
 */
export function useListingCompliance(form) {
  const stores = useStores()
  const words = ref([])
  const loaded = ref(false)
  const marketplace = computed(() => stores.value.find((s) => s.id === form.value?.storeId)?.marketplace)

  async function load() {
    const f = form.value
    if (!f?.storeId) return
    loaded.value = false
    try {
      words.value = (await applicableBannedWords(f.platform, marketplace.value)) || []
    } catch {
      words.value = []
    } finally {
      loaded.value = true
    }
  }
  watch(() => [form.value?.storeId, form.value?.platform, marketplace.value], load, { immediate: true })

  const result = computed(() => checkListing(words.value, form.value || {}))
  const levelOf = (hits) => (!hits?.length ? undefined : hits.some((h) => h.level === BannedWordLevel.BLOCK) ? 'error' : 'warning')
  /** a-form-item 的 validate-status（没有命中时为 undefined，按表单规则校验） */
  const fieldStatus = (key) => levelOf(result.value.byField[key])
  /** a-form-item 的 help：命中的词 */
  const fieldHelp = (key) => {
    const hits = result.value.byField[key]
    return hits?.length ? t('ecomAsst.check.fieldHit', { words: [...new Set(hits.map((h) => h.word))].join(t('ecomAsst.common.sep')) }) : undefined
  }

  /** 发布前检查：阻止类直接拦下，提醒类二次确认；返回 Promise<boolean> */
  function beforePublish() {
    const r = result.value
    if (r.block) {
      message.error(t('ecomAsst.check.publishBlocked', { n: r.block }))
      return Promise.resolve(false)
    }
    if (!r.warn) return Promise.resolve(true)
    return new Promise((resolve) => {
      Modal.confirm({
        title: t('ecomAsst.check.publishWarnTitle'),
        content: t('ecomAsst.check.publishWarn', { n: r.warn }),
        okText: t('ecomAsst.check.publishAnyway'),
        onOk: () => resolve(true),
        onCancel: () => resolve(false)
      })
    })
  }

  return { words, loaded, marketplace, result, fieldStatus, fieldHelp, beforePublish, reload: load }
}
