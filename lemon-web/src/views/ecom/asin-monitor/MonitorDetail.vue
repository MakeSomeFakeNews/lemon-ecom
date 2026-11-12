<template>
  <SubPage :open="open" :title="detail ? $t('ecomOper.monitor.detailTitle', { item: detail.itemId }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <DictTag type="ecom_monitor_type" :value="detail.monitorType" />
        <DictTag v-if="detail.listingStatus" type="ecom_listing_status" :value="detail.listingStatus" />
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <div class="head">
          <a-image v-if="detail.mainImage" :src="detail.mainImage" :width="120" :height="120" class="img" />
          <div class="head-text">
            <div class="title">{{ detail.title || detail.itemId }}</div>
            <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small">
              <a-descriptions-item :label="$t('ecomOper.common.platform')"><DictTag type="ecom_platform" :value="detail.platform" /> {{ detail.marketplace }}</a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.monitor.itemId')">{{ detail.itemId }}</a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.monitor.brand')">{{ detail.brand || '-' }}</a-descriptions-item>
              <a-descriptions-item v-if="detail.sellerSku" :label="$t('ecomOper.monitor.sellerSku')">{{ detail.sellerSku }}</a-descriptions-item>
              <a-descriptions-item v-if="detail.benchmarkItemId" :label="$t('ecomOper.monitor.benchmark')">{{ detail.benchmarkItemId }} {{ detail.benchmarkTitle || '' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.monitor.lastSnapshot')">{{ detail.lastSnapshotDate || '-' }}</a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.monitor.lastFetch')">
                {{ detail.lastFetchTime || '-' }}
                <span v-if="detail.lastFetchMessage" class="danger">{{ $t('ecomOper.monitor.fetchError', { msg: detail.lastFetchMessage }) }}</span>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('ecomOper.monitor.notifyUser')">{{ detail.notifyUserName || '-' }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </div>

        <a-row :gutter="[12, 12]" class="cards">
          <a-col v-for="c in cards" :key="c.key" :xs="12" :sm="8" :lg="4">
            <div class="card">
              <div class="label">{{ c.label }}</div>
              <div class="value" :class="c.cls">{{ c.value }}</div>
              <div class="sub-text ellipsis" :title="c.sub">{{ c.sub }}</div>
            </div>
          </a-col>
        </a-row>

        <div v-if="detail.bullets" class="block">
          <div class="section-title">{{ $t('ecomOper.monitor.bullets') }}</div>
          <ul class="bullets">
            <li v-for="(b, i) in detail.bullets.split('\n')" :key="i">{{ b }}</li>
          </ul>
        </div>

        <div class="section-head">
          <span class="section-title">{{ $t('ecomOper.monitor.trend') }}</span>
          <a-radio-group v-model:value="days" size="small" option-type="button" :options="dayOptions" @change="loadTrend" />
        </div>
        <a-empty v-if="!snapshots.length" :description="$t('ecomOper.monitor.noSnapshot')" />
        <a-row v-else :gutter="16">
          <a-col :xs="24" :lg="12">
            <EChart v-if="hasRank" :option="rankOption" height="260px" />
            <div v-else class="chart-empty">
              <div class="section-title small">{{ $t('ecomOper.monitor.rankTrend') }}</div>
              <a-empty :image="simpleImage" :description="$t('ecomUiOps.monitor.noRankData')" />
            </div>
          </a-col>
          <a-col :xs="24" :lg="12"><EChart :option="reviewOption" height="260px" /></a-col>
          <a-col :xs="24" :lg="12"><EChart :option="sellerOption" height="240px" /></a-col>
          <a-col :xs="24" :lg="12">
            <div class="section-title small">{{ $t('ecomOper.monitor.recentAlerts') }}</div>
            <a-table row-key="id" size="small" :columns="alertColumns" :data-source="alerts" :pagination="false" :scroll="{ x: 520, y: 200 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'alertType'"><DictTag type="ecom_monitor_alert_type" :value="record.alertType" /></template>
                <template v-else-if="column.key === 'change'">
                  <span class="ellipsis" :title="`${record.oldValue || ''} → ${record.newValue || ''}`">{{ record.oldValue }} → {{ record.newValue }}</span>
                </template>
                <template v-else-if="column.key === 'status'"><DictTag type="ecom_monitor_alert_status" :value="record.status" /></template>
              </template>
            </a-table>
          </a-col>
        </a-row>

        <div class="section-title">{{ $t('ecomOper.monitor.snapshots') }}</div>
        <a-table row-key="id" size="small" :columns="snapshotColumns" :data-source="snapshotsDesc" :pagination="{ pageSize: 10, size: 'small' }" :scroll="{ x: 980 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'rankMain'">{{ rankText(record.rankMain) }}</template>
            <template v-else-if="column.key === 'rankSub'">{{ rankText(record.rankSub) }}</template>
            <template v-else-if="column.key === 'listingStatus'"><DictTag v-if="record.listingStatus" type="ecom_listing_status" :value="record.listingStatus" /></template>
            <template v-else-if="column.key === 'source'"><DictTag type="ecom_monitor_source" :value="record.source" /></template>
            <template v-else-if="column.key === 'title'"><span class="ellipsis" :title="record.title">{{ record.title }}</span></template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :loading="a.loading" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { message, Empty } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CloudDownloadOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { fetchMonitors, getMonitor, monitorTrend } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'
import { changeText, isOwn, rankText, signed } from './monitor'

/** 监控详情子页：商品信息、最新指标、五点描述、趋势图（排名、评分与评论数、在售卖家与售价）、最近预警、快照记录。ref 调用 open(id)、reload() */
const emit = defineEmits(['changed', 'edit', 'record'])
const { t } = useI18n()
const userStore = useUserStore()
useDict('ecom_platform', 'ecom_monitor_type', 'ecom_listing_status', 'ecom_monitor_alert_type', 'ecom_monitor_alert_status', 'ecom_monitor_source')

const open = ref(false)
const loading = ref(false)
const fetching = ref(false)
const detail = ref()
const snapshots = ref([])
const alerts = ref([])
const days = ref(60)
const dayOptions = [30, 60, 90, 180].map((n) => ({ value: n, label: t('ecomOper.monitor.days', { n }) }))
let currentId

const snapshotsDesc = computed(() => [...snapshots.value].reverse())

const cards = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    { key: 'rankMain', label: t('ecomOper.monitor.rankMain'), value: rankText(d.rankMain), sub: [changeText(d.rankChange), d.category].filter(Boolean).join(' · '), cls: Number(d.rankChange) < 0 ? 'danger' : '' },
    { key: 'rankSub', label: t('ecomOper.monitor.rankSub'), value: rankText(d.rankSub), sub: d.subCategory },
    { key: 'rating', label: t('ecomOper.monitor.rating'), value: d.rating ?? '-', sub: signed(d.ratingChange, 1), cls: Number(d.ratingChange) < 0 ? 'danger' : '' },
    { key: 'reviewCount', label: t('ecomOper.monitor.reviewCount'), value: d.reviewCount ?? '-', sub: signed(d.reviewChange) },
    { key: 'sellerCount', label: t('ecomOper.monitor.sellerCount'), value: d.sellerCount ?? '-', sub: t('ecomOper.monitor.sellerCountTip'), cls: isOwn(d) && Number(d.sellerCount) > 1 ? 'danger' : '' },
    { key: 'price', label: t('ecomOper.monitor.price'), value: d.price == null ? '-' : money(d.price), sub: d.currency }
  ]
})

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
/** 有排名数据（部分平台没有类目排名）时才画排名趋势 */
const hasRank = computed(() => snapshots.value.some((s) => s.rankMain != null || s.rankSub != null))
const xAxis = () => ({ type: 'category', data: snapshots.value.map((s) => dayjs(s.snapshotDate).format('MM-DD')) })
const grid = { left: 8, right: 8, top: 36, bottom: 8, containLabel: true }
const rankOption = computed(() => ({
  title: { text: t('ecomOper.monitor.rankTrend'), textStyle: { fontSize: 13 } },
  tooltip: { trigger: 'axis' },
  legend: { top: 0, right: 0 },
  grid,
  xAxis: xAxis(),
  yAxis: [
    { type: 'value', inverse: true, minInterval: 1, scale: true },
    { type: 'value', inverse: true, minInterval: 1, scale: true, splitLine: { show: false } }
  ],
  series: [
    { name: t('ecomOper.monitor.rankMain'), type: 'line', smooth: true, connectNulls: true, data: snapshots.value.map((s) => s.rankMain) },
    { name: t('ecomOper.monitor.rankSub'), type: 'line', smooth: true, connectNulls: true, yAxisIndex: 1, data: snapshots.value.map((s) => s.rankSub) }
  ]
}))
const reviewOption = computed(() => ({
  title: { text: t('ecomOper.monitor.reviewTrend'), textStyle: { fontSize: 13 } },
  tooltip: { trigger: 'axis' },
  legend: { top: 0, right: 0 },
  grid,
  xAxis: xAxis(),
  yAxis: [
    { type: 'value', min: (v) => Math.max(0, Math.floor((v.min - 0.3) * 10) / 10), max: 5 },
    { type: 'value', minInterval: 1, scale: true, splitLine: { show: false } }
  ],
  series: [
    { name: t('ecomOper.monitor.rating'), type: 'line', smooth: true, connectNulls: true, itemStyle: { color: '#faad14' }, data: snapshots.value.map((s) => s.rating) },
    { name: t('ecomOper.monitor.reviewCount'), type: 'bar', barMaxWidth: 10, yAxisIndex: 1, itemStyle: { color: '#91caff' }, data: snapshots.value.map((s) => s.reviewCount) }
  ]
}))
const sellerOption = computed(() => ({
  title: { text: t('ecomOper.monitor.sellerTrend'), textStyle: { fontSize: 13 } },
  tooltip: { trigger: 'axis' },
  legend: { top: 0, right: 0 },
  grid,
  xAxis: xAxis(),
  yAxis: [
    { type: 'value', minInterval: 1 },
    { type: 'value', scale: true, splitLine: { show: false } }
  ],
  series: [
    { name: t('ecomOper.monitor.sellerCount'), type: 'line', step: 'end', connectNulls: true, itemStyle: { color: '#ff7875' }, data: snapshots.value.map((s) => s.sellerCount) },
    { name: t('ecomOper.monitor.price'), type: 'line', smooth: true, connectNulls: true, yAxisIndex: 1, itemStyle: { color: '#52c41a' }, data: snapshots.value.map((s) => s.price) }
  ]
}))

