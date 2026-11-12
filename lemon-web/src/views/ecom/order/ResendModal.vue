<template>
  <a-modal v-model:open="open" :title="$t('ecomOrd.resend.title', { no: order?.platformOrderNo || '' })" width="min(860px, 100vw)" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon :message="$t('ecomOrd.resend.help')" class="block" />
    <a-table row-key="id" size="small" :columns="columns" :data-source="lines" :pagination="false" :scroll="{ x: 640 }" class="block">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
        <template v-else-if="column.key === 'resendQty'">
          <a-input-number v-model:value="record.resendQty" :min="0" :max="Number(record.qty)" :precision="0" class="full" />
        </template>
      </template>
    </a-table>
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrd.resend.reason')">
            <a-input v-model:value="form.reason" :maxlength="200" :placeholder="$t('ecomOrd.resend.reasonPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrd.resend.returnDoc')">
            <a-select v-model:value="form.returnId" allow-clear :options="returnOptions" :placeholder="$t('ecomOrd.resend.returnPh')" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-checkbox v-model:checked="form.newAddress" class="block">{{ $t('ecomOrd.resend.newAddress') }}</a-checkbox>
      <AddressFields v-if="form.newAddress" :form="form" />
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import AddressFields from './AddressFields.vue'
import { createResendOrder } from '@/api/ecom/orderEnhance'
import { pageEcomReturn } from '@/api/ecom/return'
import { qty } from '@/utils/number'

/**
 * 从原订单创建补发 / 重发单：选择补发的行与件数（不超过原件数）、原因，可关联本订单的退货退款单，可改寄新地址；
 * 补发单金额为 0，出库成本与运费计入原订单的售后成本。open(order) 打开，成功后触发 done(补发单 ID)
 */
const emit = defineEmits(['done'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const order = ref()
const lines = ref([])
const returns = ref([])
const form = reactive({ reason: '', returnId: undefined, newAddress: false })

const columns = [
  { title: t('ecomOrder.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.order.itemTitle'), dataIndex: 'title', width: 260, ellipsis: true },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 80, align: 'right' },
  { title: t('ecomOrd.resend.qty'), key: 'resendQty', width: 110 }
]
const returnOptions = computed(() => returns.value.map((r) => ({ value: r.id, label: `${r.docNo}${r.reason ? ` · ${r.reason}` : ''}` })))

async function show(o) {
  order.value = o
  lines.value = (o.items || []).map((i) => ({ ...i, resendQty: 0 }))
  Object.assign(form, { reason: '', returnId: undefined, newAddress: false, shipName: o.shipName, shipPhone: o.shipPhone, shipCountry: o.shipCountry,
    shipState: o.shipState, shipCity: o.shipCity, shipPostcode: o.shipPostcode, shipAddress: o.shipAddress })
  open.value = true
  const res = await pageEcomReturn({ keyword: o.platformOrderNo, current: 1, size: 50 })
  returns.value = (res?.records || []).filter((r) => r.orderId === o.id)
}

async function submit() {
  const picked = lines.value.filter((l) => l.resendQty > 0).map((l) => ({ orderItemId: l.id, qty: l.resendQty }))
  if (!picked.length) {
    message.warning(t('ecomOrd.resend.pickLines'))
    return
  }
  saving.value = true
  try {
    const address = form.newAddress
      ? { shipName: form.shipName, shipPhone: form.shipPhone, shipCountry: form.shipCountry?.trim().toUpperCase(), shipState: form.shipState,
          shipCity: form.shipCity, shipPostcode: form.shipPostcode, shipAddress: form.shipAddress }
      : {}
    const id = await createResendOrder(order.value.id, { lines: picked, reason: form.reason || null, returnId: form.returnId ?? null, ...address })
    message.success(t('ecomOrd.resend.created'))
    open.value = false
    emit('done', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
</style>
