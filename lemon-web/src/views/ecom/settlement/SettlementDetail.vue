<template>
  <SubPage :open="open" :title="detail ? `${$t('ecomReport.settlement.title')} ${detail.settlementNo}` : ''" @close="open = false">
    <template #title>
      <template v-if="detail">
        <span>{{ $t('ecomReport.settlement.title') }} {{ detail.settlementNo }}</span>
        <DictTag type="ecom_settlement_status" :value="detail.status" class="title-tag" />
      </template>
    </template>
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
          <a-descriptions-item :label="$t('ecomDat.settlement.receipt')">
            <span v-if="detail.finSettlementId">{{ $t('ecomReport.settlement.receiptGenerated') }}</span>
            <span v-else class="muted">-</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomReport.settlement.itemCount')">{{ items.length }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.createTime')">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="4">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomDat.settlement.composition') }}</div>
        <SettlementComposition :settlement="detail" />

        <div class="items-head">
          <span class="section-title">{{ $t('ecomReport.settlement.items') }}</span>
          <a-radio-group v-model:value="typeFilter" size="small" class="type-filter" @change="pagination.current = 1">
            <a-radio-button value="">{{ $t('common.status.all') }} {{ items.length }}</a-radio-button>
            <a-radio-button v-for="g in typeGroups" :key="g.type" :value="g.type">{{ typeLabel(g.type) }} {{ g.count }}</a-radio-button>
          </a-radio-group>
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="itemColumns"
          :data-source="shownItems"
          :pagination="shownItems.length > pagination.pageSize ? pagination : false"
          :scroll="{ x: itemScrollX }"
          @change="(p) => applyPageChange(pagination, p)"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'itemType'">
              <DictTag type="ecom_settlement_item_type" :value="record.itemType" />
              <div v-if="record.rawType" class="raw-type">{{ record.rawType }}</div>
            </template>
            <template v-else-if="column.key === 'amount'">
              <span :class="{ neg: nz(record.amount) < 0 }">{{ money(record.amount) }}</span>
            </template>
          </template>
        </a-table>

        <SettlementAllocations :settlement="detail" />
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { CheckCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import SettlementAllocations from './SettlementAllocations.vue'
import SettlementComposition from './SettlementComposition.vue'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { getSettlement } from '@/api/ecom/settlement'
import { sumWidth } from '@/composables/useColumns'
import { money, nz, qty } from '@/utils/number'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { SettlementStatus } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 结算单详情子页：头部汇总（店铺币种的各项金额）与明细表。
 * open(id) 打开，reload() 重新加载当前结算单；点「确认」时向页面发出 confirm(结算单)
 */
const emit = defineEmits(['confirm'])

const dict = useDict('ecom_settlement_item_type')
const typeLabel = (v) => findDict(dict.ecom_settlement_item_type, v)?.label || v

const itemColumns = [
  { title: t('ecomReport.settlement.postedDate'), dataIndex: 'postedDate', width: 110 },
  { title: t('ecomReport.settlement.itemType'), key: 'itemType', dataIndex: 'itemType', width: 160 },
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
/** 明细按类型筛选（类型与条数按出现顺序） */
const typeFilter = ref('')
const typeGroups = computed(() => {
  const map = new Map()
  items.value.forEach((r) => map.set(r.itemType, (map.get(r.itemType) || 0) + 1))
  return [...map].map(([type, count]) => ({ type, count }))
})
const shownItems = computed(() => (typeFilter.value ? items.value.filter((r) => r.itemType === typeFilter.value) : items.value))
const pagination = reactive(localPagination(20))

async function load(id) {
  loading.value = true
  try {
    detail.value = await getSettlement(id)
    pagination.current = 1
    typeFilter.value = ''
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
.title-tag {
  margin-left: 8px;
  vertical-align: 2px;
}
.items-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.items-head .section-title {
  margin-bottom: 0;
}
.muted {
  color: var(--app-text-3);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-left: 4px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.raw-type {
  color: var(--app-text-3);
  font-size: 12px;
  margin-top: 2px;
}
</style>
