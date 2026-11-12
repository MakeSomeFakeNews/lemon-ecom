<template>
  <a-modal
    :open="open"
    :title="$t('ecomCs.ticket.title')"
    width="min(960px, 96vw)"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: !order }"
    destroy-on-close
    @ok="submit"
    @cancel="open = false"
  >
    <a-spin :spinning="loading">
      <a-alert v-if="!loading && !order" type="warning" show-icon :message="$t('ecomCs.ticket.noOrder')" class="block" />
      <template v-if="order">
        <a-alert type="info" show-icon class="block">
          <template #message>
            {{ order.platformOrderNo }} · {{ order.storeName }} · {{ order.buyerName || order.shipName }} · {{ fcMoney(order.currency, order.totalAmount) }}
          </template>
          <template #description>
            <DictTag type="ecom_order_status" :value="order.status" />
            <DictTag type="ecom_local_status" :value="order.localStatus" />
            <span class="sub-text">{{ typeHint }}</span>
          </template>
        </a-alert>
        <a-form layout="vertical">
          <a-row :gutter="16">
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item :label="$t('ecomCs.ticket.type')" required>
                <a-radio-group v-model:value="form.ticketType" button-style="solid">
                  <a-radio-button v-for="o in toOptions(dict.ecom_cs_ticket_type)" :key="o.value" :value="o.value" :disabled="o.value === CsTicketType.RETURN_REFUND && !shipped">
                    {{ o.label }}
                  </a-radio-button>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <template v-if="!isResend">
              <a-col :xs="24" :sm="12" :lg="5">
                <a-form-item :label="$t('ecomOrder.return.reason')">
                  <a-select v-model:value="form.reason" allow-clear :options="toOptions(dict.ecom_return_reason)" :placeholder="$t('common.placeholder.select')" />
                </a-form-item>
              </a-col>
              <a-col v-if="form.ticketType === CsTicketType.RETURN_REFUND" :xs="24" :sm="12" :lg="5">
                <a-form-item :label="$t('ecomOrder.return.warehouse')" required>
                  <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomOrder.return.refundTotal')">
                  <a-input-number v-model:value="form.refundAmount" :min="0" :precision="2" :addon-before="order.currency" :placeholder="money(linesTotal)" class="full" />
                </a-form-item>
              </a-col>
            </template>
            <a-col :span="24">
              <a-form-item :label="$t('common.field.remark')">
                <a-input v-model:value="form.remark" :maxlength="255" :placeholder="isResend ? $t('ecomCs.ticket.resendRemarkPh') : undefined" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
        <a-table
          row-key="orderItemId"
          size="small"
          :columns="columns"
          :data-source="form.items"
          :pagination="false"
          :row-selection="{ selectedRowKeys: form.selected, onChange: (keys) => (form.selected = keys) }"
          :scroll="{ x: 760 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="title-cell" :title="record.title">{{ record.title || '-' }}</div>
              <div v-if="record.materialCode" class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
            </template>
            <template v-else-if="column.key === 'soldQty'">{{ qty(record.soldQty) }}</template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-model:value="record.qty" :min="0" :max="Number(record.soldQty)" :precision="4" class="full" :disabled="!form.selected.includes(record.orderItemId)" />
            </template>
            <template v-else-if="column.key === 'refundAmount'">
              <a-input-number v-model:value="record.refundAmount" :min="0" :precision="2" class="full" :disabled="!form.selected.includes(record.orderItemId)" />
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { getEcomOrder } from '@/api/ecom/order'
import { addCsTicket } from '@/api/ecom/cs'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money, qty, round2, sumBy } from '@/utils/number'
import { CsTicketType, EcomLocalStatus } from '@/constants/enums'
import { fcMoney } from '@/views/ecom/order/order'

