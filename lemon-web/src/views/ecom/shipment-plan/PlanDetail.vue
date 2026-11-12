<template>
  <SubPage :open="open" :title="detail ? $t('ecomCatalog.plan.detailTitle', { no: detail.planNo }) : $t('ecomCatalog.plan.title')" @close="open = false">
    <template v-if="detail" #title>
      <span class="title-no">{{ $t('ecomCatalog.plan.detailTitle', { no: detail.planNo }) }}</span>
      <DictTag type="ecom_shipment_plan_status" :value="detail.status" class="title-tag" />
    </template>
    <template v-if="detail && actions.length" #extra>
      <a-space wrap>
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <a-skeleton v-if="!detail" active :paragraph="{ rows: 8 }" />
      <template v-else>
        <div class="progress-card block">
          <a-steps :current="flow.current" :status="flow.status" :items="flow.items" />
          <div class="ship-progress">
            <span class="sub-text">{{ $t('ecomCatalog.plan.shipProgress') }}</span>
            <a-progress :percent="shipPercent" size="small" :status="shipPercent >= 100 ? 'success' : 'normal'" class="bar" />
            <span class="nums">{{ qty(detail.shippedQty) }} / {{ qty(detail.totalQty) }}</span>
          </div>
        </div>
        <div class="section-title">{{ $t('ecomCatalog.plan.basicInfo') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomCatalog.plan.planNo')">{{ detail.planNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecom.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.fromWarehouse')">{{ detail.fromWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.toWarehouse')">{{ detail.toWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.planDate')">{{ detail.planDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.totalQty')">{{ qty(detail.totalQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.shippedQty')">{{ qty(detail.shippedQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.common.createBy')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomCatalog.plan.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 900 }" class="block-lg">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sku'">
              <div>{{ record.sellerSku }}</div>
              <div class="sub-text">{{ record.title }}</div>
            </template>
            <template v-else-if="column.key === 'composition'">{{ compositionText(record, $t('ecomCatalog.listing.bundle')) }}</template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'shippedQty'">{{ qty(record.shippedQty) }}</template>
            <template v-else-if="column.key === 'remaining'">
              <span :class="{ warn: remainingOf(record) > 0 && !isDraft(detail) }">{{ qty(remainingOf(record)) }}</span>
            </template>
          </template>
        </a-table>

        <div class="section-title">{{ $t('ecomCatalog.plan.inbounds') }}</div>
        <a-table row-key="id" size="small" :columns="inboundColumns" :data-source="detail.inbounds || []" :pagination="false" :scroll="{ x: 700 }"
                 :locale="{ emptyText: $t('ecomCatalog.plan.noInbound') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'docNo'">
              <router-link v-if="userStore.hasPerm('ecom:inbound:list')" :to="{ path: '/ecom/inbound', query: { keyword: record.docNo } }">{{ record.docNo }}</router-link>
              <span v-else>{{ record.docNo }}</span>
            </template>
            <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
            <template v-else-if="column.key === 'status'"><DictTag type="ecom_inbound_status" :value="record.status" /></template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <GenerateModal ref="generateRef" @success="onGenerated" />
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, DeleteOutlined, EditOutlined, RollbackOutlined, SendOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import GenerateModal from './GenerateModal.vue'
import { cancelShipmentPlan, confirmShipmentPlan, deleteShipmentPlan, getShipmentPlan, unconfirmShipmentPlan } from '@/api/ecom/shipmentPlan'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'
import { ShipmentPlanStatus } from '@/constants/enums'
import { canCancel, canDelete, compositionText, isConfirmed, isDraft, noShipped, remainingOf } from './plan'

/** 发货计划详情子页：确认、退回草稿、取消、删除、生成货件在本组件内完成（触发 changed），编辑交给页面（edit 事件）。ref 调用 open(id)、reload() */
const emit = defineEmits(['changed', 'edit'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
useDict('ecom_shipment_plan_status', 'ecom_inbound_status')

const itemColumns = [
  { title: t('ecom.common.sellerSku'), key: 'sku', width: 260 },
  { title: 'FNSKU', dataIndex: 'fnsku', width: 130 },
  { title: t('ecomCatalog.plan.composition'), key: 'composition', width: 180 },
  { title: t('ecomCatalog.plan.qty'), key: 'qty', width: 110, align: 'right' },
  { title: t('ecomCatalog.plan.shippedQty'), key: 'shippedQty', width: 110, align: 'right' },
  { title: t('ecomCatalog.plan.remaining'), key: 'remaining', width: 110, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
const inboundColumns = [
  { title: t('ecomCatalog.plan.inboundNo'), key: 'docNo', width: 160 },
  { title: t('ecomCatalog.plan.shipmentId'), dataIndex: 'shipmentId', width: 160 },
  { title: t('ecomCatalog.plan.inboundQty'), key: 'totalQty', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('ecomCatalog.common.createTime'), dataIndex: 'createTime', width: 160 }
]

const open = ref(false)
const shipPercent = computed(() => {
  const total = Number(detail.value?.totalQty) || 0
  return total ? Math.min(100, Math.round(((Number(detail.value.shippedQty) || 0) / total) * 100)) : 0
})
/** 流程：草稿 → 已确认 → 已生成货件；取消时在当前步骤显示错误 */
const flow = computed(() => {
  const d = detail.value
  const s = d.status
  const items = [
    { title: t('ecomCatalog.plan.flow.draft'), description: d.createTime ? String(d.createTime).slice(0, 10) : '' },
    { title: t('ecomCatalog.plan.flow.confirmed'), description: d.planDate ? t('ecomCatalog.plan.flow.planDate', { date: d.planDate }) : '' },
    { title: t('ecomCatalog.plan.flow.generated'), description: (d.inbounds || []).length ? t('ecomCatalog.plan.flow.inboundCount', { n: d.inbounds.length }) : '' }
  ]
  if (s === ShipmentPlanStatus.CANCELLED) return { items, current: noShipped(d) ? 1 : 2, status: 'error' }
  const current = { [ShipmentPlanStatus.DRAFT]: 0, [ShipmentPlanStatus.CONFIRMED]: 1, [ShipmentPlanStatus.GENERATED]: 2 }[s] ?? 0
  return { items, current, status: s === ShipmentPlanStatus.GENERATED ? 'finish' : 'process' }
})
const loading = ref(false)
const detail = ref()
const generateRef = ref()
let currentId

async function load(id) {
  loading.value = true
  try {
    detail.value = await getShipmentPlan(id)
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
const after = async () => {
  await load(currentId)
  emit('changed')
}

const confirm = useAction(confirmShipmentPlan, { confirm: () => t('ecomCatalog.plan.confirmConfirm', { no: detail.value.planNo }), reload: after })
const unconfirm = useAction(unconfirmShipmentPlan, { confirm: () => t('ecomCatalog.plan.unconfirmConfirm', { no: detail.value.planNo }), reload: after })
const cancel = useAction(cancelShipmentPlan, { confirm: () => ({ title: t('ecomCatalog.plan.cancelConfirm', { no: detail.value.planNo }), okType: 'danger' }), reload: after })
const remove = useAction(deleteShipmentPlan, {
  confirm: () => ({ title: t('ecomCatalog.plan.deleteConfirm', { no: detail.value.planNo }), okType: 'danger' }),
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    open.value = false
    emit('changed')
  }
})
function onGenerated() {
  after()
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (canDelete(d) && can('ecom:shipmentPlan:remove')) list.push({ key: 'delete', label: t('common.action.delete'), icon: DeleteOutlined, danger: true, run: () => remove(d.id) })
  if (canCancel(d) && can('ecom:shipmentPlan:confirm')) list.push({ key: 'cancel', label: t('ecomCatalog.plan.cancel'), icon: StopOutlined, danger: true, run: () => cancel(d.id) })
  if (isDraft(d) && can('ecom:shipmentPlan:edit')) list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  if (isConfirmed(d) && noShipped(d) && can('ecom:shipmentPlan:confirm')) list.push({ key: 'unconfirm', label: t('ecomCatalog.plan.unconfirm'), icon: RollbackOutlined, run: () => unconfirm(d.id) })
  if (isDraft(d) && can('ecom:shipmentPlan:confirm')) list.push({ key: 'confirm', label: t('ecomCatalog.plan.confirm'), icon: CheckOutlined, primary: true, run: () => confirm(d.id) })
  if (isConfirmed(d) && can('ecom:shipmentPlan:generate')) list.push({ key: 'generate', label: t('ecomCatalog.plan.generate'), icon: SendOutlined, primary: true, run: () => generateRef.value.open(d) })
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.block-lg {
  margin-bottom: 20px;
}
.title-no {
  font-weight: 600;
}
.title-tag {
  margin-left: 8px;
  vertical-align: 1px;
}
.progress-card {
  padding: 16px 24px 12px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
}
.ship-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.ship-progress .bar {
  flex: 1;
  margin: 0;
}
.nums {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.section-title {
  margin: 4px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  font-weight: 600;
  line-height: 1.2;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
</style>
