<template>
  <div class="tab-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomCs.common.allStores')" class="filter" @update:value="search" />
      <a-select v-model:value="query.rating" allow-clear :placeholder="$t('ecomCs.review.allRatings')" :options="ratingFilters()" class="filter-sm" @change="search" />
      <a-select v-model:value="query.handleStatus" allow-clear :placeholder="$t('ecomCs.review.handleStatus')" :options="toOptions(dict.ecom_cs_review_status)" class="filter-sm" @change="search" />
      <a-select v-model:value="query.replied" allow-clear :placeholder="$t('ecomCs.review.replied')" :options="repliedOptions" class="filter-sm" @change="search" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :presets="presets" @change="search" />
      <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.review.keywordPh')" class="filter" @press-enter="search" />
      <a-button type="primary" @click="search">
        <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
      </a-button>
    </div>
    <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:csReview:import'" type="primary" @click="importOpen = true">
        <template #icon><ImportOutlined /></template>{{ $t('ecomCs.review.import') }}
      </a-button>
      <a-tooltip :title="query.storeId ? $t('ecomCs.review.syncTip') : $t('ecomCs.message.syncNeedStore')">
        <a-button v-perm="'ecom:csReview:sync'" :disabled="!query.storeId" :loading="syncing" @click="sync">
          <template #icon><SyncOutlined /></template>{{ $t('ecomCs.review.sync') }}
        </a-button>
      </a-tooltip>
      <a-button v-if="selectedKeys.length" v-perm="'ecom:csReview:remove'" danger @click="removeSelected">
        <template #icon><DeleteOutlined /></template>{{ $t('ecomCs.common.deleteN', { n: selectedKeys.length }) }}
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
      :row-class-name="(r) => (r.rating <= LOW_RATING ? 'low-row' : '')"
      :scroll="{ x: scrollX + 32, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'rating'">
          <a-rate :value="record.rating" disabled class="rate" />
        </template>
        <template v-else-if="column.key === 'sku'">
          <div>{{ record.sellerSku || record.platformItemId || '-' }}</div>
          <div v-if="record.listingTitle" class="sub-text ellipsis" :title="record.listingTitle">{{ record.listingTitle }}</div>
        </template>
        <template v-else-if="column.key === 'content'">
          <a class="ellipsis block" :title="record.content" @click="reply.open('review', record)">
            <b v-if="record.title">{{ record.title }}</b> {{ record.content || '-' }}
          </a>
          <div v-if="record.replied" class="sub-text ellipsis" :title="record.replyContent">↳ {{ record.replyContent }}</div>
        </template>
        <template v-else-if="column.key === 'platformOrderNo'">
          <router-link v-if="record.orderId" :to="{ path: '/ecom/order', query: { id: record.orderId } }">{{ record.platformOrderNo }}</router-link>
          <span v-else>{{ record.platformOrderNo || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'handleStatus'">
          <DictTag type="ecom_cs_review_status" :value="record.handleStatus" />
          <div v-if="record.handlerName" class="sub-text">{{ record.handlerName }}</div>
        </template>
        <template v-else-if="column.key === 'source'">{{ $t(`ecomCs.review.source.${record.source}`) }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>

    <ImportModal v-model:open="importOpen" :title="$t('ecomCs.review.importTitle')" :columns="importColumns" :sample="importSample" :importer="importCsReviews" @success="load" />
    <ReplyModal ref="reply" @success="load" />
    <HandleModal ref="handle" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { CheckSquareOutlined, DeleteOutlined, ImportOutlined, MessageOutlined, SearchOutlined, SyncOutlined, ToolOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { deleteCsReview, getCsReview, importCsReviews, pageCsReview, syncCsReviews } from '@/api/ecom/cs'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { CsSourceType } from '@/constants/enums'
import { LOW_RATING, ratingFilters, ratingParams } from './review'
import ReplyModal from './ReplyModal.vue'
import HandleModal from './HandleModal.vue'

/**
 * 评价列表：差评（不超过 3 星）标红并排在前面（待处理优先）；导入评价报告、同步（平台支持时）、回复（同步来的调用平台接口，导入的只登记）、
 * 处理（状态、处理人、说明）、转售后。focusId 变化时打开该评价的回复窗口
 */
const props = defineProps({ focusId: Number })
const emit = defineEmits(['after-sale'])
const { t } = useI18n()
const dict = useDict('ecom_cs_review_status')
const presets = rangePresets()
const repliedOptions = [
  { value: 1, label: t('ecomCs.review.repliedYes') },
  { value: 0, label: t('ecomCs.review.repliedNo') }
]

const columns = [
  { title: t('ecomCs.review.date'), dataIndex: 'reviewDate', width: 110 },
  { title: t('ecomCs.common.store'), dataIndex: 'storeName', width: 140 },
  { title: t('ecomCs.review.sku'), key: 'sku', width: 180, exportValue: (r) => r.sellerSku || r.platformItemId },
  { title: t('ecomCs.review.rating'), key: 'rating', dataIndex: 'rating', width: 150 },
  { title: t('ecomCs.review.content'), key: 'content', width: 360, exportValue: (r) => [r.title, r.content].filter(Boolean).join(' ') },
  { title: t('ecomCs.review.reviewer'), dataIndex: 'reviewer', width: 120 },
  { title: t('ecomCs.review.order'), key: 'platformOrderNo', dataIndex: 'platformOrderNo', width: 180 },
  { title: t('ecomCs.review.handleStatus'), key: 'handleStatus', dataIndex: 'handleStatus', width: 120, dict: 'ecom_cs_review_status' },
  { title: t('ecomCs.review.sourceTitle'), key: 'source', width: 90, exportValue: (r) => t(`ecomCs.review.source.${r.source}`) },
  actionColumn(4, [t('ecomCs.review.reply')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-cs-review')

// 看板「差评待处理」跳转时带 handleStatus 作为初始筛选
const initialHandle = useRoute().query.handleStatus
const query = reactive({ storeId: undefined, rating: undefined, handleStatus: initialHandle ? String(initialHandle) : undefined, replied: undefined, range: null, keyword: '' })
const toParams = (q) => ({
  storeId: q.storeId || undefined,
  handleStatus: q.handleStatus || undefined,
  replied: q.replied ?? undefined,
  keyword: q.keyword?.trim() || undefined,
  ...ratingParams(q.rating),
  ...rangeParams(q.range)
})
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageCsReview, query, { params: toParams })

const reply = ref()
const handle = ref()
const importOpen = ref(false)

const syncing = ref(false)
async function sync() {
  syncing.value = true
  try {
    const r = await syncCsReviews(query.storeId)
    if (r.message) message.warning(t('ecomCs.common.syncFailed', { msg: r.message }))
    else message.success(t('ecomCs.review.synced', { n: r.created, m: r.updated }))
    load()
  } finally {
    syncing.value = false
  }
}

const remove = useAction(deleteCsReview, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const removeSelected = useAction(() => deleteCsReview(selectedKeys.value), {
  confirm: () => ({ title: t('ecomCs.common.deleteNConfirm', { n: selectedKeys.value.length }), okType: 'danger' }),
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    selectedKeys.value = []
    load()
  }
})

const rowActions = (record) => [
  { key: 'reply', label: t('ecomCs.review.reply'), icon: MessageOutlined, perm: 'ecom:csReview:reply', onClick: () => reply.value.open('review', record) },
  { key: 'handle', label: t('ecomCs.review.handle'), icon: CheckSquareOutlined, perm: 'ecom:csReview:edit', onClick: () => handle.value.open('review', record) },
  {
    key: 'afterSale',
    label: t('ecomCs.ticket.action'),
    icon: ToolOutlined,
    perm: 'ecom:csTicket:add',
    onClick: () => emit('after-sale', { sourceType: CsSourceType.REVIEW, sourceId: record.id, orderId: record.orderId })
  },
  { type: 'delete', perm: 'ecom:csReview:remove', more: true, confirm: { title: t('ecomCs.review.deleteConfirm'), okType: 'danger' }, onClick: () => remove(record.id) }
]

watch(
  () => props.focusId,
  async (id) => {
    if (id) reply.value.open('review', await getCsReview(id))
  }
)

// ---------------------------------------------------------------- 导入（列与后端 ReviewImportRow 一致）
const importColumns = [
  { title: t('ecomCs.common.storeCode'), key: 'storeCode', required: true, width: 12 },
  { title: t('ecomCs.review.reviewId'), key: 'reviewId', width: 18 },
  { title: t('ecomCs.review.sellerSku'), key: 'sellerSku', width: 16 },
  { title: t('ecomCs.review.platformItemId'), key: 'platformItemId', width: 16 },
  { title: t('ecomCs.review.rating'), key: 'rating', required: true, type: 'int', width: 8 },
  { title: t('ecomCs.review.title'), key: 'title', width: 24 },
  { title: t('ecomCs.review.content'), key: 'content', width: 40 },
  { title: t('ecomCs.review.reviewer'), key: 'reviewer', width: 14 },
  { title: t('ecomCs.review.date'), key: 'reviewDate', required: true, type: 'date', width: 14 },
  { title: t('ecomCs.review.order'), key: 'platformOrderNo', width: 20 },
  { title: t('ecomCs.review.replyContent'), key: 'replyContent', width: 30 }
]
const importSample = {
  storeCode: 'US01',
  reviewId: 'R1A2B3C4D5',
  sellerSku: 'SKU-001',
  platformItemId: 'B000000001',
  rating: '2',
  title: t('ecomCs.review.sampleTitle'),
  content: t('ecomCs.review.sampleContent'),
  reviewer: 'John',
  reviewDate: '2026-10-01',
  platformOrderNo: '111-1234567-1234567',
  replyContent: ''
}

onMounted(async () => {
  await load()
  if (props.focusId) reply.value.open('review', await getCsReview(props.focusId))
})
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
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.filter {
  width: 200px;
}
.filter-sm {
  width: 140px;
}
.rate {
  font-size: 13px;
}
.rate :deep(.ant-rate-star:not(:last-child)) {
  margin-inline-end: 2px;
}
.ellipsis {
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.block {
  display: block;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
/* 不透明的浅红底：固定列（操作列）不会透出下面滚动的内容 */
.tab-body :deep(.low-row) > td {
  background: color-mix(in srgb, #ff4d4f 5%, var(--app-card));
}
.tab-body :deep(.low-row:hover) > td {
  background: color-mix(in srgb, #ff4d4f 9%, var(--app-card));
}
</style>
