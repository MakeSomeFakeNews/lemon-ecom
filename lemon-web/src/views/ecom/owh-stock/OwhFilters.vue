<template>
  <a-form-item v-if="field === 'account'" :label="$t('ecomOwh.common.account')" name="accountId">
    <a-select v-model:value="model.accountId" :placeholder="$t('common.placeholder.all')" allow-clear :options="accountOptions" @change="onAccount" />
  </a-form-item>
  <a-form-item v-else :label="$t('ecomOwh.common.warehouse')" name="warehouseId">
    <a-select v-model:value="model.warehouseId" :placeholder="$t('common.placeholder.all')" allow-clear :options="warehouseOptions" @change="emit('change')" />
  </a-form-item>
</template>

<script>
import { ref } from 'vue'
import { owhAccountOptions, owhAllMappings } from '@/api/ecom/owh'

// 海外仓账号与映射在各页面之间共用一次加载
const accounts = ref([])
const mappings = ref([])
let loading
function ensureLoaded() {
  loading ||= Promise.all([owhAccountOptions(), owhAllMappings()])
    .then(([a, m]) => {
      accounts.value = a || []
      mappings.value = m || []
    })
    .catch(() => (loading = undefined))
}
</script>

<script setup>
import { computed } from 'vue'

/**
 * 海外仓页面共用的筛选项：field = account（海外仓账号）或 warehouse（海外仓，即已映射的系统仓库，按所选账号过滤）。
 * 搜索区中每个筛选项单独写一个 OwhFilters，SearchForm 才能逐项排版和折叠。选择后触发 change（页面据此立即查询）
 */
const props = defineProps({ model: { type: Object, required: true }, field: { type: String, default: 'account' } })
const emit = defineEmits(['change'])
ensureLoaded()

const accountOptions = computed(() => accounts.value.map((a) => ({ value: a.id, label: a.name })))
const warehouseOptions = computed(() =>
  mappings.value
    .filter((m) => !props.model.accountId || m.accountId === props.model.accountId)
    .map((m) => ({ value: m.warehouseId, label: `${m.warehouseName}（${m.owhCode}）` }))
)
function onAccount() {
  // 换账号后不属于该账号的海外仓清空
  const wh = props.model.warehouseId
  if (wh && !warehouseOptions.value.some((o) => o.value === wh)) props.model.warehouseId = undefined
  emit('change')
}
</script>