const alertColumns = [
  { title: t('ecomOper.alert.alertDate'), dataIndex: 'alertDate', width: 100 },
  { title: t('ecomOper.alert.alertType'), key: 'alertType', width: 100 },
  { title: t('ecomOper.alert.change'), key: 'change', ellipsis: true },
  { title: t('ecomOper.alert.status'), key: 'status', width: 90 }
]
const snapshotColumns = [
  { title: t('ecomOper.monitor.snapshotDate'), dataIndex: 'snapshotDate', width: 110 },
  { title: t('ecomOper.monitor.rankMain'), key: 'rankMain', width: 100, align: 'right' },
  { title: t('ecomOper.monitor.rankSub'), key: 'rankSub', width: 100, align: 'right' },
  { title: t('ecomOper.monitor.rating'), dataIndex: 'rating', width: 70, align: 'right' },
  { title: t('ecomOper.monitor.reviewCount'), dataIndex: 'reviewCount', width: 90, align: 'right' },
  { title: t('ecomOper.monitor.sellerCount'), dataIndex: 'sellerCount', width: 90, align: 'right' },
  { title: t('ecomOper.monitor.price'), dataIndex: 'price', width: 90, align: 'right' },
  { title: t('ecomOper.monitor.listingStatus'), key: 'listingStatus', width: 90 },
  { title: t('ecomOper.monitor.titleText'), key: 'title', width: 260 },
  { title: t('ecomOper.monitor.source'), key: 'source', width: 80 }
]

