<template>
  <div class="table-page ecom-fba-health">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="refresh" @reset="resetQuery">
        <a-form-item :label="$t('ecomFba.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomFba.health.keywordPh')" allow-clear @press-enter="refresh" />
        </a-form-item>
        <a-form-item :label="$t('ecomFba.health.advice')" name="advice">
          <a-select v-model:value="query.advice" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_fba_advice)" />
        </a-form-item>
        <a-form-item name="excessDays">
          <template #label>
            <a-tooltip :title="$t('ecomFba.health.excessDaysTip')">{{ $t('ecomFba.health.excessDays') }}</a-tooltip>
          </template>
          <a-input-number v-model:value="query.excessDays" :min="1" :max="999" :addon-after="$t('ecomFba.common.dayUnit')" />
        </a-form-item>
        <a-form-item name="excessOnly">
          <a-checkbox v-model:checked="query.excessOnly" @change="search">{{ $t('ecomFba.health.excessOnly') }}</a-checkbox>
          <a-checkbox v-model:checked="query.agedOnly" @change="search">{{ $t('ecomFba.health.agedOnly') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <HealthSummary ref="summaryRef" @advice="(a) => ((query.advice = a), search())" />

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="refresh">
        <a-tooltip :title="query.storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
          <a-button v-perm="'ecom:fbaHealth:sync'" type="primary" :disabled="!query.storeId" @click="sync(query.storeId)">
            <template #icon><CloudSyncOutlined /></template>{{ $t('ecomFba.common.syncFromAmazon') }}
          </a-button>
        </a-tooltip>
        <a-tooltip :title="query.storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
          <a-button v-perm="'ecom:fbaHealth:sync'" :disabled="!query.storeId" @click="rebuild(query.storeId)">
            <template #icon><CalculatorOutlined /></template>{{ $t('ecomFba.health.rebuild') }}
          </a-button>
        </a-tooltip>
        <a-button v-perm="'ecom:fbaHealth:sync'" @click="uploadRef.open(query.storeId)">
          <template #icon><UploadOutlined /></template>{{ $t('ecomFba.common.uploadReport') }}
        </a-button>
        <a-button v-perm="'ecom:fbaRemoval:add'" :disabled="!selectedKeys.length" @click="createRemoval">
          <template #icon><ExportOutlined /></template>{{ $t('ecomFba.health.createRemoval') }}
        </a-button>
        <a-button @click="rateRef.open()"><template #icon><SettingOutlined /></template>{{ $t('ecomFba.health.rates') }}</a-button>
        <a-tooltip :title="$t('ecomFba.health.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div>{{ record.sellerSku }}</div>
            <div class="sub-text">{{ [record.fnsku, record.asin].filter(Boolean).join(' · ') }}</div>
            <div v-if="record.title" class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <template v-if="record.materialId">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.qty">
            <span :class="{ aged: column.aged && Number(record[column.key]) > 0 }">{{ n0(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'ageSource'">{{ record.ageSource ? $t(`ecomFba.ageSource.${record.ageSource}`) : '-' }}</template>
          <template v-else-if="column.key === 'daysOfSupply'">{{ record.daysOfSupply == null ? $t('ecomFba.common.infinite') : n0(record.daysOfSupply) }}</template>
          <template v-else-if="column.key === 'excessQty'">
            <b v-if="Number(record.excessQty) > 0" class="excess">{{ n0(record.excessQty) }}</b>
            <span v-else class="sub-text">0</span>
          </template>
          <template v-else-if="column.key === 'sizeCategory'">
            <DictTag v-if="record.sizeCategory" type="ecom_fba_size_category" :value="record.sizeCategory" />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.fee">
            <span v-if="record[column.key] != null">{{ money(record[column.key]) }} <span class="sub-text">{{ record.currency }}</span></span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'advice'"><DictTag type="ecom_fba_advice" :value="record.advice" /></template>
        </template>
      </a-table>
    </a-card>

    <AmzReportUploadModal ref="uploadRef" :types="[AmzReportType.FBA_INVENTORY_PLANNING, AmzReportType.FBA_MYI_INVENTORY]" @success="refresh" />
    <StorageRateDrawer ref="rateRef" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CalculatorOutlined, CloudSyncOutlined, ExportOutlined, InfoCircleOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import HealthSummary from './HealthSummary.vue'
import StorageRateDrawer from './StorageRateDrawer.vue'
import { pageFbaHealth, rebuildFbaHealth, syncFbaHealth } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { n0 } from './fba'

/**
 * FBA 库存健康：可售 / 在途 / 预留 / 不可售 / 调查中、库龄分段、日均与可售天数、冗余、预估仓储费与处理建议（金额为店铺币种，汇总为本位币）
 */
const router = useRouter()
const dict = useDict('ecom_fba_advice')

const q = (key, title, extra = {}) => ({ title, key, dataIndex: key, width: 90, align: 'right', qty: true, ...extra })
const f = (key, title) => ({ title, key, dataIndex: key, width: 120, align: 'right', fee: true })
const columns = [
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true, fixed: 'left' },
  {
    title: t('ecomFba.common.sku'),
    key: 'sku',
    width: 220,
    fixed: 'left',
    exportColumns: [
      { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku' },
      { title: t('ecomFba.common.fnsku'), dataIndex: 'fnsku' },
      { title: t('ecomFba.common.asin'), dataIndex: 'asin' },
      { title: t('ecomFba.common.title'), dataIndex: 'title' }
    ]
  },
  {
    title: t('ecomFba.common.material'),
    key: 'material',
    width: 160,
    exportColumns: [
      { title: t('ecomFba.common.material'), dataIndex: 'materialCode' },
      { title: t('common.field.name'), dataIndex: 'materialName' }
    ]
  },
  q('availableQty', t('ecomFba.health.available')),
  q('inboundQty', t('ecomFba.health.inbound')),
  q('reservedQty', t('ecomFba.health.reserved')),
  q('unfulfillableQty', t('ecomFba.health.unfulfillable')),
  q('researchingQty', t('ecomFba.health.researching')),
  q('age0To90', t('ecomFba.age.a0')),
  q('age91To180', t('ecomFba.age.a91')),
  q('age181To270', t('ecomFba.age.a181'), { aged: true }),
  q('age271To365', t('ecomFba.age.a271'), { aged: true }),
  q('age365Plus', t('ecomFba.age.a365'), { aged: true }),
  { title: t('ecomFba.health.ageSource'), key: 'ageSource', dataIndex: 'ageSource', width: 100, exportValue: (r) => (r.ageSource ? t(`ecomFba.ageSource.${r.ageSource}`) : '') },
  q('dailySales', t('ecomFba.health.dailySales')),
  { title: t('ecomFba.health.daysOfSupply'), key: 'daysOfSupply', dataIndex: 'daysOfSupply', width: 90, align: 'right' },
  { title: t('ecomFba.health.excessQty'), key: 'excessQty', dataIndex: 'excessQty', width: 90, align: 'right' },
  q('agedQty', t('ecomFba.health.agedQty'), { aged: true }),
  { title: t('ecomFba.health.sizeCategory'), key: 'sizeCategory', dataIndex: 'sizeCategory', width: 90, dict: 'ecom_fba_size_category' },
  f('monthlyFee', t('ecomFba.health.monthlyFee')),
  f('agedFee', t('ecomFba.health.agedFee')),
  { title: t('ecomFba.health.advice'), key: 'advice', dataIndex: 'advice', width: 110, dict: 'ecom_fba_advice', fixed: 'right' },
  { title: t('ecomFba.health.snapshotDate'), dataIndex: 'snapshotDate', width: 110 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-health')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, keyword: undefined, advice: undefined, excessOnly: false, agedOnly: false, excessDays: 90 })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll, rowSelection, selectedKeys } = useTable(pageFbaHealth, query, {
  params: (p) => ({ ...p, excessOnly: p.excessOnly || undefined, agedOnly: p.agedOnly || undefined })
})
const summaryRef = ref()
function refresh() {
  search()
  summaryRef.value?.load({ storeId: query.storeId || undefined, excessDays: query.excessDays || undefined })
}
onMounted(refresh)
function resetQuery() {
  Object.assign(query, emptyQuery())
  refresh()
}

const uploadRef = ref()
const rateRef = ref()
const sync = useAction(syncFbaHealth, { successKey: (ids) => t('ecomFba.common.syncCreated', { n: (ids || []).length }) })
const rebuild = useAction(rebuildFbaHealth, {
  confirm: t('ecomFba.health.rebuildConfirm'),
  successKey: (n) => t('ecomFba.health.rebuildDone', { n }),
  reload: () => refresh()
})

/** 勾选的 SKU 生成移除计划：数量取超龄，没有超龄取冗余，都没有取可售 */
function createRemoval() {
  const rows = list.value.filter((r) => selectedKeys.value.includes(r.id))
  const stores = new Set(rows.map((r) => r.storeId))
  if (stores.size !== 1) {
    message.warning(t('ecomFba.health.sameStore'))
    return
  }
  const lines = rows.map((r) => ({ sku: r.sellerSku, qty: Number(r.agedQty) || Number(r.excessQty) || Number(r.availableQty) || 1 }))
  router.push({ path: '/ecom/fba-removal', query: { storeId: rows[0].storeId, plan: JSON.stringify(lines) } })
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.aged {
  color: #d46b08;
}
.excess {
  color: #cf1322;
}
</style>
