<template>
  <SubPage :open="open" :title="detail ? $t('ecomCatalog.plan.detailTitle', { no: detail.planNo }) : $t('ecomCatalog.plan.title')" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_shipment_plan_status" :value="detail.status" />
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
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
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 900 }" class="block">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sku'">
              <div>{{ record.sellerSku }}</div>
              <div class="sub-text">{{ record.title }}</div>
            </template>
            <template v-else-if="column.key === 'composition'">{{ compositionText(record, $t('ecomCatalog.listing.bundle')) }}</template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'shippedQty'">{{ qty(record.shippedQty) }}</template>
            <template v-else-if="column.key === 'remaining'">{{ qty(remainingOf(record)) }}</template>
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

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
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
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
