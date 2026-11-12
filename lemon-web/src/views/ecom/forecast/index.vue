<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomAnalytics.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomAnalytics.common.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecomAnalytics.forecast.method')" name="method">
          <a-select v-model:value="query.method" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_forecast_method)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:forecast:generate'" type="primary" :loading="generating" @click="generate">
          <template #icon><ThunderboltOutlined /></template>{{ $t('ecomAnalytics.forecast.generate') }}
        </a-button>
        <a-button v-perm="'ecom:forecast:edit'" @click="settingModal.open()">
          <template #icon><SettingOutlined /></template>{{ $t('ecomAnalytics.forecast.setting') }}
        </a-button>
        <a-button @click="eventDrawer.open(query.storeId)">
          <template #icon><CalendarOutlined /></template>{{ $t('ecomAnalytics.forecast.events') }}
        </a-button>
        <span class="sub-text">{{ $t('ecomAnalytics.forecast.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="listingId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <a @click="openDetail(record)">{{ record.sellerSku }}</a>
            <div class="sub-text cell-ellipsis" :title="record.title">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode || '-' }}</div>
            <div class="sub-text cell-ellipsis">{{ record.materialName }}</div>
          </template>
          <template v-else-if="column.key === 'method'">
            <DictTag type="ecom_forecast_method" :value="record.method" />
            <a-tag v-if="record.fixedMethod" color="purple" :bordered="false" class="mini-tag">{{ $t('ecomAnalytics.forecast.manual') }}</a-tag>
          </template>
          <template v-else-if="column.mape">
            <span :class="{ best: record.method === column.mape }">{{ percent(record[column.key], 1) }}</span>
          </template>
          <template v-else-if="column.key === 'stockoutDays'">
            <a-tooltip v-if="record.stockoutDays" :title="$t('ecomFix.stockout.listTip')">
              <a-tag color="red" :bordered="false">{{ record.stockoutDays }}</a-tag>
            </a-tooltip>
            <span v-else class="sub-text">0</span>
          </template>
          <template v-else-if="column.key === 'adjustedDaily'">
            <span :class="{ adjusted: record.stockoutDays && Number(record.adjustedDaily) !== Number(record.historyDaily) }">{{ qty(record.adjustedDaily, 2) }}</span>
          </template>
          <template v-else-if="['historyDaily', 'forecast30', 'forecast60', 'forecast90'].includes(column.key)">
            {{ qty(record[column.key], column.key === 'historyDaily' ? 2 : 0) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'detail', onClick: () => openDetail(record) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ForecastDetail ref="detailRef" @changed="load" />
    <SettingModal ref="settingModal" />
    <EventDrawer ref="eventDrawer" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CalendarOutlined, SettingOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { actionColumn } from '@/components/common/tableActions'
import { percent } from '@/components/charts/charts'
import { generateForecast, pageForecast } from '@/api/ecom/analytics'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { qty } from '@/utils/number'
import { ForecastMethod } from '@/constants/enums'
import { t } from '@/locales'
import ForecastDetail from './ForecastDetail.vue'
import SettingModal from './SettingModal.vue'
import EventDrawer from './EventDrawer.vue'

/**
 * 销量预测（/ecom/analytics/forecast）：每个在售 SKU 的预测方法（回测 MAPE 最小或手工指定）、各方法的回测 MAPE、
 * 最近 28 天日均与未来 30 / 60 / 90 天预测；点击 SKU 看单品预测图与方法对比。定时任务每天 04:00 生成，也可立即生成
 */
const dict = useDict('ecom_forecast_method')
const mapeCol = (key, method) => ({ title: `${t('ecomAnalytics.forecast.mape')} · ${t(`ecomAnalytics.forecast.short${method}`)}`, key, dataIndex: key,
  width: 110, align: 'right', mape: method })
const columns = [
  { title: t('ecomAnalytics.common.store'), dataIndex: 'storeName', width: 150, ellipsis: true },
  { title: t('ecomAnalytics.common.sku'), key: 'sku', width: 220, exportValue: (r) => `${r.sellerSku} ${r.title || ''}` },
  { title: t('ecomAnalytics.common.material'), key: 'material', width: 180, exportValue: (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ') },
  { title: t('ecomAnalytics.forecast.method'), key: 'method', dataIndex: 'method', width: 150, dict: 'ecom_forecast_method' },
  mapeCol('mapeMa', ForecastMethod.MA),
  mapeCol('mapeWma', ForecastMethod.WMA),
  mapeCol('mapeHolt', ForecastMethod.HOLT),
  mapeCol('mapeSeasonal', ForecastMethod.SEASONAL),
  { title: t('ecomAnalytics.forecast.historyDaily'), key: 'historyDaily', dataIndex: 'historyDaily', width: 110, align: 'right' },
  { title: t('ecomFix.stockout.adjustedDaily'), key: 'adjustedDaily', dataIndex: 'adjustedDaily', width: 120, align: 'right' },
  { title: t('ecomFix.stockout.days'), key: 'stockoutDays', dataIndex: 'stockoutDays', width: 100, align: 'center' },
  { title: t('ecomAnalytics.forecast.next30'), key: 'forecast30', dataIndex: 'forecast30', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.next60'), key: 'forecast60', dataIndex: 'forecast60', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.next90'), key: 'forecast90', dataIndex: 'forecast90', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.generatedTime'), dataIndex: 'generatedTime', width: 160 },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-forecast')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, keyword: undefined, method: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageForecast, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detailRef = ref()
const settingModal = ref()
const eventDrawer = ref()
const openDetail = (r) => detailRef.value.open(r.listingId)

const generating = ref(false)
async function generate() {
  generating.value = true
  try {
    const r = await generateForecast(query.storeId)
    if (r.failed) message.warning(t('ecomAnalytics.forecast.generatedPartly', { total: r.total, failed: r.failed, error: r.firstError || '' }))
    else message.success(t('ecomAnalytics.forecast.generated', { total: r.total }))
    load()
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.best {
  color: var(--status-success, #389e0d);
  font-weight: 600;
}
.mini-tag {
  margin-left: 4px;
}
.adjusted {
  color: #d46b08;
  font-weight: 600;
}
</style>
