<template>
  <TableToolbar :exporter="exportRows" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="emit('refresh')">
    <slot name="toolbar" :selected="selectedRows" />
  </TableToolbar>

  <a-table
    row-key="listingId"
    :size="size"
    :columns="visibleColumns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :row-selection="selectable ? rowSelection : undefined"
    :row-class-name="(r) => (r.level === ReplenishLevel.URGENT ? 'row-urgent' : '')"
    :scroll="{ x: scrollX + (selectable ? 32 : 0), y: '100%' }"
    @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'sku'">
        <div class="sku-cell">
          <span :class="['level-dot', levelTone(record.level)]" />
          <div class="sku-main">
            <div class="cell-ellipsis" :title="record.sellerSku">{{ record.sellerSku }}</div>
            <div class="sub-text cell-ellipsis" :title="record.storeName">{{ record.storeName }}</div>
          </div>
        </div>
      </template>
      <template v-else-if="column.key === 'storeName'">
        <div>{{ record.storeName }}</div>
        <div v-if="record.platformItemId" class="sub-text">{{ record.platformItemId }}</div>
      </template>
      <template v-else-if="column.key === 'title'">
        <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'material'">
        <div>{{ record.materialCode }}</div>
        <div class="sub-text">{{ record.materialName }}</div>
      </template>
      <template v-else-if="column.key === 'packQty' || column.key === 'platformQty' || column.key === 'localQty'">{{ qty(record[column.key]) }}</template>
      <template v-else-if="column.key === 'sales'">{{ salesText(record) }}</template>
      <template v-else-if="column.key === 'dailySales'">
        <div>
          {{ qty(record.dailySales, 2) }}
          <a-tag v-if="record.dailyOverridden" color="purple" :bordered="false" class="mini-tag">{{ $t('ecomPurchase.replenish.manual') }}</a-tag>
        </div>
        <div v-if="!record.dailyOverridden" class="sub-text">
          {{ $t('ecomPurchase.replenish.weightedDaily', { v: qty(record.weightedDaily, 2) }) }}
          <span v-if="Number(record.factor) !== 1">{{ $t('ecomPurchase.replenish.factorText', { v: qty(record.factor, 2) }) }}</span>
        </div>
        <a-tooltip v-if="record.stockoutDays > 0" :title="$t('ecomFix.stockout.replenishTip', { raw: qty(record.rawWeightedDaily, 2), n: record.stockoutDays })">
          <a-tag color="red" :bordered="false" class="mini-tag">{{ $t('ecomFix.stockout.excludedN', { n: record.stockoutDays }) }}</a-tag>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'inTransitQty'">
        <a v-if="Number(record.inTransitQty) > 0" :title="$t('ecomPurchase.replenish.viewTransit')" @click="emit('transit', record)">{{ qty(record.inTransitQty) }}</a>
        <span v-else>0</span>
        <div v-if="record.inTransitEta" class="sub-text">{{ $t('ecomPurchase.replenish.eta', { date: record.inTransitEta }) }}</div>
      </template>
      <template v-else-if="column.key === 'coverDays'">
        <CoverDaysCell :days="record.coverDays" :level="record.level" :lead="record.leadDays" :safety="record.safetyDays" />
      </template>
      <template v-else-if="column.key === 'stockoutDate'">
        <span v-if="record.stockoutDate" :class="{ urgent: record.level === ReplenishLevel.URGENT }">{{ record.stockoutDate }}</span>
        <span v-else class="sub-text">-</span>
      </template>
      <template v-else-if="column.key === 'leadDays'">{{ leadText(record) }}</template>
      <template v-else-if="column.key === 'targetQty'">{{ qty(record.targetQty) }}</template>
      <template v-else-if="column.key === 'suggestQty'">
        <a-tag v-if="record.disabled" :bordered="false">{{ $t('ecomPurchase.replenish.disabled') }}</a-tag>
        <b v-else-if="Number(record.suggestQty) > 0" :class="['suggest', levelTone(record.level)]">{{ qty(record.suggestQty) }}</b>
        <span v-else class="sub-text">0</span>
      </template>
      <template v-else-if="column.key === 'level'">
        <DictTag type="ecom_replenish_level" :value="record.level" />
        <div v-if="record.hasSetting" class="sub-text">{{ $t('ecomPurchase.replenish.hasSetting') }}</div>
      </template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CarOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { writeExcel } from '@/utils/excel'
import { qty } from '@/utils/number'
import { ReplenishLevel } from '@/constants/enums'
import CoverDaysCell from './CoverDaysCell.vue'

/**
 * 平台仓补货建议表格（店铺 × 在线商品，平台 SKU 件数）：selectable 时可勾选（生成发货计划），
 * 工具栏插槽 toolbar 拿到已勾选的行；行操作「补货设置」「在途构成」通过事件交给页面
 */
const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: Boolean,
  selectable: Boolean
})
const emit = defineEmits(['refresh', 'setting', 'transit'])
const { t } = useI18n()
const dict = useDict('ecom_replenish_level')

const salesText = (r) => t('ecomPurchase.replenish.salesText', { s7: qty(r.sales7), s14: qty(r.sales14), s30: qty(r.sales30), s60: qty(r.sales60) })
const leadText = (r) => t('ecom.replenish.leadText', { lead: r.leadDays ?? '-', safety: r.safetyDays ?? '-' })

/** 紧急程度排序值：紧急 → 预警 → 充足 */
const LEVEL_ORDER = { [ReplenishLevel.URGENT]: 0, [ReplenishLevel.WARN]: 1, [ReplenishLevel.OK]: 2 }
const byNum = (k) => (a, b) => (a[k] == null ? Infinity : Number(a[k])) - (b[k] == null ? Infinity : Number(b[k]))
const levelTone = (level) => (level === ReplenishLevel.URGENT ? 'urgent' : level === ReplenishLevel.WARN ? 'warn' : 'ok')

