<template>
  <div class="table-page ecom-replenish">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecom.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.replenish.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecom.replenish.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_replenish_level)" @change="pagination.current = 1" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <!-- 紧急 / 预警 / 充足数量：点击按紧急程度筛选 -->
      <div class="stats">
        <div
          v-for="s in STATS"
          :key="s.level"
          class="stat"
          :class="[s.cls, { active: query.level === s.level }]"
          role="button"
          tabindex="0"
          @click="toggleLevel(s.level)"
          @keydown.enter="toggleLevel(s.level)"
        >
          <component :is="s.icon" class="stat-icon" />
          <div class="stat-body">
            <div class="stat-label">{{ levelLabel(s.level) }}</div>
            <div class="stat-value">{{ num(counts[s.level]) }}</div>
          </div>
        </div>
        <span class="sub-text stats-tip">{{ $t('ecom.replenish.tip') }}</span>
      </div>

      <TableToolbar :exporter="exportRows" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load" />

      <a-table
        row-key="listingId"
        :size="size"
        :columns="visibleColumns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="(r) => (r.level === ReplenishLevel.URGENT ? 'row-urgent' : '')"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div>{{ record.sellerSku }}</div>
            <div v-if="record.platformItemId" class="sub-text">{{ record.platformItemId }}</div>
          </template>
          <template v-else-if="column.key === 'title'">
            <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <template v-if="record.materialId">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }}</div>
            </template>
            <a-tag v-else color="warning" :bordered="false">{{ $t('ecom.common.unmapped') }}</a-tag>
          </template>
          <template v-else-if="QTY_KEYS.includes(column.key)">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'dailySales'">{{ qty(record.dailySales, 2) }}</template>
          <template v-else-if="column.key === 'coverDays'">
            <span v-if="record.coverDays != null">{{ qty(record.coverDays, 1) }}</span>
            <span v-else class="sub-text">{{ $t('ecom.replenish.noSales') }}</span>
          </template>
          <template v-else-if="column.key === 'stockoutDate'">
            <span v-if="record.stockoutDate" :class="{ urgent: record.level === ReplenishLevel.URGENT }">{{ record.stockoutDate }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'leadDays'">{{ leadText(record) }}</template>
          <template v-else-if="column.key === 'suggestQty'">
            <b v-if="Number(record.suggestQty) > 0">{{ qty(record.suggestQty) }}</b>
            <span v-else class="sub-text">0</span>
          </template>
          <template v-else-if="column.key === 'level'">
            <DictTag type="ecom_replenish_level" :value="record.level" />
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AlertOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { replenishRows } from '@/api/ecom/replenish'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { findDict, toOptions } from '@/utils/dict'
import { writeExcel } from '@/utils/excel'
import { num, qty } from '@/utils/number'
import { ReplenishLevel } from '@/constants/enums'

const { t } = useI18n()
const dict = useDict('ecom_replenish_level')

/** 顶部统计：紧急、预警、充足 */
const STATS = [
  { level: ReplenishLevel.URGENT, icon: AlertOutlined, cls: 'stat-urgent' },
  { level: ReplenishLevel.WARN, icon: WarningOutlined, cls: 'stat-warn' },
  { level: ReplenishLevel.OK, icon: CheckCircleOutlined, cls: 'stat-ok' }
]
const levelLabel = (level) => findDict(dict.ecom_replenish_level, level)?.label || level

/** 件数列（按平台 SKU 件数） */
const QTY_KEYS = ['packQty', 'sales30', 'platformQty', 'inTransitQty', 'localQty']
const leadText = (r) => t('ecom.replenish.leadText', { lead: r.leadDays ?? '-', safety: r.safetyDays ?? '-' })

const columns = [
  { title: t('ecom.common.store'), dataIndex: 'storeName', width: 170 },
  { title: t('ecom.common.sellerSku'), key: 'sku', dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.listing.title'), key: 'title', dataIndex: 'title', width: 240 },
  {
    title: t('ecom.common.material'),
    key: 'material',
    width: 200,
    exportColumns: [
      { title: t('ecom.listing.materialCode'), dataIndex: 'materialCode' },
      { title: t('ecom.listing.materialName'), dataIndex: 'materialName' }
    ]
  },
  { title: t('ecom.common.packQty'), key: 'packQty', dataIndex: 'packQty', width: 90, align: 'right' },
  { title: t('ecom.replenish.sales30'), key: 'sales30', dataIndex: 'sales30', width: 120, align: 'right' },
  { title: t('ecom.replenish.dailySales'), key: 'dailySales', dataIndex: 'dailySales', width: 100, align: 'right' },
  { title: t('ecom.replenish.platformQty'), key: 'platformQty', dataIndex: 'platformQty', width: 110, align: 'right' },
  { title: t('ecom.replenish.inTransitQty'), key: 'inTransitQty', dataIndex: 'inTransitQty', width: 90, align: 'right' },
  { title: t('ecom.replenish.localQty'), key: 'localQty', dataIndex: 'localQty', width: 100, align: 'right' },
  { title: t('ecom.replenish.coverDays'), key: 'coverDays', dataIndex: 'coverDays', width: 100, align: 'right' },
  { title: t('ecom.replenish.stockoutDate'), key: 'stockoutDate', dataIndex: 'stockoutDate', width: 130 },
  { title: t('ecom.replenish.leadSafety'), key: 'leadDays', width: 150, exportValue: (r) => leadText(r) },
  { title: t('ecom.replenish.suggestQty'), key: 'suggestQty', dataIndex: 'suggestQty', width: 120, align: 'right' },
  { title: t('ecom.replenish.level'), key: 'level', dataIndex: 'level', width: 100, dict: 'ecom_replenish_level' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-replenish')
const showSearch = ref(true)
const size = ref('middle')

const emptyQuery = () => ({ storeId: undefined, keyword: undefined, level: undefined })
const query = reactive(emptyQuery())
// 从看板「紧急补货」跳转过来时带上紧急程度
const route = useRoute()
if (route.query.level) query.level = String(route.query.level)
const all = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 50,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200'],
  showTotal: (total) => t('common.table.total', { total })
})

/** 按店铺、关键字取全部行，紧急程度在前端筛选，统计数量始终是全部行的 */
async function load() {
  loading.value = true
  try {
    all.value = (await replenishRows({ storeId: query.storeId || undefined, keyword: query.keyword || undefined })) || []
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

const counts = computed(() => {
  const c = { [ReplenishLevel.URGENT]: 0, [ReplenishLevel.WARN]: 0, [ReplenishLevel.OK]: 0 }
  all.value.forEach((r) => (c[r.level] = (c[r.level] || 0) + 1))
  return c
})
const rows = computed(() => (query.level ? all.value.filter((r) => r.level === query.level) : all.value))

function toggleLevel(level) {
  query.level = query.level === level ? undefined : level
  pagination.current = 1
}

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
    t('ecom.replenish.exportName'),
    fields.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })),
    rows.value.map((r) => fields.map((c) => valueOf(c, r)))
  )
}
</script>

