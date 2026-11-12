<template>
  <div class="tab-body">
    <div class="filters">
      <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.field.status')" :options="statusOptions" class="w140" @change="search" />
      <a-select v-model:value="query.claimType" allow-clear :placeholder="$t('ecomFba.claim.type')" :options="toOptions(dict.ecom_fba_claim_type)" class="w160" @change="search" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomFba.claim.keywordPh')" allow-clear class="w260" @search="search" />
      <a-checkbox v-model:checked="query.expiring" @change="search">{{ $t('ecomFba.claim.expiringDays', { n: 7 }) }}</a-checkbox>
    </div>
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <a-button v-perm="'ecom:fbaClaim:add'" @click="createRef.open(storeId)"><template #icon><PlusOutlined /></template>{{ $t('ecomFba.claim.add') }}</a-button>
      <a-tooltip :title="$t('ecomFba.claim.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
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
        <template v-if="column.key === 'claimType'"><DictTag type="ecom_fba_claim_type" :value="record.claimType" /></template>
        <template v-else-if="column.key === 'status'"><DictTag type="ecom_fba_claim_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'sku'">
          <div>{{ record.sellerSku || '-' }}</div>
          <div v-if="record.fnsku" class="sub-text">{{ record.fnsku }}</div>
        </template>
        <template v-else-if="column.key === 'qty'">{{ n0(record.qty) }}</template>
        <template v-else-if="column.key === 'amount'">
          <div>{{ money(record.estAmount) }} <span class="sub-text">{{ record.currency }}</span></div>
          <div class="sub-text">{{ money(record.unitValue) }} × {{ n0(record.qty) }}</div>
        </template>
        <template v-else-if="column.key === 'deadline'">
          <template v-if="record.deadline">
            <div>{{ record.deadline }}</div>
            <div v-if="open(record) && record.daysLeft != null" :class="record.daysLeft < 0 ? 'expired' : record.daysLeft <= 7 ? 'soon' : 'sub-text'">
              {{ record.daysLeft < 0 ? $t('ecomFba.claim.expired', { n: -record.daysLeft }) : $t('ecomFba.claim.daysLeft', { n: record.daysLeft }) }}
            </div>
          </template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'paid'">
          <template v-if="Number(record.paidAmount) || Number(record.paidQty)">
            <div>{{ money(record.paidAmount) }} <span class="sub-text">{{ record.currency }} · {{ n0(record.paidQty) }}</span></div>
            <div class="sub-text">{{ money(record.paidBase) }} · {{ record.paidDate }}</div>
          </template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
    <ClaimEditModal ref="editRef" @success="changed" />
    <ClaimCreateModal ref="createRef" @success="changed" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { InfoCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import ClaimEditModal from './ClaimEditModal.vue'
import ClaimCreateModal from './ClaimCreateModal.vue'
import { deleteClaim, pageClaim } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { FbaClaimStatus } from '@/constants/enums'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'

/** 索赔清单：storeId 由页面传入；summary 用于状态下拉显示条数；@changed 处理后刷新汇总 */
const props = defineProps({ storeId: Number, summary: { type: Object, default: () => ({}) } })
const emit = defineEmits(['changed'])
const dict = useDict('ecom_fba_claim_type', 'ecom_fba_claim_status')

const statusOptions = computed(() =>
  Object.values(FbaClaimStatus).map((s) => {
    const cnt = (props.summary.byStatus || []).find((x) => x.status === s)?.cnt || 0
    return { value: s, label: `${findDict(dict.ecom_fba_claim_status, s)?.label || s} (${cnt})` }
  })
)
const open = (r) => r.status === FbaClaimStatus.PENDING || r.status === FbaClaimStatus.SUBMITTED

const columns = [
  { title: t('ecomFba.claim.claimNo'), dataIndex: 'claimNo', width: 150, fixed: 'left' },
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomFba.claim.type'), key: 'claimType', dataIndex: 'claimType', width: 110, dict: 'ecom_fba_claim_type' },
  {
    title: t('ecomFba.common.sku'),
    key: 'sku',
    width: 170,
    exportColumns: [
      { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku' },
      { title: t('ecomFba.common.fnsku'), dataIndex: 'fnsku' }
    ]
  },
  { title: t('ecomFba.claim.sourceRef'), dataIndex: 'sourceRef', width: 150, ellipsis: true },
  { title: t('ecomFba.claim.qty'), key: 'qty', dataIndex: 'qty', width: 70, align: 'right' },
  {
    title: t('ecomFba.claim.estAmount'),
    key: 'amount',
    width: 140,
    align: 'right',
    exportColumns: [
      { title: t('ecomFba.claim.unitValue'), dataIndex: 'unitValue' },
      { title: t('ecomFba.claim.estAmount'), dataIndex: 'estAmount' },
      { title: t('ecomFba.common.currency'), dataIndex: 'currency' }
    ]
  },
  { title: t('ecomFba.claim.eventDate'), dataIndex: 'eventDate', width: 110 },
  { title: t('ecomFba.claim.deadline'), key: 'deadline', dataIndex: 'deadline', width: 120 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'ecom_fba_claim_status' },
  { title: t('ecomFba.claim.caseNo'), dataIndex: 'caseNo', width: 130 },
  { title: t('ecomFba.claim.submitDate'), dataIndex: 'submitDate', width: 110 },
  {
    title: t('ecomFba.claim.paid'),
    key: 'paid',
    width: 170,
    align: 'right',
    exportColumns: [
      { title: t('ecomFba.claim.paidAmount'), dataIndex: 'paidAmount' },
      { title: t('ecomFba.claim.paidQty'), dataIndex: 'paidQty' },
      { title: t('ecomFba.claim.paidBase'), dataIndex: 'paidBase' },
      { title: t('ecomFba.claim.paidDate'), dataIndex: 'paidDate' }
    ]
  },
  { title: t('ecomFba.claim.detail'), dataIndex: 'detail', width: 200, ellipsis: true },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  actionColumn(2, [t('ecomFba.claim.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-claim')
const query = reactive({ status: undefined, claimType: undefined, keyword: undefined, expiring: false })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageClaim, query, {
  params: (p) => ({ storeId: props.storeId || undefined, status: p.status, claimType: p.claimType, keyword: p.keyword, expiringDays: p.expiring ? 7 : undefined })
})
onMounted(load)
watch(() => props.storeId, () => search())

const editRef = ref()
const createRef = ref()
function changed() {
  load()
  emit('changed')
}
const remove = useAction(deleteClaim, { successKey: 'common.msg.deleteSuccess', reload: changed })
const rowActions = (record) => [
  { type: 'edit', label: t('ecomFba.claim.edit'), perm: 'ecom:fbaClaim:edit', onClick: () => editRef.value.open(record) },
  {
    type: 'delete',
    perm: 'ecom:fbaClaim:remove',
    show: record.status === FbaClaimStatus.PENDING || record.status === FbaClaimStatus.IGNORED || record.status === FbaClaimStatus.REJECTED,
    confirm: t('ecomFba.claim.deleteConfirm', { no: record.claimNo }),
    onClick: () => remove(record.id)
  }
]

defineExpose({ reload: () => load() })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.w140 {
  width: 160px;
}
.w160 {
  width: 160px;
}
.w260 {
  width: 260px;
  max-width: 100%;
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
.expired {
  color: var(--status-critical, #cf1322);
  font-size: 12px;
}
.soon {
  color: #d46b08;
  font-size: 12px;
}
</style>
