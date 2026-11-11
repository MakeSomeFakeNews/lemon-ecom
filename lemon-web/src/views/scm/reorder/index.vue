<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('scm.reorder.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.reorder.keywordPh')" allow-clear />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="$t('scm.reorder.tip')"
      />
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportList"
        :columns="columns"
        @refresh="load"
      >
        <span class="toolbar-switch">
          <a-switch v-model:checked="query.onlyNeeded" size="small" @change="load" /> {{ $t('scm.reorder.onlyNeeded') }}
        </span>
      </TableToolbar>

      <a-table
        row-key="materialId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.code }} {{ record.name }}
          </template>
          <template v-else-if="column.key === 'point'">
            <div>{{ formatQty(record.reorderPoint) }}</div>
            <div class="sub-text">{{ $t('scm.reorder.safetyStock', { qty: formatQty(record.safetyStock) }) }}</div>
          </template>
          <template v-else-if="['available', 'poOpen', 'soOpen', 'reorderQty'].includes(column.key)">
            {{ formatQty(record[column.key]) }}
          </template>
          <template v-else-if="column.key === 'projected'">
            <span :class="{ short: isShort(record) }">{{ formatQty(record.projected) }}</span>
          </template>
          <template v-else-if="column.key === 'suggestQty'">
            <b v-if="Number(record.suggestQty) > 0">{{ formatQty(record.suggestQty) }}</b>
            <span v-else class="sub-text">0</span>
            <span class="sub-text"> {{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'supplier'">
            <div>{{ record.supplierName || '-' }}</div>
            <div v-if="record.price != null" class="sub-text">{{ $t('scm.reorder.refPrice', { price: formatMoney(record.price) }) }}</div>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { listReorder } from '@/api/scm/planning'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { writeExcel } from '@/utils/excel'

const { t } = useI18n()

/** 预计库存低于再订货点（未设置时取安全库存） */
const isShort = (r) => Number(r.projected) < Number(Number(r.reorderPoint) > 0 ? r.reorderPoint : r.safetyStock || 0)

const columns = [
  { title: t('scm.reorder.material'), key: 'material', width: 240, exportValue: (r) => `${r.code} ${r.name}` },
  { title: t('scm.reorder.spec'), dataIndex: 'spec', width: 120, ellipsis: true },
  { title: t('scm.reorder.unit'), dataIndex: 'unit', width: 70 },
  { title: t('scm.reorder.reorderPoint'), key: 'point', width: 130, align: 'right', exportValue: (r) => r.reorderPoint },
  { title: t('scm.reorder.reorderQty'), key: 'reorderQty', width: 110, align: 'right' },
  { title: t('scm.reorder.available'), key: 'available', width: 130, align: 'right' },
  { title: t('scm.reorder.poOpen'), key: 'poOpen', width: 150, align: 'right' },
  { title: t('scm.reorder.soOpen'), key: 'soOpen', width: 170, align: 'right' },
  { title: t('scm.reorder.projected'), key: 'projected', width: 130, align: 'right' },
  { title: t('scm.reorder.suggestQty'), key: 'suggestQty', width: 130, align: 'right' },
  { title: t('scm.reorder.supplier'), key: 'supplier', width: 200, exportValue: (r) => r.supplierName || '' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'scm-reorder')
const showSearch = ref(true)
const size = ref('middle')

const emptyQuery = () => ({ keyword: undefined, onlyNeeded: true })
const query = reactive(emptyQuery())
const list = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 50,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200'],
  showTotal: (total) => t('common.table.total', { total })
})

async function load() {
  loading.value = true
  try {
    list.value = await listReorder({ keyword: query.keyword || undefined, onlyNeeded: query.onlyNeeded })
    pagination.current = 1
  } finally {
    loading.value = false
  }
}
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  load()
}

/** 列表不分页，直接按当前数据导出 */
async function exportList(cols) {
  const fields = cols.filter((c) => c.key !== 'action')
  const valueOf = (c, r) => {
    const v = c.exportValue ? c.exportValue(r) : r[c.dataIndex || c.key]
    return v === null || v === undefined ? '' : v
  }
  await writeExcel(
    t('scm.reorder.exportName'),
    fields.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })),
    list.value.map((r) => fields.map((c) => valueOf(c, r)))
  )
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.short {
  color: #ff4d4f;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
