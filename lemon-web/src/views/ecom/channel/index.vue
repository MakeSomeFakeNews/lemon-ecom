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

          <a-spin :spinning="loading">
            <a-empty v-if="!loading && !groups.length" :description="$t('ecom.hub.channel.noStores')" />
            <a-empty v-else-if="!loading && !visibleGroups.length" :description="$t('ecom.hub.channel.noProblems')" />
            <div v-for="g in visibleGroups" :key="g.platform" class="platform">
              <div class="platform-head">
                <DictTag type="ecom_platform" :value="g.platform" />
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
              <a-table row-key="id" size="small" :columns="columns" :data-source="g.stores" :pagination="false" :scroll="{ x: 1280 }">
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
                    <div v-if="record.authorizedTime" class="sub-text">{{ $t('ecom.hub.authorizedAt', { time: record.authorizedTime }) }}</div>
                  </template>
                  <template v-else-if="column.sync">
                    <SyncCell :health="syncOf(record, column.sync)" :off="syncOff(record, column.sync)" @click="openLogs(record, column.sync)" />
                  </template>
                  <template v-else-if="column.key === 'failures'">
                    <a v-if="record.failures24h" class="error-text" @click="openLogs(record, undefined, 0)">{{ record.failures24h }}</a>
                    <span v-else class="sub-text">0</span>
                  </template>
                  <template v-else-if="column.key === 'lastError'">
                    <a-tooltip v-if="record.lastSyncMessage" :title="record.lastSyncMessage">
                      <span class="error-text">{{ record.lastSyncMessage }}</span>
                    </a-tooltip>
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
import SyncCell from './SyncCell.vue'
import SyncLogTable from './SyncLogTable.vue'
import { channelOverview, pageChannelLogs } from '@/api/ecom/channel'
import { useDict } from '@/composables/useDict'
import { STATUS_ENABLED } from '@/utils/constants'
import { SyncType } from '@/constants/enums'

/**
 * 渠道接入：按平台分组列出全部店铺的授权状态、环境、各接口最近一次同步结果与时间、近 24 小时失败次数（GET /ecom/channel/overview），
 * 以及全部店铺的同步日志（GET /ecom/channel/logs）
 */
const { t, te } = useI18n()
useDict('ecom_platform', 'ecom_marketplace', 'ecom_sync_type', 'sys_common_status')

const tab = ref('overview')
const loading = ref(false)
const groups = ref([])
const onlyProblems = ref(false)
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

const SYNC_COLUMNS = [SyncType.ORDERS, SyncType.SHIP_CONFIRM, SyncType.INVENTORY_PUSH, SyncType.LISTINGS, SyncType.AUTH]
const capLabel = (v) => (te(`ecom.hub.cap.${v}`) ? t(`ecom.hub.cap.${v}`) : v)
const authTypeName = (v) => (te(`ecom.hub.form.authTypeName.${v}`) ? t(`ecom.hub.form.authTypeName.${v}`) : v)

const columns = [
  { title: t('ecom.common.store'), key: 'name', width: 200, fixed: 'left' },
  { title: t('ecom.hub.env'), key: 'env', width: 70 },
  { title: t('ecom.hub.authStatus'), key: 'auth', width: 170 },
  ...SYNC_COLUMNS.map((s) => ({ title: capLabel(s), key: `sync-${s}`, sync: s, width: 150 })),
  { title: t('ecom.hub.channel.failures24h'), key: 'failures', width: 100, align: 'right' },
  { title: t('ecom.hub.lastError'), key: 'lastError', ellipsis: true, width: 220 }
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
.platform {
  margin-bottom: 20px;
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
