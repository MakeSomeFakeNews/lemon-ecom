<template>
  <a-modal v-model:open="visible" :title="$t('ecomFba.removal.receiveTitle', { no: removal.docNo || '' })" width="min(860px, 100vw)" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon :message="$t('ecomFba.removal.receiveTip')" class="block" />
    <a-form layout="vertical">
      <a-row :gutter="12">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomFba.removal.receiveDate')"><a-date-picker v-model:value="form.receiveDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomFba.removal.warehouse')"><WarehouseSelect v-model:value="form.warehouseId" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomFba.removal.defectiveWarehouse')"><WarehouseSelect v-model:value="form.defectiveWarehouseId" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomFba.removal.carrier')"><a-input v-model:value="form.carrier" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('ecomFba.removal.trackingNo')"><a-input v-model:value="form.trackingNo" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-table row-key="id" size="small" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 700 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'disposition'"><DictTag type="ecom_fba_disposition" :value="record.disposition" /></template>
        <template v-else-if="column.key === 'shipped'">{{ qty(record.shippedQty) }}</template>
        <template v-else-if="column.key === 'left'"><b>{{ qty(record.left) }}</b></template>
        <template v-else-if="column.key === 'good'">
          <a-input-number v-model:value="record.goodQty" :min="0" :max="record.left" :precision="0" class="full" />
        </template>
        <template v-else-if="column.key === 'bad'">
          <a-input-number v-model:value="record.defectiveQty" :min="0" :max="record.left" :precision="0" class="full" />
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { receiveRemoval } from '@/api/ecom/fba'
import { nz, qty } from '@/utils/number'
import { FbaDisposition } from '@/constants/enums'
import { t } from '@/locales'

/** 移除订单收货：ref 调用 open(详情)，可售行默认收良品、不可售行默认收不良品；@success */
const emit = defineEmits(['success'])
const columns = [
  { title: t('ecomFba.common.sku'), dataIndex: 'sellerSku', width: 180 },
  { title: t('ecomFba.removal.disposition'), key: 'disposition', width: 90 },
  { title: t('ecomFba.removal.shipped'), key: 'shipped', width: 80, align: 'right' },
  { title: t('ecomFba.removal.left'), key: 'left', width: 80, align: 'right' },
  { title: t('ecomFba.removal.goodQty'), key: 'good', width: 120 },
  { title: t('ecomFba.removal.badQty'), key: 'bad', width: 120 }
]
const visible = ref(false)
const saving = ref(false)
const removal = ref({})
const form = ref({})
const rows = ref([])

/** 可收件数：亚马逊有已发数量时按已发，否则按 申请 − 取消，减去已收 */
const expected = (i) => (nz(i.shippedQty) > 0 ? nz(i.shippedQty) : Math.max(0, nz(i.requestedQty) - nz(i.cancelledQty)))

function open(detail) {
  removal.value = detail
  form.value = {
    receiveDate: dayjs().format('YYYY-MM-DD'),
    warehouseId: detail.warehouseId ?? undefined,
    defectiveWarehouseId: detail.defectiveWarehouseId ?? undefined,
    carrier: detail.carrier || '',
    trackingNo: detail.trackingNo || ''
  }
  rows.value = (detail.items || []).map((i) => {
    const left = Math.max(0, expected(i) - nz(i.receivedQty) - nz(i.defectiveQty))
    const bad = i.disposition === FbaDisposition.UNSELLABLE
    return { ...i, left, goodQty: bad ? 0 : left, defectiveQty: bad ? left : 0 }
  })
  visible.value = true
}

async function submit() {
  const lines = rows.value
    .filter((r) => nz(r.goodQty) > 0 || nz(r.defectiveQty) > 0)
    .map((r) => ({ itemId: r.id, goodQty: nz(r.goodQty), defectiveQty: nz(r.defectiveQty) }))
  if (!lines.length) {
    message.warning(t('ecomFba.removal.receiveEmpty'))
    return
  }
  saving.value = true
  try {
    const f = form.value
    const nos = await receiveRemoval(removal.value.id, {
      receiveDate: f.receiveDate,
      warehouseId: f.warehouseId ?? null,
      defectiveWarehouseId: f.defectiveWarehouseId ?? null,
      carrier: f.carrier || null,
      trackingNo: f.trackingNo || null,
      lines
    })
    message.success(t('ecomFba.removal.receiveDone', { nos: (nos || []).join(', ') }))
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
.full {
  width: 100%;
}
</style>
