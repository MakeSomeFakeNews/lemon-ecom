<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.fee.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.common.account')" name="accountId">
          <a-select v-model:value="query.accountId" :placeholder="$t('common.placeholder.all')" allow-clear :options="accounts.map((a) => ({ value: a.id, label: a.name }))" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.feeType')" name="feeType">
          <a-select v-model:value="query.feeType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_fee_type)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_fee_status)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.fee.feeDate')" name="range" data-wide>
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <OwhStatBar :items="stats" />

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:owhFee:sync'" type="primary" @click="openSync">
          <template #icon><SyncOutlined /></template>{{ $t('ecomOwh.fee.sync') }}
        </a-button>
        <a-tooltip :title="$t('ecomOwh.fee.tip')" :overlay-style="{ maxWidth: '420px' }"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
        <template v-if="selectedKeys.length">
          <a-button v-perm="'ecom:owhFee:sync'" :loading="reposting" @click="repost">
            <template #icon><RedoOutlined /></template>{{ $t('ecomOwh.fee.repost') }}
          </a-button>
          <span class="selected-tip">
            {{ $t('common.table.selected', { n: selectedKeys.length }) }} <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
          </span>
        </template>
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
            <div class="nowrap">{{ record.refNo || '-' }}</div>
            <div v-if="record.outboundNo" class="sub-text nowrap">
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
          <template v-else-if="column.key === 'amount'">{{ money(record.amount) }} <span class="sub-text">{{ record.currency }}</span></template>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InfoCircleOutlined, RedoOutlined, SyncOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import OwhStatBar from '@/components/ecom/owh/OwhStatBar.vue'
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
  { title: t('ecomOwh.fee.feeType'), key: 'feeType', dataIndex: 'feeType', width: 110, dict: 'ecom_owh_fee_type' },
  { title: t('ecomOwh.fee.ref'), key: 'ref', width: 280, exportColumns: [{ title: t('ecomOwh.fee.ref'), dataIndex: 'refNo' }, { title: t('ecomOwh.fee.outboundNo'), dataIndex: 'outboundNo' }] },
  { title: t('ecomOwh.common.warehouse'), dataIndex: 'warehouseName', width: 180, ellipsis: true },
  { title: t('ecomOwh.fee.sku'), key: 'sku', width: 160, ellipsis: true, exportValue: (r) => r.sku },
  { title: t('ecomOwh.fee.amount'), key: 'amount', width: 120, align: 'right', exportValue: (r) => Number(r.amount) },
  { title: t('ecomOwh.fee.rate'), dataIndex: 'exchangeRate', width: 90, align: 'right' },
  { title: t('ecomOwh.fee.amountBase'), key: 'amountBase', dataIndex: 'amountBase', width: 120, align: 'right' },
  { title: t('ecomOwh.fee.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_owh_fee_status' },
  { title: t('ecomOwh.fee.postTarget'), dataIndex: 'postTarget', width: 180, ellipsis: true },
  { title: t('ecomOwh.fee.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(1, [t('ecomOwh.fee.allocations')])
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
const stats = computed(() => [
  { key: 'total', label: t('ecomOwh.fee.total'), value: `¥${money(summary.value.total)}` },
  ...(dict.ecom_owh_fee_type || []).map((d) => ({ key: d.value, label: d.label, value: `¥${money(summary.value.byType?.[d.value])}` }))
])
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

const reposting = ref(false)
async function repost() {
  reposting.value = true
  try {
    const n = await repostOwhFee([...selectedKeys.value])
    message.success(t('ecomOwh.fee.reposted', { n }))
    selectedKeys.value = []
    reload()
  } finally {
    reposting.value = false
  }
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.nowrap {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.tip-icon {
  align-self: center;
  color: var(--app-text-3);
  cursor: help;
}
.selected-tip {
  align-self: center;
  color: var(--app-text-3);
}
</style>
