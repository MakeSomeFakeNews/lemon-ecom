<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.order.detailTitle', { no: detail.platformOrderNo }) : $t('ecomOrder.order.title')" @close="open = false">
    <template v-if="detail" #title>
      <span class="head-title">
        {{ $t('ecomOrder.order.detailTitle', { no: detail.platformOrderNo }) }}
        <a-typography-text :copyable="{ text: detail.platformOrderNo, tooltip: false }" />
      </span>
      <DictTag type="ecom_local_status" :value="detail.localStatus" />
      <a-tag v-if="isOverdue(detail)" color="error" :bordered="false">{{ $t('ecomOrder.order.overdue') }}</a-tag>
      <DictTag v-if="detail.orderType && detail.orderType !== EcomOrderType.PLATFORM" type="ecom_order_type" :value="detail.orderType" />
      <a-tag v-for="tg in detail.tags || []" :key="tg.id" :color="tg.color" :bordered="false">{{ tg.name }}</a-tag>
    </template>
    <template v-if="detail && actions.length" #extra>
      <a-space :size="8">
        <a-button v-for="a in headActions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
        <a-dropdown v-if="moreActions.length" trigger="click" placement="bottomRight">
          <a-button>{{ $t('ecomOrdUi.batch.more') }}<DownOutlined /></a-button>
          <template #overlay>
            <a-menu @click="({ key }) => moreActions.find((a) => a.key === key)?.run()">
              <a-menu-item v-for="a in moreActions" :key="a.key" :danger="a.danger">
                <template #icon><component :is="a.icon" /></template>{{ a.label }}
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <a-skeleton v-if="!detail" active :paragraph="{ rows: 12 }" />
      <template v-else>
        <a-alert v-if="detail.localStatus === EcomLocalStatus.ON_HOLD" type="warning" show-icon class="block" :message="$t('ecomOrder.ops.held')" :description="detail.holdReason || '-'" />
        <a-alert
          v-if="detail.exception && detail.localStatus !== EcomLocalStatus.SHIPPED"
          :type="detail.localStatus === EcomLocalStatus.EXCEPTION ? 'error' : 'warning'"
          show-icon
          class="block"
          :message="$t('ecomOrder.order.exception')"
          :description="detail.exception"
        />
        <a-alert v-if="check && (check.issues.length || check.blacklistId)" :type="check.invalid || check.blacklistId ? 'error' : 'warning'" show-icon class="block">
          <template #message>{{ check.invalid ? $t('ecomOrd.check.invalid') : check.blacklistId ? $t('ecomOrd.check.blacklisted') : $t('ecomOrd.check.warnings') }}</template>
          <template #description>
            <div v-if="check.blacklistDesc" class="issue"><a-tag color="magenta" :bordered="false">{{ $t('ecomOrd.check.blacklist') }}</a-tag>{{ check.blacklistDesc }}</div>
            <div v-for="(i, k) in check.issues" :key="k" class="issue">
              <a-tag :color="i.error ? 'error' : 'warning'" :bordered="false">{{ i.error ? $t('ecomOrd.addr.error') : $t('ecomOrd.addr.warn') }}</a-tag>{{ i.message }}
            </div>
            <a-space class="issue-actions">
              <a-button v-if="canEdit(detail) && can('ecom:order:edit')" size="small" @click="addressModal.open(detail, check.issues)">{{ $t('ecomOrd.addr.edit') }}</a-button>
              <a-button v-if="can('ecom:order:audit')" size="small" :loading="rechecking" @click="recheck">{{ $t('ecomOrd.check.recheck') }}</a-button>
            </a-space>
          </template>
        </a-alert>

        <div class="stats">
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrder.order.totalAmount') }}</div>
            <div class="stat-value">{{ fcMoney(detail.currency, detail.totalAmount) }}</div>
            <div class="stat-sub">{{ baseMoney(detail.totalBase) }} · {{ $t('ecomOrder.order.exchangeRate') }} {{ detail.exchangeRate != null ? Number(detail.exchangeRate) : '-' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ isShipped ? $t('ecomOrder.order.profit') : $t('ecomOrd.estimate.profit') }}</div>
            <div class="stat-value" :class="{ negative: Number(shownProfit) < 0 }">{{ shownProfit != null ? baseMoney(shownProfit) : '-' }}</div>
            <div class="stat-sub">{{ $t('ecomOrdUi.detail.rate') }} {{ shownRate != null ? `${shownRate}%` : '-' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ $t('ecomOrdUi.detail.items') }}</div>
            <div class="stat-value">{{ $t('ecomOrdUi.detail.itemsValue', { lines: (detail.items || []).length, qty: totalQty }) }}</div>
            <div class="stat-sub">{{ $t('ecomOrdUi.list.packages', { n: (detail.packages || []).length }) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">{{ detail.shipTime ? $t('ecomOrder.order.shipTime') : $t('ecomOrder.order.shipBy') }}</div>
            <div class="stat-value" :class="{ negative: isOverdue(detail) }">{{ (detail.shipTime || detail.shipBy || '-').slice(0, 16) }}</div>
            <div class="stat-sub"><DictTag type="ecom_fulfillment" :value="detail.fulfillment" /> <DictTag type="ecom_order_status" :value="detail.status" /></div>
          </div>
        </div>

        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :xl="16">
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrder.order.sectionItems') }}</div>
              <OrderDetailItems :items="detail.items || []" />
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.detail.logistics') }}</div>
              <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
                <a-descriptions-item :label="$t('ecomOrder.order.outWarehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.channelName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.tradeDoc')">{{ detail.tradeDocNo || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.carrier')">{{ detail.carrier || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.shipTime')">{{ detail.shipTime || $t('ecomOrder.order.notShipped') }}</a-descriptions-item>
              </a-descriptions>
              <OrderLogistics :detail="detail" :tracks="tracks" @pull="pull" @reconfirm="reconfirm" />
            </section>
            <section v-if="detail.orderType !== EcomOrderType.RESEND && resends.resends.length" class="panel">
              <div class="panel-title">{{ $t('ecomOrd.resend.list') }} · {{ $t('ecomOrd.resend.afterSaleCost', { amount: baseMoney(resends.afterSaleCost) }) }}</div>
              <a-table row-key="id" size="small" :columns="resendColumns" :data-source="resends.resends" :pagination="false" :scroll="{ x: 700 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'no'"><a @click="show(record.id)">{{ record.platformOrderNo }}</a></template>
                  <template v-else-if="column.key === 'localStatus'"><DictTag type="ecom_local_status" :value="record.localStatus" /></template>
                  <template v-else-if="column.key === 'cost'">{{ baseMoney(Number(record.costAmount || 0) + Number(record.shippingCost || 0)) }}</template>
                </template>
              </a-table>
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.detail.timeline') }}</div>
              <OrderTimeline :logs="logs" :adding="adding" @add="onAddNote" />
            </section>
          </a-col>
          <a-col :xs="24" :xl="8">
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.detail.buyer') }}</div>
              <a-descriptions :column="1" size="small" class="side-desc">
                <a-descriptions-item :label="$t('ecomOrder.order.buyer')">{{ detail.buyerName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.buyerEmail')"><span class="break">{{ detail.buyerEmail || '-' }}</span></a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.receiver')">{{ detail.shipName || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.shipPhone')">{{ detail.shipPhone || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.address')">
                  <div class="address">
                    <div>{{ detail.shipAddress || '-' }}</div>
                    <div>{{ [detail.shipCity, detail.shipState, detail.shipPostcode].filter(Boolean).join(', ') }}</div>
                    <div><span v-if="detail.shipCountry" class="country">{{ detail.shipCountry }}</span></div>
                  </div>
                </a-descriptions-item>
                <a-descriptions-item v-if="detail.note" :label="$t('ecomOrder.order.note')">{{ detail.note }}</a-descriptions-item>
              </a-descriptions>
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrdUi.detail.profit') }}</div>
              <OrderProfitCard :detail="detail" />
            </section>
            <section class="panel">
              <div class="panel-title">{{ $t('ecomOrder.order.sectionInfo') }}</div>
              <a-descriptions :column="1" size="small" class="side-desc">
                <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.storeName || '-' }} <DictTag type="ecom_platform" :value="detail.platform" /></a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.orderTime')">{{ detail.orderTime || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.paidTime')">{{ detail.paidTime || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.shipBy')"><span :class="{ negative: isOverdue(detail) }">{{ detail.shipBy || '-' }}</span></a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.order.deliverBy')">{{ detail.deliverBy || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.ops.planShipTime')">{{ detail.planShipTime || '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.ops.weight')">{{ detail.weight != null ? `${Number(detail.weight)} ${$t('ecomOrder.common.kg')}` : '-' }}</a-descriptions-item>
                <a-descriptions-item :label="$t('ecomOrder.ops.matchedRules')">{{ detail.matchedRules || '-' }}</a-descriptions-item>
                <a-descriptions-item v-if="detail.sourceOrderNo" :label="$t('ecomOrd.resend.sourceOrder')"><a @click="show(detail.sourceOrderId)">{{ detail.sourceOrderNo }}</a></a-descriptions-item>
                <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
              </a-descriptions>
            </section>
          </a-col>
        </a-row>
      </template>
    </a-spin>

    <ResendModal ref="resendModal" @done="(id) => { emit('changed'); show(id) }" />
    <AddressModal ref="addressModal" @done="afterEnhance" />
    <BlacklistModal ref="blacklistModal" @done="afterEnhance" />
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  AuditOutlined,
  DownOutlined,
  CarOutlined,
  CloudUploadOutlined,
  EditOutlined,
  ExportOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  RedoOutlined,
  ReloadOutlined,
  RollbackOutlined,
  ScissorOutlined,
  StopOutlined,
  SyncOutlined,
  UserDeleteOutlined
} from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import OrderDetailItems from './OrderDetailItems.vue'
import OrderLogistics from './OrderLogistics.vue'
import OrderProfitCard from './OrderProfitCard.vue'
import OrderTimeline from './OrderTimeline.vue'
import { useOrderEnhance } from './useOrderEnhance'
import ResendModal from './ResendModal.vue'
import AddressModal from './AddressModal.vue'
import BlacklistModal from './BlacklistModal.vue'
import { auditEcomOrders, cancelEcomOrder, confirmEcomOrderShipment, getEcomOrder, recheckEcomOrders, rematchEcomOrder, unholdEcomOrders } from '@/api/ecom/order'
import { useUserStore } from '@/store/user'
import { EcomLocalStatus, EcomOrderType } from '@/constants/enums'
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

const resendColumns = [
  { title: t('ecomOrder.common.platformOrderNo'), key: 'no', width: 180 },
  { title: t('ecomOrder.order.orderTime'), dataIndex: 'orderTime', width: 160 },
  { title: t('ecomOrder.order.localStatus'), key: 'localStatus', width: 100 },
  { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo', width: 160 },
  { title: t('ecomOrd.resend.cost'), key: 'cost', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
const resendModal = ref()
const addressModal = ref()
const blacklistModal = ref()
let currentId

/** 订单增强的操作（备注、修改地址、拉黑、重新校验）后：刷新详情并通知页面 */
async function afterEnhance() {
  await load(currentId)
  emit('changed')
}

const { check, logs, resends, tracks, adding, rechecking, addNote, recheck, pull, reconfirm } = useOrderEnhance(detail, () => emit('changed'))
async function onAddNote(content, done) {
  if (await addNote(content)) done()
}

const isShipped = computed(() => [EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED].includes(detail.value?.localStatus))
const shownProfit = computed(() => (isShipped.value ? detail.value?.profit : detail.value?.estProfit))
const shownRate = computed(() => {
  const d = detail.value
  if (!d) return null
  if (!isShipped.value) return d.estProfitRate != null ? Number(d.estProfitRate) : null
  return Number(d.totalBase) ? Math.round((Number(d.profit) / Number(d.totalBase)) * 10000) / 100 : null
})
const totalQty = computed(() => (detail.value?.items || []).reduce((sum, it) => sum + (Number(it.qty) || 0), 0))

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
  // 补发 / 重发：已出库 / 已发货的非补发订单
  if ([EcomLocalStatus.OUTBOUND, EcomLocalStatus.SHIPPED, EcomLocalStatus.PARTIAL].includes(d.localStatus) && d.orderType !== EcomOrderType.RESEND
    && can('ecom:order:resend')) {
    list.push({ key: 'resend', label: t('ecomOrd.resend.action'), icon: RedoOutlined, run: () => resendModal.value.open(d) })
  }
  if (can('ecom:blacklist:add')) {
    list.push({ key: 'blacklist', label: t('ecomOrd.blacklist.action'), icon: UserDeleteOutlined, danger: true, run: () => blacklistModal.value.open(d) })
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

/** 标题栏直接显示主要操作（审核、出库、发货等）和修改，其余收进「更多」 */
const HEAD_KEYS = ['audit', 'recheck', 'outbound', 'ship', 'unhold', 'rematch', 'edit']
const headActions = computed(() =>
  actions.value.filter((a) => HEAD_KEYS.includes(a.key)).slice(0, 3).sort((a, b) => Number(!!a.primary) - Number(!!b.primary))
)
const moreActions = computed(() => actions.value.filter((a) => !headActions.value.includes(a)))

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.head-title {
  margin-right: 8px;
  font-weight: 600;
}
.issue {
  margin-bottom: 4px;
}
.issue-actions {
  margin-top: 6px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  margin: 4px 0 2px;
  overflow: hidden;
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.stat-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-sub :deep(.ant-tag) {
  font-size: 11px;
  line-height: 16px;
}
@media (max-width: 991px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.panel {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.panel-title {
  margin-bottom: 12px;
  font-weight: 600;
}
.side-desc :deep(.ant-descriptions-item-label) {
  min-width: 84px;
  color: var(--app-text-3);
  white-space: nowrap;
}
.address {
  line-height: 20px;
}
.country {
  padding: 0 4px;
  border: 1px solid var(--app-border);
  border-radius: 3px;
  font-size: 12px;
}
.break {
  word-break: break-all;
}
.negative {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
