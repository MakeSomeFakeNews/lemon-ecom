<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder || $t('ecomOrder.common.selectChannel')"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    option-filter-prop="label"
    :dropdown-match-select-width="320"
    :not-found-content="loading ? $t('common.msg.loading') : $t('ecomOrder.common.noChannel')"
    @change="onChange"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { logisticsOptions } from '@/api/ecom/logistics'
import { cached } from '@/utils/cache'

/**
 * 物流渠道下拉：启用的渠道（GET /ecom/logistics/options，登录即可访问），同一页面内多个下拉共用一次请求。
 * - v-model:value：渠道 ID；label：当前值不在选项中（渠道已停用）时的回显文字
 * - @select(channel)：选中后返回渠道（id、code、name、provider、transportMode、daysMin、daysMax）
 */
const props = defineProps({
  value: Number,
  label: String,
  placeholder: String,
  allowClear: { type: Boolean, default: true },
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const list = ref([])
const loading = ref(false)
const toLabel = (c) => [c.name, c.provider].filter(Boolean).join(' · ')
const options = computed(() => {
  const opts = list.value.map((c) => ({ value: c.id, label: toLabel(c) }))
  if (props.value != null && !opts.some((o) => o.value === props.value)) {
    opts.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return opts
})

onMounted(async () => {
  loading.value = true
  try {
    list.value = (await cached('ecom-logistics-options', logisticsOptions)) || []
  } finally {
    loading.value = false
  }
})

function onChange(v) {
  emit('update:value', v ?? null)
  const channel = v == null ? undefined : list.value.find((c) => c.id === v)
  if (channel) emit('select', channel)
}
</script>
