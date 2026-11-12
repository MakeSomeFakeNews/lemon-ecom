<template>
  <div class="channel-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="overview" :tab="$t('ecom.hub.channel.overview')">
          <div class="toolbar">
            <a-alert type="info" show-icon :message="$t('ecom.hub.channel.tip')" class="tip" />
            <div class="toolbar-actions">
              <a-switch v-model:checked="onlyProblems" size="small" />
              <span>{{ $t('ecom.hub.channel.onlyProblems') }}</span>
              <a-button :loading="loading" @click="load">
                <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
              </a-button>
            </div>
          </div>

          <div v-if="groups.length" class="stats">
            <div v-for="c in statCards" :key="c.key" class="stat" :class="[c.tone, { clickable: !!c.onClick, active: c.key === 'problem' && onlyProblems }]" @click="c.onClick && c.onClick()">
              <div class="stat-label">{{ c.label }}</div>
              <div class="stat-value">{{ c.value }}</div>
            </div>
          </div>
          <a-spin :spinning="loading">
            <a-empty v-if="!loading && !groups.length" :description="$t('ecom.hub.channel.noStores')" />
            <a-empty v-else-if="!loading && !visibleGroups.length" :description="$t('ecom.hub.channel.noProblems')" />
            <div v-for="g in visibleGroups" :key="g.platform" class="platform">
              <div class="platform-head">
                <PlatformAvatar :platform="g.platform" :size="28" />
                <span class="platform-name">{{ platformName(g.platform) }}</span>
                <template v-if="g.supported">
                  <a-tag color="blue" :bordered="false">{{ authTypeName(g.authType) }}</a-tag>
                  <a-tag v-for="c in g.capabilities" :key="c" :bordered="false">{{ capLabel(c) }}</a-tag>
                  <a v-if="g.docUrl" :href="g.docUrl" target="_blank" rel="noopener noreferrer" class="doc-link">
                    <LinkOutlined /> {{ $t('ecom.hub.form.docs') }}
                  </a>
                </template>
                <a-tooltip v-else :title="$t('ecom.hub.channel.notSupportedTip')">
                  <a-tag :bordered="false"><DisconnectOutlined /> {{ $t('ecom.hub.channel.notSupported') }}</a-tag>
                </a-tooltip>
                <span class="sub-text">{{ $t('ecom.hub.channel.storesCount', { n: g.allCount }) }}</span>
                <span v-if="g.problemCount" class="error-text">{{ $t('ecom.hub.channel.problemCount', { n: g.problemCount }) }}</span>
              </div>
              <a-table row-key="id" size="small" :columns="columns" :data-source="g.stores" :pagination="false" :scroll="{ x: sumWidth(columns) }"
                       :row-class-name="(r) => (r.problem ? 'row-problem' : '')">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'name'">
                    <a @click="openLogs(record)">{{ record.name }}</a>
                    <DictTag v-if="record.status !== STATUS_ENABLED" type="sys_common_status" :value="record.status" class="status-tag" />
                    <div class="sub-text">{{ record.code }} · <DictTag type="ecom_marketplace" :value="record.marketplace" /></div>
                  </template>
                  <template v-else-if="column.key === 'env'">
                    <a-tag v-if="record.sandbox" color="orange" :bordered="false">{{ $t('ecom.hub.sandbox') }}</a-tag>
                    <a-tag v-else color="blue" :bordered="false">{{ $t('ecom.hub.production') }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'auth'">
                    <AuthStatusTag :status="record.authStatus" />
                    <div v-if="record.authorizedTime" class="sub-text" :title="record.authorizedTime">{{ $t('ecom.hub.authorizedAt', { time: relativeTime(record.authorizedTime) }) }}</div>
                  </template>
                  <template v-else-if="column.sync">
                    <SyncCell :health="syncOf(record, column.sync)" :off="syncOff(record, column.sync)" @click="openLogs(record, column.sync)" />
                  </template>
                  <template v-else-if="column.key === 'failures'">
                    <a v-if="record.failures24h" class="error-text" @click="openLogs(record, undefined, 0)">{{ record.failures24h }}</a>
                    <span v-else class="sub-text">0</span>
                  </template>
                  <template v-else-if="column.key === 'lastError'">
                    <a-tooltip v-if="lastError(record)" :title="lastError(record)">
                      <span class="error-text">{{ lastError(record) }}</span>
                    </a-tooltip>
                    <span v-else class="sub-text">-</span>
                  </template>
                </template>
              </a-table>
            </div>
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="logs" :tab="$t('ecom.hub.channel.allLogs')" force-render>
          <SyncLogTable ref="logTable" :api="pageChannelLogs" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DisconnectOutlined, LinkOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AuthStatusTag from '../store/AuthStatusTag.vue'
import PlatformAvatar from '../store/PlatformAvatar.vue'
import { relativeTime } from '../store/health'
import { sumWidth } from '@/composables/useColumns'
import { useRoute } from 'vue-router'
import SyncCell from './SyncCell.vue'
import SyncLogTable from './SyncLogTable.vue'
import { channelOverview, pageChannelLogs } from '@/api/ecom/channel'
import { useDict } from '@/composables/useDict'
import { STATUS_ENABLED } from '@/utils/constants'
import { StoreAuthStatus, SyncType } from '@/constants/enums'

/**
 * 渠道接入：按平台分组列出全部店铺的授权状态、环境、各接口最近一次同步结果与时间、近 24 小时失败次数（GET /ecom/channel/overview），
 * 以及全部店铺的同步日志（GET /ecom/channel/logs）
 */
const { t, te } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace', 'ecom_sync_type', 'sys_common_status')
const platformName = (p) => (dict.ecom_platform || []).find((d) => d.value === p)?.label || p

const tab = ref('overview')
const loading = ref(false)
const groups = ref([])
// ?problem=1：从店铺列表等页面跳转过来时只看有问题的店铺
const onlyProblems = ref(useRoute().query.problem === '1')
const logTable = ref()

async function load() {
  loading.value = true
  try {
    groups.value = (await channelOverview()) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const visibleGroups = computed(() =>
  groups.value
    .map((g) => ({
      ...g,
      allCount: g.stores.length,
      problemCount: g.stores.filter((s) => s.problem).length,
      stores: onlyProblems.value ? g.stores.filter((s) => s.problem) : g.stores
    }))
    .filter((g) => g.stores.length)
)

const allStores = computed(() => groups.value.flatMap((g) => g.stores))
const statCards = computed(() => {
  const all = allStores.value
  return [
    { key: 'total', label: t('ecom.hub.channel.statStores'), value: all.length },
    { key: 'auth', label: t('ecom.hub.summaryAuthorized'), value: all.filter((s) => s.authStatus === StoreAuthStatus.AUTHORIZED).length, tone: 'ok' },
    { key: 'reauth', label: t('ecom.hub.summaryReauth'), value: all.filter((s) => s.authStatus === StoreAuthStatus.REAUTH).length, tone: 'bad' },
    { key: 'problem', label: t('ecom.hub.channel.statProblem'), value: all.filter((s) => s.problem).length, tone: 'bad', onClick: () => (onlyProblems.value = !onlyProblems.value) },
    { key: 'fail', label: t('ecom.hub.channel.failures24h'), value: all.reduce((n, s) => n + (s.failures24h || 0), 0), tone: 'bad' }
  ]
})
/** 最近失败原因：店铺记录的同步信息，没有时取最近一次失败的接口 */
const lastError = (record) =>
  record.lastSyncMessage ||
  (record.syncs || [])
    .filter((s) => s.success === false && s.message)
    .sort((a, b) => (a.time < b.time ? 1 : -1))[0]?.message

const SYNC_COLUMNS = [SyncType.ORDERS, SyncType.SHIP_CONFIRM, SyncType.INVENTORY_PUSH, SyncType.LISTINGS, SyncType.AUTH]
const capLabel = (v) => (te(`ecom.hub.cap.${v}`) ? t(`ecom.hub.cap.${v}`) : v)
const authTypeName = (v) => (te(`ecom.hub.form.authTypeName.${v}`) ? t(`ecom.hub.form.authTypeName.${v}`) : v)

const columns = [
  { title: t('ecom.common.store'), key: 'name', width: 200, fixed: 'left' },
  { title: t('ecom.hub.env'), key: 'env', width: 70 },
  { title: t('ecom.hub.authStatus'), key: 'auth', width: 170 },
  ...SYNC_COLUMNS.map((s) => ({ title: capLabel(s), key: `sync-${s}`, sync: s, width: 150 })),
  { title: t('ecom.hub.channel.failures24h'), key: 'failures', width: 100, align: 'right' },
  { title: t('ecom.hub.lastError'), key: 'lastError', ellipsis: true, width: 260 }
]

const syncOf = (record, type) => record.syncs.find((s) => s.type === type)
/** 自动同步开关关闭时的说明（只对订单、库存回传） */
const syncOff = (record, type) => {
  if (type === SyncType.ORDERS && !record.orderSync) return t('ecom.hub.channel.orderSyncOff')
  if (type === SyncType.INVENTORY_PUSH && !record.inventorySync) return t('ecom.hub.channel.inventorySyncOff')
  return undefined
}

/** 切换到同步日志并按店铺（及类型、结果）筛选 */
async function openLogs(record, syncType, success) {
  tab.value = 'logs'
  await nextTick()
  logTable.value?.filter({ storeId: record.id, syncType, success })
}
</script>

<style scoped>
.channel-page {
  height: 100%;
  overflow: auto;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.tip {
  flex: 1 1 360px;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.stat.clickable {
  cursor: pointer;
}
.stat.active {
  border-color: #ff4d4f;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 13px;
}
.stat-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 600;
  color: var(--app-text);
  line-height: 1.2;
}
.stat.ok .stat-value {
  color: #52c41a;
}
.stat.bad .stat-value {
  color: #ff4d4f;
}
.platform {
  margin-bottom: 16px;
  padding: 12px 12px 4px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.platform-name {
  margin-right: 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
}
.platform-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.doc-link {
  font-size: 12px;
}
.status-tag {
  margin-left: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: var(--ant-color-error, #ff4d4f);
  font-size: 12px;
}
</style>

<style>
.channel-page .ant-table-tbody > tr.row-problem > td:first-child {
  box-shadow: inset 3px 0 0 #ff4d4f;
}
</style>
