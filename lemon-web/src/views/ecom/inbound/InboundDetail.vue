<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.inbound.detailTitle', { no: detail.docNo }) : $t('ecomOrder.inbound.title')" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_inbound_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.shipmentId')">{{ detail.shipmentId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.transferDoc')">{{ detail.transferDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.planNo" :label="$t('ecomCatalog.plan.planNo')">{{ detail.planNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.fromWarehouse')">{{ detail.fromWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.toWarehouse')">{{ detail.toWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.carrier')">{{ detail.carrier || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.shipDate')">{{ detail.shipDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.etaDate')">{{ detail.etaDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.receiveDate')">{{ detail.receiveDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.boxCount')">{{ detail.boxCount ?? '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.weight')">{{ detail.weight != null ? qty(detail.weight, 3) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.volume')">{{ detail.volume != null ? qty(detail.volume) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.totalQty')">{{ qty(detail.totalQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.freight')">{{ baseMoney(detail.freight) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.duty')">{{ baseMoney(detail.duty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.otherCost')">{{ baseMoney(detail.otherCost) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomProfit.inbound.importVat')">{{ baseMoney(detail.importVat) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.totalCost')"><b>{{ baseMoney(totalCost(detail)) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.unitLanded')">
            <a-tooltip :title="$t('ecomOrder.inbound.unitLandedHint')">
              <b class="dashed">¥{{ price(unitLanded) }}</b>
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.createBy')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.inbound.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.materialSpec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }} {{ record.unit || '' }}</template>
            <template v-else-if="column.key === 'receivedQty'">{{ record.receivedQty != null ? qty(record.receivedQty) : '-' }}</template>
            <template v-else-if="column.key === 'unitLanded'">{{ record.unitLanded != null ? `¥${price(record.unitLanded)}` : '-' }}</template>
          </template>
        </a-table>

        <InboundBoxes :detail="detail" @changed="load(currentId)" />
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
import { DeleteOutlined, DollarOutlined, EditOutlined, ImportOutlined, SendOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { cancelEcomInbound, deleteEcomInbound, getEcomInbound, submitEcomInbound } from '@/api/ecom/inbound'
import { useUserStore } from '@/store/user'
import { price, qty } from '@/utils/number'
import { baseMoney } from '../order/order'
import { canCancel, canDelete, canEditCosts, canReceive, isDraft, totalCost, unitLandedOf } from './inbound'
import InboundBoxes from './InboundBoxes.vue'

/**
 * 头程货件详情子页：提交发运、取消、删除在本组件内完成（触发 changed）；编辑、补录费用、签收交给页面打开表单 / 日期弹窗（事件带单据）。
 * ref 调用 open(id)、reload()
 */
const emit = defineEmits(['changed', 'edit', 'costs', 'receive'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const itemColumns = [
  { title: t('ecomOrder.common.material'), key: 'material', width: 260 },
  { title: t('ecomOrder.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 120, align: 'right' },
  { title: t('ecomOrder.inbound.receivedQty'), key: 'receivedQty', width: 110, align: 'right' },
  { title: t('ecomOrder.inbound.unitLanded'), key: 'unitLanded', width: 140, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
let currentId

/** 单件头程成本：已分摊时取明细的 unitLanded，草稿按当前费用与数量预估 */
const unitLanded = computed(() => {
  const d = detail.value
  if (!d) return 0
  const saved = (d.items || []).find((i) => i.unitLanded != null)
  return saved ? Number(saved.unitLanded) : unitLandedOf(d, d.totalQty)
})

async function load(id) {
  loading.value = true
  try {
    detail.value = await getEcomInbound(id)
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
  if (canDelete(d) && can('ecom:inbound:remove')) {
    list.push({
      key: 'delete',
      label: t('common.action.delete'),
      icon: DeleteOutlined,
      danger: true,
      run: () => confirmAction(t('ecomOrder.inbound.deleteConfirm', { no: d.docNo }), '', deleteEcomInbound, t('common.msg.deleteSuccess'), { danger: true, closeAfter: true })
    })
  }
  if (canCancel(d) && can('ecom:inbound:edit')) {
    list.push({
      key: 'cancel',
      label: t('ecomOrder.inbound.cancel'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(t('ecomOrder.inbound.cancelConfirm', { no: d.docNo }), '', cancelEcomInbound, t('ecomOrder.inbound.cancelled'), { danger: true })
    })
  }
  if (isDraft(d) && can('ecom:inbound:edit')) {
    list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (canEditCosts(d) && can('ecom:inbound:edit')) {
    list.push({ key: 'costs', label: t('ecomOrder.inbound.costs'), icon: DollarOutlined, run: () => emit('costs', d) })
  }
  if (isDraft(d) && can('ecom:inbound:ship')) {
    list.push({
      key: 'submit',
      label: t('ecomOrder.inbound.submit'),
      icon: SendOutlined,
      primary: true,
      run: () =>
        confirmAction(t('ecomOrder.inbound.submitConfirm', { no: d.docNo }), t('ecomOrder.inbound.submitContent'), submitEcomInbound, (flow) =>
          flow ? t('ecomOrder.inbound.submitted') : t('ecomOrder.inbound.shipped')
        )
    })
  }
  if (canReceive(d) && can('ecom:inbound:receive')) {
    list.push({ key: 'receive', label: t('ecomOrder.inbound.receive'), icon: ImportOutlined, primary: true, run: () => emit('receive', d) })
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
.dashed {
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
</style>
