<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomOrder.return.addTitle')" :edit-title="$t('ecomOrder.return.addTitle')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomOrder.return.order')" name="orderId">
            <RemoteSelect
              v-model:value="form.orderId"
              :label="order ? orderLabel(order) : undefined"
              :fetch="fetchOrders"
              :fetch-by-ids="fetchOrdersByIds"
              :to-label="orderLabel"
              :dropdown-width="480"
              :placeholder="$t('ecomOrder.return.orderPh')"
              @select="(o) => loadOrder(o.id)"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.return.returnType')" name="returnType">
            <a-select v-model:value="form.returnType" :options="toOptions(dict.ecom_return_type)" :placeholder="$t('ecomOrder.return.selectType')" @change="onTypeChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.return.reason')" name="reason">
            <a-select v-model:value="form.reason" allow-clear :options="toOptions(dict.ecom_return_reason)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col v-if="isReturn" :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.return.warehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.return.refundTotal')" name="refundAmount" :extra="$t('ecomOrder.return.refundTotalHint', { amount: fcMoney(order?.currency, linesTotal) })">
            <a-input-number v-model:value="form.refundAmount" :min="0" :precision="2" :addon-before="order?.currency" class="full" :placeholder="money(linesTotal)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="isReturn ? 12 : 18">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert
        v-if="order"
        type="info"
        show-icon
        class="order-tip"
        :message="`${order.storeName || ''} · ${order.buyerName || order.shipName || ''} · ${$t('ecomOrder.return.orderPaid')} ${fcMoney(order.currency, order.totalAmount)}`"
      >
        <template #description>
          <span>{{ $t('ecomOrder.return.orderStatus') }}: </span>
          <DictTag type="ecom_local_status" :value="order.localStatus" />
          <span class="sub-text"> {{ isReturn ? $t('ecomOrder.return.returnRefundHint') : $t('ecomOrder.return.refundOnlyHint') }}</span>
        </template>
      </a-alert>

      <div class="items-head">
        <span class="section-title">{{ $t('ecomOrder.return.items') }}</span>
        <span class="sub-text">{{ $t('ecomOrder.return.itemsHint') }}</span>
      </div>
      <a-form-item name="items" :rules="itemRules" class="items-item">
        <a-table
          row-key="orderItemId"
          size="small"
          :columns="shippedPackages.length > 1 ? [...itemColumns, packageColumn] : itemColumns"
          :data-source="form.items"
          :pagination="false"
          :row-selection="{ selectedRowKeys: form.selected, onChange: (keys) => (form.selected = keys) }"
          :scroll="{ x: 900 }"
          :locale="{ emptyText: $t('ecomOrder.return.selectOrder') }"
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
            <template v-else-if="column.key === 'packageId'">
              <a-select v-model:value="record.packageId" allow-clear class="full" :placeholder="$t('ecomOrd.returnPkg.auto')"
                        :options="shippedPackages.map((p) => ({ value: p.id, label: `${p.packageNo} · ${p.trackingNo || '-'}` }))"
                        :disabled="!form.selected.includes(record.orderItemId)" />
            </template>
            <template v-else-if="column.key === 'refundAmount'">
              <a-input-number v-model:value="record.refundAmount" :min="0" :precision="2" class="full" :disabled="!form.selected.includes(record.orderItemId)" />
            </template>
          </template>
        </a-table>
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import DictTag from '@/components/DictTag.vue'
import RemoteSelect from '@/components/common/RemoteSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { getEcomOrder, pageEcomOrder } from '@/api/ecom/order'
import { addEcomReturn } from '@/api/ecom/return'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { money, qty, round2, sumBy } from '@/utils/number'
import { EcomLocalStatus, EcomPackageStatus, ReturnType } from '@/constants/enums'
import { fcMoney } from '../order/order'

/**
 * 新增退货退款（页式表单）：选择平台订单（远程搜索平台订单号 / 买家，或从订单页带入 orderId）→ 类型、原因、退回仓库（退货退款必填）→
 * 从订单明细勾选退回的行、填退回数量与退款金额（订单币种）→ 退款总额（留空按明细合计）。ref 调用 openCreate({ orderId? })
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_return_type', 'ecom_return_reason', 'ecom_local_status')

const itemColumns = [
  { title: t('ecomOrder.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.return.itemTitle'), key: 'title', width: 300 },
  { title: t('ecomOrder.return.soldQty'), key: 'soldQty', width: 110, align: 'right' },
  { title: t('ecomOrder.return.returnQty'), key: 'qty', width: 140 },
  { title: t('ecomOrder.return.lineRefund'), key: 'refundAmount', width: 160 }
]
/** 多包裹订单：可指定退回的包裹（按该包裹的发货单生成销售退货），不指定按发货顺序分配 */
const packageColumn = { title: t('ecomOrd.returnPkg.package'), key: 'packageId', width: 240 }

