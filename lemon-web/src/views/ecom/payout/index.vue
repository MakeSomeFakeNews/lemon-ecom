<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomReport.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomReport.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('ecomProfit.payout.date')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomProfit.payout.keywordPh')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="query.status" class="status-tabs" @change="search">
        <a-tab-pane v-for="s in STATUSES" :key="s">
          <template #tab>
            {{ $t(`ecomProfit.payout.tab.${s}`) }}
            <a-badge :count="counts[s]?.cnt || 0" :overflow-count="999" :number-style="badgeStyle(s)" />
          </template>
        </a-tab-pane>
      </a-tabs>
      <TableToolbar v-model:size="size" v-model:show-search="showSearch" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="refresh">
        <a-button v-perm="'ecom:payout:import'" type="primary" @click="importRef.open()">
          <template #icon><ImportOutlined /></template>{{ $t('ecomProfit.payout.import') }}
        </a-button>
        <a-tooltip :title="$t('ecomProfit.payout.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
        <span v-if="counts[query.status]" class="sub-text">{{ summaryText }}</span>
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
            <span v-if="record.settlementNo">{{ record.settlementNo }}</span>
            <a-tag v-else color="red">{{ $t('ecomProfit.payout.unmatched') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'expected'">
            <template v-if="record.expectedAmount != null">
              <div>{{ money(record.expectedAmount) }} <span class="sub-text">{{ record.currency }}</span></div>
              <div class="sub-text">{{ money(record.expectedBase) }} · {{ record.expectedDate }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'received'">
            <template v-if="record.amount != null">
              <div>{{ money(record.amount) }} <span class="sub-text">{{ record.payCurrency || record.currency }}</span></div>
              <div class="sub-text">{{ record.payoutDate }}<template v-if="record.exchangeRate"> · {{ qty(record.exchangeRate, 6) }}</template></div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.money">
            <span v-if="record[column.key] != null" :class="{ neg: nz(record[column.key]) < 0, pos: column.diff && nz(record[column.key]) > 0 }">
              {{ money(record[column.key]) }}
            </span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="ecom_payout_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'channel'">
            <DictTag v-if="record.channel" type="ecom_payout_channel" :value="record.channel" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <PayoutImportModal ref="importRef" @success="refresh" />
    <PayoutReceiveModal ref="receiveRef" @success="refresh" />
    <PayoutMatchModal ref="matchRef" @success="refresh" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CheckCircleOutlined, ImportOutlined, InfoCircleOutlined, LinkOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import PayoutImportModal from './PayoutImportModal.vue'
import PayoutReceiveModal from './PayoutReceiveModal.vue'
import PayoutMatchModal from './PayoutMatchModal.vue'
import { deletePayout, pagePayout, payoutSummary, unmatchPayout } from '@/api/ecom/payout'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { money, nz, qty } from '@/utils/number'
import { PayoutStatus } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 回款对账：结算单导入后按净额生成「未到账」记录；登记到账或导入银行 / 收款平台流水自动匹配后为「已到账」（金额一致）或「有差异」，
 * 没有匹配到结算单的流水也在「有差异」中，可手工匹配。到账汇率与结算汇率的差额、到账金额差额生成凭证（结算单已生成收款单时）
 */
const STATUSES = Object.values(PayoutStatus)

const m = (key, title, extra = {}) => ({ title, key, dataIndex: key, width: 120, align: 'right', money: true, ...extra })
const columns = [
  { title: t('ecomProfit.payout.settlementNo'), key: 'settlementNo', dataIndex: 'settlementNo', width: 170, fixed: 'left' },
  { title: t('ecomReport.common.store'), dataIndex: 'storeName', width: 150, ellipsis: true },
  {
    title: t('ecomProfit.payout.expected'),
    key: 'expected',
    width: 160,
    align: 'right',
    exportColumns: [
      { title: t('ecomProfit.payout.expectedAmount'), dataIndex: 'expectedAmount' },
      { title: t('ecomReport.common.currency'), dataIndex: 'currency' },
      { title: t('ecomProfit.payout.expectedBase'), dataIndex: 'expectedBase' },
      { title: t('ecomProfit.payout.expectedDate'), dataIndex: 'expectedDate' }
    ]
  },
  {
    title: t('ecomProfit.payout.received'),
    key: 'received',
    width: 170,
    align: 'right',
    exportColumns: [
      { title: t('ecomProfit.payout.amount'), dataIndex: 'amount' },
      { title: t('ecomProfit.payout.payCurrency'), dataIndex: 'payCurrency' },
      { title: t('ecomProfit.payout.date'), dataIndex: 'payoutDate' },
      { title: t('ecomProfit.payout.exchangeRate'), dataIndex: 'exchangeRate' }
    ]
  },
  m('amountBase', t('ecomProfit.payout.amountBase'), { width: 130 }),
  m('fxDiff', t('ecomProfit.payout.fxDiff'), { diff: true }),
  m('amountDiff', t('ecomProfit.payout.amountDiff'), { diff: true }),
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'ecom_payout_status' },
  { title: t('ecomProfit.payout.account'), dataIndex: 'accountName', width: 130, ellipsis: true },
  { title: t('ecomProfit.payout.channel'), key: 'channel', dataIndex: 'channel', width: 120, dict: 'ecom_payout_channel' },
  { title: t('ecomProfit.payout.bankRef'), dataIndex: 'bankRef', width: 150, ellipsis: true },
  { title: t('ecomProfit.payout.voucherNo'), dataIndex: 'voucherNo', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  actionColumn(2, [t('ecomProfit.payout.receive')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-payout')

const showSearch = ref(true)
const emptyQuery = () => ({ status: PayoutStatus.PENDING, storeId: undefined, range: undefined, keyword: '' })
const query = reactive(emptyQuery())
const params = (q) => ({ status: q.status, storeId: q.storeId || undefined, beginDate: q.range?.[0], endDate: q.range?.[1], keyword: q.keyword })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePayout, query, { params })

/** 各状态的记录数与金额（按店铺筛选） */
const counts = ref({})
async function loadCounts() {
  const res = (await payoutSummary({ storeId: query.storeId || undefined })) || []
  counts.value = Object.fromEntries(res.map((r) => [r.status, r]))
}
function refresh() {
  load()
  loadCounts()
}
onMounted(refresh)
function resetQuery() {
  Object.assign(query, { ...emptyQuery(), status: query.status })
  search()
  loadCounts()
}
const badgeStyle = (s) => ({ backgroundColor: s === PayoutStatus.DIFF ? '#ff4d4f' : s === PayoutStatus.PENDING ? '#fa8c16' : '#52c41a' })
const summaryText = computed(() => {
  const c = counts.value[query.status]
  return query.status === PayoutStatus.PENDING
    ? t('ecomProfit.payout.pendingSummary', { n: c.cnt, amount: money(c.expectedBase) })
    : t('ecomProfit.payout.receivedSummary', { n: c.cnt, amount: money(c.amountBase) })
})

const importRef = ref()
const receiveRef = ref()
const matchRef = ref()
const unmatch = useAction(unmatchPayout, { successKey: 'common.msg.operationSuccess', reload: () => refresh() })
const remove = useAction(deletePayout, { successKey: 'common.msg.deleteSuccess', reload: () => refresh() })

const rowActions = (record) => {
  const flow = !record.settlementId
  const pending = record.status === PayoutStatus.PENDING
  return [
    { key: 'receive', label: t('ecomProfit.payout.receive'), icon: CheckCircleOutlined, show: pending, perm: 'ecom:payout:receive', onClick: () => receiveRef.value.open(record) },
    { key: 'match', label: t('ecomProfit.payout.match'), icon: LinkOutlined, show: flow, perm: 'ecom:payout:receive', onClick: () => matchRef.value.open(record) },
    {
      key: 'unmatch',
      label: t('ecomProfit.payout.unmatch'),
      icon: RollbackOutlined,
      show: !flow && !pending,
      perm: 'ecom:payout:remove',
      confirm: t('ecomProfit.payout.confirmUnmatch', { no: record.settlementNo }),
      onClick: () => unmatch(record.id)
    },
    { type: 'delete', show: flow, perm: 'ecom:payout:remove', confirm: t('ecomProfit.payout.confirmDelete'), onClick: () => remove(record.id) }
  ]
}
</script>

<style scoped>
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.status-tabs :deep(.ant-badge) {
  margin-left: 6px;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  align-self: center;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.pos {
  color: var(--status-success, #389e0d);
}
</style>
