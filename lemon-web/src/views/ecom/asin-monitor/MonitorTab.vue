<template>
  <div class="tab-body">
    <SearchForm v-show="showSearch" :model="query" @search="search" @reset="resetQuery">
      <a-form-item :label="$t('ecomOper.common.store')" name="storeId">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomOper.common.allStores')" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.common.platform')" name="platform">
        <a-select v-model:value="query.platform" allow-clear :placeholder="$t('ecomOper.common.allPlatforms')" :options="toOptions(dict.ecom_platform)" />
      </a-form-item>
      <a-form-item :label="$t('ecomOper.monitor.monitorType')" name="monitorType">
        <a-select v-model:value="query.monitorType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_monitor_type)" />
      </a-form-item>
      <a-form-item :label="$t('common.field.keyword')" name="keyword">
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomOper.monitor.keywordPh')" @press-enter="search" />
      </a-form-item>
      <a-form-item :label="$t('common.field.status')" name="status">
        <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.sys_common_status, true)" />
      </a-form-item>
      <a-form-item name="alertOnly">
        <a-checkbox v-model:checked="query.alertOnly">{{ $t('ecomOper.monitor.alertOnly') }}</a-checkbox>
      </a-form-item>
    </SearchForm>

    <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:monitor:add'" type="primary" @click="form.openCreate()">
        <template #icon><PlusOutlined /></template>{{ $t('ecomOper.monitor.add') }}
      </a-button>
      <a-button v-perm="'ecom:monitor:fetch'" :loading="fetching" @click="fetchSelected">
        <template #icon><CloudDownloadOutlined /></template>
        {{ selectedKeys.length ? $t('ecomOper.monitor.fetchSelected', { n: selectedKeys.length }) : $t('ecomOper.monitor.fetch') }}
      </a-button>
      <a-button v-perm="'ecom:monitor:snapshot'" @click="importOpen = true">
        <template #icon><ImportOutlined /></template>{{ $t('ecomOper.monitor.import') }}
      </a-button>
      <a-tooltip :title="fetchTip">
        <QuestionCircleOutlined class="tip-icon" />
      </a-tooltip>
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
          <div class="product">
            <a-image v-if="record.mainImage" :src="record.mainImage" :width="44" :height="44" class="thumb" />
            <div v-else class="thumb empty" />
            <div class="product-text">
              <a class="ellipsis" :title="record.title" @click="detailRef.open(record.id)">{{ record.title || record.itemId }}</a>
              <div class="sub-text">
                <span class="mono">{{ record.itemId }}</span>
                <span v-if="record.sellerSku"> · {{ record.sellerSku }}</span>
                <span v-if="record.brand"> · {{ record.brand }}</span>
              </div>
              <div v-if="record.benchmarkItemId" class="sub-text">{{ $t('ecomOper.monitor.benchmarkOf', { item: record.benchmarkItemId }) }}</div>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'monitorType'">
          <DictTag type="ecom_monitor_type" :value="record.monitorType" />
          <div v-if="record.status === STATUS_DISABLED" class="sub-text">{{ $t('ecomOper.monitor.paused') }}</div>
        </template>
        <template v-else-if="column.key === 'platform'">
          <DictTag type="ecom_platform" :value="record.platform" />
          <div class="sub-text">{{ record.marketplace }}<span v-if="record.storeName"> · {{ record.storeName }}</span></div>
        </template>
        <template v-else-if="column.key === 'rankMain'">
          <div>{{ rankText(record.rankMain) }} <span :class="trendClass(record.rankChange)">{{ changeText(record.rankChange) }}</span></div>
          <div class="sub-text ellipsis" :title="record.category">{{ record.category }}</div>
        </template>
        <template v-else-if="column.key === 'rankSub'">
          <div>{{ rankText(record.rankSub) }}</div>
          <div class="sub-text ellipsis" :title="record.subCategory">{{ record.subCategory }}</div>
        </template>
        <template v-else-if="column.key === 'rating'">
          <span>{{ record.rating ?? '-' }}</span>
          <span :class="trendClass(record.ratingChange)" class="delta">{{ signed(record.ratingChange, 1) }}</span>
        </template>
        <template v-else-if="column.key === 'reviewCount'">
          <span>{{ record.reviewCount ?? '-' }}</span>
          <span :class="trendClass(record.reviewChange)" class="delta">{{ signed(record.reviewChange) }}</span>
        </template>
        <template v-else-if="column.key === 'sellerCount'">
          <span :class="{ danger: isOwn(record) && Number(record.sellerCount) > 1 }">{{ record.sellerCount ?? '-' }}</span>
        </template>
        <template v-else-if="column.key === 'price'">{{ record.price == null ? '-' : `${money(record.price)} ${record.currency || ''}` }}</template>
        <template v-else-if="column.key === 'listingStatus'">
          <DictTag v-if="record.listingStatus" type="ecom_listing_status" :value="record.listingStatus" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'newAlerts'">
          <a v-if="record.newAlerts" @click="emit('alerts', record.id)"><a-badge :count="record.newAlerts" /></a>
          <span v-else class="sub-text">0</span>
        </template>
        <template v-else-if="column.key === 'lastSnapshotDate'">
          <div>{{ record.lastSnapshotDate || '-' }}</div>
          <a-tooltip v-if="record.lastFetchMessage" :title="$t('ecomOper.monitor.fetchError', { msg: record.lastFetchMessage })">
            <WarningOutlined class="danger" />
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>

    <MonitorForm ref="form" @success="onRecorded" />
    <SnapshotModal ref="snapshotRef" @success="onRecorded" />
    <MonitorDetail ref="detailRef" @changed="load" @edit="(r) => form.openEdit(r)" @record="(r) => snapshotRef.open(r)" />
    <ImportModal v-model:open="importOpen" :title="$t('ecomOper.monitor.importTitle')" :columns="importColumns" :sample="importSample" :importer="importSnapshots" @success="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CloudDownloadOutlined, ImportOutlined, PlusOutlined, QuestionCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changeMonitorStatus, deleteMonitor, fetchMonitors, importSnapshots, monitorFetchPlatforms, pageMonitor } from '@/api/ecom/oper'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { changeText, isOwn, rankText, signed } from './monitor'