// 关键列（紧急程度、可售天数、建议补货）放在前面，不用横向滚动就能看到
const columns = [
  {
    title: t('ecom.common.sellerSku'),
    key: 'sku',
    dataIndex: 'sellerSku',
    width: 190,
    fixed: 'left',
    exportColumns: [
      { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku' },
      { title: t('ecom.common.store'), dataIndex: 'storeName' }
    ]
  },
  {
    title: t('ecom.replenish.level'),
    key: 'level',
    dataIndex: 'level',
    width: 90,
    dict: 'ecom_replenish_level',
    sorter: (a, b) => (LEVEL_ORDER[a.level] ?? 9) - (LEVEL_ORDER[b.level] ?? 9)
  },
  { title: t('ecom.replenish.coverDays'), key: 'coverDays', dataIndex: 'coverDays', width: 120, align: 'right', sorter: byNum('coverDays') },
  { title: t('ecom.replenish.suggestQty'), key: 'suggestQty', dataIndex: 'suggestQty', width: 110, align: 'right', sorter: byNum('suggestQty') },
  { title: t('ecom.replenish.stockoutDate'), key: 'stockoutDate', dataIndex: 'stockoutDate', width: 130, sorter: (a, b) => String(a.stockoutDate || '9').localeCompare(String(b.stockoutDate || '9')) },
  { title: t('ecom.replenish.dailySales'), key: 'dailySales', dataIndex: 'dailySales', width: 150, align: 'right', sorter: byNum('dailySales') },
  { title: t('ecomPurchase.replenish.sales'), key: 'sales', width: 160, align: 'right', exportValue: salesText },
  { title: t('ecom.replenish.platformQty'), key: 'platformQty', dataIndex: 'platformQty', width: 100, align: 'right' },
  { title: t('ecomPurchase.replenish.inTransit'), key: 'inTransitQty', dataIndex: 'inTransitQty', width: 140, align: 'right' },
  { title: t('ecom.replenish.localQty'), key: 'localQty', dataIndex: 'localQty', width: 100, align: 'right' },
  { title: t('ecomPurchase.replenish.target'), key: 'targetQty', dataIndex: 'targetQty', width: 90, align: 'right' },
  { title: t('ecom.replenish.leadSafety'), key: 'leadDays', width: 140, exportValue: leadText },
  { title: t('ecom.listing.title'), key: 'title', dataIndex: 'title', width: 200 },
  {
    title: t('ecom.common.material'),
    key: 'material',
    width: 170,
    exportColumns: [
      { title: t('ecom.listing.materialCode'), dataIndex: 'materialCode' },
      { title: t('ecom.listing.materialName'), dataIndex: 'materialName' }
    ]
  },
  { title: t('ecom.common.packQty'), key: 'packQty', dataIndex: 'packQty', width: 80, align: 'right' },
  { title: t('ecomUiOps.replenish.storeItem'), key: 'storeName', dataIndex: 'storeName', width: 160 },
  actionColumn(2, [t('ecomPurchase.replenish.setting')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-replenish-platform')
const size = ref('middle')
const pagination = reactive({
  current: 1,
  pageSize: 50,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200'],
  showTotal: (total) => t('common.table.total', { total })
})

const selectedKeys = ref([])
const selectedRows = computed(() => props.rows.filter((r) => selectedKeys.value.includes(r.listingId)))
const rowSelection = computed(() => ({ selectedRowKeys: selectedKeys.value, onChange: (keys) => (selectedKeys.value = keys) }))
// 数据刷新后回到第一页，去掉已不存在的勾选
watch(
  () => props.rows,
  (rows) => {
    pagination.current = 1
    const ids = new Set(rows.map((r) => r.listingId))
    selectedKeys.value = selectedKeys.value.filter((k) => ids.has(k))
  }
)

const rowActions = (record) => [
  { type: 'setting', label: t('ecomPurchase.replenish.setting'), perm: 'ecom:replenish:setting', onClick: () => emit('setting', record) },
  { key: 'transit', label: t('ecomPurchase.replenish.viewTransit'), icon: CarOutlined, onClick: () => emit('transit', record) }
]

/** 列表不分页，直接按当前筛选结果导出 */
async function exportRows(cols) {
  const fields = cols.filter((c) => c.key !== 'action').flatMap((c) => (c.exportColumns ? c.exportColumns.map((e) => ({ width: c.width, ...e })) : [c]))
  const valueOf = (c, r) => {
    if (c.exportValue) return c.exportValue(r)
    let v = r[c.dataIndex || c.key]
    if (c.dict) v = findDict(dict[c.dict], v)?.label ?? v
    return v === null || v === undefined ? '' : v
  }
  await writeExcel(
    t('ecomPurchase.replenish.exportName'),
    fields.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })),
    props.rows.map((r) => fields.map((c) => valueOf(c, r)))
  )
}

defineExpose({ clearSelection: () => (selectedKeys.value = []) })
</script>

<style scoped>
.sku-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.sku-main {
  min-width: 0;
  flex: 1;
}
.level-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.level-dot.urgent {
  background: #ff4d4f;
  box-shadow: 0 0 0 3px rgba(255, 77, 79, 0.18);
}
.level-dot.warn {
  background: #faad14;
}
.level-dot.ok {
  background: #52c41a;
}
.suggest.urgent {
  color: #ff4d4f;
}
.suggest.warn {
  color: #d46b08;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.urgent {
  color: #ff4d4f;
  font-weight: 500;
}
.mini-tag {
  margin-inline-end: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
</style>