/**
 * 转售后：会话 / 评价 / 店铺反馈关联的订单 → 选择类型（退货退款、仅退款、补发）和明细的件数（退款金额），
 * 退货退款 / 仅退款生成退货退款单，补发生成金额为 0 的补发订单。open({ sourceType, sourceId, orderId }) 打开，成功后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_cs_ticket_type', 'ecom_return_reason', 'ecom_order_status', 'ecom_local_status')

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const order = ref(null)
const source = reactive({ sourceType: null, sourceId: null })
const form = reactive({ ticketType: CsTicketType.REFUND_ONLY, reason: undefined, warehouseId: undefined, refundAmount: undefined, remark: '', items: [], selected: [] })

/** 已出库 / 已发货的订单才能退货退款 */
const shipped = computed(() => [EcomLocalStatus.OUTBOUND, EcomLocalStatus.SHIPPED].includes(order.value?.localStatus))
const isResend = computed(() => form.ticketType === CsTicketType.RESEND)
const typeHint = computed(() =>
  isResend.value ? t('ecomCs.ticket.resendHint') : form.ticketType === CsTicketType.RETURN_REFUND ? t('ecomCs.ticket.returnHint') : t('ecomCs.ticket.refundHint')
)
const selectedItems = () => form.items.filter((i) => form.selected.includes(i.orderItemId) && Number(i.qty) > 0)
const linesTotal = computed(() => sumBy(selectedItems(), 'refundAmount', round2))

const columns = computed(() => [
  { title: t('ecomOrder.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.return.itemTitle'), key: 'title', width: 260 },
  { title: t('ecomOrder.return.soldQty'), key: 'soldQty', width: 100, align: 'right' },
  { title: isResend.value ? t('ecomCs.ticket.resendQty') : t('ecomOrder.return.returnQty'), key: 'qty', width: 130 },
  ...(isResend.value ? [] : [{ title: t('ecomOrder.return.lineRefund'), key: 'refundAmount', width: 140 }])
])

async function show({ sourceType, sourceId, orderId }) {
  Object.assign(source, { sourceType, sourceId })
  Object.assign(form, { ticketType: CsTicketType.REFUND_ONLY, reason: undefined, warehouseId: undefined, refundAmount: undefined, remark: '', items: [], selected: [] })
  order.value = null
  open.value = true
  if (!orderId) return
  loading.value = true
  try {
    const o = await getEcomOrder(orderId)
    order.value = o
    form.warehouseId = o.warehouseId || undefined
    form.items = (o.items || []).map((i) => ({
      orderItemId: i.id,
      sellerSku: i.sellerSku,
      title: i.title,
      materialCode: i.materialCode,
      materialName: i.materialName,
      soldQty: i.qty,
      qty: Number(i.qty),
      refundAmount: Number(i.amount) || 0
    }))
    form.selected = form.items.map((i) => i.orderItemId)
  } finally {
    loading.value = false
  }
}

async function submit() {
  const rows = selectedItems()
  if (!rows.length) return message.warning(t('ecomOrder.return.noItems'))
  if (rows.some((i) => Number(i.qty) > Number(i.soldQty))) return message.warning(t('ecomOrder.return.qtyExceeds'))
  if (form.ticketType === CsTicketType.RETURN_REFUND && !form.warehouseId) return message.warning(t('ecomOrder.return.warehouseRequired'))
  saving.value = true
  try {
    await addCsTicket({
      sourceType: source.sourceType,
      sourceId: source.sourceId,
      ticketType: form.ticketType,
      orderId: order.value.id,
      reason: isResend.value ? null : form.reason || null,
      refundAmount: isResend.value ? null : form.refundAmount ?? null,
      warehouseId: form.ticketType === CsTicketType.RETURN_REFUND ? form.warehouseId : null,
      remark: form.remark || null,
      items: rows.map((i) => ({ orderItemId: i.orderItemId, qty: i.qty, refundAmount: isResend.value ? null : i.refundAmount ?? null }))
    })
    message.success(isResend.value ? t('ecomCs.ticket.resendCreated') : t('ecomCs.ticket.returnCreated'))
    open.value = false
    emit('success')
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
.title-cell {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-left: 4px;
}
</style>
