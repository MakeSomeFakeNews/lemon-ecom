<template>
  <a-select
    :value="null"
    show-search
    size="small"
    class="tpl-picker"
    :placeholder="$t('ecomCs.template.pick')"
    :options="groups"
    :loading="loading"
    :filter-option="filterOption"
    :dropdown-match-select-width="380"
    :not-found-content="loading ? undefined : $t('ecomCs.template.none')"
    @dropdown-visible-change="(v) => v && load()"
    @select="onSelect"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { csTemplateOptions, renderCsTemplate } from '@/api/ecom/cs'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/**
 * 快捷回复模板选择：按语言分组列出启用的、适用于平台的模板；选中后按会话 / 评价 / 订单替换变量，
 * 触发 apply({ templateId, content, missing })，取不到值的变量（如还没有运单号）提示人工补充
 * - platform：只列适用于该平台的模板（EcomPlatform），不传列出全部
 * - threadId / reviewId / orderId：替换变量的来源（优先级同顺序）
 */
const props = defineProps({
  platform: String,
  threadId: Number,
  reviewId: Number,
  orderId: Number
})
const emit = defineEmits(['apply'])
const { t } = useI18n()
const dict = useDict('ecom_cs_language')

const list = ref([])
const loading = ref(false)
let loadedFor = null

async function load() {
  if (loadedFor === (props.platform || '*')) return
  loading.value = true
  try {
    list.value = (await csTemplateOptions(props.platform || undefined)) || []
    loadedFor = props.platform || '*'
  } finally {
    loading.value = false
  }
}

const groups = computed(() => {
  const byLang = new Map()
  for (const tpl of list.value) {
    if (!byLang.has(tpl.language)) byLang.set(tpl.language, [])
    byLang.get(tpl.language).push({ value: tpl.id, label: tpl.scene ? `${tpl.name} · ${tpl.scene}` : tpl.name, search: `${tpl.name} ${tpl.scene || ''} ${tpl.content}` })
  }
  return [...byLang.entries()].map(([lang, options]) => ({ label: findDict(dict.ecom_cs_language, lang)?.label || lang, options }))
})

const filterOption = (input, option) => (option.search || option.label || '').toLowerCase().includes(input.trim().toLowerCase())

async function onSelect(id) {
  const res = await renderCsTemplate(id, { threadId: props.threadId, reviewId: props.reviewId, orderId: props.orderId })
  if (res.missing?.length) {
    message.warning(t('ecomCs.template.missing', { vars: res.missing.map((v) => t(`ecomCs.template.var.${v}`)).join(t('ecomCs.common.sep')) }))
  }
  emit('apply', { templateId: id, content: res.content, missing: res.missing || [] })
}
</script>

<style scoped>
.tpl-picker {
  width: 220px;
}
</style>
