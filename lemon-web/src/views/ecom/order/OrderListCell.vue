<template>
  <template v-if="columnKey === 'order'">
    <div class="no-line">
      <a class="order-no" :title="record.platformOrderNo" @click="emit('open')">{{ record.platformOrderNo }}</a>
      <a-typography-text :copyable="{ text: record.platformOrderNo, tooltip: false }" class="copy" />
    </div>
    <div class="meta-line">
      <DictTag type="ecom_platform" :value="record.platform" />
      <span class="store" :title="record.storeName">{{ record.storeName }}</span>
    </div>
    <div class="time-line">
      <span class="sub-text">{{ record.orderTime }}</span>
      <OrderTags :order="record" class="inline-tags" />
    </div>
  </template>
  <template v-else-if="columnKey === 'items'">
    <OrderItemsCell :order="record" />
  </template>
  <template v-else-if="columnKey === 'buyer'">
    <div class="ellipsis" :title="record.buyerName || record.shipName">{{ record.buyerName || record.shipName || '-' }}</div>
    <div class="sub-text addr-line">
      <span v-if="record.shipCountry" class="country">{{ record.shipCountry }}</span>
      <span class="ellipsis" :title="[record.shipState, record.shipCity].filter(Boolean).join(', ')">{{ [record.shipState, record.shipCity].filter(Boolean).join(', ') }}</span>
      <a-tooltip v-if="record.addressIssue" :title="record.addressIssue">
        <EnvironmentOutlined :class="record.addressInvalid === 1 ? 'exception-icon' : 'warn-icon'" />
      </a-tooltip>
    </div>
    <a-tag v-if="record.remoteArea === 1" color="purple" :bordered="false" class="mini-tag">{{ $t('ecomOrdUi.list.remote') }}</a-tag>
  </template>
  <template v-else-if="columnKey === 'paid'">
    <div class="nowrap amount">{{ fcMoney(record.currency, record.totalAmount) }}</div>
    <div class="sub-text nowrap">{{ baseMoney(record.totalBase) }}</div>
  </template>
  <template v-else-if="columnKey === 'profit'">
    <template v-if="isActualProfit(record)">
      <div class="nowrap amount" :class="{ 'profit-negative': Number(record.profit) < 0 }">{{ baseMoney(record.profit) }}</div>
      <div class="sub-text">{{ $t('ecomOrdUi.list.actual') }}</div>
    </template>
    <template v-else-if="record.estProfit != null">
      <OrderEstimate :order="record" />
    </template>
    <span v-else>-</span>
  </template>
  <template v-else-if="columnKey === 'localStatus'">
    <div class="nowrap">
      <DictTag type="ecom_local_status" :value="record.localStatus" />
      <a-tooltip v-if="record.localStatus === EcomLocalStatus.ON_HOLD ? record.holdReason : record.exception" :title="record.localStatus === EcomLocalStatus.ON_HOLD ? record.holdReason : record.exception">
        <ExclamationCircleOutlined class="exception-icon" />
      </a-tooltip>
    </div>
    <div class="sub-text status-sub">{{ dictLabel(dict.ecom_order_status, record.status) }} · {{ dictLabel(dict.ecom_fulfillment, record.fulfillment) }}</div>
  </template>
  <template v-else-if="columnKey === 'ship'">
    <template v-if="record.trackingNo">
      <div class="ellipsis" :title="record.trackingNo">{{ record.trackingNo }}</div>
      <div class="sub-text ellipsis">{{ [record.carrier, record.shipTime && record.shipTime.slice(5, 16)].filter(Boolean).join(' · ') }}</div>
    </template>
    <template v-else>
      <div v-if="record.shipBy" class="ship-by">
        <span class="sub-text">{{ $t('ecomOrdUi.list.shipBy') }} </span>
        <span :class="{ 'overdue-text': isOverdue(record) }">{{ record.shipBy.slice(5, 16) }}</span>
        <a-tag v-if="isOverdue(record)" color="error" :bordered="false" class="due-tag">{{ $t('ecomOrder.order.overdue') }}</a-tag>
        <a-tag v-else-if="dueHours(record)" color="warning" :bordered="false" class="due-tag">{{ $t('ecomOrder.ops.dueIn', { h: dueHours(record) }) }}</a-tag>
      </div>
      <div v-if="record.planShipTime" class="sub-text nowrap">{{ $t('ecomOrder.ops.planShip', { time: record.planShipTime.slice(5, 16) }) }}</div>
    </template>
    <div v-if="!record.trackingNo && !record.shipBy && !record.planShipTime && !record.warehouseName && !record.packageCount">-</div>
    <div v-if="record.warehouseName || record.packageCount" class="sub-text ellipsis" :title="record.warehouseName">
      {{ [record.warehouseName, record.packageCount ? $t('ecomOrdUi.list.packages', { n: record.packageCount }) : ''].filter(Boolean).join(' · ') }}
    </div>
  </template>
</template>

<script setup>
import { EnvironmentOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import OrderTags from './OrderTags.vue'
import OrderEstimate from './OrderEstimate.vue'
import OrderItemsCell from './OrderItemsCell.vue'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { EcomLocalStatus } from '@/constants/enums'
import { baseMoney, dueHours, fcMoney, isOverdue } from './order'

/** 平台订单列表中多项信息紧凑显示的单元格（订单、商品、买家、金额、利润、状态、发货），columnKey 为列的 key；点单号触发 open */
defineProps({
  columnKey: { type: String, required: true },
  record: { type: Object, required: true }
})
const emit = defineEmits(['open'])
const dict = useDict('ecom_order_status', 'ecom_fulfillment')
const dictLabel = (list, value) => findDict(list, value)?.label || value || '-'
/** 已发货的订单显示实际利润，其余显示预估利润 */
const isActualProfit = (o) => [EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED].includes(o.localStatus) && o.profit != null
</script>

<style scoped>
.ship-by {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  column-gap: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.nowrap {
  white-space: nowrap;
}
.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.no-line {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 0;
}
.order-no {
  overflow: hidden;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.copy {
  flex: none;
}
.time-line {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 6px;
  align-items: center;
}
.inline-tags {
  margin-top: 0 !important;
}
.inline-tags :deep(.ant-tag) {
  font-size: 11px;
  line-height: 16px;
}
.meta-line {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 0;
  margin: 2px 0;
}
.meta-line :deep(.ant-tag) {
  margin-inline-end: 0;
  padding: 0 4px;
  font-size: 11px;
  line-height: 16px;
}
.store {
  overflow: hidden;
  color: var(--app-text-2);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.addr-line {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 0;
}
.country {
  flex: none;
  padding: 0 4px;
  border: 1px solid var(--app-border);
  border-radius: 3px;
  color: var(--app-text-2);
  font-size: 11px;
  line-height: 16px;
}
.mini-tag {
  margin-top: 2px;
  font-size: 11px;
  line-height: 16px;
}
.amount {
  font-weight: 500;
}
.status-sub {
  margin-top: 2px;
  white-space: nowrap;
}
.warn-icon {
  margin-left: 4px;
  color: var(--ant-color-warning, #faad14);
  cursor: help;
}
.exception-icon {
  margin-left: 4px;
  color: var(--ant-color-error, #ff4d4f);
  cursor: help;
}
.due-tag {
  margin-inline: 0;
  font-size: 11px;
  line-height: 16px;
}
.overdue-text {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 500;
}
.profit-negative {
  color: var(--ant-color-error, #ff4d4f);
}
.copy :deep(.ant-typography-copy) {
  color: var(--app-text-3);
}
</style>
