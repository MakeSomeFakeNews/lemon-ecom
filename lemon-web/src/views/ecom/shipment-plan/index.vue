<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomCatalog.plan.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="query.status" class="status-tabs" @change="search">
        <a-tab-pane key="">
          <template #tab>{{ $t('ecomCatalog.plan.allStatus') }}<span class="tab-count">{{ allCount }}</span></template>
        </a-tab-pane>
        <a-tab-pane v-for="s in dict.ecom_shipment_plan_status" :key="s.value">
          <template #tab>{{ s.label }}<span class="tab-count">{{ counts[s.value] || 0 }}</span></template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:shipmentPlan:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomCatalog.plan.addTitle') }}
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
          <template v-if="column.key === 'planNo'">
            <a @click="detailRef.open(record.id)">{{ record.planNo }}</a>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div>{{ record.fromWarehouseName || '-' }}</div>
            <div class="sub-text">→ {{ record.toWarehouseName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'shippedQty'">
            <div>{{ qty(record.shippedQty) }}</div>
            <a-progress :percent="shipPct(record)" :show-info="false" size="small" :status="shipPct(record) >= 100 ? 'success' : 'normal'" class="mini-bar" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_shipment_plan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <PlanForm ref="formRef" @success="afterSave" />
    <PlanDetail ref="detailRef" @changed="load" @edit="(r) => formRef.openEdit(r)" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import PlanForm from './PlanForm.vue'
import PlanDetail from './PlanDetail.vue'
import { deleteShipmentPlan, pageShipmentPlan, shipmentPlanStatusCounts } from '@/api/ecom/shipmentPlan'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { qty } from '@/utils/number'
import { canDelete, isDraft } from './plan'

/** 发货计划：草稿（手工或补货建议生成）→ 确认 → 生成头程货件（可拆成多个货件） */
const { t } = useI18n()
const dict = useDict('ecom_shipment_plan_status')

const columns = [
  { title: t('ecomCatalog.plan.planNo'), key: 'planNo', dataIndex: 'planNo', width: 160, fixed: 'left' },
  { title: t('ecom.common.store'), dataIndex: 'storeName', width: 150, ellipsis: true },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_shipment_plan_status' },
  { title: t('ecomCatalog.plan.warehouse'), key: 'warehouse', width: 200 },
  { title: t('ecomCatalog.plan.planDate'), dataIndex: 'planDate', width: 110 },
  { title: t('ecomCatalog.plan.totalQty'), key: 'totalQty', dataIndex: 'totalQty', width: 110, align: 'right' },
  { title: t('ecomCatalog.plan.shippedQty'), key: 'shippedQty', dataIndex: 'shippedQty', width: 110, align: 'right' },
  { title: t('ecomCatalog.common.createBy'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-shipment-plan')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, status: '', keyword: undefined })
const query = reactive(emptyQuery())
const route = useRoute()
if (route.query.status) query.status = String(route.query.status)
const shipPct = (r) => (Number(r.totalQty) ? Math.min(100, Math.round(((Number(r.shippedQty) || 0) / Number(r.totalQty)) * 100)) : 0)
const counts = ref({})
const allCount = computed(() => Object.values(counts.value).reduce((n, v) => n + Number(v || 0), 0))
/** 标签页计数：条件与列表相同、不含状态，列表每次加载时一起刷新 */
const fetchPage = (p) => {
  shipmentPlanStatusCounts({ storeId: query.storeId || undefined, keyword: query.keyword || undefined }).then((c) => (counts.value = c || {})).catch(() => {})
  return pageShipmentPlan(p)
}
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(fetchPage, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const detailRef = ref()
function afterSave() {
  load()
  detailRef.value?.reload()
}
const remove = useAction(deleteShipmentPlan, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'detail', onClick: () => detailRef.value.open(record.id) },
  { type: 'edit', perm: 'ecom:shipmentPlan:edit', show: isDraft(record), onClick: () => formRef.value.openEdit(record) },
  { type: 'delete', perm: 'ecom:shipmentPlan:remove', show: canDelete(record), confirm: t('ecomCatalog.plan.deleteConfirm', { no: record.planNo }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.tab-count {
  margin-left: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.mini-bar {
  margin: 0;
  line-height: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
