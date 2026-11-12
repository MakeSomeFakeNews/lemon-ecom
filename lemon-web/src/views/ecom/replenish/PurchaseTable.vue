<template>
  <TableToolbar :exporter="exportRows" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="emit('refresh')">
    <slot name="toolbar" :selected="selectedRows" />
  </TableToolbar>

  <a-table
    row-key="materialId"
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
      <template v-if="column.key === 'material'">
        <div class="sku-cell">
          <span :class="['level-dot', levelTone(record.level)]" />
          <div class="sku-main">
            <div class="cell-ellipsis" :title="`${record.materialCode} ${record.materialName}`">{{ record.materialCode }} {{ record.materialName }}</div>
            <div v-if="record.spec" class="sub-text cell-ellipsis" :title="record.spec">{{ record.spec }}</div>
          </div>
        </div>
      </template>
      <template v-else-if="column.key === 'dailySales'">
        {{ qty(record.dailySales, 2) }}
        <a-tag v-if="record.dailyOverridden" color="purple" :bordered="false" class="mini-tag">{{ $t('ecomPurchase.replenish.manual') }}</a-tag>
      </template>
      <template v-else-if="QTY_KEYS.includes(column.key)">{{ qty(record[column.key]) }}</template>
      <template v-else-if="column.key === 'inboundTransitQty'">
        <a v-if="Number(record.inboundTransitQty) > 0" :title="$t('ecomPurchase.replenish.viewTransit')" @click="emit('transit', record)">{{ qty(record.inboundTransitQty) }}</a>
        <span v-else>0</span>
        <div v-if="record.inboundEta" class="sub-text">{{ $t('ecomPurchase.replenish.eta', { date: record.inboundEta }) }}</div>
      </template>
      <template v-else-if="column.key === 'purchaseTransitQty'">
        <a v-if="Number(record.purchaseTransitQty) > 0" :title="$t('ecomPurchase.replenish.viewTransit')" @click="emit('transit', record)">{{ qty(record.purchaseTransitQty) }}</a>
        <span v-else>0</span>
        <div v-if="record.purchaseEta" class="sub-text">{{ $t('ecomPurchase.replenish.purchaseEta', { date: record.purchaseEta }) }}</div>
      </template>
      <template v-else-if="column.key === 'localSafetyQty'">
        <div>{{ qty(record.localSafetyQty) }}</div>
        <div v-if="Number(record.safetyStock) > 0" class="sub-text">{{ $t('ecomPurchase.replenish.safetyStockText', { v: qty(record.safetyStock) }) }}</div>
      </template>
      <template v-else-if="column.key === 'purchaseDays'">
        <div>{{ $t('ecomPurchase.replenish.purchaseDaysText', { n: record.purchaseDays }) }}</div>
        <div class="sub-text">{{ fromText(record) }}</div>
      </template>
      <template v-else-if="column.key === 'localCoverDays'">
        <CoverDaysCell :days="record.localCoverDays" :level="record.level" :lead="record.purchaseDays" />
      </template>
      <template v-else-if="column.key === 'suggestQty'">
        <a-tag v-if="record.disabled" :bordered="false">{{ $t('ecomPurchase.replenish.disabled') }}</a-tag>
        <b v-else-if="Number(record.suggestQty) > 0" :class="['suggest', levelTone(record.level)]">{{ qty(record.suggestQty) }} {{ record.unit || '' }}</b>
        <span v-else class="sub-text">0</span>
        <div v-if="record.minQty != null" class="sub-text">{{ $t('ecomPurchase.replenish.minQtyText', { v: qty(record.minQty) }) }}</div>
      </template>
      <template v-else-if="column.key === 'supplier'">
        <template v-if="record.supplierId">
          <div class="cell-ellipsis" :title="record.supplierName">{{ record.supplierName }}</div>
          <div v-if="record.price != null" class="sub-text">{{ $t('ecomPurchase.replenish.price') }} {{ price(record.price) }}</div>
        </template>
        <span v-else class="sub-text">{{ $t('ecomPurchase.replenish.noSupplier') }}</span>
      </template>
      <template v-else-if="column.key === 'demands'">
        <a-popover v-if="record.demands?.length" :title="$t('ecomPurchase.replenish.demands')">
          <template #content>
            <div v-for="d in record.demands" :key="d.listingId">{{ demandText(d) }}</div>
          </template>
          <a class="cell-ellipsis demand-link">{{ demandSummary(record) }}</a>
        </a-popover>
        <span v-else class="sub-text">-</span>
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
import { price, qty } from '@/utils/number'
import { ReplenishLevel } from '@/constants/enums'
import { demandText } from './replenish'
import CoverDaysCell from './CoverDaysCell.vue'

/**
 * 本地仓采购建议表格（物料，汇总全部店铺）：可勾选生成采购计划（工具栏插槽 toolbar 拿到已勾选的行），
 * 行操作「补货设置」（物料级）「在途构成」通过事件交给页面
 */
const props = defineProps({
  rows: { type: Array, default: () => [] },
  loading: Boolean,
  selectable: Boolean
})
const emit = defineEmits(['refresh', 'setting', 'transit'])
const { t } = useI18n()
const dict = useDict('ecom_replenish_level')

