<template>
  <a-select
    :value="value"
    mode="multiple"
    allow-clear
    show-search
    :max-tag-count="maxTagCount"
    :placeholder="placeholder || $t('ecomAsst.common.allVisibleStores')"
    :options="options"
    :filter-option="filterOption"
    @update:value="(v) => emit('update:value', v)"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/** 多选店铺（当前用户可见的启用店铺），为空表示全部；v-model:value 为店铺 ID 数组 */
defineProps({
  value: { type: Array, default: () => [] },
  placeholder: String,
  maxTagCount: { type: [Number, String], default: 'responsive' }
})
const emit = defineEmits(['update:value'])
const stores = useStores()
const dict = useDict('ecom_platform')
const options = computed(() =>
  stores.value.map((s) => ({ value: s.id, label: `${s.name}（${findDict(dict.ecom_platform, s.platform)?.label || s.platform} ${s.marketplace || ''}）`, code: s.code }))
)
const filterOption = (input, option) => {
  const kw = (input || '').trim().toLowerCase()
  return !kw || option.label.toLowerCase().includes(kw) || (option.code || '').toLowerCase().includes(kw)
}
</script>
