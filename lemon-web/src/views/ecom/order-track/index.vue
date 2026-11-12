<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @update:value="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrd.track.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.track.shipDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.common.channel')" name="channelId">
          <ChannelSelect v-model:value="query.channelId" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.track.alert')" name="alert">
          <a-select v-model:value="query.alert" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_track_alert)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrder.order.country')" name="country">
          <a-input v-model:value="query.country" :maxlength="8" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="status-tabs" @change="onTab">
        <a-tab-pane v-for="tb in tabs" :key="tb.key">
          <template #tab>
            <span :class="{ 'alert-tab': tb.key === ALERT_TAB && tb.count }">{{ tb.label }}</span><span v-if="tb.count != null" class="tab-count" :class="{ 'tab-count-alert': tb.key === ALERT_TAB && tb.count }">{{ tb.count }}</span>
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <BatchActionBar v-if="selectedKeys.length" :count="selectedKeys.length" :text="$t('ecomOrdUi.pkg.selected', { n: selectedKeys.length })" :actions="batchActions" @clear="selectedKeys = []" />
        <template v-else>
          <a-button v-perm="'ecom:track:setting'" @click="settingModal.open()">
            <template #icon><SettingOutlined /></template>{{ $t('ecomOrd.setting.title') }}
          </a-button>
          <span class="batch-hint">{{ $t('ecomOrdUi.track.hint') }}</span>
        </template>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :row-selection="rowSelection"
               :scroll="{ x: scrollX + 32, y: '100%' }" :locale="{ emptyText: $t('ecomOrdUi.track.empty') }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'packageNo'">
            <a class="pkg-no" @click="openDetail(record)">{{ record.packageNo }}</a>
            <div class="sub-text ellipsis">{{ record.orderNos }}<span v-if="record.orderCount > 1"> +{{ record.orderCount - 1 }}</span></div>
          </template>
          <template v-else-if="column.key === 'tracking'">
            <div>{{ record.trackingNo }}</div>
            <div class="sub-text">{{ record.carrierName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'trackStatus'">
            <DictTag type="ecom_track_status" :value="record.trackStatus || EcomTrackStatus.NOT_FOUND" />
          </template>
          <template v-else-if="column.key === 'trackAlert'">
            <DictTag v-if="record.trackAlert" type="ecom_track_alert" :value="record.trackAlert" /><span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'lastEvent'">
            <div class="ellipsis" :title="record.trackLastEvent">{{ record.trackLastEvent || '-' }}</div>
            <div class="sub-text">{{ record.trackLastTime || '' }}</div>
          </template>
          <template v-else-if="column.key === 'days'">{{ daysOf(record) }}</template>
          <template v-else-if="column.key === 'receiver'">
            <div class="ellipsis" :title="record.shipName">{{ record.shipName || '-' }}</div>
            <div class="sub-text"><span v-if="record.shipCountry" class="country">{{ record.shipCountry }}</span></div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'view', onClick: () => openDetail(record) },
              { key: 'pull', label: $t('ecomOrd.track.pull'), icon: SyncOutlined, perm: 'ecom:track:sync', onClick: () => pullOne(record.id) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" :title="detail ? `${detail.pkg.packageNo} · ${detail.pkg.trackingNo}` : ''" width="min(640px, 100vw)">
      <a-skeleton v-if="detailLoading && !detail" active :paragraph="{ rows: 8 }" />
      <template v-if="detail">
        <a-descriptions :column="2" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.pkg.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.pkg.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrd.track.shipTime')">{{ detail.pkg.shipTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrd.track.firstTime')">{{ detail.pkg.trackFirstTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrd.track.deliveredTime')">{{ detail.pkg.trackDeliveredTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrd.track.receiver')">{{ detail.pkg.shipName }} {{ detail.pkg.shipCountry }}</a-descriptions-item>
        </a-descriptions>
        <TrackTimeline :pkg="detail.pkg" :events="detail.events">
          <template #extra>
            <a v-if="can('ecom:track:sync')" @click="pullOne(detail.pkg.id)">{{ $t('ecomOrd.track.pull') }}</a>
          </template>
        </TrackTimeline>
      </template>
    </a-drawer>
    <TrackSettingModal ref="settingModal" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SettingOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import BatchActionBar from '@/components/ecom/BatchActionBar.vue'
import { actionColumn } from '@/components/common/tableActions'
import TrackTimeline from './TrackTimeline.vue'
import TrackSettingModal from './TrackSettingModal.vue'
import { getPackageTracks, pageTrack, pullPackageTracks, pullTracks, trackCounts } from '@/api/ecom/orderEnhance'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { EcomTrackStatus } from '@/constants/enums'
import { ORDER_RESULT_KEYS, showBatchResult } from '../order/order'

/**
 * 物流跟踪：已发货包裹的轨迹（物流商接口或 17TRACK），按状态分标签页，「预警」页列出超时未上网、运输超时、轨迹异常的包裹；
 * 定时任务每 2 小时自动拉取，也可选中后手工拉取。右上角「跟踪设置」配置 17TRACK 密钥、预警时限与审核前地址校验
 */
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const dict = useDict('ecom_track_status', 'ecom_track_alert')
const ALERT_TAB = 'alert'

const columns = [
  { title: t('ecomOrder.pkg.packageNo'), key: 'packageNo', dataIndex: 'packageNo', width: 190 },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('ecomOrder.common.trackingNo'), key: 'tracking', dataIndex: 'trackingNo', width: 190 },
  { title: t('ecomOrd.track.status'), key: 'trackStatus', dataIndex: 'trackStatus', width: 100, dict: 'ecom_track_status' },
  { title: t('ecomOrd.track.alert'), key: 'trackAlert', dataIndex: 'trackAlert', width: 110, dict: 'ecom_track_alert' },
  { title: t('ecomOrd.track.lastEvent'), key: 'lastEvent', dataIndex: 'trackLastEvent', width: 280 },
  { title: t('ecomOrd.track.shipTime'), dataIndex: 'shipTime', width: 165 },
  { title: t('ecomOrd.track.days'), key: 'days', width: 80, align: 'right', exportValue: (r) => daysOf(r) },
  {
    title: t('ecomOrd.track.receiver'),
    key: 'receiver',
    width: 140,
    exportColumns: [
      { title: t('ecomOrd.track.receiver'), dataIndex: 'shipName' },
      { title: t('ecomOrder.order.country'), dataIndex: 'shipCountry' }
    ]
  },
  { title: t('ecomOrder.common.channel'), dataIndex: 'channelName', width: 140, ellipsis: true },
  actionColumn(2, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order-track')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, keyword: undefined, range: undefined, channelId: undefined, alert: undefined, country: undefined })
const query = reactive(emptyQuery())
const tab = ref('all')
const counts = ref({})

const baseParams = ({ range, country, ...rest }) => ({ ...rest, country: country?.trim().toUpperCase() || undefined, beginDate: range?.[0], endDate: range?.[1] })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageTrack, query, {
  params: (q) => ({
    ...baseParams(q),
    alertOnly: tab.value === ALERT_TAB || undefined,
    trackStatus: tab.value !== 'all' && tab.value !== ALERT_TAB ? tab.value : undefined
  })
})

const tabs = computed(() => {
  const c = counts.value
  const alertCount = Object.entries(c).filter(([k]) => k.startsWith('A:')).reduce((s, [, v]) => s + v, 0)
  return [
    { key: 'all', label: t('ecomOrd.track.tabAll'), count: Object.entries(c).filter(([k]) => k.startsWith('S:')).reduce((s, [, v]) => s + v, 0) },
    { key: ALERT_TAB, label: t('ecomOrd.track.tabAlert'), count: alertCount },
    ...(dict.ecom_track_status || []).map((d) => ({ key: d.value, label: d.label, count: c[`S:${d.value}`] || 0 }))
  ]
})

async function loadCounts() {
  counts.value = (await trackCounts(baseParams({ ...query }))) || {}
}
function reload() {
  load()
  loadCounts()
}
function search() {
  doSearch()
  loadCounts()
}
function onTab() {
  selectedKeys.value = []
  doSearch()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(reload)

const daysOf = (r) => (r.shipTime ? dayjs(r.trackDeliveredTime || undefined).diff(dayjs(r.shipTime), 'day') : '-')

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await getPackageTracks(record.id)
  } finally {
    detailLoading.value = false
  }
}
const batchActions = computed(() => [
  { key: 'pull', label: t('ecomOrd.track.pullSelected'), icon: SyncOutlined, perm: 'ecom:track:sync', primary: true, loading: pulling.value, run: pullSelected }
])

const pulling = ref(false)
async function pullSelected() {
  pulling.value = true
  try {
    const ids = [...selectedKeys.value]
    showBatchResult(await pullTracks(ids), ids.length, ORDER_RESULT_KEYS)
    selectedKeys.value = []
    reload()
  } finally {
    pulling.value = false
  }
}
async function pullOne(id) {
  const res = await pullPackageTracks(id)
  message.success(t('ecomOrd.track.pulled'))
  if (detailOpen.value && detail.value?.pkg.id === id) detail.value = res
  reload()
}

const settingModal = ref()
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pkg-no {
  font-weight: 500;
}
.country {
  padding: 0 4px;
  border: 1px solid var(--app-border);
  border-radius: 3px;
  color: var(--app-text-2);
  font-size: 11px;
}
.batch-hint {
  align-self: center;
  color: var(--app-text-3);
  font-size: 12px;
}
.status-tabs {
  margin-top: -12px;
}
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
.tab-count {
  display: inline-block;
  min-width: 20px;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--app-hover);
  color: var(--app-text-2);
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
.status-tabs :deep(.ant-tabs-tab-active) .tab-count {
  background: var(--app-primary-bg, #e6f4ff);
  color: var(--app-primary, #1677ff);
}
.status-tabs .tab-count.tab-count-alert {
  background: rgba(255, 77, 79, 0.1);
  color: var(--ant-color-error, #ff4d4f);
}
.alert-tab {
  color: var(--ant-color-error, #ff4d4f);
}
.block {
  margin-bottom: 16px;
}
</style>
