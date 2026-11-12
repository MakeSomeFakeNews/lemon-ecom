<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.fee.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.common.account')" name="accountId">
          <a-select v-model:value="query.accountId" :placeholder="$t('common.placeholder.all')" allow-clear :options="accounts.map((a) => ({ value: a.id, label: a.name }))" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.feeType')" name="feeType">
          <a-select v-model:value="query.feeType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_fee_type)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_fee_status)" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.feeDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card summary-card">
      <div class="summary">
        <div class="stat">
          <div class="stat-label">{{ $t('ecomOwh.fee.total') }}</div>
          <div class="stat-value">¥{{ money(summary.total) }}</div>
        </div>
        <div v-for="d in dict.ecom_owh_fee_type || []" :key="d.value" class="stat">
          <div class="stat-label">{{ d.label }}</div>
          <div class="stat-value">¥{{ money(summary.byType?.[d.value]) }}</div>
        </div>
      </div>
      <div class="sub-text tip">{{ $t('ecomOwh.fee.tip') }}</div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhFee:sync'" type="primary" @click="openSync">
          <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.fee.sync') }}
        </a-button>
        <a-button v-perm="'ecom:owhFee:sync'" :disabled="!selectedKeys.length" @click="repost">
          <template #icon><RedoOutlined /></template>{{ $t('ecomOwh.fee.repost') }}
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
          <template v-if="column.key === 'feeType'">
            <DictTag type="ecom_owh_fee_type" :value="record.feeType" />
          </template>
          <template v-else-if="column.key === 'ref'">
            <div>{{ record.refNo || '-' }}</div>
            <div v-if="record.outboundNo" class="sub-text">
              <a @click="router.push({ path: '/ecom/owh-outbound', query: { id: record.outboundId } })">{{ record.outboundNo }}</a> · {{ record.packageNo }}
            </div>
            <div v-else-if="record.inboundNo" class="sub-text">
              <a @click="router.push({ path: '/ecom/owh-inbound', query: { id: record.inboundId } })">{{ record.inboundNo }}</a>
            </div>
          </template>
          <template v-else-if="column.key === 'sku'">
            <div>{{ record.sku || '-' }}</div>
            <div v-if="record.materialName" class="sub-text">{{ record.materialName }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">{{ record.currency }} {{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'amountBase'">{{ record.amountBase != null ? `¥${money(record.amountBase)}` : '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_owh_fee_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'detail', label: $t('ecomOwh.fee.allocations'), onClick: () => showAllocations(record) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="syncOpen" :title="$t('ecomOwh.fee.sync')" :confirm-loading="syncing" @ok="doSync">
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomOwh.common.account')">
          <a-select v-model:value="syncForm.accountId" allow-clear :placeholder="$t('ecomOwh.fee.allAccounts')" :options="accounts.map((a) => ({ value: a.id, label: a.name }))" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.feeDate')">
          <a-range-picker v-model:value="syncForm.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="allocOpen" :title="$t('ecomOwh.fee.allocations')" :footer="null" width="min(760px, 100vw)">
      <a-table row-key="id" size="small" :columns="allocColumns" :data-source="allocations" :pagination="false" :locale="{ emptyText: $t('ecomOwh.fee.noAllocations') }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'target'">
            <div>{{ record.storeName || '-' }}</div>
            <div class="sub-text">{{ record.orderNo ? $t('ecomOwh.fee.toOrder', { no: record.orderNo }) : record.sellerSku || $t('ecomOwh.fee.toStore') }}</div>
          </template>
          <template v-else-if="column.key === 'itemType'">
            <DictTag type="ecom_settlement_item_type" :value="record.itemType" />
          </template>
          <template v-else-if="column.key === 'amountBase'">¥{{ money(record.amountBase) }}</template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { RedoOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { owhAccountOptions, owhFeeAllocations, owhFeeSummary, pageOwhFee, repostOwhFee, syncOwhFee } from '@/api/ecom/owh'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { showSyncResults } from '../owh-outbound/owh'

/**
 * 海外仓费用：同步服务商的仓租、操作费、尾程运费、入库费等（本位币按费用日期所在月汇率折算）；
 * 出库单的操作费、尾程运费计入订单物流成本，仓租、入库费按 SKU 分摊到店铺 SKU（利润报表扣除），可查看入账明细
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('ecom_owh_fee_type', 'ecom_owh_fee_status')
const accounts = ref([])

const columns = [
  { title: t('ecomOwh.fee.feeDate'), dataIndex: 'feeDate', width: 110 },
  { title: t('ecomOwh.common.account'), dataIndex: 'accountName', width: 140, ellipsis: true },
  { title: t('ecomOwh.fee.feeType'), key: 'feeType', dataIndex: 'feeType', width: 100, dict: 'ecom_owh_fee_type' },
  { title: t('ecomOwh.fee.ref'), key: 'ref', width: 200, exportColumns: [{ title: t('ecomOwh.fee.ref'), dataIndex: 'refNo' }, { title: t('ecomOwh.fee.outboundNo'), dataIndex: 'outboundNo' }] },
  { title: t('ecomOwh.common.warehouse'), dataIndex: 'warehouseName', width: 130, ellipsis: true },
  { title: t('ecomOwh.fee.sku'), key: 'sku', width: 160, exportValue: (r) => r.sku },
  { title: t('ecomOwh.fee.amount'), key: 'amount', width: 120, align: 'right', exportValue: (r) => Number(r.amount) },
  { title: t('ecomOwh.fee.rate'), dataIndex: 'exchangeRate', width: 90, align: 'right' },
  { title: t('ecomOwh.fee.amountBase'), key: 'amountBase', dataIndex: 'amountBase', width: 110, align: 'right' },
  { title: t('ecomOwh.fee.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_owh_fee_status' },
  { title: t('ecomOwh.fee.postTarget'), dataIndex: 'postTarget', width: 180, ellipsis: true },
  { title: t('ecomOwh.fee.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-fee')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, accountId: undefined, feeType: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const params = ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange, exportAll } = useTable(pageOwhFee, query, { params })

const summary = ref({})
async function loadSummary() {
  const p = params({ ...query })
  summary.value = (await owhFeeSummary({ accountId: p.accountId, beginDate: p.beginDate, endDate: p.endDate })) || {}
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
onMounted(async () => {
  accounts.value = (await owhAccountOptions()) || []
  reload()
})

const syncOpen = ref(false)
const syncing = ref(false)
const syncForm = reactive({ accountId: undefined, range: undefined })
function openSync() {
  syncForm.accountId = query.accountId
  syncForm.range = [dayjs().subtract(7, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  syncOpen.value = true
}
async function doSync() {
  syncing.value = true
  try {
    showSyncResults(await syncOwhFee({ accountId: syncForm.accountId || null, beginDate: syncForm.range?.[0], endDate: syncForm.range?.[1] }), 'ecomOwh.fee.synced')
    syncOpen.value = false
    reload()
  } finally {
    syncing.value = false
  }
}

async function repost() {
  const n = await repostOwhFee([...selectedKeys.value])
  message.success(t('ecomOwh.fee.reposted', { n }))
  selectedKeys.value = []
  reload()
}

const allocOpen = ref(false)
const allocations = ref([])
const allocColumns = [
  { title: t('ecomOwh.fee.allocTarget'), key: 'target', width: 180 },
  { title: t('ecomOwh.fee.itemType'), key: 'itemType', width: 120 },
  { title: t('ecomOwh.fee.allocDate'), dataIndex: 'allocDate', width: 110 },
  { title: t('ecomOwh.fee.basis'), dataIndex: 'basisValue', width: 90, align: 'right' },
  { title: t('ecomOwh.fee.amountBase'), key: 'amountBase', width: 110, align: 'right' }
]
async function showAllocations(record) {
  allocations.value = (await owhFeeAllocations(record.id)) || []
  allocOpen.value = true
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
  font-size: 18px;
  font-weight: 500;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.selected-tip {
  color: var(--app-text-3);
}
</style>
