<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.diff.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.diff.source')" name="sourceType">
          <a-select v-model:value="query.sourceType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_diff_source)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.diff.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_diff_status)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.diff.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <span class="sub-text">{{ $t('ecomOwh.diff.pageTip') }}</span>
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
          <template v-if="column.key === 'source'">
            <DictTag type="ecom_owh_diff_source" :value="record.sourceType" />
            <a @click="openSource(record)">{{ record.sourceNo }}</a>
            <div v-if="record.refNo" class="sub-text">{{ record.refNo }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }}</div>
            <div class="sub-text">{{ record.materialName }}</div>
          </template>
          <template v-else-if="['expectedQty', 'receivedQty'].includes(column.key)">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'diffQty'">
            <span :class="Number(record.diffQty) > 0 ? 'loss' : 'gain'">{{ Number(record.diffQty) > 0 ? '-' : '+' }}{{ qty(Math.abs(Number(record.diffQty))) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_diff_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'docs'">
            <div>{{ record.diffDocNo || '-' }}</div>
            <div v-if="record.handleDocNo" class="sub-text">{{ record.handleDocNo }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="handleOpen" :title="$t('ecomOwh.diff.handleTitle')" :confirm-loading="saving" @ok="doHandle">
      <p v-if="current">{{ $t('ecomOwh.diff.handleTip', { no: current.sourceNo, material: `${current.materialCode} ${current.materialName || ''}`, diff: qty(Math.abs(Number(current.diffQty))) }) }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomOwh.diff.action')">
          <a-radio-group v-model:value="action">
            <template v-if="current && Number(current.diffQty) > 0">
              <a-radio :value="OwhDiffStatus.LOSS">{{ current.sourceType === OwhDiffSource.TRANSFER ? $t('ecomOwh.diff.lossTransfer') : $t('ecomOwh.diff.loss') }}</a-radio>
              <a-radio :value="OwhDiffStatus.FOUND">{{ $t('ecomOwh.diff.found') }}</a-radio>
            </template>
            <a-radio v-else :value="OwhDiffStatus.ACCEPTED">{{ $t('ecomOwh.diff.accept') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.diff.reason')" :required="action === OwhDiffStatus.LOSS">
          <a-input v-model:value="reason" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { handleOwhDiff, pageOwhDiff } from '@/api/ecom/owh'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { OwhDiffSource, OwhDiffStatus } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { qty } from '@/utils/number'

/**
 * 收货差异：入库单、退件单、调拨单的实收与单据数量不一致的物料。ERP 只入实收（入库单 / 调拨单短少的已差异出库，退件按实收入库）；
 * 短少可确认损耗（调拨为调出方损耗，入库关联头程货件）或找回补收（其他入库），溢收确认
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_owh_diff_status', 'ecom_owh_diff_source')
const SOURCE_PAGES = { [OwhDiffSource.INBOUND]: '/ecom/owh-inbound', [OwhDiffSource.RETURN]: '/ecom/owh-return', [OwhDiffSource.TRANSFER]: '/ecom/owh-transfer' }

const columns = [
  { title: t('ecomOwh.diff.source'), key: 'source', width: 200, exportValue: (r) => `${r.sourceNo} ${r.refNo || ''}` },
  { title: t('ecomOwh.diff.warehouse'), dataIndex: 'warehouseName', width: 140, ellipsis: true },
  { title: t('ecomOwh.stock.material'), key: 'material', width: 200, exportValue: (r) => `${r.materialCode} ${r.materialName || ''}` },
  { title: t('ecomOwh.diff.expected'), key: 'expectedQty', dataIndex: 'expectedQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.received'), key: 'receivedQty', dataIndex: 'receivedQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.diff'), key: 'diffQty', dataIndex: 'diffQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_owh_diff_status' },
  { title: t('ecomOwh.diff.reason'), dataIndex: 'reason', width: 180, ellipsis: true },
  { title: t('ecomOwh.diff.diffDoc'), key: 'docs', width: 160, exportValue: (r) => r.diffDocNo || '' },
  { title: t('ecomOwh.diff.createTime'), dataIndex: 'createTime', width: 160 },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-diff')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, sourceType: undefined, status: OwhDiffStatus.PENDING, warehouseId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageOwhDiff, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
const openSource = (r) => router.push({ path: SOURCE_PAGES[r.sourceType], query: { id: r.sourceId } })

const handleOpen = ref(false)
const saving = ref(false)
const current = ref(null)
const action = ref()
const reason = ref('')
function openHandle(r) {
  current.value = r
  action.value = Number(r.diffQty) > 0 ? OwhDiffStatus.LOSS : OwhDiffStatus.ACCEPTED
  reason.value = r.reason || ''
  handleOpen.value = true
}
async function doHandle() {
  saving.value = true
  try {
    await handleOwhDiff(current.value.id, { action: action.value, reason: reason.value || null })
    message.success(t('ecomOwh.diff.handled'))
    handleOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
const rowActions = (r) => [
  { key: 'handle', label: t('ecomOwh.diff.handle'), icon: CheckOutlined, perm: 'ecom:owhDiff:handle', show: r.status === OwhDiffStatus.PENDING, onClick: () => openHandle(r) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.loss {
  color: #ff4d4f;
}
.gain {
  color: #52c41a;
}
</style>
