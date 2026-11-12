<template>
  <div class="tab-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomOper.common.allStores')" class="filter" />
      <a-select v-model:value="query.alertType" allow-clear :placeholder="$t('ecomOper.alert.alertType')" :options="toOptions(dict.ecom_monitor_alert_type)" class="filter" @change="search" />
      <a-select v-model:value="query.status" allow-clear :placeholder="$t('ecomOper.alert.status')" :options="toOptions(dict.ecom_monitor_alert_status)" class="filter" @change="search" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :presets="presets" @change="search" />
      <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomOper.monitor.keywordPh')" class="filter" @press-enter="search" />
      <a-tag v-if="monitorId" closable color="blue" @close="emit('clear-monitor')">{{ $t('ecomOper.alert.viewMonitor') }} #{{ monitorId }}</a-tag>
      <a-button type="primary" @click="search"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    </div>
    <div class="counts">
      <a-tag v-for="c in counts" :key="c.alertType" :color="findDict(dict.ecom_monitor_alert_type, c.alertType)?.tagColor || 'default'" class="count" @click="filterType(c.alertType)">
        {{ findDict(dict.ecom_monitor_alert_type, c.alertType)?.label || c.alertType }} {{ c.count }}
      </a-tag>
    </div>

    <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:monitor:alert'" type="primary" @click="openHandle(EcomMonitorAlertStatus.HANDLED)">
        <template #icon><CheckOutlined /></template>
        {{ selectedKeys.length ? $t('ecomOper.alert.handleSelected', { n: selectedKeys.length }) : $t('ecomOper.alert.handle') }}
      </a-button>
      <a-button v-perm="'ecom:monitor:alert'" @click="openHandle(EcomMonitorAlertStatus.IGNORED)">
        <template #icon><StopOutlined /></template>
        {{ selectedKeys.length ? $t('ecomOper.alert.ignoreSelected', { n: selectedKeys.length }) : $t('ecomOper.alert.ignore') }}
      </a-button>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'product'">
          <div class="ellipsis" :title="record.title">{{ record.title || record.itemId }}</div>
          <div class="sub-text">
            <DictTag type="ecom_monitor_type" :value="record.monitorType" />
            <span class="mono">{{ record.itemId }}</span> · {{ record.marketplace }}<span v-if="record.storeName"> · {{ record.storeName }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'alertType'"><DictTag type="ecom_monitor_alert_type" :value="record.alertType" /></template>
        <template v-else-if="column.key === 'oldValue'"><span class="ellipsis" :title="record.oldValue">{{ record.oldValue || '-' }}</span></template>
        <template v-else-if="column.key === 'newValue'"><span class="ellipsis strong" :title="record.newValue">{{ record.newValue || '-' }}</span></template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="ecom_monitor_alert_status" :value="record.status" />
          <div v-if="record.handlerName" class="sub-text">{{ record.handlerName }} {{ record.handleTime }}</div>
          <div v-if="record.handleNote" class="sub-text ellipsis" :title="record.handleNote">{{ record.handleNote }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="handleOpen" :title="$t('ecomOper.alert.handleTitle', { n: handleIds.length })" :confirm-loading="handling" @ok="submitHandle">
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomOper.alert.status')">
          <a-radio-group v-model:value="handleStatus">
            <a-radio :value="EcomMonitorAlertStatus.HANDLED">{{ findDict(dict.ecom_monitor_alert_status, EcomMonitorAlertStatus.HANDLED)?.label }}</a-radio>
            <a-radio :value="EcomMonitorAlertStatus.IGNORED">{{ findDict(dict.ecom_monitor_alert_status, EcomMonitorAlertStatus.IGNORED)?.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('ecomOper.alert.handleNote')">
          <a-textarea v-model:value="handleNote" :maxlength="255" :auto-size="{ minRows: 2, maxRows: 5 }" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, SearchOutlined, StopOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { handleMonitorAlerts, monitorAlertCounts, pageMonitorAlert } from '@/api/ecom/oper'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { EcomMonitorAlertStatus } from '@/constants/enums'

/** 变化预警：按类型的未处理数，筛选（默认未处理）、批量处理 / 忽略。monitorId 只看一个监控的预警 */
const props = defineProps({ monitorId: Number })
const emit = defineEmits(['changed', 'clear-monitor'])
const { t } = useI18n()
const dict = useDict('ecom_monitor_alert_type', 'ecom_monitor_alert_status', 'ecom_monitor_type')
const presets = rangePresets()

const columns = [
  { title: t('ecomOper.alert.alertDate'), dataIndex: 'alertDate', width: 110 },
  { title: t('ecomOper.monitor.product'), key: 'product', width: 300, exportValue: (r) => `${r.itemId} ${r.title || ''}` },
  { title: t('ecomOper.alert.alertType'), key: 'alertType', dataIndex: 'alertType', width: 110, dict: 'ecom_monitor_alert_type' },
  { title: t('ecomOper.alert.oldValue'), key: 'oldValue', dataIndex: 'oldValue', width: 220 },
  { title: t('ecomOper.alert.newValue'), key: 'newValue', dataIndex: 'newValue', width: 220 },
  { title: t('ecomOper.alert.status'), key: 'status', dataIndex: 'status', width: 180, dict: 'ecom_monitor_alert_status' },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-monitor-alert')

const query = reactive({ storeId: undefined, alertType: undefined, status: EcomMonitorAlertStatus.NEW, range: undefined, keyword: undefined })
const params = (q) => ({ storeId: q.storeId, alertType: q.alertType, status: q.status, keyword: q.keyword || undefined, monitorId: props.monitorId || undefined, ...rangeParams(q.range) })
const { list, loading, size, pagination, load, search: doSearch, onTableChange, exportAll, selectedKeys, rowSelection } = useTable(pageMonitorAlert, query, { params })

const counts = ref([])
async function loadCounts() {
  const q = params(query)
  delete q.status
  counts.value = (await monitorAlertCounts(q)) || []
}
function search() {
  doSearch()
  loadCounts()
}
function filterType(type) {
  query.alertType = type
  query.status = EcomMonitorAlertStatus.NEW
  search()
}
watch(() => [props.monitorId, query.storeId], () => search())

const handleOpen = ref(false)
const handling = ref(false)
const handleIds = ref([])
const handleStatus = ref(EcomMonitorAlertStatus.HANDLED)
const handleNote = ref('')
function openHandle(status, ids) {
  const list = ids || selectedKeys.value
  if (!list.length) return message.info(t('ecomOper.alert.selectFirst'))
  handleIds.value = [...list]
  handleStatus.value = status
  handleNote.value = ''
  handleOpen.value = true
}
async function submitHandle() {
  handling.value = true
  try {
    await handleMonitorAlerts({ ids: handleIds.value, status: handleStatus.value, note: handleNote.value || null })
    message.success(t('common.msg.operationSuccess'))
    handleOpen.value = false
    selectedKeys.value = []
    search()
    emit('changed')
  } finally {
    handling.value = false
  }
}

const rowActions = (record) => record.status === EcomMonitorAlertStatus.NEW
  ? [
      { type: 'confirm', label: t('ecomOper.alert.handle'), perm: 'ecom:monitor:alert', onClick: () => openHandle(EcomMonitorAlertStatus.HANDLED, [record.id]) },
      { type: 'disable', label: t('ecomOper.alert.ignore'), perm: 'ecom:monitor:alert', onClick: () => openHandle(EcomMonitorAlertStatus.IGNORED, [record.id]) }
    ]
  : []

onMounted(search)
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.filter {
  width: 180px;
}
.counts {
  margin-bottom: 8px;
}
.count {
  cursor: pointer;
}
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.strong {
  font-weight: 500;
}
.mono {
  font-family: monospace;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
