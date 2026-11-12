<template>
  <a-select
    :value="value || []"
    mode="multiple"
    show-search
    :filter-option="false"
    :options="options"
    :loading="loading"
    :placeholder="placeholder"
    :max-tag-count="4"
    :not-found-content="loading ? $t('common.msg.loading') : undefined"
    @search="onSearch"
    @focus="onSearch('')"
    @change="(v) => emit('update:value', v)"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { pageIdentityUsers } from '@/api/workflow/identity'

/**
 * 多选用户（通知 / 待办的接收人）：按用户名或昵称远程搜索（登录即可访问的用户搜索接口）；names 为已选用户的昵称（编辑时回显）
 */
const props = defineProps({
  value: { type: Array, default: () => [] },
  names: { type: Object, default: () => ({}) },
  placeholder: String
})
const emit = defineEmits(['update:value'])
const loading = ref(false)
const found = ref([])
const known = ref({})
let timer = null
let seq = 0

const options = computed(() => {
  const map = new Map()
  ;(props.value || []).forEach((id) => map.set(id, { value: id, label: known.value[id] || props.names?.[id] || String(id) }))
  found.value.forEach((u) => map.set(u.value, u))
  return [...map.values()]
})

function onSearch(keyword) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    const my = ++seq
    loading.value = true
    try {
      const res = await pageIdentityUsers({ current: 1, size: 20, username: keyword || undefined })
      if (my !== seq) return
      found.value = (res.records || []).map((u) => ({ value: u.id, label: u.nickname ? `${u.nickname} (${u.username})` : u.username }))
      found.value.forEach((u) => (known.value[u.value] = u.label))
    } finally {
      if (my === seq) loading.value = false
    }
  }, 250)
}
</script>
