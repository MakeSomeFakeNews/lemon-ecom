<template>
  <SubPage :open="open" :title="detail ? $t('ecomFba.removal.detailTitle', { no: detail.docNo }) : ''" @close="open = false">
    <template v-if="detail" #title>
      <span class="title-no">{{ $t('ecomFba.removal.detailTitle', { no: detail.docNo }) }}</span>
      <DictTag type="ecom_fba_removal_status" :value="detail.status" class="title-tag" />
    </template>
    <template v-if="detail" #extra>
      <a-space wrap>
        <a-button v-if="canCancel(detail) && can('ecom:fbaRemoval:edit')" danger @click="emit('cancel', detail)">
          <template #icon><StopOutlined /></template>{{ $t('ecomFba.removal.cancel') }}
        </a-button>
        <a-button v-if="isEditable(detail) && can('ecom:fbaRemoval:edit')" @click="emit('edit', detail)">
          <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
        </a-button>
        <a-button v-if="isEditable(detail) && can('ecom:fbaRemoval:edit')" type="primary" @click="emit('submit', detail)">
          <template #icon><SendOutlined /></template>{{ $t('ecomFba.removal.submit') }}
        </a-button>
        <a-button v-if="canDispose(detail) && can('ecom:fbaRemoval:receive')" @click="emit('dispose', detail)">
          <template #icon><DeleteOutlined /></template>{{ $t('ecomFba.removal.dispose') }}
        </a-button>
        <a-button v-if="canReceive(detail) && can('ecom:fbaRemoval:receive')" type="primary" @click="emit('receive', detail)">
          <template #icon><InboxOutlined /></template>{{ $t('ecomFba.removal.receive') }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <a-skeleton v-if="!detail" active :paragraph="{ rows: 8 }" />
      <template v-else>
        <div class="progress-card block">
          <a-steps :current="flow.current" :status="flow.status" :items="flow.items" size="small" />
        </div>
        <div class="kpis block">
          <div v-for="k in kpis" :key="k.key" class="kpi">
            <div class="kpi-label">{{ k.label }}</div>
            <div class="kpi-value">{{ qty(k.value) }}</div>
          </div>
          <div class="kpi">
            <div class="kpi-label">{{ $t('ecomFba.removal.removalFee') }}</div>
            <div class="kpi-value">{{ money(detail.removalFee) }} <span class="kpi-unit">{{ detail.currency }}</span></div>
            <div class="kpi-sub">{{ $t('ecomFba.removal.feeBase') }} {{ symMoney(detail.feeBase) }}</div>
          </div>
        </div>
        <div class="section-title">{{ $t('ecomFba.removal.basicInfo') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomFba.removal.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.common.store')">{{ detail.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.type')"><DictTag type="ecom_fba_removal_type" :value="detail.removalType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.platformOrderId')">{{ detail.platformOrderId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.requestDate')">{{ detail.requestDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.completeDate')">{{ detail.completeDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.warehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.defectiveWarehouse')">{{ detail.defectiveWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.removalFee')">{{ money(detail.removalFee) }} {{ detail.currency }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.feeBase')">{{ money(detail.feeBase) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.tracking')">{{ [detail.carrier, detail.trackingNo].filter(Boolean).join(' ') || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.lastSync')">{{ detail.lastSyncTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFba.removal.stockDocNos')" :span="2">{{ detail.stockDocNos || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.shipAddress" :label="$t('ecomFba.removal.shipAddress')" :span="2">{{ detail.shipAddress }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="4">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('ecomFba.removal.items') }}</div>
        <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 1200 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'disposition'"><DictTag type="ecom_fba_disposition" :value="record.disposition" /></template>
            <template v-else-if="column.key === 'material'">
              <span v-if="record.materialCode">{{ record.materialCode }} {{ record.materialName }}</span>
              <a-tag v-else color="warning" :bordered="false">{{ $t('ecom.common.unmapped') }}</a-tag>
            </template>
            <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
            <template v-else-if="column.key === 'removalFee'">{{ money(record.removalFee) }}</template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DeleteOutlined, EditOutlined, InboxOutlined, SendOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getRemoval } from '@/api/ecom/fba'
import { money, qty, symMoney } from '@/utils/number'
import { t } from '@/locales'
import { useCan } from '../fba-health/fba'
import { canCancel, canDispose, canReceive, isEditable } from './removal'
import { FbaRemovalStatus, FbaRemovalType } from '@/constants/enums'

/** 移除订单详情：ref 调用 open(id)、reload()；按状态显示操作，@edit / @receive / @submit / @dispose / @cancel(detail) 交给页面处理 */
const emit = defineEmits(['edit', 'receive', 'submit', 'dispose', 'cancel'])
const can = useCan()
const q = (key, title) => ({ title, key, dataIndex: key, width: 90, align: 'right', qty: true })
const columns = [
  { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku', width: 170 },
  { title: t('ecomFba.common.fnsku'), dataIndex: 'fnsku', width: 120 },
  { title: t('ecomFba.removal.disposition'), key: 'disposition', width: 90 },
  { title: t('ecomFba.common.material'), key: 'material', width: 200 },
  q('requestedQty', t('ecomFba.removal.requested')),
  q('cancelledQty', t('ecomFba.removal.cancelled')),
  q('shippedQty', t('ecomFba.removal.shipped')),
  q('disposedQty', t('ecomFba.removal.disposed')),
  q('receivedQty', t('ecomFba.removal.receivedGood')),
  q('defectiveQty', t('ecomFba.removal.receivedBad')),
  q('scrappedQty', t('ecomFba.removal.scrapped')),
  { title: t('ecomFba.removal.removalFee'), key: 'removalFee', width: 100, align: 'right' }
]

const sumItems = (key) => (detail.value?.items || []).reduce((a, i) => a + (Number(i[key]) || 0), 0)
const kpis = computed(() => [
  { key: 'requested', label: t('ecomFba.removal.requested'), value: sumItems('requestedQty') },
  { key: 'shipped', label: t('ecomFba.removal.shipped'), value: sumItems('shippedQty') },
  { key: 'disposed', label: t('ecomFba.removal.disposed'), value: sumItems('disposedQty') },
  { key: 'received', label: t('ecomFba.removal.received'), value: sumItems('receivedQty') + sumItems('defectiveQty') }
])
const FLOW = [FbaRemovalStatus.DRAFT, FbaRemovalStatus.SUBMITTED, FbaRemovalStatus.PROCESSING, FbaRemovalStatus.COMPLETED, FbaRemovalStatus.RECEIVED]
/** 流程：计划 → 已提交 → 处理中 → 亚马逊已完成 → 本地已收货；取消时停在提交前后并显示错误 */
const flow = computed(() => {
  const d = detail.value
  const items = FLOW.map((s) => ({ title: t(`ecomFba.removal.flow.${s}`) }))
  items[0].description = d.createTime ? String(d.createTime).slice(0, 10) : ''
  items[1].description = d.requestDate || ''
  items[3].description = d.completeDate || ''
  if (d.removalType === FbaRemovalType.DISPOSAL) items[4].title = t('ecomFba.removal.flow.DISPOSED')
  if (d.status === FbaRemovalStatus.CANCELLED) return { items, current: d.platformOrderId ? 1 : 0, status: 'error' }
  const current = Math.max(0, FLOW.indexOf(d.status))
  return { items, current, status: d.status === FbaRemovalStatus.RECEIVED ? 'finish' : 'process' }
})

const open = ref(false)
const loading = ref(false)
const detail = ref(null)
let currentId = null

async function load() {
  loading.value = true
  try {
    detail.value = await getRemoval(currentId)
  } finally {
    loading.value = false
  }
}
function show(id) {
  currentId = id
  detail.value = null
  open.value = true
  load()
}
const reload = () => (open.value && currentId ? load() : undefined)

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.title-no {
  font-weight: 600;
}
.title-tag {
  margin-left: 8px;
  vertical-align: 1px;
}
.progress-card {
  padding: 16px 24px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
}
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.kpi {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-bg);
  border: 1px solid var(--app-border);
}
.kpi-label,
.kpi-sub,
.kpi-unit {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.kpi-value {
  margin-top: 2px;
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.section-title {
  margin: 4px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  font-weight: 600;
  line-height: 1.2;
}
</style>