const emptyForm = () => ({
  orderId: undefined,
  returnType: undefined,
  reason: undefined,
  warehouseId: undefined,
  refundAmount: undefined,
  remark: '',
  items: [],
  selected: []
})

const drawer = useFormDrawer({
  emptyForm,
  create: addEcomReturn,
  update: () => Promise.reject(new Error('unsupported')),
  toPayload: (f) => ({
    orderId: f.orderId,
    returnType: f.returnType,
    reason: f.reason || null,
    warehouseId: f.returnType === ReturnType.RETURN_REFUND ? f.warehouseId : null,
    refundAmount: f.refundAmount ?? null,
    remark: f.remark || null,
    items: selectedItems(f).map((i) => ({ orderItemId: i.orderItemId, qty: i.qty, refundAmount: i.refundAmount ?? null, packageId: i.packageId ?? null }))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

/** 已读取的订单详情（含明细），按 ID 缓存 */
const orderCache = new Map()
/** 当前订单 */
const order = computed(() => orderCache.get(form.value.orderId))
const isReturn = computed(() => form.value.returnType === ReturnType.RETURN_REFUND)
const shippedPackages = computed(() => (order.value?.packages || []).filter((p) => p.status === EcomPackageStatus.SHIPPED))
const selectedItems = (f) => f.items.filter((i) => f.selected.includes(i.orderItemId) && Number(i.qty) > 0)
const linesTotal = computed(() => sumBy(selectedItems(form.value), 'refundAmount', round2))

const rules = {
  orderId: [{ required: true, message: t('ecomOrder.return.selectOrder') }],
  returnType: [{ required: true, message: t('ecomOrder.return.selectType') }],
  warehouseId: [
    {
      validator: (_, v) => (form.value.returnType === ReturnType.RETURN_REFUND && v == null ? Promise.reject(t('ecomOrder.return.warehouseRequired')) : Promise.resolve())
    }
  ]
}
const itemRules = [
  {
    validator: () => {
      const rows = selectedItems(form.value)
      if (!rows.length) return Promise.reject(t('ecomOrder.return.noItems'))
      if (rows.some((i) => Number(i.qty) > Number(i.soldQty))) return Promise.reject(t('ecomOrder.return.qtyExceeds'))
      return Promise.resolve()
    }
  }
]

const orderLabel = (o) => [o.platformOrderNo, o.storeName, o.buyerName || o.shipName].filter(Boolean).join(' · ')
const fetchOrders = ({ keyword, current, size }) => pageEcomOrder({ keyword, current, size })
const fetchOrdersByIds = (ids) => Promise.all(ids.map((id) => getEcomOrder(id)))

/** 读取订单明细，默认全部勾选、退回全部数量、退款金额为该行商品金额 */
async function loadOrder(id) {
  if (!id) return
  const o = await getEcomOrder(id)
  orderCache.set(id, o)
  if (form.value.orderId !== id) form.value.orderId = id
  form.value.items = (o.items || []).map((i) => ({
    orderItemId: i.id,
    sellerSku: i.sellerSku,
    title: i.title,
    materialCode: i.materialCode,
    materialName: i.materialName,
    soldQty: i.qty,
    qty: Number(i.qty),
    refundAmount: Number(i.amount) || 0,
    packageId: undefined
  }))
  form.value.selected = form.value.items.map((i) => i.orderItemId)
  form.value.refundAmount = undefined
  // 已出库 / 已发货的订单默认退货退款（退回出库仓库），否则仅退款
  const outbound = [EcomLocalStatus.OUTBOUND, EcomLocalStatus.SHIPPED, EcomLocalStatus.PARTIAL].includes(o.localStatus)
  if (!form.value.returnType) form.value.returnType = outbound ? ReturnType.RETURN_REFUND : ReturnType.REFUND_ONLY
  if (!form.value.warehouseId) form.value.warehouseId = o.warehouseId ?? undefined
}

// 清掉订单时同时清掉明细
watch(
  () => form.value.orderId,
  (id) => {
    if (id == null) {
      form.value.items = []
      form.value.selected = []
    }
  }
)

function onTypeChange(v) {
  if (v === ReturnType.RETURN_REFUND && !form.value.warehouseId) form.value.warehouseId = order.value?.warehouseId ?? undefined
}

/** 新增：可带入订单 ID（从订单页跳转） */
async function openCreate(defaults = {}) {
  await drawer.openCreate(defaults)
  if (defaults.orderId) await loadOrder(defaults.orderId)
}

defineExpose({ openCreate })
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.order-tip {
  margin-bottom: 16px;
}
.items-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.items-item :deep(.ant-form-item-control-input) {
  min-height: 0;
}
.title-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
