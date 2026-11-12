<template>
  <div class="cs-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomCs.common.allStores')" class="filter" @update:value="search" />
        <a-select v-model:value="query.platform" allow-clear :placeholder="$t('ecomCs.common.allPlatforms')" :options="toOptions(dict.ecom_platform)" class="filter-sm" @change="search" />
        <IdentityUserSelect v-model:value="query.ownerId" :placeholder="$t('ecomCs.message.owner')" class="filter" @update:value="search" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.message.keywordPh')" class="filter" @press-enter="search" />
        <a-checkbox v-model:checked="query.unread" @change="search">{{ $t('ecomCs.message.unreadOnly') }}</a-checkbox>
        <a-button type="primary" @click="search">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
        </a-button>
        <a-tooltip :title="query.storeId ? '' : $t('ecomCs.message.syncNeedStore')">
          <a-button v-perm="'ecom:csMessage:sync'" :loading="syncing" :disabled="!query.storeId" @click="sync">
            <template #icon><SyncOutlined /></template>{{ $t('ecomCs.message.sync') }}
          </a-button>
        </a-tooltip>
      </div>
      <a-segmented v-model:value="tab" :options="tabs" class="tabs" @change="search" />
    </a-card>

    <div class="cs-body">
      <a-card :bordered="false" class="page-card col-list" :body-style="{ padding: 0, height: '100%' }">
        <ThreadList ref="listRef" :query="params" :selected-id="selectedId" @select="onSelect" />
      </a-card>
      <a-card :bordered="false" class="page-card col-chat" :body-style="{ padding: 0, height: '100%' }">
        <ChatWindow v-if="selectedId" :thread-id="selectedId" @changed="onChanged" @after-sale="openAfterSale" />
        <a-empty v-else class="empty" :description="$t('ecomCs.message.selectThread')" />
      </a-card>
      <a-card :bordered="false" class="page-card col-order" :body-style="{ padding: 0, height: '100%' }">
        <OrderPanel v-if="selectedId" :thread-id="selectedId" :refresh-key="orderKey" @linked="onChanged" />
        <a-empty v-else class="empty" :description="$t('ecomCs.order.none')" />
      </a-card>
    </div>

    <AfterSaleModal ref="afterSale" @success="onChanged" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined, SyncOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AfterSaleModal from '@/components/ecom/cs/AfterSaleModal.vue'
import { csThreadCounts, syncCsMessages } from '@/api/ecom/cs'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { CsSourceType, CsThreadStatus } from '@/constants/enums'
import ThreadList from './ThreadList.vue'
import ChatWindow from './ChatWindow.vue'
import OrderPanel from './OrderPanel.vue'

/**
 * 买家消息（统一收件箱）：左侧会话列表（按待回复 / 超时 / 已回复 / 已关闭筛选，店铺、平台、负责人、关键字），
 * 中间聊天窗口（消息、快捷回复模板、发送 / 只登记、失败重发、负责人与标签、关闭、转售后），右侧订单信息（关联订单、买家最近订单）。
 * 选择店铺后可手工同步该店铺的消息（定时任务每 10 分钟同步一次）
 */
const { t } = useI18n()
const route = useRoute()
const dict = useDict('ecom_platform')

const query = reactive({ storeId: undefined, platform: undefined, ownerId: undefined, keyword: '', unread: false })
const tab = ref(route.query.tab ? String(route.query.tab) : CsThreadStatus.PENDING)
const counts = ref({})
const tabs = computed(() => [
  { value: CsThreadStatus.PENDING, label: `${t('ecomCs.message.tabPending')} ${counts.value.PENDING ?? ''}` },
  { value: 'OVERDUE', label: `${t('ecomCs.message.tabOverdue')} ${counts.value.OVERDUE ?? ''}` },
  { value: CsThreadStatus.REPLIED, label: t('ecomCs.message.tabReplied') },
  { value: CsThreadStatus.CLOSED, label: t('ecomCs.message.tabClosed') },
  { value: 'ALL', label: `${t('ecomCs.message.tabAll')} ${counts.value.ALL ?? ''}` }
])

const base = () => ({
  storeId: query.storeId || undefined,
  platform: query.platform || undefined,
  ownerId: query.ownerId || undefined,
  keyword: query.keyword?.trim() || undefined,
  unread: query.unread || undefined
})
const toParams = () => ({
  ...base(),
  status: [CsThreadStatus.PENDING, CsThreadStatus.REPLIED, CsThreadStatus.CLOSED].includes(tab.value) ? tab.value : undefined,
  overdue: tab.value === 'OVERDUE' || undefined
})
const params = ref(toParams())
const selectedId = ref(Number(route.query.id) || null)
const orderKey = ref(0)
const listRef = ref()
const afterSale = ref()

async function loadCounts() {
  counts.value = (await csThreadCounts(base())) || {}
}

function search() {
  params.value = toParams()
  loadCounts()
}

function onSelect(thread) {
  selectedId.value = thread.id
}

/** 会话有变化（回复、关闭、转售后等）：刷新列表与计数 */
function onChanged() {
  listRef.value?.reload(true)
  loadCounts()
  orderKey.value++
}

function openAfterSale(thread) {
  afterSale.value.open({ sourceType: CsSourceType.THREAD, sourceId: thread.id, orderId: thread.orderId })
}

const syncing = ref(false)
async function sync() {
  syncing.value = true
  try {
    const r = await syncCsMessages(query.storeId)
    if (r.message) message.warning(t('ecomCs.common.syncFailed', { msg: r.message }))
    else message.success(t('ecomCs.message.synced', { n: r.created }))
    onChanged()
  } finally {
    syncing.value = false
  }
}

onMounted(loadCounts)
</script>

<style scoped>
.cs-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 200px;
}
.filter-sm {
  width: 150px;
}
.tabs {
  margin-top: 12px;
}
.cs-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 320px;
  gap: 16px;
}
.cs-body > .page-card {
  min-height: 0;
  overflow: hidden;
}
.cs-body > .page-card + .page-card {
  margin-top: 0;
}
.empty {
  padding-top: 120px;
}
@media (max-width: 1280px) {
  .cs-body {
    grid-template-columns: 280px minmax(0, 1fr);
  }
  .col-order {
    display: none;
  }
}
@media (max-width: 768px) {
  .cs-body {
    grid-template-columns: 1fr;
    grid-auto-rows: minmax(360px, auto);
  }
}
</style>
