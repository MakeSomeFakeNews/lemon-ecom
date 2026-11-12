<template>
  <div class="tab-body">
    <div class="filters">
      <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" class="w220" @change="search" />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :placeholder="[$t('ecomFba.audit.range'), $t('ecomFba.audit.range')]" @change="search" />
      <a-checkbox v-model:checked="query.overOnly" @change="search">{{ $t('ecomFba.audit.overOnly') }}</a-checkbox>
    </div>
    <div class="stats">
      <div class="stat">
        <div class="stat-label">{{ $t('ecomFba.audit.skuCount') }}</div>
        <div class="stat-value">{{ result.skuCount ?? 0 }}</div>
      </div>
      <div class="stat clickable" :class="{ active: query.overOnly }" @click="toggleOver">
        <div class="stat-label">{{ $t('ecomFba.audit.overSkus') }}</div>
        <div class="stat-value neg">{{ result.overSkus ?? 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">{{ $t('ecomFba.audit.missingDims') }}</div>
        <div class="stat-value warn">{{ result.missingDims ?? 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">{{ $t('ecomFba.audit.overchargeBase') }}</div>
        <div class="stat-value neg">{{ symMoney(result.overchargeBase ?? 0) }}</div>
      </div>
    </div>
    <a-alert v-if="result.missingRates?.length" type="warning" show-icon class="rates-alert" :message="$t('ecomFba.common.missingRates', { list: result.missingRates.join(', ') })" />
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <a-button v-if="selectedKeys.length" v-perm="'ecom:fbaClaim:add'" type="primary" @click="generate">
        <template #icon><SafetyCertificateOutlined /></template>{{ $t('ecomFba.audit.generate') }} ({{ selectedKeys.length }})
      </a-button>
      <span v-else class="sub-text hint">{{ $t('ecomFba.audit.generateTip') }}</span>
      <a-tooltip :title="$t('ecomFba.audit.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
    </TableToolbar>
    <a-table
      :row-key="rowKey"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.flag !== FLAG_OVER || !!r.claimNo }) }"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode || '-' }}</div>
          <div class="sub-text">{{ record.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'dims'">
          <span v-if="record.lengthCm">{{ n0(record.lengthCm) }} × {{ n0(record.widthCm) }} × {{ n0(record.heightCm) }} cm · {{ n0(record.weightKg) }} kg</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'shippingWeight'">
          <span v-if="record.shippingWeight != null">{{ n0(record.shippingWeight) }} {{ record.weightUnit }}</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.money">
          <span v-if="record[column.key] != null" :class="{ neg: column.key === 'overcharge' && Number(record.overcharge) > 0 }">{{ money(record[column.key]) }}</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'flag'">
          <a-tag :color="FLAG_COLORS[record.flag]" :bordered="false">{{ $t(`ecomFba.audit.flagLabel.${record.flag}`) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'claimNo'">
          <a-tag v-if="record.claimNo" color="purple" :bordered="false">{{ record.claimNo }}</a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { auditClaims, feeAudit } from '@/api/ecom/fba'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money, symMoney, qty } from '@/utils/number'
import { EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'

/** 配送费核查：应收（尺寸重量 × 费率表）与结算实扣对比，勾选多收的 SKU 生成索赔 */
/** 多收（后端 FbaFeeAuditService.FLAG_OVER） */
const FLAG_OVER = 'OVER'
const FLAG_COLORS = { MISSING_DIMS: 'warning', NO_RATE: 'default', OVER: 'red', UNDER: 'blue', OK: 'green' }
const rowKey = (r) => `${r.storeId}|${r.sellerSku}`
const m = (key, title, width = 100) => ({ title, key, dataIndex: key, width, align: 'right', money: true })
const columns = [
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true, fixed: 'left' },
  { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku', width: 160, fixed: 'left' },
  { title: t('ecomFba.common.material'), key: 'material', width: 160, exportColumns: [{ title: t('ecomFba.common.material'), dataIndex: 'materialCode' }, { title: t('common.field.name'), dataIndex: 'materialName' }] },
  { title: t('ecomFba.audit.orderCount'), dataIndex: 'orderCount', width: 80, align: 'right' },
  { title: t('ecomFba.audit.qty'), dataIndex: 'qty', width: 70, align: 'right', customRender: ({ text }) => qty(text) },
  m('actualFee', t('ecomFba.audit.actualFee')),
  m('actualUnit', t('ecomFba.audit.actualUnit')),
  { title: t('ecomFba.audit.dims'), key: 'dims', width: 220, exportValue: (r) => (r.lengthCm ? `${r.lengthCm}x${r.widthCm}x${r.heightCm} cm ${r.weightKg} kg` : '') },
  { title: t('ecomFba.audit.tier'), dataIndex: 'tierName', width: 140, ellipsis: true },
  { title: t('ecomFba.audit.shippingWeight'), key: 'shippingWeight', width: 100, align: 'right', exportValue: (r) => (r.shippingWeight != null ? `${r.shippingWeight} ${r.weightUnit}` : '') },
  m('expectedUnit', t('ecomFba.audit.expectedUnit')),
  m('expectedFee', t('ecomFba.audit.expectedFee')),
  m('platformFee', t('ecomFba.audit.platformFee')),
  m('diffUnit', t('ecomFba.audit.diffUnit')),
  m('overcharge', t('ecomFba.audit.overcharge')),
  { title: t('ecomFba.common.currency'), dataIndex: 'currency', width: 70 },
  { title: t('ecomFba.audit.flag'), key: 'flag', dataIndex: 'flag', width: 100, exportValue: (r) => t(`ecomFba.audit.flagLabel.${r.flag}`) },
  { title: t('ecomFba.audit.claimNo'), key: 'claimNo', dataIndex: 'claimNo', width: 140 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-fee-audit')
const query = reactive({ storeId: undefined, range: undefined, overOnly: false })
const result = ref({})

/** 核查结果一次返回，前端分页 */
async function fetcher({ current, size: pageSize }) {
  const q = { storeId: query.storeId || undefined, beginDate: query.range?.[0], endDate: query.range?.[1], overOnly: query.overOnly || undefined }
  const res = (await feeAudit(q)) || { rows: [] }
  result.value = res
  const rows = res.rows || []
  return { records: rows.slice((current - 1) * pageSize, current * pageSize), total: rows.length }
}
const { list, loading, size, pagination, load, search, onTableChange, exportAll, rowSelection, selectedKeys } = useTable(fetcher, query)
onMounted(load)

function toggleOver() {
  query.overOnly = !query.overOnly
  search()
}

async function generate() {
  const items = selectedKeys.value.map((k) => {
    const i = k.indexOf('|')
    return { storeId: Number(k.slice(0, i)), sellerSku: k.slice(i + 1) }
  })
  if (!items.length) {
    message.warning(t('ecomFba.audit.selectOver'))
    return
  }
  const n = await auditClaims({ storeId: query.storeId || null, beginDate: query.range?.[0] || null, endDate: query.range?.[1] || null, items })
  message.success(t('ecomFba.audit.generateDone', { n }))
  selectedKeys.value = []
  load()
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.w220 {
  width: 220px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 10px;
}
.stat {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
}
.stat.clickable {
  cursor: pointer;
}
.stat.clickable:hover,
.stat.active {
  border-color: var(--status-critical, #cf1322);
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.rates-alert {
  margin-bottom: 8px;
}
.hint {
  align-self: center;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.warn {
  color: #d46b08;
}
</style>
