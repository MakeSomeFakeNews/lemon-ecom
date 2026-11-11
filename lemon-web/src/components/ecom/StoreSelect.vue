<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder || $t('ecom.common.selectStore')"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    :filter-option="filterOption"
    :dropdown-match-select-width="320"
    @change="onChange"
  />
</template>

<script>
import { ref } from 'vue'
import { storeOptions } from '@/api/ecom/store'

/** 启用的店铺列表：全局共享，同一时刻多个下拉只请求一次 */
const stores = ref([])
const storesLoading = ref(false)
let pending = null
let loaded = false

/** 加载（或强制刷新）店铺列表，返回 ref；加载状态见 storesLoading */
export function useStores(force = false) {
  if ((!loaded || force) && !pending) {
    storesLoading.value = true
    pending = storeOptions()
      .then((list) => {
        stores.value = list || []
        loaded = true
      })
      .catch(() => {})
      .finally(() => {
        pending = null
        storesLoading.value = false
      })
  }
  return stores
}
</script>

<script setup>
import { computed } from 'vue'
import { useDictStore } from '@/store/dict'
import { findDict } from '@/utils/dict'

/**
 * 店铺下拉：启用的平台店铺（GET /ecom/store/options，登录即可访问），选项显示「名称（平台 站点）」，可按名称、编码、站点搜索。
 * 列表全局共享，同一页面多个下拉只请求一次。
 * - v-model:value：店铺 ID
 * - label：当前值的显示名（店铺已停用、不在选项中时回显；不传时显示 ID）
 * - platform：只列出某个平台的店铺（EcomPlatform 取值）
 * - allowClear（默认 true）、disabled、placeholder（默认「选择店铺」）
 * - @select(store)：选中后返回店铺（id、code、name、platform、marketplace、currency、warehouseId、platformWarehouseId、customerId），清空时不触发
 * 导出的 useStores() 返回共享的店铺列表 ref，页面需要按 ID 取店铺信息时使用
 */
const props = defineProps({
  value: Number,
  label: String,
  platform: String,
  placeholder: String,
  allowClear: { type: Boolean, default: true },
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const list = useStores()
const loading = storesLoading
const dictStore = useDictStore()
dictStore.load('ecom_platform').catch(() => {})

const platformLabel = (p) => findDict(dictStore.cache.ecom_platform, p)?.label || p || ''
/** 选项文字：名称（平台 站点），搜索时也能匹配编码 */
const toLabel = (s) => `${s.name}（${[platformLabel(s.platform), s.marketplace].filter(Boolean).join(' ')}）`

const options = computed(() => {
  const rows = props.platform ? list.value.filter((s) => s.platform === props.platform) : list.value
  const opts = rows.map((s) => ({ value: s.id, label: toLabel(s), code: s.code }))
  if (props.value != null && !opts.some((o) => o.value === props.value)) {
    opts.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return opts
})

/** 按名称（平台 站点）或编码搜索 */
function filterOption(input, option) {
  const kw = (input || '').trim().toLowerCase()
  return !kw || option.label.toLowerCase().includes(kw) || (option.code || '').toLowerCase().includes(kw)
}

function onChange(v) {
  emit('update:value', v ?? null)
  const store = v == null ? undefined : list.value.find((s) => s.id === v)
  if (store) emit('select', store)
}
</script>
