<template>
  <SubPage :open="open" :title="detail ? $t('ecomOwh.outbound.detailTitle', { no: detail.outboundNo }) : ''" @close="open = false">
    <template v-if="detail" #title>
      <span class="doc-title">{{ $t('ecomOwh.outbound.detailTitle', { no: detail.outboundNo }) }}</span>
      <DictTag type="ecom_owh_outbound_status" :value="detail.status" />
    </template>
    <template v-if="detail && actions.length" #extra>
      <OwhDocActions :actions="actions" />
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <OwhFlowSteps :flow="outboundFlow(detail)" />
        <a-alert v-if="detail.errorMsg" type="warning" show-icon class="block" :message="$t('ecomOwh.common.errorMsg')" :description="detail.errorMsg" />
        <div class="section-title">{{ $t('ecomOwh.section.basic') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOwh.outbound.packageNo')">
            <a @click="router.push({ path: '/ecom/package', query: { keyword: detail.packageNo } })">{{ detail.packageNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.orderNos')">{{ detail.orderNos || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.account')">{{ detail.accountName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.warehouse')">{{ detail.warehouseName }}（{{ detail.owhCode }}）</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.receiver')">{{ [detail.shipName, detail.shipCountry].filter(Boolean).join(' · ') || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.owhOrderNo')">{{ detail.owhOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.owhStatus')">{{ detail.owhStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.syncTime')">{{ detail.syncTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('ecomOwh.section.logistics') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOwh.outbound.product')">{{ detail.productName || detail.productCode || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.carrier')">{{ detail.carrierName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.estimateFee')">{{ detail.estimateFee != null ? `${detail.feeCurrency || ''} ${money(detail.estimateFee)}` : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.feeBase')">{{ detail.feeBase != null ? `¥${money(detail.feeBase)}` : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.outbound.pushTime')">{{ detail.pushTime || '-' }}（{{ $t('ecomOwh.outbound.pushCount', { n: detail.pushCount || 0 }) }}）</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOwh.outbound.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items" :pagination="false" :scroll="{ x: 800 }" class="block">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'materialQty'">{{ qty(record.materialQty) }}</template>
          </template>
        </a-table>
      </template>
    </a-spin>

  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import OwhDocActions from '@/components/ecom/owh/OwhDocActions.vue'
import OwhFlowSteps from '@/components/ecom/owh/OwhFlowSteps.vue'
import { CloseCircleOutlined, RedoOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { getOwhOutbound, owhOutboundItems } from '@/api/ecom/owh'
import { useUserStore } from '@/store/user'
import { money, qty } from '@/utils/number'
import { canCancelOutbound, canRepush, canSyncOutbound, outboundFlow } from './owh'

/** 海外仓出库单详情：服务商单号与状态、运单号、费用、出库明细（物料编码即海外仓 SKU）。open(id)、reload()；操作交给页面（事件带出库单） */
const emit = defineEmits(['sync', 'repush', 'cancel'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const open = ref(false)
const loading = ref(false)
const detail = ref(null)
const items = ref([])
let currentId = null

const itemColumns = [
  { title: t('ecomOwh.outbound.orderNo'), dataIndex: 'platformOrderNo', width: 160 },
  { title: t('ecomOwh.outbound.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOwh.outbound.owhSku'), key: 'material', width: 220 },
  { title: t('ecomOwh.outbound.skuQty'), key: 'qty', width: 90, align: 'right' },
  { title: t('ecomOwh.outbound.materialQty'), key: 'materialQty', width: 100, align: 'right' }
]

async function load(id) {
  loading.value = true
  try {
    const [d, it] = await Promise.all([getOwhOutbound(id), owhOutboundItems(id)])
    detail.value = d
    items.value = it || []
  } finally {
    loading.value = false
  }
}

function show(id) {
  currentId = id
  detail.value = null
  open.value = true
  load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)

const actions = computed(() => {
  const d = detail.value
  const out = []
  if (canSyncOutbound(d) && userStore.hasPerm('ecom:owhOutbound:sync')) out.push({ key: 'sync', label: t('ecomOwh.outbound.sync'), icon: SyncOutlined, primary: true, run: () => emit('sync', d) })
  if (canRepush(d) && userStore.hasPerm('ecom:owhOutbound:push')) out.push({ key: 'repush', label: t('ecomOwh.outbound.repush'), icon: RedoOutlined, primary: true, run: () => emit('repush', d) })
  if (canCancelOutbound(d) && userStore.hasPerm('ecom:owhOutbound:cancel')) out.push({ key: 'cancel', label: t('ecomOwh.outbound.cancel'), icon: CloseCircleOutlined, danger: true, run: () => emit('cancel', d) })
  return out
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 8px 0 12px;
  font-weight: 600;
}
.doc-title {
  margin-right: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
