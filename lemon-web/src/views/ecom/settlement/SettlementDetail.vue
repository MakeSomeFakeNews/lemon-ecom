<template>
  <SubPage :open="open" :title="detail ? `${$t('ecomReport.settlement.title')} ${detail.settlementNo}` : ''" @close="open = false">
    <template #extra>
      <a-button
        v-if="detail && detail.status === SettlementStatus.IMPORTED"
        v-perm="'ecom:settlement:confirm'"
        type="primary"
        @click="emit('confirm', detail)"
      >
        <template #icon><CheckCircleOutlined /></template>{{ $t('common.action.confirm') }}
      </a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomReport.common.store')">{{ detail.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.common.platform')"><DictTag type="ecom_platform" :value="detail.platform" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.settlement.period')">{{ detail.periodStart }} ~ {{ detail.periodEnd }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.common.currency')">{{ detail.currency }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.settlement.exchangeRate')">{{ qty(detail.exchangeRate, 6) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">
            <DictTag type="ecom_settlement_status" :value="detail.status" />
            <span v-if="detail.finSettlementId" class="sub-text">{{ $t('ecomReport.settlement.receiptGenerated') }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.settlement.itemCount')">{{ items.length }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.createTime')">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="4">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomReport.settlement.amounts') }}</div>
        <div class="amounts">
          <div v-for="a in AMOUNTS" :key="a.key" class="amount" :class="{ net: a.net }">
            <div class="sub-text">{{ $t(`ecomReport.settlement.${a.key}`) }}</div>
            <div class="amount-value" :class="{ neg: nz(detail[a.key]) < 0 }">{{ money(detail[a.key]) }}</div>
          </div>
        </div>

        <div class="section-title">{{ $t('ecomReport.settlement.items') }}</div>
        <a-table
          row-key="id"
          size="small"
          :columns="itemColumns"
          :data-source="items"
          :pagination="items.length > pagination.pageSize ? pagination : false"
          :scroll="{ x: itemScrollX }"
          @change="(p) => applyPageChange(pagination, p)"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'itemType'"><DictTag type="ecom_settlement_item_type" :value="record.itemType" /></template>
            <template v-else-if="column.key === 'amount'">
              <span :class="{ neg: nz(record.amount) < 0 }">{{ money(record.amount) }}</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { CheckCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { getSettlement } from '@/api/ecom/settlement'
import { sumWidth } from '@/composables/useColumns'
import { money, nz, qty } from '@/utils/number'
import { SettlementStatus } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 结算单详情子页：头部汇总（店铺币种的各项金额）与明细表。
 * open(id) 打开，reload() 重新加载当前结算单；点「确认」时向页面发出 confirm(结算单)
 */
const emit = defineEmits(['confirm'])

/** 头部汇总的金额字段（与 EcomSettlement 一致），净额加粗 */
const AMOUNTS = [
  { key: 'salesAmount' },
  { key: 'refundAmount' },
  { key: 'commissionFee' },
  { key: 'fulfillmentFee' },
  { key: 'storageFee' },
  { key: 'adFee' },
  { key: 'otherFee' },
  { key: 'adjustment' },
  { key: 'netAmount', net: true },
  { key: 'netBase', net: true }
]

const itemColumns = [
  { title: t('ecomReport.settlement.postedDate'), dataIndex: 'postedDate', width: 110 },
  { title: t('ecomReport.settlement.itemType'), key: 'itemType', dataIndex: 'itemType', width: 110 },
  { title: t('ecomReport.common.platformOrderNo'), dataIndex: 'platformOrderNo', width: 190 },
  { title: t('ecomReport.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('common.field.amount'), key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'description', ellipsis: true }
]
const itemScrollX = sumWidth(itemColumns) + 200

const open = ref(false)
const loading = ref(false)
const detail = ref()
const items = computed(() => detail.value?.items || [])
const pagination = reactive(localPagination(20))

async function load(id) {
  loading.value = true
  try {
    detail.value = await getSettlement(id)
    pagination.current = 1
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  detail.value = undefined
  open.value = true
  load(id)
}

function reload() {
  if (open.value && detail.value) load(detail.value.id)
}

defineExpose({ open: openDetail, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.amounts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.amount {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-hover);
}
.amount.net {
  background: var(--app-primary-bg);
}
.amount-value {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-left: 4px;
}
.amount .sub-text {
  margin-left: 0;
}
.neg {
  color: var(--status-critical, #cf1322);
}
</style>