const QTY_KEYS = ['platformSuggestQty', 'platformQty', 'localQty']
const fromText = (r) => (r.purchaseDaysFrom ? t(`ecomPurchase.replenish.purchaseDaysFrom.${r.purchaseDaysFrom}`) : '')
const demandSummary = (r) => r.demands.map((d) => d.storeName).filter((v, i, a) => a.indexOf(v) === i).join(' / ')

const LEVEL_ORDER = { [ReplenishLevel.URGENT]: 0, [ReplenishLevel.WARN]: 1, [ReplenishLevel.OK]: 2 }
const byNum = (k) => (a, b) => (a[k] == null ? Infinity : Number(a[k])) - (b[k] == null ? Infinity : Number(b[k]))
const levelTone = (level) => (level === ReplenishLevel.URGENT ? 'urgent' : level === ReplenishLevel.WARN ? 'warn' : 'ok')

// 关键列（紧急程度、本地可用天数、建议采购、供应商）放在前面
const columns = [
  {
    title: t('ecom.common.material'),
    key: 'material',
    width: 240,
    fixed: 'left',
    exportColumns: [
      { title: t('ecom.listing.materialCode'), dataIndex: 'materialCode' },
      { title: t('ecom.listing.materialName'), dataIndex: 'materialName' }
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
  { title: t('ecomPurchase.replenish.localCover'), key: 'localCoverDays', dataIndex: 'localCoverDays', width: 120, align: 'right', sorter: byNum('localCoverDays') },
  { title: t('ecomPurchase.replenish.suggestPurchase'), key: 'suggestQty', dataIndex: 'suggestQty', width: 130, align: 'right', sorter: byNum('suggestQty') },
  { title: t('ecomPurchase.replenish.supplier'), key: 'supplier', width: 170, exportValue: (r) => r.supplierName || '' },
  { title: t('ecomPurchase.replenish.arrivalDate'), dataIndex: 'arrivalDate', width: 120 },
  { title: t('ecom.replenish.dailySales'), key: 'dailySales', dataIndex: 'dailySales', width: 100, align: 'right', sorter: byNum('dailySales') },
  { title: t('ecomPurchase.replenish.platformSuggest'), key: 'platformSuggestQty', dataIndex: 'platformSuggestQty', width: 110, align: 'right' },
  { title: t('ecomPurchase.replenish.platformQty'), key: 'platformQty', dataIndex: 'platformQty', width: 100, align: 'right' },
  { title: t('ecomPurchase.replenish.inTransit'), key: 'inboundTransitQty', dataIndex: 'inboundTransitQty', width: 140, align: 'right' },
  { title: t('ecomPurchase.replenish.localQty'), key: 'localQty', dataIndex: 'localQty', width: 100, align: 'right' },
  { title: t('ecomPurchase.replenish.purchaseTransit'), key: 'purchaseTransitQty', dataIndex: 'purchaseTransitQty', width: 140, align: 'right' },
  { title: t('ecomPurchase.replenish.localSafety'), key: 'localSafetyQty', dataIndex: 'localSafetyQty', width: 150, align: 'right' },
  { title: t('ecomPurchase.replenish.purchaseDays'), key: 'purchaseDays', dataIndex: 'purchaseDays', width: 120, exportValue: (r) => r.purchaseDays },
  { title: t('ecomPurchase.replenish.demands'), key: 'demands', width: 160, exportValue: (r) => (r.demands || []).map(demandText).join('; ') },
  actionColumn(2, [t('ecomPurchase.replenish.setting')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-replenish-purchase')
const size = ref('middle')
const pagination = reactive({
  current: 1,
  pageSize: 50,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200'],
  showTotal: (total) => t('common.table.total', { total })
})

const selectedKeys = ref([])
const selectedRows = computed(() => props.rows.filter((r) => selectedKeys.value.includes(r.materialId)))
const rowSelection = computed(() => ({ selectedRowKeys: selectedKeys.value, onChange: (keys) => (selectedKeys.value = keys) }))
watch(
  () => props.rows,
  (rows) => {
    pagination.current = 1
    const ids = new Set(rows.map((r) => r.materialId))
    selectedKeys.value = selectedKeys.value.filter((k) => ids.has(k))
  }
)

const rowActions = (record) => [
  { type: 'setting', label: t('ecomPurchase.replenish.setting'), perm: 'ecom:replenish:setting', onClick: () => emit('setting', record) },
  { key: 'transit', label: t('ecomPurchase.replenish.viewTransit'), icon: CarOutlined, onClick: () => emit('transit', record) }
]

async function exportRows(cols) {
  const fields = cols.filter((c) => c.key !== 'action').flatMap((c) => (c.exportColumns ? c.exportColumns.map((e) => ({ width: c.width, ...e })) : [c]))
  const valueOf = (c, r) => {
    if (c.exportValue) return c.exportValue(r)
    let v = r[c.dataIndex || c.key]
    if (c.dict) v = findDict(dict[c.dict], v)?.label ?? v
    return v === null || v === undefined ? '' : v
  }
  await writeExcel(
    t('ecomPurchase.replenish.exportPurchaseName'),
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
.demand-link {
  display: block;
}
.mini-tag {
  margin-inline-end: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
</style>
