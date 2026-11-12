<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.store.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecom.store.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:store:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecom.store.add') }}
        </a-button>
        <div v-if="summary.total" class="health-summary">
          <span class="chip"><span class="dot ok" />{{ $t('ecom.hub.summaryAuthorized') }} <b>{{ summary.authorized }}</b></span>
          <span class="chip" :class="{ warn: summary.reauth }"><span class="dot error" />{{ $t('ecom.hub.summaryReauth') }} <b>{{ summary.reauth }}</b></span>
          <span class="chip"><span class="dot idle" />{{ $t('ecom.hub.summaryNone') }} <b>{{ summary.none }}</b></span>
          <span class="chip" :class="{ warn: summary.error }"><span class="dot error" />{{ $t('ecom.hub.summaryError') }} <b>{{ summary.error }}</b></span>
          <router-link v-if="summary.reauth || summary.error" :to="{ path: '/ecom/channel', query: { problem: '1' } }" class="overview-link">{{ $t('ecom.hub.viewOverview') }} <RightOutlined /></router-link>
        </div>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="store-cell">
              <PlatformAvatar :platform="record.platform" />
              <div class="store-text">
                <div class="ellipsis" :title="record.name">
                  <a @click="openEdit(record)">{{ record.name }}</a>
                  <a-tag v-if="record.demo === 1" color="purple" class="demo-tag" :title="$t('ecom.store.demoTip')">{{ $t('ecom.store.demo') }}</a-tag>
                </div>
                <div class="sub-text ellipsis">{{ record.code }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'platform'">
            <DictTag type="ecom_platform" :value="record.platform" />
            <a-tooltip v-if="!byPlatform[record.platform]" :title="$t('ecom.hub.unsupportedTip')">
              <DisconnectOutlined class="sub-text" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'marketplace'">
            <DictTag type="ecom_marketplace" :value="record.marketplace" />
          </template>
          <template v-else-if="column.key === 'sandbox'">
            <a-tag v-if="record.sandbox === 1" color="orange" :bordered="false">{{ $t('ecom.hub.sandbox') }}</a-tag>
            <a-tag v-else color="blue" :bordered="false">{{ $t('ecom.hub.production') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'listingCount'">{{ num(record.listingCount) }}</template>
          <template v-else-if="column.key === 'authStatus'">
            <AuthStatusTag :status="record.authStatus" />
            <div v-if="record.authStatus === StoreAuthStatus.AUTHORIZED && record.authorizedTime" class="sub-text" :title="record.authorizedTime">
              {{ $t('ecom.hub.authorizedAt', { time: relativeTime(record.authorizedTime) }) }}
            </div>
          </template>
          <template v-else-if="column.key === 'health'">
            <a-tooltip placement="topLeft" :overlay-style="{ maxWidth: '420px' }">
              <template #title>
                <div v-for="s in healthOf(record).syncs" :key="s.type" class="health-tip-row">
                  <span class="dot" :class="s.success === false ? 'error' : 'ok'" />
                  {{ $t(`ecom.hub.cap.${s.type}`) }}：{{ s.time }}<template v-if="s.message"> · {{ s.message }}</template>
                </div>
                <div v-if="!healthOf(record).syncs.length">{{ $t('ecom.store.neverSynced') }}</div>
              </template>
              <div class="health-cell" @click="logRef.open(record)">
                <div class="health-line">
                  <span class="dot" :class="healthOf(record).level" />
                  <span v-if="healthOf(record).latest">
                    {{ $t(`ecom.hub.cap.${healthOf(record).latest.type}`) }} · {{ relativeTime(healthOf(record).latest.time) }}
                  </span>
                  <span v-else-if="record.lastSyncTime">{{ relativeTime(record.lastSyncTime) }}</span>
                  <span v-else class="sub-text">{{ $t('ecom.store.neverSynced') }}</span>
                  <a-tag v-if="healthOf(record).failures24h" color="error" :bordered="false" class="fail-count">
                    {{ $t('ecom.hub.failures24h', { n: healthOf(record).failures24h }) }}
                  </a-tag>
                </div>
                <div v-if="healthOf(record).failed" class="error-text">
                  <ExclamationCircleOutlined /> {{ $t('ecom.hub.failedAt', { action: $t(`ecom.hub.cap.${healthOf(record).failed.type}`) }) }}<template v-if="healthOf(record).failed.message">：{{ healthOf(record).failed.message }}</template>
                </div>
                <div v-else-if="record.lastSyncMessage" class="error-text"><ExclamationCircleOutlined /> {{ record.lastSyncMessage }}</div>
              </div>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <StoreForm ref="formRef" @success="load" />
    <StoreSyncDialog ref="syncRef" @done="load" />
    <SyncLogDrawer ref="logRef" />
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import {
  ApiOutlined,
  CloudUploadOutlined,
  DisconnectOutlined,
  ExclamationCircleOutlined,
  LinkOutlined,
  PlusOutlined,
  RightOutlined,
  SyncOutlined,
  TagsOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreForm from './StoreForm.vue'
import StoreSyncDialog from './StoreSyncDialog.vue'
import SyncLogDrawer from './SyncLogDrawer.vue'
import AuthStatusTag from './AuthStatusTag.vue'
import PlatformAvatar from './PlatformAvatar.vue'
import { relativeTime, syncHealth } from './health'
import { channelOverview } from '@/api/ecom/channel'
import { supports, useConnectors } from './connectors'
import { changeStoreStatus, deleteStore, pageStore, storeAuthorizeUrl, syncStoreListings, testStore } from '@/api/ecom/store'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { num } from '@/utils/number'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { AuthType, ConnectorCapability, StoreAuthStatus } from '@/constants/enums'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'ecom_platform', 'ecom_marketplace')
const { byPlatform } = useConnectors()

const authLabel = (s) => t(`ecom.hub.auth.${s || StoreAuthStatus.NONE}`)
const columns = [
  { title: t('ecom.common.store'), key: 'name', width: 230, fixed: 'left', exportValue: (r) => `${r.code} ${r.name}` },
  { title: t('ecom.store.platform'), key: 'platform', dataIndex: 'platform', width: 130, dict: 'ecom_platform' },
  { title: t('ecom.store.marketplace'), key: 'marketplace', dataIndex: 'marketplace', width: 90, dict: 'ecom_marketplace' },
  { title: t('ecom.hub.env'), key: 'sandbox', width: 80, exportValue: (r) => (r.sandbox === 1 ? t('ecom.hub.sandbox') : t('ecom.hub.production')) },
  { title: t('ecom.hub.authStatus'), key: 'authStatus', width: 190, exportValue: (r) => authLabel(r.authStatus) },
  { title: t('ecom.hub.health'), key: 'health', dataIndex: 'lastSyncTime', width: 280 },
  { title: t('ecom.store.currency'), dataIndex: 'currency', width: 80 },
  { title: t('ecom.store.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('ecom.store.warehouse'), dataIndex: 'warehouseName', width: 150, ellipsis: true },
  { title: t('ecom.store.platformWarehouse'), dataIndex: 'platformWarehouseName', width: 150, ellipsis: true },
  { title: t('ecom.store.listingCount'), key: 'listingCount', dataIndex: 'listingCount', width: 110, align: 'right' },
  { title: t('ecom.store.owner'), dataIndex: 'ownerName', width: 110 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  // 编辑、授权 / 测试、同步订单直接显示，其余在「更多」中
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-store')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, platform: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load: loadPage, search, onTableChange, exportAll } = useTable(pageStore, query)

/** 渠道总览：各店铺各接口最近一次同步结果，用于同步健康列和顶部统计 */
const overview = ref([])
const overviewStores = computed(() => overview.value.flatMap((p) => p.stores || []))
const healthMap = computed(() => Object.fromEntries(overviewStores.value.map((s) => [s.id, s])))
const healthOf = (record) => syncHealth(healthMap.value[record.id], record.authStatus)
const summary = computed(() => {
  const all = overviewStores.value
  return {
    total: all.length,
    authorized: all.filter((s) => s.authStatus === StoreAuthStatus.AUTHORIZED).length,
    reauth: all.filter((s) => s.authStatus === StoreAuthStatus.REAUTH).length,
    none: all.filter((s) => !s.authStatus || s.authStatus === StoreAuthStatus.NONE).length,
    error: all.filter((s) => syncHealth(s, s.authStatus).level === 'error').length
  }
})
const loadOverview = () =>
  channelOverview()
    .then((res) => (overview.value = res || []))
    .catch(() => {})
function load() {
  loadOverview()
  return loadPage()
}
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const openEdit = (record) => (userStore.hasPerm('ecom:store:edit') ? formRef.value.openEdit(record) : logRef.value.open(record))
const syncRef = ref()
const logRef = ref()

const remove = useAction(deleteStore, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

/** 启用 / 停用 */
const toggleStatus = useAction((record) => changeStoreStatus(record.id, record.status === STATUS_ENABLED ? STATUS_DISABLED : STATUS_ENABLED), {
  confirm: (record) => {
    const enable = record.status !== STATUS_ENABLED
    return {
      title: enable ? t('components.status.confirmEnable') : t('components.status.confirmDisable'),
      content: enable ? t('components.status.enableContent', { name: record.name }) : t('components.status.disableContent', { name: record.name }),
      okType: enable ? 'primary' : 'danger'
    }
  },
  successKey: (_res, record) => (record.status !== STATUS_ENABLED ? t('components.status.enabled') : t('components.status.disabled')),
  reload: () => load()
})

/** 正在执行的操作：店铺 ID + 操作，按钮显示加载中 */
const running = ref('')
async function runFor(record, key, fn) {
  running.value = `${record.id}:${key}`
  try {
    await fn()
  } finally {
    running.value = ''
  }
}

/** 测试渠道授权：成功提示授权有效，失败由拦截器提示后端返回的原因；结果写授权日志，刷新授权状态 */
const test = (record) =>
  runFor(record, 'test', async () => {
    try {
      await testStore(record.id)
      message.success(t('ecom.store.testOk', { name: record.name }))
    } finally {
      load()
    }
  })

/** OAuth 授权：新窗口打开平台授权页，卖家同意后平台回调本系统保存令牌；回到这里刷新授权状态 */
const authorize = (record) =>
  runFor(record, 'auth', async () => {
    const url = await storeAuthorizeUrl(record.id)
    window.open(url, '_blank', 'noopener')
    Modal.confirm({
      title: t('ecom.hub.authorizeTitle'),
      content: h('div', [
        h('p', t('ecom.hub.authorizeContent')),
        h('a', { href: url, target: '_blank', rel: 'noopener noreferrer' }, [h(LinkOutlined), ' ', t('ecom.hub.authorizeLink')])
      ]),
      okText: t('ecom.hub.authorizeDone'),
      onOk: () => load()
    })
  })

/** 拉取在线商品 */
const syncListings = (record) =>
  runFor(record, 'listings', async () => {
    const r = await syncStoreListings(record.id)
    if (r.message) {
      Modal.warning({ title: t('ecom.hub.actionFailed', { name: record.name, action: t('ecom.hub.syncListings') }), content: r.message })
    } else {
      message.success(t('ecom.hub.syncListingsDone', { name: record.name, fetched: r.fetched, created: r.created, updated: r.updated }))
    }
    load()
  })

const rowActions = (record) => {
  const enabled = record.status === STATUS_ENABLED
  const connector = byPlatform.value[record.platform]
  const oauth = connector?.authType === AuthType.OAUTH
  const noCred = !record.hasCredentials
  const unsupported = !connector ? t('ecom.hub.unsupportedTip') : undefined
  /** 需要授权、启用、平台支持该能力的操作 */
  const gate = (capability) => {
    if (!connector) return { disabled: true, tip: unsupported }
    if (capability && !supports(connector, capability)) return { disabled: true, tip: t('ecom.hub.channel.capUnsupported') }
    if (noCred) return { disabled: true, tip: t('ecom.store.needCred') }
    if (!enabled) return { disabled: true, tip: t('ecom.store.needEnabled') }
    return {}
  }
  const loadingOf = (key) => running.value === `${record.id}:${key}`
  return [
    { type: 'edit', perm: 'ecom:store:edit', onClick: () => formRef.value.openEdit(record) },
    oauth
      ? {
          key: 'auth',
          label: t('ecom.hub.authorize'),
          icon: ApiOutlined,
          perm: 'ecom:store:auth',
          disabled: !enabled,
          tip: !enabled ? t('ecom.store.needEnabled') : undefined,
          loading: loadingOf('auth'),
          onClick: () => authorize(record)
        }
      : {
          key: 'test',
          label: t('ecom.store.testAuth'),
          icon: ApiOutlined,
          perm: 'ecom:store:sync',
          ...(connector ? (noCred ? { disabled: true, tip: t('ecom.store.needCred') } : {}) : { disabled: true, tip: unsupported }),
          loading: loadingOf('test'),
          onClick: () => test(record)
        },
    {
      key: 'sync',
      label: t('ecom.hub.syncOrders'),
      icon: SyncOutlined,
      perm: 'ecom:store:sync',
      ...gate(ConnectorCapability.ORDERS),
      onClick: () => syncRef.value.open(record, 'orders')
    },
    {
      key: 'listings',
      label: t('ecom.hub.syncListings'),
      icon: TagsOutlined,
      perm: 'ecom:store:syncListing',
      more: true,
      ...gate(ConnectorCapability.LISTINGS),
      loading: loadingOf('listings'),
      onClick: () => syncListings(record)
    },
    {
      key: 'push',
      label: t('ecom.hub.pushInventory'),
      icon: CloudUploadOutlined,
      perm: 'ecom:store:push',
      more: true,
      ...gate(ConnectorCapability.INVENTORY_PUSH),
      onClick: () => syncRef.value.open(record, 'inventory')
    },
    ...(oauth
      ? [
          {
            key: 'test',
            label: t('ecom.store.testAuth'),
            icon: ApiOutlined,
            perm: 'ecom:store:sync',
            more: true,
            ...(noCred ? { disabled: true, tip: t('ecom.store.needCred') } : {}),
            loading: loadingOf('test'),
            onClick: () => test(record)
          }
        ]
      : []),
    { type: 'log', key: 'logs', label: t('ecom.hub.logs'), more: true, onClick: () => logRef.value.open(record) },
    { type: enabled ? 'disable' : 'enable', key: 'toggle', perm: 'ecom:store:edit', more: true, onClick: () => toggleStatus(record) },
    { type: 'delete', perm: 'ecom:store:remove', more: true, confirm: t('ecom.store.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
  ]
}
</script>

<style scoped>
.demo-tag {
  margin-left: 6px;
}
.store-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.store-text {
  flex: 1;
  min-width: 0;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.health-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 10px;
  border-radius: 14px;
  background: var(--app-bg);
  color: var(--app-text-2);
  font-size: 13px;
}
.chip b {
  color: var(--app-text);
}
.chip.warn b {
  color: #cf1322;
}
.overview-link {
  font-size: 13px;
}
.dot {
  display: inline-block;
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bfbfbf;
}
.dot.ok {
  background: #52c41a;
}
.dot.error {
  background: #ff4d4f;
}
.health-cell {
  cursor: pointer;
  min-width: 0;
}
.health-line {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.fail-count {
  margin-inline-end: 0;
  white-space: nowrap;
}
.health-tip-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: var(--ant-color-error, #ff4d4f);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
