<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <OwhFilters :model="query" />
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.stock.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.stock.diffOnly')" name="diffOnly">
          <a-switch v-model:checked="query.diffOnly" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card summary-card">
      <div class="summary">
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.skuCount') }}</div>
          <div class="stat-value">{{ num(summary.skuCount) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.diffCount') }}</div>
          <div class="stat-value" :class="{ warn: Number(summary.diffCount) > 0 }">{{ num(summary.diffCount) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.available') }}</div>
          <div class="stat-value">{{ qty(summary.available) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.transit') }}</div>
          <div class="stat-value">{{ qty(summary.transit) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.locked') }}</div>
          <div class="stat-value">{{ qty(summary.locked) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.erpQty') }}</div>
          <div class="stat-value">{{ qty(summary.erpQty) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.stock.syncTime') }}</div>
          <div class="stat-time">{{ summary.syncTime || '-' }}</div>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhStock:sync'" type="primary" :loading="syncing" @click="sync">
          <template #icon><SyncOutlined /></template>{{ query.accountId ? $t('ecomOwh.stock.syncAccount') : $t('ecomOwh.stock.syncAll') }}
        </a-button>
        <a-button v-perm="'ecom:owhStock:adjust'" :disabled="!selectedKeys.length" @click="openAdjust">
          <template #icon><DiffOutlined /></template>{{ $t('ecomOwh.stock.adjust') }}
        </a-button>
        <span v-if="selectedKeys.length" class="selected-tip">
          {{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: scrollX + 32, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div>{{ record.sku }}</div>
            <div v-if="record.materialId" class="sub-text">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
            <a-tag v-else color="red" :bordered="false">{{ $t('ecomOwh.stock.unmatched') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <div>{{ record.warehouseName }}</div>
            <div class="sub-text">{{ record.accountName }} · {{ record.owhCode }}</div>
          </template>
          <template v-else-if="['availableQty', 'transitQty', 'lockedQty', 'erpQty', 'erpTransitQty'].includes(column.key)">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'diffQty'">
            <QtyDiff :value="record.materialId ? Number(record.diffQty) : null" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="adjustOpen" :title="$t('ecomOwh.stock.adjustTitle')" :confirm-loading="adjusting" @ok="doAdjust">
      <p>{{ $t('ecomOwh.stock.adjustTip', { n: selectedKeys.length }) }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomOwh.stock.bizDate')">
          <a-date-picker v-model:value="adjustDate" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { DiffOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import QtyDiff from '@/components/wms/QtyDiff.vue'
import OwhFilters from './OwhFilters.vue'
import { adjustOwhStock, owhStockSummary, pageOwhStock, syncOwhStock } from '@/api/ecom/owh'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { num, qty } from '@/utils/number'
import { showSyncResults } from '../owh-outbound/owh'

/**
 * 海外仓库存：同步服务商的可用 / 在途 / 锁定，与 ERP 账面库存（系统仓库现存量）对比，差异 = 海外仓（可用 + 锁定）− 账面；
 * 选中行「按海外仓调整」生成其他入库（盘盈）/ 其他出库（盘亏）
 */
const { t } = useI18n()

const columns = [
  { title: t('ecomOwh.stock.sku'), key: 'sku', width: 220, exportColumns: [{ title: t('ecomOwh.stock.sku'), dataIndex: 'sku' }, { title: t('ecomOwh.stock.material'), dataIndex: 'materialName' }] },
  { title: t('ecomOwh.common.warehouse'), key: 'warehouse', width: 200, exportValue: (r) => `${r.warehouseName} ${r.owhCode}` },
  { title: t('ecomOwh.stock.available'), key: 'availableQty', dataIndex: 'availableQty', width: 100, align: 'right' },
  { title: t('ecomOwh.stock.transit'), key: 'transitQty', dataIndex: 'transitQty', width: 100, align: 'right' },
  { title: t('ecomOwh.stock.locked'), key: 'lockedQty', dataIndex: 'lockedQty', width: 100, align: 'right' },
  { title: t('ecomOwh.stock.erpQty'), key: 'erpQty', dataIndex: 'erpQty', width: 110, align: 'right' },
  { title: t('ecomOwh.stock.erpTransit'), key: 'erpTransitQty', dataIndex: 'erpTransitQty', width: 110, align: 'right' },
  { title: t('ecomOwh.stock.diff'), key: 'diffQty', dataIndex: 'diffQty', width: 100, align: 'right' },
  { title: t('ecomOwh.stock.syncTime'), dataIndex: 'syncTime', width: 160 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-stock')
const showSearch = ref(true)

const emptyQuery = () => ({ accountId: undefined, warehouseId: undefined, keyword: undefined, diffOnly: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageOwhStock, query)

const summary = ref({})
async function loadSummary() {
  summary.value = (await owhStockSummary({ accountId: query.accountId, warehouseId: query.warehouseId })) || {}
}
function reload() {
  load()
  loadSummary()
}
function search() {
  doSearch()
  loadSummary()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(reload)

const syncing = ref(false)
async function sync() {
  syncing.value = true
  try {
    showSyncResults(await syncOwhStock(query.accountId), 'ecomOwh.stock.synced')
    reload()
  } finally {
    syncing.value = false
  }
}

const adjustOpen = ref(false)
const adjusting = ref(false)
const adjustDate = ref()
function openAdjust() {
  adjustDate.value = dayjs().format('YYYY-MM-DD')
  adjustOpen.value = true
}
async function doAdjust() {
  adjusting.value = true
  try {
    const r = await adjustOwhStock({ ids: [...selectedKeys.value], bizDate: adjustDate.value })
    adjustOpen.value = false
    Modal.success({
      title: t('ecomOwh.stock.adjusted', { n: r.lines }),
      content: [r.inDocNo && t('ecomOwh.stock.inDoc', { no: r.inDocNo }), r.outDocNo && t('ecomOwh.stock.outDoc', { no: r.outDocNo })].filter(Boolean).join(' / ')
    })
    selectedKeys.value = []
    reload()
  } finally {
    adjusting.value = false
  }
}
</script>

<style scoped>
.summary-card :deep(.ant-card-body) {
  padding: 12px 16px;
}
.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 32px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 20px;
  font-weight: 500;
}
.stat-value.warn {
  color: var(--ant-color-error, #ff4d4f);
}
.stat-time {
  margin-top: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.selected-tip {
  color: var(--app-text-3);
}
</style>
