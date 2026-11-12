<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomReport.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomReport.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_settlement_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomReport.settlement.keywordPlaceholder')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:show-search="showSearch"
        v-model:checked="checked"
        :columns="columns"
        :exporter="exportAll"
        @refresh="load"
      >
        <a-button v-perm="'ecom:settlement:import'" type="primary" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('ecomReport.settlement.import') }}
        </a-button>
        <a-tooltip :title="`${$t('ecomReport.settlement.importTip')} ${$t('ecomProfit.settlement.rawTypeTip')}`"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
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
          <template v-if="column.key === 'settlementNo'">
            <a @click="detailRef.open(record.id)">{{ record.settlementNo }}</a>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.periodStart }} ~ {{ record.periodEnd }}</template>
          <template v-else-if="column.key === 'exchangeRate'">{{ qty(record.exchangeRate, 6) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="ecom_settlement_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'netBase'">
            <span :class="{ neg: nz(record.netBase) < 0 }" class="strong">¥{{ money(record.netBase) }}</span>
          </template>
          <template v-else-if="column.money">
            <span :class="{ neg: nz(record[column.key]) < 0, strong: column.strong }">{{ money(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ImportModal
      v-model:open="importOpen"
      :title="$t('ecomReport.settlement.importTitle')"
      :columns="importColumns"
      :sample="importSample"
      :importer="importSettlements"
      @success="load"
    />
    <SettlementDetail ref="detailRef" @confirm="(s) => confirmRef.open(s)" />
    <SettlementConfirmModal ref="confirmRef" @success="onConfirmed" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ImportOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import SettlementDetail from './SettlementDetail.vue'
import SettlementConfirmModal from './SettlementConfirmModal.vue'
import { deleteSettlement, importSettlements, pageSettlement } from '@/api/ecom/settlement'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, nz, qty } from '@/utils/number'
import { SettlementStatus } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 平台结算：导入结算报告（同一店铺 + 结算编号的行合并为一张结算单）→ 查看明细 → 确认（回写订单费用，可选生成收款单）。
 * 金额为店铺币种，本位币净额按汇率折算
 */
const dict = useDict('ecom_settlement_status')

const m = (key, extra = {}) => ({ title: t(`ecomReport.settlement.${key}`), key, dataIndex: key, width: 120, align: 'right', money: true, ...extra })
const columns = [
  { title: t('ecomReport.settlement.settlementNo'), key: 'settlementNo', dataIndex: 'settlementNo', width: 170, fixed: 'left' },
  { title: t('ecomReport.common.store'), key: 'storeName', dataIndex: 'storeName', width: 160, ellipsis: true },
  {
    title: t('ecomReport.settlement.period'),
    key: 'period',
    width: 200,
    exportColumns: [
      { title: t('ecomReport.settlement.periodStart'), dataIndex: 'periodStart' },
      { title: t('ecomReport.settlement.periodEnd'), dataIndex: 'periodEnd' }
    ]
  },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'ecom_settlement_status' },
  { title: t('ecomReport.common.currency'), key: 'currency', dataIndex: 'currency', width: 70 },
  // 净额在前（最关心），各项收入 / 费用在后
  m('netAmount', { width: 130, strong: true }),
  m('netBase', { width: 140, strong: true }),
  m('salesAmount'),
  m('refundAmount'),
  m('commissionFee'),
  m('fulfillmentFee'),
  m('storageFee', { width: 110 }),
  m('adFee', { width: 110 }),
  m('otherFee', { width: 110 }),
  m('adjustment', { width: 110 }),
  { title: t('ecomReport.settlement.exchangeRate'), key: 'exchangeRate', dataIndex: 'exchangeRate', width: 100, align: 'right' },
  { title: t('ecomReport.settlement.itemCount'), key: 'itemCount', dataIndex: 'itemCount', width: 90, align: 'right' },
  // 查看、确认、删除（已确认的只有查看）
  actionColumn(3, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-settlement')

const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, status: undefined, keyword: '' })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageSettlement, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const detailRef = ref()
const confirmRef = ref()
const remove = useAction(deleteSettlement, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const rowActions = (record) => {
  const imported = record.status === SettlementStatus.IMPORTED
  return [
    { type: 'view', onClick: () => detailRef.value.open(record.id) },
    { type: 'confirm', show: imported, perm: 'ecom:settlement:confirm', onClick: () => confirmRef.value.open(record) },
    {
      type: 'delete',
      show: imported,
      perm: 'ecom:settlement:remove',
      confirm: t('ecomReport.settlement.confirmDelete', { no: record.settlementNo }),
      onClick: () => remove(record.id)
    }
  ]
}

/** 确认成功：刷新列表，详情打开时一并刷新 */
function onConfirmed() {
  load()
  detailRef.value?.reload()
}

// ---------------------------------------------------------------- 导入（列与后端 SettlementImportRow 一致）
const importOpen = ref(false)
const importColumns = [
  { title: t('ecomReport.settlement.storeCode'), key: 'storeCode', required: true, width: 12 },
  { title: t('ecomReport.settlement.settlementNo'), key: 'settlementNo', required: true, width: 20 },
  { title: t('ecomReport.settlement.periodStart'), key: 'periodStart', required: true, type: 'date', width: 14 },
  { title: t('ecomReport.settlement.periodEnd'), key: 'periodEnd', required: true, type: 'date', width: 14 },
  { title: t('ecomReport.common.currency'), key: 'currency', width: 8 },
  { title: t('ecomReport.settlement.postedDate'), key: 'postedDate', type: 'date', width: 14 },
  { title: t('ecomReport.settlement.itemType'), key: 'itemType', dict: 'ecom_settlement_item_type', width: 12 },
  { title: t('ecomProfit.settlement.rawType'), key: 'rawType', width: 28 },
  { title: t('ecomReport.common.platformOrderNo'), key: 'platformOrderNo', width: 24 },
  { title: t('ecomReport.common.sellerSku'), key: 'sellerSku', width: 16 },
  { title: t('common.field.amount'), key: 'amount', required: true, type: 'number', width: 12 },
  { title: t('common.field.description'), key: 'description', width: 30 }
]
const importSample = {
  storeCode: 'US01',
  settlementNo: '12345678901',
  periodStart: '2026-10-01',
  periodEnd: '2026-10-14',
  currency: 'USD',
  postedDate: '2026-10-03',
  itemType: t('ecomReport.settlement.sampleItemType'),
  rawType: 'Principal',
  platformOrderNo: '111-2223334-5556667',
  sellerSku: 'SKU-001',
  amount: '25.99',
  description: t('ecomReport.settlement.sampleDescription')
}
</script>

<style scoped>
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.strong {
  font-weight: 600;
}
</style>
