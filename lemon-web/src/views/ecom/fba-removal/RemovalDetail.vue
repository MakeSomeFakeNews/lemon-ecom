<template>
  <SubPage :open="open" :title="detail ? $t('ecomFba.removal.detailTitle', { no: detail.docNo }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <DictTag type="ecom_fba_removal_status" :value="detail.status" />
        <a-button v-if="isEditable(detail) && can('ecom:fbaRemoval:edit')" @click="emit('edit', detail)">
          <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
        </a-button>
        <a-button v-if="canReceive(detail) && can('ecom:fbaRemoval:receive')" type="primary" @click="emit('receive', detail)">
          <template #icon><InboxOutlined /></template>{{ $t('ecomFba.removal.receive') }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
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
import { ref } from 'vue'
import { EditOutlined, InboxOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getRemoval } from '@/api/ecom/fba'
import { money, qty } from '@/utils/number'
import { t } from '@/locales'
import { useCan } from '../fba-health/fba'
import { canReceive, isEditable } from './removal'

/** 移除订单详情：ref 调用 open(id)、reload()；@edit(detail)、@receive(detail) */
const emit = defineEmits(['edit', 'receive'])
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
.section-title {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
