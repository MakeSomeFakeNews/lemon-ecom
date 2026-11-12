<template>
  <div class="tab-body">
    <a-alert type="info" show-icon class="tip" :message="$t('ecomCs.request.tip')" />
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomCs.common.allStores')" class="filter" />
      <a-select v-model:value="query.state" :options="stateOptions" class="filter-sm" />
      <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.request.keywordPh')" class="filter" @press-enter="search" />
      <a-button type="primary" @click="search">
        <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
      </a-button>
      <a-button v-perm="'ecom:csReview:request'" :disabled="!selectedKeys.length" :loading="requesting" @click="requestSelected">
        <template #icon><StarOutlined /></template>{{ $t('ecomCs.request.requestN', { n: selectedKeys.length }) }}
      </a-button>
    </div>
    <a-table
      row-key="orderId"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="selection"
      :scroll="{ x: 1100, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'platformOrderNo'">
          <router-link :to="{ path: '/ecom/order', query: { id: record.orderId } }">{{ record.platformOrderNo }}</router-link>
        </template>
        <template v-else-if="column.key === 'shipTime'">
          {{ record.shipTime || '-' }}
          <div class="sub-text">{{ $t('ecomCs.request.daysAgo', { n: daysAgo(record.shipTime) }) }}</div>
        </template>
        <template v-else-if="column.key === 'requestStatus'">
          <a-tag v-if="record.requestStatus" :color="STATUS_COLORS[record.requestStatus]" :bordered="false">{{ $t(`ecomCs.request.status.${record.requestStatus}`) }}</a-tag>
          <span v-else class="sub-text">{{ $t('ecomCs.request.notRequested') }}</span>
          <div v-if="record.requestMessage" class="sub-text ellipsis" :title="record.requestMessage">{{ record.requestMessage }}</div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { SearchOutlined, StarOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { pageCsReviewRequest, requestCsReviews } from '@/api/ecom/cs'
import { useTable } from '@/composables/useTable'
import { CsRequestStatus, EcomPlatform } from '@/constants/enums'

/**
 * 请求评价（亚马逊 Solicitations）：发货 5–30 天的订单与请求结果，勾选后逐张请求；已成功请求过的不能再选，
 * 不符合条件（平台判定）、失败的可以再次请求。定时任务「自动请求评价」默认停用，开启后每天自动请求
 */
const { t } = useI18n()
const STATUS_COLORS = { [CsRequestStatus.SENT]: 'green', [CsRequestStatus.NOT_ELIGIBLE]: 'default', [CsRequestStatus.FAILED]: 'red' }
const stateOptions = [
  { value: 'pending', label: t('ecomCs.request.pendingOnly') },
  { value: 'all', label: t('ecomCs.request.all') },
  ...Object.values(CsRequestStatus).map((s) => ({ value: s, label: t(`ecomCs.request.status.${s}`) }))
]
const columns = [
  { title: t('ecomCs.review.order'), key: 'platformOrderNo', dataIndex: 'platformOrderNo', width: 200 },
  { title: t('ecomCs.common.store'), dataIndex: 'storeName', width: 160 },
  { title: t('ecomCs.request.buyer'), dataIndex: 'buyerName', width: 160 },
  { title: t('ecomCs.request.shipTime'), key: 'shipTime', width: 180 },
  { title: t('ecomCs.request.result'), key: 'requestStatus', width: 260 },
  { title: t('ecomCs.request.requestTime'), dataIndex: 'requestTime', width: 170 }
]

const query = reactive({ storeId: undefined, state: 'pending', keyword: '' })
const toParams = (q) => ({
  platform: EcomPlatform.AMAZON,
  storeId: q.storeId || undefined,
  keyword: q.keyword?.trim() || undefined,
  pendingOnly: q.state === 'pending',
  status: Object.values(CsRequestStatus).includes(q.state) ? q.state : undefined
})
const { list, loading, pagination, selectedKeys, load, search, onTableChange } = useTable(pageCsReviewRequest, query, { params: toParams })
const selection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  onChange: (keys) => (selectedKeys.value = keys),
  getCheckboxProps: (r) => ({ disabled: r.requestStatus === CsRequestStatus.SENT })
}))

const daysAgo = (v) => (v ? dayjs().diff(dayjs(v), 'day') : '-')

const requesting = ref(false)
async function requestSelected() {
  requesting.value = true
  try {
    const res = (await requestCsReviews(selectedKeys.value)) || []
    const count = (s) => res.filter((r) => r.status === s).length
    Modal.info({
      title: t('ecomCs.request.doneTitle'),
      content: t('ecomCs.request.doneContent', { sent: count(CsRequestStatus.SENT), notEligible: count(CsRequestStatus.NOT_ELIGIBLE), failed: count(CsRequestStatus.FAILED) })
    })
    selectedKeys.value = []
    load()
  } finally {
    requesting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tip {
  margin-bottom: 12px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.filter {
  width: 200px;
}
.filter-sm {
  width: 150px;
}
.ellipsis {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
