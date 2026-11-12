<template>
  <a-alert type="info" show-icon :message="$t('ecomAds.schedule.tip')" class="tip" />
  <div class="toolbar">
    <a-button v-perm="'ecom:ad:schedule'" type="primary" @click="editorRef.open(null, storeId)">
      <template #icon><PlusOutlined /></template>{{ $t('ecomAds.schedule.add') }}
    </a-button>
  </div>
  <a-table row-key="id" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1000 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'campaignName'">
        {{ record.campaignName }}
        <div class="sub-text">{{ record.storeName }}<template v-if="record.timezone"> · {{ record.timezone }}</template></div>
      </template>
      <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
      <template v-else-if="column.key === 'range'">{{ range(record) }}</template>
      <template v-else-if="column.key === 'lastFactor'">{{ record.lastFactor ?? '-' }}</template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="actions(record)" />
      </template>
    </template>
  </a-table>
  <ScheduleEditor ref="editorRef" @success="load" />
</template>

<script setup>
import { ref, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { applySchedule, deleteSchedule, listSchedules } from '@/api/ecom/ads'
import { useAction } from '@/composables/useAction'
import { STATUS_ENABLED } from '@/utils/constants'
import { t } from '@/locales'
import ScheduleEditor from './ScheduleEditor.vue'

/** 分时策略页签：各活动的策略、当前系数、最近应用结果；新增 / 编辑、立即应用、删除（恢复基准竞价） */
const props = defineProps({ storeId: { type: Number, default: undefined } })
const rows = ref([])
const loading = ref(false)
const editorRef = ref()
const columns = [
  { title: t('ecomAds.schedule.campaign'), key: 'campaignName', width: 260 },
  { title: t('ecomAds.common.status'), key: 'status', width: 80 },
  { title: `${t('ecomAds.schedule.minBid')} ~ ${t('ecomAds.schedule.maxBid')}`, key: 'range', width: 160 },
  { title: t('ecomAds.schedule.lastFactor'), key: 'lastFactor', width: 100, align: 'right' },
  { title: t('ecomAds.schedule.lastApplied'), dataIndex: 'lastAppliedTime', key: 'lastAppliedTime', width: 160 },
  { title: t('ecomAds.schedule.lastMessage'), dataIndex: 'lastMessage', key: 'lastMessage', width: 280, ellipsis: true },
  actionColumn(3)
]
const range = (r) => (r.minBid == null && r.maxBid == null ? '-' : `${r.minBid ?? ''} ~ ${r.maxBid ?? ''}`)

async function load() {
  loading.value = true
  try {
    rows.value = (await listSchedules({ storeId: props.storeId })) || []
  } finally {
    loading.value = false
  }
}
watch(() => props.storeId, load, { immediate: true })

const apply = useAction((r) => applySchedule(r.campaignId), { successKey: (res) => res?.message, reload: () => load() })
const remove = useAction((r) => deleteSchedule(r.campaignId), {
  successKey: (res) => res?.message,
  reload: () => load()
})
const actions = (r) => [
  { type: 'edit', perm: 'ecom:ad:schedule', onClick: () => editorRef.value.open(r.campaignId) },
  { type: 'run', label: t('ecomAds.schedule.apply'), perm: 'ecom:ad:schedule', show: r.status === STATUS_ENABLED, onClick: () => apply(r) },
  { type: 'delete', perm: 'ecom:ad:schedule', confirm: t('ecomAds.schedule.confirmDelete'), onClick: () => remove(r) }
]
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
