<template>
  <div class="tab-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomCs.common.allStores')" class="filter" />
      <a-select v-model:value="query.rating" allow-clear :placeholder="$t('ecomCs.review.allRatings')" :options="ratingFilters()" class="filter-sm" />
      <a-select v-model:value="query.handleStatus" allow-clear :placeholder="$t('ecomCs.review.handleStatus')" :options="toOptions(dict.ecom_cs_review_status)" class="filter-sm" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :presets="presets" />
      <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.feedback.keywordPh')" class="filter" @press-enter="search" />
      <a-button type="primary" @click="search">
        <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
      </a-button>
    </div>
    <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:csReview:import'" type="primary" @click="importOpen = true">
        <template #icon><ImportOutlined /></template>{{ $t('ecomCs.feedback.import') }}
      </a-button>
      <span class="sub-text">{{ $t('ecomCs.feedback.tip') }}</span>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-class-name="(r) => (r.rating <= LOW_RATING ? 'low-row' : '')"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'rating'">
          <a-rate :value="record.rating" disabled class="rate" />
        </template>
        <template v-else-if="column.key === 'comments'">
          <a class="ellipsis block" :title="record.comments" @click="reply.open('feedback', record)">{{ record.comments || '-' }}</a>
          <div v-if="record.response" class="sub-text ellipsis" :title="record.response">↳ {{ record.response }}</div>
        </template>
        <template v-else-if="column.key === 'platformOrderNo'">
          <router-link v-if="record.orderId" :to="{ path: '/ecom/order', query: { id: record.orderId } }">{{ record.platformOrderNo }}</router-link>
          <span v-else>{{ record.platformOrderNo || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'handleStatus'">
          <DictTag type="ecom_cs_review_status" :value="record.handleStatus" />
          <div v-if="record.handlerName" class="sub-text">{{ record.handlerName }}</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>

    <ImportModal v-model:open="importOpen" :title="$t('ecomCs.feedback.importTitle')" :columns="importColumns" :sample="importSample" :importer="importCsFeedback" @success="load" />
    <ReplyModal ref="reply" @success="load" />
    <HandleModal ref="handle" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckSquareOutlined, ImportOutlined, MessageOutlined, SearchOutlined, ToolOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { deleteCsFeedback, importCsFeedback, pageCsFeedback } from '@/api/ecom/cs'
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
 * 店铺反馈（亚马逊 Feedback Manager 报告等）：导入、低分待处理并提醒店铺负责人、登记卖家回复、处理、转售后。focusId 变化时打开该反馈
 */
const props = defineProps({ focusId: Number })
const emit = defineEmits(['after-sale'])
const { t } = useI18n()
const dict = useDict('ecom_cs_review_status')
const presets = rangePresets()

const columns = [
  { title: t('ecomCs.review.date'), dataIndex: 'feedbackDate', width: 110 },
  { title: t('ecomCs.common.store'), dataIndex: 'storeName', width: 140 },
  { title: t('ecomCs.review.rating'), key: 'rating', dataIndex: 'rating', width: 150 },
  { title: t('ecomCs.feedback.comments'), key: 'comments', width: 360, exportValue: (r) => r.comments },
  { title: t('ecomCs.feedback.rater'), dataIndex: 'rater', width: 160 },
  { title: t('ecomCs.review.order'), key: 'platformOrderNo', dataIndex: 'platformOrderNo', width: 180 },
  { title: t('ecomCs.review.handleStatus'), key: 'handleStatus', dataIndex: 'handleStatus', width: 120, dict: 'ecom_cs_review_status' },
  actionColumn(4, [t('ecomCs.review.reply')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-cs-feedback')

const query = reactive({ storeId: undefined, rating: undefined, handleStatus: undefined, range: null, keyword: '' })
const toParams = (q) => ({
  storeId: q.storeId || undefined,
  handleStatus: q.handleStatus || undefined,
  keyword: q.keyword?.trim() || undefined,
  ...ratingParams(q.rating),
  ...rangeParams(q.range)
})
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCsFeedback, query, { params: toParams })

const reply = ref()
const handle = ref()
const importOpen = ref(false)
const remove = useAction(deleteCsFeedback, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const rowActions = (record) => [
  { key: 'reply', label: t('ecomCs.review.reply'), icon: MessageOutlined, perm: 'ecom:csReview:reply', onClick: () => reply.value.open('feedback', record) },
  { key: 'handle', label: t('ecomCs.review.handle'), icon: CheckSquareOutlined, perm: 'ecom:csReview:edit', onClick: () => handle.value.open('feedback', record) },
  {
    key: 'afterSale',
    label: t('ecomCs.ticket.action'),
    icon: ToolOutlined,
    perm: 'ecom:csTicket:add',
    onClick: () => emit('after-sale', { sourceType: CsSourceType.FEEDBACK, sourceId: record.id, orderId: record.orderId })
  },
  { type: 'delete', perm: 'ecom:csReview:remove', more: true, confirm: { title: t('ecomCs.feedback.deleteConfirm'), okType: 'danger' }, onClick: () => remove(record.id) }
]

/** 跳转过来时：在第一页中找到该反馈并打开（待处理的排在前面，提醒的低分反馈一般在第一页） */
async function focus(id) {
  const found = list.value.find((r) => r.id === id)
  if (found) reply.value.open('feedback', found)
}
watch(() => props.focusId, (id) => id && focus(id))

// ---------------------------------------------------------------- 导入（列与后端 FeedbackImportRow 一致）
const importColumns = [
  { title: t('ecomCs.common.storeCode'), key: 'storeCode', required: true, width: 12 },
  { title: t('ecomCs.review.date'), key: 'feedbackDate', required: true, type: 'date', width: 14 },
  { title: t('ecomCs.review.rating'), key: 'rating', required: true, type: 'int', width: 8 },
  { title: t('ecomCs.feedback.comments'), key: 'comments', width: 40 },
  { title: t('ecomCs.feedback.response'), key: 'response', width: 30 },
  { title: t('ecomCs.review.order'), key: 'platformOrderNo', width: 20 },
  { title: t('ecomCs.feedback.rater'), key: 'rater', width: 24 },
  { title: t('ecomCs.feedback.feedbackId'), key: 'feedbackId', width: 18 }
]
const importSample = {
  storeCode: 'US01',
  feedbackDate: '2026-10-01',
  rating: '1',
  comments: t('ecomCs.feedback.sampleComments'),
  response: '',
  platformOrderNo: '111-1234567-1234567',
  rater: 'buyer@example.com',
  feedbackId: ''
}

onMounted(async () => {
  await load()
  if (props.focusId) focus(props.focusId)
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
.tab-body :deep(.low-row) > td {
  background: rgba(255, 77, 79, 0.04);
}
</style>
