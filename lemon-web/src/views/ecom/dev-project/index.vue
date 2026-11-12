<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_dev_status)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomDev.project.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomDev.project.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomDev.project.source')" name="source">
          <a-select v-model:value="query.source" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_dev_source)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomDev.project.owner')" name="ownerId">
          <UserSelect v-model:value="query.ownerId" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:devProject:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomDev.project.add') }}
        </a-button>
        <a-button @click="router.push('/ecom/dev-board')"><template #icon><ProjectOutlined /></template>{{ $t('ecomDev.project.board') }}</a-button>
        <a-button v-perm="'ecom:profitCalc:list'" @click="router.push('/ecom/profit-calc')">
          <template #icon><CalculatorOutlined /></template>{{ $t('ecomDev.project.calc') }}
        </a-button>
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
          <template v-if="column.key === 'product'">
            <div class="product">
              <img v-if="imagesOf(record)[0]" :src="imagesOf(record)[0]" class="thumb" alt="" />
              <div v-else class="thumb empty"><PictureOutlined /></div>
              <div class="info">
                <a class="ellipsis" :title="record.name" @click="openDetail(record)">{{ record.name }}</a>
                <div class="sub-text ellipsis">{{ record.docNo }}<span v-if="record.categoryName"> · {{ record.categoryName }}</span></div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'platform'">
            <DictTag type="ecom_platform" :value="record.platform" /> <span>{{ record.marketplace || '' }}</span>
          </template>
          <template v-else-if="column.key === 'source'"><DictTag type="ecom_dev_source" :value="record.source" /></template>
          <template v-else-if="column.key === 'targetPrice'">
            {{ record.targetPrice != null ? `${record.currency || ''} ${money(record.targetPrice)}` : '-' }}
            <div v-if="record.competitorPrice != null" class="sub-text">{{ $t('ecomDev.project.competitorShort', { v: money(record.competitorPrice) }) }}</div>
          </template>
          <template v-else-if="column.key === 'estPurchasePrice'">{{ record.estPurchasePrice != null ? money(record.estPurchasePrice) : '-' }}</template>
          <template v-else-if="column.key === 'estMargin'">
            <span v-if="record.estMargin != null" :class="['margin', marginClass(record.estMargin)]">{{ Number(record.estMargin) }}%</span>
            <span v-else class="sub-text">-</span>
            <div v-if="record.estProfit != null" class="sub-text">¥ {{ money(record.estProfit) }}</div>
          </template>
          <template v-else-if="column.key === 'samples'">
            <span v-if="record.sampleCount">{{ record.passedSampleCount }} / {{ record.sampleCount }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_dev_status" :value="record.status" />
            <div v-if="record.materialCode" class="sub-text">{{ record.materialCode }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ProjectForm ref="form" @success="afterChange" />
    <ProjectDetail ref="detail" @changed="load" @edit="(r) => form.openEdit(r)" />
    <RemarkModal ref="remarkModal" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { CalculatorOutlined, PictureOutlined, PlusOutlined, ProjectOutlined, SendOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import UserSelect from '@/components/user/UserSelect.vue'
import ProjectForm from './ProjectForm.vue'
import ProjectDetail from './ProjectDetail.vue'
import RemarkModal from './RemarkModal.vue'
import { pageDevProject } from '@/api/ecom/dev'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { canDelete, canManualApprove, canSubmit, imagesOf, isEditable, marginClass, MOVES } from './dev'
import { useDevActions } from './useDevActions'

/**
 * 开发项目：候选产品从调研、打样到审批通过生成物料的全过程。列表按状态、平台、来源、负责人筛选；
 * 详情中维护样品、利润试算（回写试算利润率）、流转记录与附件
 */
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const dict = useDict('ecom_dev_status', 'ecom_platform', 'ecom_dev_source')

const columns = [
  { title: t('ecomDev.project.product'), key: 'product', dataIndex: 'name', width: 260, fixed: 'left' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_dev_status' },
  { title: t('ecomDev.project.platform'), key: 'platform', width: 120, exportValue: (r) => `${r.platform || ''} ${r.marketplace || ''}` },
  { title: t('ecomDev.project.source'), key: 'source', dataIndex: 'source', width: 100, dict: 'ecom_dev_source' },
  { title: t('ecomDev.project.targetPrice'), key: 'targetPrice', dataIndex: 'targetPrice', width: 120, align: 'right' },
  { title: t('ecomDev.project.estPurchasePrice'), key: 'estPurchasePrice', dataIndex: 'estPurchasePrice', width: 100, align: 'right' },
  { title: t('ecomDev.project.estMargin'), key: 'estMargin', dataIndex: 'estMargin', width: 100, align: 'right' },
  { title: t('ecomDev.project.estMonthlySales'), dataIndex: 'estMonthlySales', width: 90, align: 'right' },
  { title: t('ecomDev.project.samples'), key: 'samples', width: 110, align: 'center', exportValue: (r) => `${r.passedSampleCount || 0}/${r.sampleCount || 0}` },
  { title: t('ecomDev.project.owner'), dataIndex: 'ownerName', width: 100, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 160 },
  actionColumn(4, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-dev-project')
const showSearch = ref(true)

const emptyQuery = () => ({ status: undefined, keyword: undefined, platform: undefined, source: undefined, ownerId: undefined })
const query = reactive(emptyQuery())
if (route.query.status) query.status = String(route.query.status)
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageDevProject, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detail = ref()
const remarkModal = ref()
const openDetail = (record) => detail.value.open(record.id)
function afterChange() {
  load()
  detail.value?.reload()
}
useRouteOpen({ id: (id) => detail.value.open(id), keyword: (kw) => (query.keyword = kw), reload: () => search() })
onMounted(load)

const { move, submit, approve, reject, remove } = useDevActions(remarkModal, () => load())

const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', perm: 'ecom:devProject:edit', show: isEditable(record), onClick: () => form.value.openEdit(record) },
  { key: 'submit', label: t('ecomDev.action.submit'), icon: SendOutlined, perm: 'ecom:devProject:edit', show: canSubmit(record), onClick: () => submit(record) },
  { type: 'approve', label: t('ecomDev.action.approve'), perm: 'ecom:devProject:approve', show: canManualApprove(record), onClick: () => approve(record) },
  { type: 'reject', label: t('ecomDev.action.reject'), perm: 'ecom:devProject:approve', show: canManualApprove(record), more: true, onClick: () => reject(record) },
  { type: 'move', label: t('ecomDev.action.move'), perm: 'ecom:devProject:edit', show: (MOVES[record.status] || []).length > 0, more: true, onClick: () => move(record) },
  { type: 'delete', perm: 'ecom:devProject:remove', show: canDelete(record), more: true, onClick: () => remove(record) }
]
</script>

<style scoped>
.product {
  display: flex;
  align-items: center;
  gap: 8px;
}
.thumb {
  border: 1px solid var(--app-border);
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  flex: none;
}
.thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-hover);
  color: var(--app-text-3);
}
.info {
  min-width: 0;
  flex: 1;
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
.margin.good {
  color: #389e0d;
}
.margin.low {
  color: #d48806;
}
.margin.loss {
  color: #cf1322;
}
</style>
