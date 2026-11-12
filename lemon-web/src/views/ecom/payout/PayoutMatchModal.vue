<template>
  <a-modal
    :open="visible"
    :title="$t('ecomProfit.payout.matchTitle')"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: !selected }"
    width="min(760px, 100vw)"
    @ok="submit"
    @cancel="visible = false"
  >
    <a-descriptions v-if="flow" :column="3" size="small" class="block">
      <a-descriptions-item :label="$t('ecomProfit.payout.date')">{{ flow.payoutDate }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomProfit.payout.amount')">{{ money(flow.amount) }} {{ flow.payCurrency || flow.currency }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomProfit.payout.bankRef')">{{ flow.bankRef || '-' }}</a-descriptions-item>
      <a-descriptions-item v-if="flow.remark" :label="$t('common.field.remark')" :span="3">{{ flow.remark }}</a-descriptions-item>
    </a-descriptions>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="candidates"
      :loading="loading"
      :pagination="false"
      :scroll="{ y: 320 }"
      :row-selection="{ type: 'radio', selectedRowKeys: selected ? [selected] : [], onChange: (keys) => (selected = keys[0]) }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'expectedAmount'">{{ money(record.expectedAmount) }} <span class="sub-text">{{ record.currency }}</span></template>
        <template v-else-if="column.key === 'expectedBase'">{{ money(record.expectedBase) }}</template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { BASE_CURRENCY } from '@/components/fin/currency'
import { matchPayout, pagePayout } from '@/api/ecom/payout'
import { money } from '@/utils/number'
import { PayoutStatus } from '@/constants/enums'

/**
 * 手工匹配：把一条没有匹配到结算单的流水并入一条未到账记录。候选为同币种（流水为本位币时不限币种）的未到账记录，
 * 按与流水金额的差额从小到大排列。open(流水) 打开，成功后发出 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const loading = ref(false)
const flow = ref()
const candidates = ref([])
const selected = ref()

const columns = [
  { title: t('ecomProfit.payout.settlementNo'), dataIndex: 'settlementNo', width: 170 },
  { title: t('ecomReport.common.store'), dataIndex: 'storeName', width: 150, ellipsis: true },
  { title: t('ecomProfit.payout.expectedDate'), dataIndex: 'expectedDate', width: 110 },
  { title: t('ecomProfit.payout.expectedAmount'), key: 'expectedAmount', width: 140, align: 'right' },
  { title: t('ecomProfit.payout.expectedBase'), key: 'expectedBase', width: 130, align: 'right' }
]

async function open(f) {
  flow.value = f
  selected.value = undefined
  candidates.value = []
  visible.value = true
  loading.value = true
  try {
    const res = await pagePayout({ current: 1, size: 500, status: PayoutStatus.PENDING, storeId: f.storeId || undefined })
    const currency = f.payCurrency || f.currency
    const base = currency === BASE_CURRENCY
    const expected = (r) => (r.currency === currency ? Number(r.expectedAmount) : Number(r.expectedBase))
    candidates.value = (res?.records || [])
      .filter((r) => r.settlementId && (base || r.currency === currency))
      .sort((a, b) => Math.abs(expected(a) - f.amount) - Math.abs(expected(b) - f.amount))
  } finally {
    loading.value = false
  }
}

async function submit() {
  saving.value = true
  try {
    await matchPayout(flow.value.id, selected.value)
    message.success(t('common.msg.operationSuccess'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
