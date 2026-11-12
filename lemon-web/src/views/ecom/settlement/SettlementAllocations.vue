<template>
  <div class="alloc">
    <div class="alloc-head">
      <span class="section-title">{{ $t('ecomProfit.settlement.allocations') }}</span>
      <span class="sub-text">{{ $t('ecomProfit.settlement.allocTip') }}</span>
      <span class="spacer" />
      <template v-if="userStore.hasPerm('ecom:settlement:allocate')">
        <a-select v-model:value="method" :options="methodOptions" :placeholder="$t('ecomProfit.settlement.defaultMethod')" allow-clear size="small" class="method" />
        <a-button size="small" :loading="saving" @click="reallocate">
          <template #icon><ReloadOutlined /></template>{{ $t('ecomProfit.settlement.reallocate') }}
        </a-button>
      </template>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="rows.length > pagination.pageSize ? pagination : false"
      :scroll="{ x: scrollX }"
      @change="(p) => applyPageChange(pagination, p)"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'itemType'"><DictTag type="ecom_settlement_item_type" :value="record.itemType" /></template>
        <template v-else-if="column.key === 'method'"><DictTag type="ecom_fee_alloc_method" :value="record.method" /></template>
        <template v-else-if="column.key === 'sellerSku'">
          <span v-if="record.sellerSku">{{ record.sellerSku }}</span>
          <span v-else class="sub-text">{{ $t('ecomProfit.settlement.storeLevel') }}</span>
        </template>
        <template v-else-if="column.key === 'basisValue'">{{ record.basisValue == null ? '-' : qty(record.basisValue) }}</template>
        <template v-else-if="column.money">
          <span :class="{ neg: nz(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { allocateSettlement, settlementAllocations } from '@/api/ecom/settlement'
import { sumWidth } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'
import { money, nz, qty } from '@/utils/number'
import { FeeAllocMethod } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 结算单中不挂订单的费用（仓储费、订阅费、移除费等）分摊到 SKU 的结果；可改为按销量 / 按销售额重新分摊（为空按类型默认）
 */
const props = defineProps({ settlement: { type: Object, required: true } })
const userStore = useUserStore()
const dict = useDict('ecom_fee_alloc_method')

const columns = [
  { title: t('ecomReport.settlement.itemType'), key: 'itemType', dataIndex: 'itemType', width: 130 },
  { title: t('ecomReport.common.sellerSku'), key: 'sellerSku', dataIndex: 'sellerSku', width: 160 },
  { title: t('ecomProfit.settlement.method'), key: 'method', dataIndex: 'method', width: 130 },
  { title: t('ecomProfit.settlement.allocDate'), dataIndex: 'allocDate', width: 110 },
  { title: t('ecomProfit.settlement.basisValue'), key: 'basisValue', dataIndex: 'basisValue', width: 120, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', dataIndex: 'amount', width: 120, align: 'right', money: true },
  { title: t('ecomProfit.settlement.amountBase'), key: 'amountBase', dataIndex: 'amountBase', width: 130, align: 'right', money: true }
]
const scrollX = sumWidth(columns)
const pagination = reactive(localPagination(10))
const methodOptions = computed(() =>
  [FeeAllocMethod.QTY, FeeAllocMethod.SALES].map((v) => ({ value: v, label: findDict(dict.ecom_fee_alloc_method, v)?.label || v }))
)

const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const method = ref()

async function load() {
  loading.value = true
  try {
    rows.value = (await settlementAllocations(props.settlement.id)) || []
    pagination.current = 1
  } finally {
    loading.value = false
  }
}

async function reallocate() {
  saving.value = true
  try {
    const n = await allocateSettlement(props.settlement.id, method.value || undefined)
    message.success(t('ecomProfit.settlement.reallocated', { n }))
    await load()
  } finally {
    saving.value = false
  }
}

watch(
  () => props.settlement?.id,
  () => {
    method.value = props.settlement?.allocMethod || undefined
    load()
  },
  { immediate: true }
)
</script>

<style scoped>
.alloc {
  margin-top: 20px;
}
.alloc-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 500;
}
.spacer {
  flex: 1;
}
.method {
  width: 140px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
</style>