import MonitorForm from './MonitorForm.vue'
import SnapshotModal from './SnapshotModal.vue'
import MonitorDetail from './MonitorDetail.vue'

/** 监控列表：最新排名（较上一次的变化）、评分、评论数、在售卖家（自己的商品有跟卖标红）、售价、上下架、未处理预警 */
const emit = defineEmits(['alerts'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_monitor_type', 'ecom_listing_status', 'sys_common_status')

const columns = [
  { title: t('ecomOper.monitor.product'), key: 'product', width: 320, exportValue: (r) => `${r.itemId} ${r.title || ''}` },
  { title: t('ecomOper.monitor.monitorType'), key: 'monitorType', dataIndex: 'monitorType', width: 100, dict: 'ecom_monitor_type' },
  { title: t('ecomOper.common.platform'), key: 'platform', dataIndex: 'platform', width: 150, dict: 'ecom_platform' },
  { title: t('ecomOper.monitor.rankMain'), key: 'rankMain', dataIndex: 'rankMain', width: 170 },
  { title: t('ecomOper.monitor.rankSub'), key: 'rankSub', dataIndex: 'rankSub', width: 150 },
  { title: t('ecomOper.monitor.rating'), key: 'rating', dataIndex: 'rating', width: 90, align: 'right' },
  { title: t('ecomOper.monitor.reviewCount'), key: 'reviewCount', dataIndex: 'reviewCount', width: 100, align: 'right' },
  { title: t('ecomOper.monitor.sellerCount'), key: 'sellerCount', dataIndex: 'sellerCount', width: 90, align: 'right' },
  { title: t('ecomOper.monitor.price'), key: 'price', dataIndex: 'price', width: 110, align: 'right' },
  { title: t('ecomOper.monitor.listingStatus'), key: 'listingStatus', dataIndex: 'listingStatus', width: 90, dict: 'ecom_listing_status' },
  { title: t('ecomOper.monitor.newAlerts'), key: 'newAlerts', dataIndex: 'newAlerts', width: 90, align: 'center' },
  { title: t('ecomOper.monitor.lastSnapshot'), key: 'lastSnapshotDate', dataIndex: 'lastSnapshotDate', width: 110 },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-asin-monitor')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, platform: undefined, monitorType: undefined, keyword: undefined, status: undefined, alertOnly: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll, selectedKeys, rowSelection } = useTable(pageMonitor, query, {
  params: (q) => ({ ...q, alertOnly: q.alertOnly || undefined })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const trendClass = (v) => (v == null || Number(v) === 0 ? '' : Number(v) > 0 ? 'up' : 'down')

const fetchPlatforms = ref([])
const fetchTip = computed(() => t('ecomOper.monitor.fetchTip', {
  platforms: fetchPlatforms.value.map((p) => findDict(dict.ecom_platform, p)?.label || p).join(t('ecomOper.common.sep')) || t('ecomOper.common.none')
}))

const form = ref()
const snapshotRef = ref()
const detailRef = ref()
const importOpen = ref(false)
const fetching = ref(false)

async function runFetch(ids) {
  fetching.value = true
  try {
    const r = await fetchMonitors(ids)
    message.success(t('ecomOper.monitor.fetched', r))
    if (r.message) message.warning(t('ecomOper.monitor.fetchFailed', { msg: r.message }), 6)
    load()
  } finally {
    fetching.value = false
  }
}
function fetchSelected() {
  if (!selectedKeys.value.length) return message.info(t('ecomOper.monitor.fetchSelect'))
  runFetch(selectedKeys.value)
}

function onRecorded() {
  load()
  detailRef.value?.reload()
}

const remove = useAction(deleteMonitor, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const toggle = useAction((r) => changeMonitorStatus(r.id, r.status === STATUS_ENABLED ? STATUS_DISABLED : STATUS_ENABLED), { reload: () => load() })
const rowActions = (record) => [
  { type: 'detail', onClick: () => detailRef.value.open(record.id) },
  { type: 'edit', perm: 'ecom:monitor:edit', onClick: () => form.value.openEdit(record) },
  { type: 'add', label: t('ecomOper.monitor.record'), perm: 'ecom:monitor:snapshot', onClick: () => snapshotRef.value.open(record) },
  { type: 'run', label: t('ecomOper.monitor.fetch'), icon: CloudDownloadOutlined, perm: 'ecom:monitor:fetch', onClick: () => runFetch([record.id]) },
  record.status === STATUS_ENABLED
    ? { type: 'disable', label: t('ecomOper.monitor.pause'), perm: 'ecom:monitor:edit', onClick: () => toggle(record) }
    : { type: 'enable', label: t('ecomOper.monitor.resume'), perm: 'ecom:monitor:edit', onClick: () => toggle(record) },
  { type: 'delete', perm: 'ecom:monitor:remove', confirm: { title: t('ecomOper.monitor.deleteConfirm', { name: record.itemId }), okType: 'danger' }, onClick: () => remove(record.id) }
]

// 导入（列与后端 SnapshotImportRow 一致）
const importColumns = [
  { title: t('ecomOper.common.platform'), key: 'platform', required: true, width: 12 },
  { title: t('ecomOper.common.marketplace'), key: 'marketplace', required: true, width: 8 },
  { title: t('ecomOper.monitor.itemId'), key: 'itemId', required: true, width: 16 },
  { title: t('ecomOper.monitor.snapshotDate'), key: 'snapshotDate', required: true, type: 'date', width: 14 },
  { title: t('ecomOper.monitor.category'), key: 'category', width: 20 },
  { title: t('ecomOper.monitor.rankMain'), key: 'rankMain', type: 'int', width: 10 },
  { title: t('ecomOper.monitor.subCategory'), key: 'subCategory', width: 20 },
  { title: t('ecomOper.monitor.rankSub'), key: 'rankSub', type: 'int', width: 10 },
  { title: t('ecomOper.monitor.rating'), key: 'rating', type: 'number', width: 8 },
  { title: t('ecomOper.monitor.reviewCount'), key: 'reviewCount', type: 'int', width: 10 },
  { title: t('ecomOper.monitor.sellerCount'), key: 'sellerCount', type: 'int', width: 10 },
  { title: t('ecomOper.monitor.price'), key: 'price', type: 'number', width: 10 },
  { title: t('ecomOper.monitor.titleText'), key: 'title', width: 40 },
  { title: t('ecomOper.monitor.mainImage'), key: 'mainImage', width: 30 },
  { title: t('ecomOper.monitor.listingStatus'), key: 'listingStatus', width: 10 }
]
const importSample = {
  platform: 'AMAZON',
  marketplace: 'US',
  itemId: 'B0EXAMPLE1',
  snapshotDate: '2026-11-01',
  category: 'Electronics',
  rankMain: '1520',
  subCategory: 'Earbud Headphones',
  rankSub: '35',
  rating: '4.4',
  reviewCount: '2310',
  sellerCount: '1',
  price: '29.99',
  title: t('ecomOper.monitor.sampleTitle'),
  mainImage: '',
  listingStatus: 'ACTIVE'
}

onMounted(async () => {
  load()
  fetchPlatforms.value = (await monitorFetchPlatforms()) || []
})
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.product {
  display: flex;
  gap: 8px;
  align-items: center;
}
.thumb {
  border-radius: 4px;
  object-fit: cover;
  flex: none;
}
.thumb.empty {
  width: 44px;
  height: 44px;
  background: var(--app-fill, #f5f5f5);
}
.product-text {
  min-width: 0;
  flex: 1;
}
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mono {
  font-family: monospace;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.delta {
  margin-left: 4px;
  font-size: 12px;
}
.up {
  color: #52c41a;
}
.down,
.danger {
  color: #ff4d4f;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
}
</style>