async function loadTrend() {
  const r = await monitorTrend(currentId, days.value)
  snapshots.value = r.snapshots || []
  alerts.value = r.alerts || []
}
async function load(id) {
  loading.value = true
  try {
    detail.value = await getMonitor(id)
    await loadTrend()
  } finally {
    loading.value = false
  }
}
function show(id) {
  currentId = id
  detail.value = undefined
  snapshots.value = []
  alerts.value = []
  open.value = true
  return load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)

async function fetchNow() {
  fetching.value = true
  try {
    const r = await fetchMonitors([currentId])
    message.success(t('ecomOper.monitor.fetched', r))
    if (r.message) message.warning(t('ecomOper.monitor.fetchFailed', { msg: r.message }), 6)
    await load(currentId)
    emit('changed')
  } finally {
    fetching.value = false
  }
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (userStore.hasPerm('ecom:monitor:edit')) list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  if (userStore.hasPerm('ecom:monitor:fetch')) list.push({ key: 'fetch', label: t('ecomOper.monitor.fetch'), icon: CloudDownloadOutlined, loading: fetching.value, run: fetchNow })
  if (userStore.hasPerm('ecom:monitor:snapshot')) list.push({ key: 'record', label: t('ecomOper.monitor.record'), icon: PlusOutlined, primary: true, run: () => emit('record', d) })
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.head {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}
.img {
  border-radius: 6px;
  object-fit: cover;
}
.head-text {
  flex: 1;
  min-width: 0;
}
.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.cards {
  margin-bottom: 12px;
}
.card {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-fill, #fafafa);
}
.label {
  color: var(--app-text-3);
  font-size: 12px;
}
.value {
  font-size: 20px;
  font-weight: 600;
}
.block {
  margin-bottom: 12px;
}
.bullets {
  margin: 0;
  padding-left: 18px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0 8px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.section-head .section-title {
  margin: 0;
}
.section-title.small {
  margin-top: 0;
}
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.chart-empty {
  height: 260px;
}
.chart-empty :deep(.ant-empty) {
  margin-top: 60px;
}
</style>