<style scoped>
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
.stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 150px;
  padding: 8px 14px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-left-width: 4px;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: box-shadow 0.2s;
}
.stat:hover,
.stat.active {
  box-shadow: 0 0 0 2px var(--ant-color-primary-border, #91caff);
}
.stat-icon {
  font-size: 20px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}
.stat-urgent {
  border-left-color: #ff4d4f;
}
.stat-urgent .stat-icon {
  color: #ff4d4f;
}
.stat-warn {
  border-left-color: #faad14;
}
.stat-warn .stat-icon {
  color: #faad14;
}
.stat-ok {
  border-left-color: #52c41a;
}
.stat-ok .stat-icon {
  color: #52c41a;
}
.stats-tip {
  flex: 1;
  min-width: 200px;
}
</style>

<style>
/* 紧急行高亮 */
.ecom-replenish .ant-table-tbody > tr.row-urgent > td {
  background: #fff2f0;
}
.ecom-replenish .ant-table-tbody > tr.row-urgent:hover > td {
  background: #ffe7e3;
}
html[data-theme='dark'] .ecom-replenish .ant-table-tbody > tr.row-urgent > td {
  background: #2c1618;
}
html[data-theme='dark'] .ecom-replenish .ant-table-tbody > tr.row-urgent:hover > td {
  background: #3a1d20;
}
</style>
