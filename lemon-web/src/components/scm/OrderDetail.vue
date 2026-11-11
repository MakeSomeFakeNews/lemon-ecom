<template>
  <SubPage
    :open="open"
    :title="detail ? `${conf.label} ${detail.orderNo}${detail.version > 1 ? ` V${detail.version}` : ''}` : conf.label"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <DictTag v-if="detail" type="scm_order_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.source === OrderSource.DIRECT" type="info" show-icon class="direct-alert" :message="$t('scm.order.directHint')" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('scm.order.orderNo')">
            {{ detail.orderNo }}
            <a-tag v-if="detail.version > 1" color="blue" :bordered="false" class="ver-tag">V{{ detail.version }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerCode }} {{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.order.orderDate')">{{ detail.orderDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.order.deliveryDate')">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="conf.warehouseLabel">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.order.source')"><DictTag type="scm_order_source" :value="detail.source" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.order.totalAmount')">
            <b>{{ symbol }}{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">{{ $t('scm.order.taxAmountParen', { amount: formatMoney(detail.taxAmount) }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="foreign" :label="$t('scm.order.currencyRate')">
            {{ detail.currency }} @{{ Number(detail.exchangeRate) }}
            <span class="sub-text">（≈ ¥{{ formatMoney(Number(detail.totalAmount) * Number(detail.exchangeRate || 0)) }}）</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.wholeDiscount)" :label="$t('scm.order.discount')">
            <span>{{ $t('scm.wholeDiscount.value', { amount: symbol + formatMoney(detail.wholeDiscount) }) }}</span>
            <div class="sub-text">{{ $t('scm.wholeDiscount.grossOnly', { amount: symbol + formatMoney(grossTotal) }) }}</div>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.closeAdjust)" :label="$t('scm.order.closeAdjust')">
            <div>
              <span class="nowrap">{{ symbol }}{{ formatMoney(detail.closeAdjust) }}</span>
              <div class="sub-text">{{ $t('scm.order.closeAdjustHint') }}</div>
            </div>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.invoiceKind" :label="$t('scm.taxRate.invoiceKind')">
            <DictTag type="scm_invoice_kind" :value="detail.invoiceKind" />
          </a-descriptions-item>
          <a-descriptions-item v-if="kind === 'sales'" :label="$t('scm.order.salesman')">
            <template v-if="!salesmanEditing">
              {{ detail.salesmanName || '-' }}
              <a v-if="userStore.hasPerm('scm:sales:edit')" class="ml" @click="salesmanEditing = true">{{ $t('common.action.modify') }}</a>
            </template>
            <a-space v-else>
              <IdentityUserSelect v-model:value="salesmanId" :label="detail.salesmanName" class="salesman-select" />
              <a @click="saveSalesman">{{ $t('common.action.save') }}</a>
              <a @click="salesmanEditing = false">{{ $t('common.action.cancel') }}</a>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.order.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.approveTime" :label="$t('scm.order.approved')">
            {{ detail.approveByName || '-' }} {{ detail.approveTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <CustomFieldsView :key="detail.id" :biz-type="kind === 'sales' ? 'scm_sales' : 'scm_purchase'" :biz-id="detail.id" class="cf-view" />

        <a-alert
          v-if="detail.status === OrderStatus.APPROVING"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('scm.order.approvingTip')"
        />

        <a-tabs v-model:active-key="tab" size="small">
          <a-tab-pane key="items" :tab="$t('scm.order.tabItems')">
            <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 950 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">
                  <div>{{ record.materialCode }} {{ record.materialName }}</div>
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'qty'">{{ qtyText(record) }}</template>
                <template v-else-if="column.key === 'price'">
                  <!-- 整单优惠只在合计处扣减：明细显示优惠前的单价 -->
                  <div>{{ symbol }}{{ formatMoney(grossPrice(record)) }}</div>
                  <div v-if="hasUnit(record)" class="sub-text">{{ $t('scm.order.perUnit', { unit: record.unitName, price: symbol + formatMoney(grossPrice(record) * record.unitRatio) }) }}</div>
                  <div class="sub-text">{{ $t('scm.order.taxRateValue', { rate: Number(record.taxRate) }) }}</div>
                </template>
                <template v-else-if="column.key === 'amount'">{{ symbol }}{{ formatMoney(grossAmount(record)) }}</template>
                <template v-else-if="column.key === 'progress'">
                  <a-progress :percent="percent(record)" size="small" :status="percent(record) >= 100 ? 'success' : 'normal'" />
                  <div class="sub-text">
                    {{ $t(kind === 'purchase' ? 'scm.order.receivedQty' : 'scm.order.shippedQty', { qty: formatQty(record.doneQty) }) }}
                    <template v-if="Number(record.returnQty)">{{ $t('scm.order.returnedQty', { qty: formatQty(record.returnQty) }) }}</template>
                  </div>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-for="t in conf.trades" :key="t" :tab="$t('scm.order.tradeRecords', { kind: TRADE_KINDS[t].label })">
            <a-table row-key="id" size="small" :columns="tradeColumns" :data-source="trades[t]" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'totalAmount'">¥{{ formatMoney(record.totalAmount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('scm.order.tabAttachment')">
            <AttachmentPanel :biz-type="`scm_${kind}`" :biz-id="orderId" :editable="can(`${kind}:edit`)" />
          </a-tab-pane>
          <a-tab-pane key="changes" :tab="$t('scm.order.tabChanges')">
            <OrderChangeList v-if="tab === 'changes'" :key="changeSeq" :kind="kind" :order-id="detail.id" :currency="detail.currency" />
          </a-tab-pane>
          <a-tab-pane key="history" :tab="$t('scm.order.tabHistory')">
            <ChangeLogPanel :biz-type="`scm_${kind}`" :biz-id="orderId" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
    <OrderChangeDrawer v-model:open="changeOpen" :kind="kind" :order="detail" @success="onChanged" />
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import {
  AuditOutlined,
  DiffOutlined,
  EditOutlined,
  ExportOutlined,
  ImportOutlined,
  PrinterOutlined,
  RollbackOutlined,
  StopOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import OrderChangeDrawer from './OrderChangeDrawer.vue'
import OrderChangeList from './OrderChangeList.vue'
import { currencySymbol, isForeign } from '@/components/fin/currency'
import { approveOrder, changeSalesman, closeOrder, getOrder, unapproveOrder, withdrawOrder } from '@/api/scm/order'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import CustomFieldsView from '@/components/common/CustomFieldsView.vue'
import { pageTrade } from '@/api/scm/trade'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, grossAmount, grossPrice, netQty } from './scm'
import { materialColumns, printDocs } from '@/utils/print'

import { OrderSource, OrderStatus } from '@/constants/enums'
/**
 * 订单详情：明细执行进度、入库 / 出库与退货记录，按状态提供审核、撤回、反审核、关闭、入库 / 出库、退货等操作。
 * 触发 edit(order)、trade(tradeKind, order)、changed
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  orderId: Number
})
const emit = defineEmits(['update:open', 'edit', 'trade', 'changed'])

const { t } = useI18n()
const userStore = useUserStore()
const conf = computed(() => ORDER_KINDS[props.kind])
const can = (perm) => userStore.hasPerm(`scm:${perm}`)

const itemColumns = [
  { title: t('scm.order.material'), key: 'material', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 150, align: 'right' },
  { title: t('scm.order.taxPrice'), key: 'price', width: 170, align: 'right' },
  { title: t('scm.order.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.order.deliveryDate'), dataIndex: 'deliveryDate', width: 120 },
  { title: t('scm.order.progress'), key: 'progress', width: 200 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 120, ellipsis: true }
]
const tradeColumns = [
  { title: t('scm.order.docNo'), dataIndex: 'docNo', width: 170 },
  { title: t('scm.order.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('scm.order.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 100, align: 'right' },
  { title: t('scm.order.totalAmount'), key: 'totalAmount', width: 130, align: 'right' },
  { title: t('scm.order.wmsDoc'), dataIndex: 'wmsDocNo', width: 170 },
  { title: t('scm.order.preparedBy'), dataIndex: 'createByName', width: 110 }
]
const detail = ref()
/** 优惠前合计（明细金额合计）= 价税合计 + 整单优惠 */
const grossTotal = computed(() => Math.round((Number(detail.value?.totalAmount || 0) + Number(detail.value?.wholeDiscount || 0)) * 100) / 100)

const invoiceKinds = useDict('scm_invoice_kind')
function print() {
  const d = detail.value
  const c = conf.value
  printDocs({
    title: c.label,
    no: d.orderNo,
    fields: [
      [c.partnerLabel, `${d.partnerCode || ''} ${d.partnerName || ''}`],
      [t('scm.order.orderDate'), d.orderDate],
      [t('scm.order.deliveryDate'), d.deliveryDate],
      [c.warehouseLabel, d.warehouseName],
      ...(d.invoiceKind ? [[t('print.name.invoiceKind'), findDict(invoiceKinds.scm_invoice_kind, d.invoiceKind)?.label || d.invoiceKind]] : []),
      ...(Number(d.wholeDiscount) ? [[t('print.name.grossTotal'), `${currencySymbol(d.currency)}${formatMoney(grossTotal.value)}`],
        [t('scm.wholeDiscount.label'), `${currencySymbol(d.currency)}${formatMoney(d.wholeDiscount)}`]] : []),
      [Number(d.wholeDiscount) ? t('print.name.discountedTotal') : t('scm.order.totalAmount'), `${currencySymbol(d.currency)}${formatMoney(d.totalAmount)}`],
      [t('scm.order.taxAmount'), `${currencySymbol(d.currency)}${formatMoney(d.taxAmount)}`],
      ...(isForeign(d.currency) ? [[t('scm.order.currencyRate'), `${d.currency} @${Number(d.exchangeRate)}`]] : []),
      ...(d.version > 1 ? [[t('scm.order.version'), `V${d.version}`]] : [])
    ],
    columns: [
      ...materialColumns(),
      { title: t('common.field.quantity'), value: qtyText, align: 'right' },
      { title: t('scm.order.taxPrice'), value: (r) => formatMoney(grossPrice(r)), align: 'right' },
      { title: t('scm.order.taxRate'), value: (r) => `${Number(r.taxRate)}%`, align: 'right' },
      { title: t('scm.order.totalAmount'), key: 'amount', value: (r) => formatMoney(grossAmount(r)), align: 'right' },
      { title: t('scm.order.deliveryDate'), key: 'deliveryDate' }
    ],
    rows: d.items,
    totals: { amount: formatMoney(grossTotal.value) },
    note: d.remark,
    signs: [[t('scm.order.prepared'), d.createByName], [t('scm.order.approved'), d.approveByName], t('scm.order.partnerSign', { partner: c.partnerLabel })]
  }, `${c.label} ${d.orderNo}`, props.kind === 'purchase' ? 'scm.purchaseOrder' : 'scm.salesOrder')
}
const loading = ref(false)
const tab = ref('items')

const foreign = computed(() => isForeign(detail.value?.currency))
const symbol = computed(() => currencySymbol(detail.value?.currency))

// 订单变更
const changeOpen = ref(false)
const changeSeq = ref(0)
async function onChanged() {
  changeSeq.value++
  tab.value = 'changes'
  await load()
  emit('changed')
}
const trades = reactive({})

/** 按辅助单位下单的明细：「2 箱（24 个）」；数量、单价都按基本单位存储 */
const hasUnit = (i) => !!i.unitName && Number(i.unitRatio) > 0
const qtyText = (i) =>
  hasUnit(i)
    ? t('scm.order.unitQty', { qty: formatQty(Number(i.qty) / Number(i.unitRatio)), unit: i.unitName, base: `${formatQty(i.qty)} ${i.unit || ''}` })
    : `${formatQty(i.qty)} ${i.unit || ''}`
const percent = (i) => Math.min(Math.round((netQty(i) / Number(i.qty)) * 100), 100)

// 业务员（销售提成按此统计）
const salesmanEditing = ref(false)
const salesmanId = ref()
watch(salesmanEditing, (v) => v && (salesmanId.value = detail.value?.salesmanId))
async function saveSalesman() {
  await changeSalesman(detail.value.id, salesmanId.value || null)
  message.success(t('scm.order.salesmanChanged'))
  salesmanEditing.value = false
  load()
}

async function load() {
  loading.value = true
  try {
    detail.value = await getOrder(props.kind, props.orderId)
    await Promise.all(
      conf.value.trades.map(async (t) => {
        trades[t] = can(`${t}:list`) ? (await pageTrade(t, { orderId: props.orderId, current: 1, size: 100 })).records : []
      })
    )
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.orderId,
  (v) => {
    detail.value = undefined
    tab.value = 'items'
    if (v) load()
  }
)

function confirmAction(title, content, api, success) {
  Modal.confirm({
    title,
    content,
    async onOk() {
      const res = await api(props.kind, props.orderId)
      message.success(typeof success === 'function' ? success(res) : success)
      await load()
      emit('changed')
    }
  })
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const k = props.kind
  const [doTrade, returnTrade] = conf.value.trades
  const list = []
  // 直接开单生成的订单只能退货（及查看），不能反审核、关闭、变更
  const direct = d.source === OrderSource.DIRECT
  if (d.status === OrderStatus.DRAFT) {
    if (can(`${k}:edit`)) list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
    if (can(`${k}:approve`))
      list.push({
        key: 'approve',
        label: t('common.action.audit'),
        icon: AuditOutlined,
        primary: true,
        run: () =>
          confirmAction(t('scm.order.approveTitle'), t('scm.order.approveContent'), approveOrder, (flow) =>
            flow ? t('scm.order.submittedForApproval') : t('scm.order.approveSuccess')
          )
      })
  }
  if (d.status === OrderStatus.APPROVING && can(`${k}:approve`)) {
    list.push({
      key: 'withdraw',
      label: t('scm.order.withdraw'),
      icon: UndoOutlined,
      run: () => confirmAction(t('scm.order.withdrawTitle'), t('scm.order.withdrawContent'), withdrawOrder, t('scm.order.withdrawn'))
    })
  }
  if (!direct && d.status === OrderStatus.APPROVED && can(`${k}:approve`)) {
    list.push({
      key: 'unapprove',
      label: t('common.action.unaudit'),
      icon: UndoOutlined,
      run: () => confirmAction(t('scm.order.unapproveTitle'), t('scm.order.unapproveContent'), unapproveOrder, t('scm.order.unapproved'))
    })
  }
  if (!direct && [OrderStatus.APPROVED, OrderStatus.PARTIAL].includes(d.status) && can(`${k}:close`)) {
    list.push({
      key: 'close',
      label: t('common.action.close'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(
          t('scm.order.closeTitle'),
          t(k === 'purchase' ? 'scm.order.closeContentIn' : 'scm.order.closeContentOut'),
          closeOrder,
          t('scm.order.closed')
        )
    })
  }
  if (!direct && [OrderStatus.APPROVED, OrderStatus.PARTIAL].includes(d.status) && can(`${k}:change`)) {
    list.push({ key: 'change', label: t('scm.order.change'), icon: DiffOutlined, run: () => (changeOpen.value = true) })
  }
  if ([OrderStatus.PARTIAL, OrderStatus.COMPLETED, OrderStatus.CLOSED].includes(d.status) && can(`${returnTrade}:add`)) {
    list.push({ key: 'return', label: TRADE_KINDS[returnTrade].label, icon: RollbackOutlined, run: () => emit('trade', returnTrade, d) })
  }
  if ([OrderStatus.APPROVED, OrderStatus.PARTIAL].includes(d.status) && can(`${doTrade}:add`)) {
    list.push({
      key: 'trade',
      label: TRADE_KINDS[doTrade].label,
      icon: k === 'purchase' ? ImportOutlined : ExportOutlined,
      primary: true,
      run: () => emit('trade', doTrade, d)
    })
  }
  return list
})

defineExpose({ reload: load })
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}
.ml {
  margin-left: 6px;
}
.salesman-select {
  width: 200px;
}
.direct-alert {
  margin-bottom: 12px;
}
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ver-tag {
  margin-left: 4px;
}
</style>
