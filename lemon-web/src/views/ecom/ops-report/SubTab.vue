<template>
  <div class="tab-body">
    <a-alert type="info" show-icon class="block">
      <template #message>{{ $t('ecomAsst.sub.tip', { time: pushTime }) }}</template>
      <template #description>
        {{ imChannels.length ? $t('ecomAsst.sub.imOn', { list: imText }) : $t('ecomAsst.sub.imOff') }}
      </template>
    </a-alert>
    <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <a-button v-perm="'ecom:opsReport:subscribe'" type="primary" @click="formRef.open()">
        <template #icon><PlusOutlined /></template>{{ $t('ecomAsst.sub.add') }}
      </a-button>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: scrollX, y: '100%' }"
      :locale="{ emptyText: $t('ecomAsst.sub.empty') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userName'">
          <b>{{ record.userName || record.userId }}</b>
        </template>
        <template v-else-if="column.key === 'stores'">
          <span v-if="!record.storeNames?.length" class="sub-text">{{ $t('ecomAsst.common.allVisibleStores') }}</span>
          <a-tooltip v-else :title="record.storeNames.join(sep)">
            <div class="ellipsis">{{ record.storeNames.join(sep) }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'channels'">
          <DictTag v-for="c in channelList(record)" :key="c" type="ecom_ops_push_channel" :value="c" />
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="sys_common_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'lastPushTime'">{{ record.lastPushTime || '-' }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
    <SubForm ref="formRef" :im-channels="imChannels" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deleteOpsReportSub, listOpsReportSubs, opsReportSubOptions } from '@/api/ecom/assistant'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import SubForm from './SubForm.vue'

/** 日报订阅：订阅人、店铺范围、推送渠道；推送时间与已接入的第三方平台在顶部说明 */
const { t } = useI18n()
const dict = useDict('ecom_ops_push_channel', 'sys_common_status')
const sep = computed(() => t('ecomAsst.common.sep'))
const columns = [
  { title: t('ecomAsst.sub.user'), key: 'userName', dataIndex: 'userName', width: 140 },
  { title: t('ecomAsst.sub.stores'), key: 'stores', width: 320 },
  { title: t('ecomAsst.sub.channels'), key: 'channels', width: 240 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90 },
  { title: t('ecomAsst.sub.lastPush'), key: 'lastPushTime', dataIndex: 'lastPushTime', width: 170 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ops-report-sub')
const size = ref('middle')
const list = ref([])
const loading = ref(false)
const imChannels = ref([])
const defaultCron = ref('')
const formRef = ref()

/** Cron「秒 分 时 ...」→ HH:mm */
const pushTime = computed(() => {
  const p = String(defaultCron.value || '').split(/\s+/)
  return p.length >= 3 && /^\d+$/.test(p[1]) && /^\d+$/.test(p[2]) ? `${p[2].padStart(2, '0')}:${p[1].padStart(2, '0')}` : '08:30'
})
const imText = computed(() => imChannels.value.map((c) => findDict(dict.ecom_ops_push_channel, c)?.label || c).join(sep.value))
const channelList = (r) => String(r.channels || '').split(',').filter(Boolean)

async function load() {
  loading.value = true
  try {
    list.value = (await listOpsReportSubs()) || []
  } finally {
    loading.value = false
  }
}
const remove = useAction(deleteOpsReportSub, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:opsReport:subscribe', onClick: () => formRef.value.open(r) },
  { type: 'delete', perm: 'ecom:opsReport:subscribe', confirm: { title: t('ecomAsst.sub.deleteConfirm', { name: r.userName }), okType: 'danger' }, onClick: () => remove(r.id) }
]

onMounted(async () => {
  load()
  const o = await opsReportSubOptions()
  imChannels.value = o?.imChannels || []
  defaultCron.value = o?.defaultCron || ''
})
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.block {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
