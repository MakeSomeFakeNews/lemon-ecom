<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.order.detailTitle', { no: detail.platformOrderNo }) : $t('ecomOrder.order.title')" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <DictTag type="ecom_order_status" :value="detail.status" />
        <DictTag type="ecom_local_status" :value="detail.localStatus" />
        <a-tag v-if="isOverdue(detail)" color="error" :bordered="false">{{ $t('ecomOrder.order.overdue') }}</a-tag>
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert
          v-if="detail.localStatus === EcomLocalStatus.ON_HOLD"
          type="warning"
          show-icon
          class="block"
          :message="$t('ecomOrder.ops.held')"
          :description="detail.holdReason || '-'"
        />
        <a-alert
          v-if="detail.exception && detail.localStatus !== EcomLocalStatus.SHIPPED"
          :type="detail.localStatus === EcomLocalStatus.EXCEPTION ? 'error' : 'warning'"
          show-icon
          class="block"
          :message="$t('ecomOrder.order.exception')"
          :description="detail.exception"
        />

        <div class="section-title">{{ $t('ecomOrder.order.sectionInfo') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.store')">
            {{ detail.storeName || '-' }} <DictTag type="ecom_platform" :value="detail.platform" />
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.platformOrderNo')">{{ detail.platformOrderNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.fulfillment')"><DictTag type="ecom_fulfillment" :value="detail.fulfillment" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.orderTime')">{{ detail.orderTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.paidTime')">{{ detail.paidTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.shipBy')">
            <span :class="{ 'overdue-text': isOverdue(detail) }">{{ detail.shipBy || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.deliverBy')">{{ detail.deliverBy || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.ops.planShipTime')">{{ detail.planShipTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.ops.weight')">{{ detail.weight != null ? `${Number(detail.weight)} ${$t('ecomOrder.common.kg')}` : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.ops.matchedRules')">{{ detail.matchedRules || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.buyer')">
            {{ detail.buyerName || '-' }}<span v-if="detail.buyerEmail" class="sub-text"> {{ detail.buyerEmail }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.receiver')">{{ [detail.shipName, detail.shipPhone].filter(Boolean).join(' ') || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.address')" :span="3">{{ address || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.note" :label="$t('ecomOrder.order.note')" :span="3">{{ detail.note }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.order.sectionAmount') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.order.itemAmount')">{{ fcMoney(detail.currency, detail.itemAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.shippingAmount')">{{ fcMoney(detail.currency, detail.shippingAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.taxAmount')">{{ fcMoney(detail.currency, detail.taxAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.discountAmount')">{{ fcMoney(detail.currency, detail.discountAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.totalAmount')"><b>{{ fcMoney(detail.currency, detail.totalAmount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.exchangeRate')">{{ detail.exchangeRate != null ? Number(detail.exchangeRate) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.totalBase')" :span="2"><b>{{ baseMoney(detail.totalBase) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.platformFee')">
            {{ fcMoney(detail.currency, detail.platformFee) }}
            <a-tag :bordered="false" :color="detail.feeActual === 1 ? 'green' : 'default'" class="fee-tag">
              {{ detail.feeActual === 1 ? $t('ecomOrder.order.feeActual') : $t('ecomOrder.order.feeEstimated') }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.fulfillmentFee')">{{ fcMoney(detail.currency, detail.fulfillmentFee) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.otherFee')">{{ fcMoney(detail.currency, detail.otherFee) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.refundAmount')">{{ fcMoney(detail.currency, detail.refundAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.costAmount')">{{ baseMoney(detail.costAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.shippingCost')">{{ baseMoney(detail.shippingCost) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.profit')" :span="2">
            <b :class="{ 'profit-negative': Number(detail.profit) < 0 }">{{ baseMoney(detail.profit) }}</b>
          </a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.order.sectionItems') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 1400 }" class="block">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="title-cell" :title="record.title">{{ record.title || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'price'">{{ price(record.price) }}</template>
            <template v-else-if="['amount', 'shipping', 'tax', 'discount'].includes(column.key)">{{ money(record[column.key]) }}</template>
            <template v-else-if="column.key === 'material'">
              <template v-if="record.bundle === 1 && record.parts?.length">
                <a-tag color="purple" :bordered="false">{{ $t('ecomCatalog.listing.bundle') }}</a-tag>
                <div v-for="p in record.parts" :key="p.id" class="sub-text">{{ p.materialCode }} {{ p.materialName }} × {{ qty(p.unitQty) }}</div>
              </template>
              <template v-else-if="record.materialId">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.materialSpec }}</div>
              </template>
              <a-tag v-else color="error" :bordered="false">{{ $t('ecomOrder.common.unmapped') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'materialQty'">
              <template v-if="record.bundle === 1 && record.parts?.length">
                <div v-for="p in record.parts" :key="p.id">{{ qty(p.materialQty) }} {{ p.unit || '' }}</div>
              </template>
              <template v-else>{{ record.materialId ? `${qty(record.materialQty)} ${record.unit || ''}` : '-' }}</template>
            </template>
            <template v-else-if="column.key === 'allocatedQty'">{{ qty(record.allocatedQty) }}</template>
            <template v-else-if="column.key === 'cost'">{{ record.cost != null ? baseMoney(record.cost) : '-' }}</template>
          </template>
        </a-table>

        <div class="section-title">{{ $t('ecomOrder.ops.packages') }}</div>
        <OrderPackages :packages="detail.packages || []" class="block" />

        <div class="section-title">{{ $t('ecomOrder.order.sectionShip') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small">
          <a-descriptions-item :label="$t('ecomOrder.order.tradeDoc')">{{ detail.tradeDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.outWarehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.carrier')">{{ detail.carrier || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.order.shipTime')">{{ detail.shipTime || $t('ecomOrder.order.notShipped') }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  AuditOutlined,
  CarOutlined,
  CloudUploadOutlined,
  EditOutlined,
  ExportOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  RollbackOutlined,
  ScissorOutlined,
  StopOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import OrderPackages from './OrderPackages.vue'
import { auditEcomOrders, cancelEcomOrder, confirmEcomOrderShipment, getEcomOrder, recheckEcomOrders, rematchEcomOrder, unholdEcomOrders } from '@/api/ecom/order'
import { useUserStore } from '@/store/user'
import { money, price, qty } from '@/utils/number'
import { EcomLocalStatus } from '@/constants/enums'
import {
  ORDER_RESULT_KEYS,
  baseMoney,
  canAudit,
  canCancel,
  canEdit,
  canHold,
  canOutbound,
  canRecheck,
  canRematch,
  canReturn,
  canShip,
  canShipConfirm,
  canSplit,
  canUnhold,
  fcMoney,
  isOverdue,
  showBatchResult
} from './order'

/**
 * 平台订单详情子页：订单信息（含挂起原因、命中规则、计划发货）、金额（原币 + 本位币、平台费用、成本与利润）、商品明细（映射物料、已分配包裹的件数）、
 * 包裹、发货信息。重新匹配、审核、重新检查库存、取消挂起、重新回传发货、取消在本组件内完成（触发 changed）；
 * 出库、发货、修改、退货退款、挂起、拆分交给页面打开对应弹窗（事件带订单）。ref 调用 open(id)、reload()
 */
const emit = defineEmits(['changed', 'outbound', 'ship', 'edit', 'return', 'hold', 'split'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const itemColumns = [
  { title: t('ecomOrder.order.itemSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.order.itemPlatformId'), dataIndex: 'platformItemId', width: 140 },
  { title: t('ecomOrder.order.itemTitle'), key: 'title', width: 260 },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 80, align: 'right' },
  { title: t('ecomOrder.order.itemPrice'), key: 'price', width: 100, align: 'right' },
  { title: t('ecomOrder.order.itemAmountCol'), key: 'amount', width: 110, align: 'right' },
  { title: t('ecomOrder.order.itemShipping'), key: 'shipping', width: 90, align: 'right' },
  { title: t('ecomOrder.order.itemTax'), key: 'tax', width: 90, align: 'right' },
  { title: t('ecomOrder.order.itemDiscount'), key: 'discount', width: 90, align: 'right' },
  { title: t('ecomOrder.order.itemMaterial'), key: 'material', width: 220 },
  { title: t('ecomOrder.order.itemMaterialQty'), key: 'materialQty', width: 110, align: 'right' },
  { title: t('ecomOrder.ops.allocated'), key: 'allocatedQty', width: 80, align: 'right' },
  { title: t('ecomOrder.order.itemCost'), key: 'cost', width: 120, align: 'right' }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
let currentId

const address = computed(() => {
  const d = detail.value
  return d ? [d.shipCountry, d.shipState, d.shipCity, d.shipPostcode, d.shipAddress].filter(Boolean).join(' ') : ''
})

async function load(id) {
  loading.value = true
  try {
    detail.value = await getEcomOrder(id)
  } finally {
    loading.value = false
  }
}

function show(id) {
  currentId = id
  detail.value = undefined
  open.value = true
  return load(id)
}

/** 页面在出库、发货、修改后调用：详情打开时重新读取 */
const reload = () => (open.value && currentId ? load(currentId) : undefined)

function confirmAction(title, content, api, success, danger = false) {
  Modal.confirm({
    title,
    content,
    okType: danger ? 'danger' : 'primary',
    async onOk() {
      await api(currentId)
      message.success(success)
      await load(currentId)
      emit('changed')
    }
  })
}

/** 批量接口用于单张订单：列出未完成的原因，刷新详情 */
async function runOne(api) {
  showBatchResult(await api([currentId]), 1, ORDER_RESULT_KEYS)
  await load(currentId)
  emit('changed')
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (canUnhold(d) && can('ecom:order:audit')) {
    list.push({
      key: 'unhold',
      label: t('ecomOrder.ops.unhold'),
      icon: PlayCircleOutlined,
      run: () => confirmAction(t('ecomOrder.ops.unholdConfirm', { n: 1 }), '', (id) => unholdEcomOrders([id]), t('ecomOrder.ops.unheld'))
    })
  }
  if (canHold(d) && can('ecom:order:audit')) {
    list.push({ key: 'hold', label: t('ecomOrder.ops.hold'), icon: PauseCircleOutlined, run: () => emit('hold', d) })
  }
  if (canSplit(d) && can('ecom:order:merge')) {
    list.push({ key: 'split', label: t('ecomOrder.ops.split'), icon: ScissorOutlined, run: () => emit('split', d) })
  }
  if (canShipConfirm(d) && can('ecom:order:ship')) {
    list.push({
      key: 'shipConfirm',
      label: t('ecomOrder.ops.shipConfirm'),
      icon: CloudUploadOutlined,
      run: () => confirmAction(t('ecomOrder.ops.shipConfirmConfirm', { no: d.platformOrderNo }), '', confirmEcomOrderShipment, t('ecomOrder.ops.shipConfirmed'))
    })
  }
  if (canRematch(d) && can('ecom:order:outbound')) {
    list.push({
      key: 'rematch',
      label: t('ecomOrder.order.rematch'),
      icon: ReloadOutlined,
      run: () => confirmAction(t('ecomOrder.order.rematchConfirm', { no: d.platformOrderNo }), '', rematchEcomOrder, t('ecomOrder.order.rematched'))
    })
  }
  if (canEdit(d) && can('ecom:order:edit')) {
    list.push({ key: 'edit', label: t('ecomOrder.order.editInfo'), icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (canReturn(d) && can('ecom:return:add')) {
    list.push({ key: 'return', label: t('ecomOrder.order.return'), icon: RollbackOutlined, run: () => emit('return', d) })
  }
  if (canCancel(d) && can('ecom:order:cancel')) {
    list.push({
      key: 'cancel',
      label: t('ecomOrder.order.cancel'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(t('ecomOrder.order.cancelConfirm', { no: d.platformOrderNo }), t('ecomOrder.order.cancelContent'), cancelEcomOrder, t('ecomOrder.order.cancelled'), true)
    })
  }
  if (canRecheck(d) && can('ecom:order:audit')) {
    list.push({ key: 'recheck', label: t('ecomOrder.ops.recheck'), icon: SyncOutlined, primary: true, run: () => runOne(recheckEcomOrders) })
  }
  if (canAudit(d) && can('ecom:order:audit')) {
    list.push({ key: 'audit', label: t('ecomOrder.ops.audit'), icon: AuditOutlined, primary: true, run: () => runOne(auditEcomOrders) })
  }
  if (canOutbound(d) && can('ecom:order:outbound')) {
    list.push({ key: 'outbound', label: t('ecomOrder.order.outbound'), icon: ExportOutlined, primary: !canAudit(d), run: () => emit('outbound', d) })
  }
  if (canShip(d) && can('ecom:order:ship')) {
    list.push({ key: 'ship', label: t('ecomOrder.order.ship'), icon: CarOutlined, primary: true, run: () => emit('ship', d) })
  }
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.fee-tag {
  margin-left: 6px;
}
.title-cell {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.overdue-text,
.profit-negative {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
