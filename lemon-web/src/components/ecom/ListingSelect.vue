<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :filter-option="false"
    :placeholder="placeholder || $t('ecomCatalog.common.selectListing')"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    :dropdown-match-select-width="420"
    :not-found-content="loading ? $t('common.msg.loading') : undefined"
    @search="onSearch"
    @focus="() => search('')"
    @change="onChange"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { pageListing } from '@/api/ecom/listing'

/**
 * 在线商品（平台 SKU）下拉：按店铺远程搜索（SKU / 商品 ID / 标题），选项显示「SKU  标题」。
 * - v-model:value：在线商品 ID；label：当前值的显示名
 * - storeId：只列出该店铺的在线商品（为空时不查询）
 * - @select(listing)：选中后返回在线商品（含 composeType、materialId、packQty、fnsku、title）
 */
const props = defineProps({
  value: Number,
  label: String,
  storeId: Number,
  placeholder: String,
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const list = ref([])
const loading = ref(false)
let timer = null
let seq = 0

const toLabel = (l) => `${l.sellerSku}${l.title ? `  ${l.title}` : ''}`
const options = computed(() => {
  const opts = list.value.map((l) => ({ value: l.id, label: toLabel(l) }))
  if (props.value != null && !opts.some((o) => o.value === props.value)) opts.unshift({ value: props.value, label: props.label || String(props.value) })
  return opts
})

async function search(keyword) {
  if (props.storeId == null) {
    list.value = []
    return
  }
  const n = ++seq
  loading.value = true
  try {
    const res = await pageListing({ storeId: props.storeId, keyword: keyword || undefined, current: 1, size: 30 })
    if (n === seq) list.value = res.records || []
  } finally {
    if (n === seq) loading.value = false
  }
}
function onSearch(kw) {
  clearTimeout(timer)
  timer = setTimeout(() => search(kw), 300)
}
function onChange(v) {
  emit('update:value', v ?? null)
  const l = list.value.find((x) => x.id === v)
  if (l) emit('select', l)
}
</script>
