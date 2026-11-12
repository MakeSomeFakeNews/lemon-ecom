<template>
  <a-form-item :label="$t('ecomOwh.common.account')" name="accountId">
    <a-select v-model:value="model.accountId" :placeholder="$t('common.placeholder.all')" allow-clear :options="accounts.map((a) => ({ value: a.id, label: a.name }))" />
  </a-form-item>
  <a-form-item :label="$t('ecomOwh.common.warehouse')" name="warehouseId">
    <a-select
      v-model:value="model.warehouseId"
      :placeholder="$t('common.placeholder.all')"
      allow-clear
      :options="mappings.filter((m) => !model.accountId || m.accountId === model.accountId).map((m) => ({ value: m.warehouseId, label: `${m.warehouseName}（${m.owhCode}）` }))"
    />
  </a-form-item>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { owhAccountOptions, owhAllMappings } from '@/api/ecom/owh'

/** 海外仓页面共用的筛选项：海外仓账号、海外仓（已映射的系统仓库）。model 为页面的查询条件（accountId、warehouseId） */
defineProps({ model: { type: Object, required: true } })

const accounts = ref([])
const mappings = ref([])
onMounted(async () => {
  const [a, m] = await Promise.all([owhAccountOptions(), owhAllMappings()])
  accounts.value = a || []
  mappings.value = m || []
})
</script>
