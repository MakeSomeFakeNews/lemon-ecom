<template>
  <SubPage :open="open" :title="detail ? $t('ecomPurchase.plan.detailTitle', { no: detail.docNo }) : $t('ecomPurchase.plan.title')" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_purchase_plan_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert type="info" show-icon class="block" :message="$t('ecomPurchase.plan.flowTip')" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomPurchase.plan.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.planDate')">{{ detail.planDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.warehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.itemCount')">{{ detail.itemCount }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.totalQty')">{{ qty(detail.totalQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.orderedQty')">{{ qty(detail.orderedQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.arrivedQty')">{{ qty(detail.arrivedQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.progress')"><a-progress :percent="progressOf(detail)" size="small" class="progress" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPurchase.plan.createBy')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.approveByName" :label="$t('ecomPurchase.plan.approveBy')">{{ detail.approveByName }} {{ detail.approveTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomPurchase.plan.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 1300 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div v-if="record.materialSpec" class="sub-text">{{ record.materialSpec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }} {{ record.unit || '' }}</template>
            <template v-else-if="column.key === 'suggestQty'">{{ record.suggestQty != null ? qty(record.suggestQty) : '-' }}</template>
            <template v-else-if="column.key === 'price'">{{ record.price != null ? price(record.price) : '-' }}</template>
            <template v-else-if="column.key === 'taxRate'">{{ record.taxRate != null ? qty(record.taxRate, 2) : '-' }}</template>
            <template v-else-if="column.key === 'amount'">{{ amountOf(record) != null ? money(amountOf(record)) : '-' }}</template>
            <template v-else-if="column.key === 'demand'">
              <a-popover v-if="demandList(record.demand).length" :title="$t('ecomPurchase.plan.demand')">
                <template #content>
                  <div v-for="(d, i) in demandList(record.demand)" :key="i">{{ demandText(d) }}</div>
                </template>
                <a>{{ demandList(record.demand).length }}</a>
              </a-popover>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'order'">
              <template v-if="record.orderId && record.orderStatus">
                <a v-if="can('scm:purchase:list')" @click="goOrder(record.orderId)">{{ record.orderNo }}</a>
                <span v-else>{{ record.orderNo }}</span>
                <div><DictTag type="scm_order_status" :value="record.orderStatus" /></div>
              </template>
              <span v-else-if="record.orderId" class="sub-text">{{ record.orderNo }} {{ $t('ecomPurchase.plan.orderDeleted') }}</span>
              <span v-else class="sub-text">{{ $t('ecomPurchase.plan.notOrdered') }}</span>
            </template>
            <template v-else-if="column.key === 'arrivedQty'">
              <b v-if="Number(record.arrivedQty) > 0">{{ qty(record.arrivedQty) }}</b>
              <span v-else class="sub-text">0</span>
            </template>
          </template>
        </a-table>
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
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CheckCircleOutlined, DeleteOutlined, EditOutlined, ShoppingCartOutlined, StopOutlined, UndoOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import {
  approvePurchasePlan,
  closePurchasePlan,
  deletePurchasePlan,
  generatePurchaseOrders,
  getPurchasePlan,
  unapprovePurchasePlan
} from '@/api/ecom/purchasePlan'
import { useUserStore } from '@/store/user'
import { money, price, qty } from '@/utils/number'
import { demandList, demandText } from '../replenish/replenish'
import { amountOf, canClose, canGenerate, canUnapprove, isDraft, progressOf } from './plan'

/**
 * 采购计划详情子页：审核 / 反审核、生成采购订单、关闭、删除在本组件内完成（触发 changed）；编辑交给页面打开表单（edit 事件带单据）。
 * 明细显示生成的采购订单（可跳转）和已到货数量。ref 调用 open(id)、reload()
 */
const emit = defineEmits(['changed', 'edit'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const itemColumns = [
  { title: t('ecomPurchase.plan.material'), key: 'material', width: 240 },
  { title: t('ecomPurchase.plan.suggestQty'), key: 'suggestQty', width: 90, align: 'right' },
  { title: t('ecomPurchase.plan.qty'), key: 'qty', width: 120, align: 'right' },
  { title: t('ecomPurchase.plan.supplier'), dataIndex: 'supplierName', width: 160, ellipsis: true },
  { title: t('ecomPurchase.plan.price'), key: 'price', width: 110, align: 'right' },
  { title: t('ecomPurchase.plan.taxRate'), key: 'taxRate', width: 80, align: 'right' },
  { title: t('ecomPurchase.plan.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('ecomPurchase.plan.arrivalDate'), dataIndex: 'arrivalDate', width: 110 },
  { title: t('ecomPurchase.plan.demand'), key: 'demand', width: 90, align: 'center' },
  { title: t('ecomPurchase.plan.order'), key: 'order', width: 170 },
  { title: t('ecomPurchase.plan.arrivedQty'), key: 'arrivedQty', width: 100, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
let currentId

async function load(id) {
  loading.value = true
  try {
    detail.value = await getPurchasePlan(id)
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
const reload = () => (open.value && currentId ? load(currentId) : undefined)

function goOrder(id) {
  router.push({ path: '/scm/purchase', query: { id } })
}

function confirmAction(title, content, api, success, { danger = false, closeAfter = false } = {}) {
  Modal.confirm({
    title,
    content,
    okType: danger ? 'danger' : 'primary',
    async onOk() {
      const res = await api(currentId)
      message.success(typeof success === 'function' ? success(res) : success)
      if (closeAfter) open.value = false
      else await load(currentId)
      emit('changed')
    }
  })
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (isDraft(d) && can('ecom:purchasePlan:remove')) {
    list.push({
      key: 'delete',
      label: t('common.action.delete'),
      icon: DeleteOutlined,
      danger: true,
      run: () => confirmAction(t('ecomPurchase.plan.deleteConfirm', { no: d.docNo }), '', deletePurchasePlan, t('common.msg.deleteSuccess'), { danger: true, closeAfter: true })
    })
  }
  if (canClose(d) && can('ecom:purchasePlan:edit')) {
    list.push({
      key: 'close',
      label: t('ecomPurchase.plan.close'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(t('ecomPurchase.plan.closeConfirm', { no: d.docNo }), '', closePurchasePlan, t('ecomPurchase.plan.closed'), { danger: true })
    })
  }
  if (canUnapprove(d) && can('ecom:purchasePlan:approve')) {
    list.push({
      key: 'unapprove',
      label: t('ecomPurchase.plan.unapprove'),
      icon: UndoOutlined,
      run: () => confirmAction(t('ecomPurchase.plan.unapproveConfirm', { no: d.docNo }), '', unapprovePurchasePlan, t('ecomPurchase.plan.unapproved'))
    })
  }
  if (isDraft(d) && can('ecom:purchasePlan:edit')) {
    list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (isDraft(d) && can('ecom:purchasePlan:approve')) {
    list.push({
      key: 'approve',
      label: t('ecomPurchase.plan.approve'),
      icon: CheckCircleOutlined,
      primary: true,
      run: () => confirmAction(t('ecomPurchase.plan.approveConfirm', { no: d.docNo }), '', approvePurchasePlan, t('ecomPurchase.plan.approved'))
    })
  }
  if (canGenerate(d) && can('ecom:purchasePlan:order')) {
    list.push({
      key: 'generate',
      label: t('ecomPurchase.plan.generate'),
      icon: ShoppingCartOutlined,
      primary: true,
      run: () =>
        confirmAction(t('ecomPurchase.plan.generateConfirm'), t('ecomPurchase.plan.generateContent'), generatePurchaseOrders, (nos) =>
          t('ecomPurchase.plan.generated', { nos: (nos || []).join(', ') })
        )
    })
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
.progress {
  width: 160px;
  margin: 0;
}
</style>
