<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.inbound.detailTitle', { no: detail.docNo }) : $t('ecomOrder.inbound.title')" @close="open = false">
    <template v-if="detail" #title>
      <span class="title-no">{{ $t('ecomOrder.inbound.detailTitle', { no: detail.docNo }) }}</span>
      <DictTag type="ecom_inbound_status" :value="detail.status" class="title-tag" />
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
        <InboundProgress :detail="detail" class="block" />

        <div class="kpis block">
          <div class="kpi">
            <div class="kpi-label">{{ $t('ecomOrder.inbound.totalQty') }}</div>
            <div class="kpi-value">{{ qty(detail.totalQty) }}</div>
          </div>
          <div class="kpi clickable" @click="tab = 'boxes'">
            <div class="kpi-label">{{ $t('ecomOrder.inbound.boxCount') }}</div>
            <div class="kpi-value">{{ detail.boxCount ?? '-' }}</div>
            <div class="kpi-sub">{{ detail.weight != null ? `${qty(detail.weight, 3)} kg` : '' }}{{ detail.volume != null ? ` · ${qty(detail.volume)} m³` : '' }}</div>
          </div>
          <div class="kpi">
            <div class="kpi-label">{{ $t('ecomOrder.inbound.totalCost') }}</div>
            <div class="kpi-value">{{ baseMoney(totalCost(detail)) }}</div>
            <div class="kpi-sub">{{ $t('ecomOrder.inbound.costBreak', { f: money(detail.freight), d: money(detail.duty), o: money(detail.otherCost) }) }}</div>
          </div>
          <div class="kpi">
            <div class="kpi-label">
              {{ $t('ecomOrder.inbound.unitLanded') }}
              <a-tooltip :title="$t('ecomOrder.inbound.unitLandedHint')"><QuestionCircleOutlined class="hint" /></a-tooltip>
            </div>
            <div class="kpi-value">¥{{ price(unitLanded) }}</div>
          </div>
        </div>

        <div class="section-title">{{ $t('ecomOrder.inbound.basicInfo') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.shipmentId')">{{ detail.shipmentId || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.fromWarehouse')">{{ detail.fromWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.toWarehouse')">{{ detail.toWarehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.transferDoc')">{{ detail.transferDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomCatalog.plan.planNo')">{{ detail.planNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.createBy')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.remark')">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.inbound.logisticsInfo') }}</div>
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.channel')">{{ detail.channelName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.carrier')">{{ detail.carrier || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.trackingNo')">
            <a-typography-text v-if="detail.trackingNo" :copyable="{ text: detail.trackingNo }">{{ detail.trackingNo }}</a-typography-text>
            <template v-else>-</template>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomProfit.inbound.importVat')">{{ baseMoney(detail.importVat) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.shipDate')">{{ detail.shipDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.etaDate')">{{ detail.etaDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.inbound.receiveDate')">{{ detail.receiveDate || '-' }}</a-descriptions-item>
        </a-descriptions>

        <a-tabs v-model:active-key="tab" class="detail-tabs">
          <a-tab-pane key="items" :tab="`${$t('ecomOrder.inbound.items')} (${(detail.items || []).length})`">
            <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 900 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">
                  <div>{{ record.materialCode }} {{ record.materialName }}</div>
                  <div class="sub-text">{{ record.materialSpec }}</div>
                </template>
                <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }} <span class="sub-text">{{ record.unit || '' }}</span></template>
                <template v-else-if="column.key === 'receivedQty'">
                  <span v-if="received && record.receivedQty != null" :class="{ warn: Number(record.receivedQty) < Number(record.qty) }">{{ qty(record.receivedQty) }}</span>
                  <template v-else>-</template>
                </template>
                <template v-else-if="column.key === 'unitLanded'">{{ record.unitLanded != null ? `¥${price(record.unitLanded)}` : '-' }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="boxes" :tab="(detail.boxes || []).length ? `${$t('ecomCatalog.box.section')} (${detail.boxes.length})` : $t('ecomCatalog.box.section')">
            <InboundBoxes :detail="detail" @changed="load(currentId)" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, DollarOutlined, EditOutlined, ImportOutlined, QuestionCircleOutlined, SendOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { cancelEcomInbound, deleteEcomInbound, getEcomInbound, submitEcomInbound } from '@/api/ecom/inbound'
import { useUserStore } from '@/store/user'
import { InboundStatus } from '@/constants/enums'
import { money, price, qty } from '@/utils/number'
import { baseMoney } from '../order/order'
import { canCancel, canDelete, canEditCosts, canReceive, isDraft, totalCost, unitLandedOf } from './inbound'
import InboundBoxes from './InboundBoxes.vue'
import InboundProgress from './InboundProgress.vue'

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
const tab = ref('items')
const received = computed(() => detail.value?.status === InboundStatus.RECEIVED)
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
  tab.value = 'items'
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
.title-no {
  font-weight: 600;
}
.title-tag {
  margin-left: 8px;
  vertical-align: 1px;
}
.section-title {
  margin: 4px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  font-weight: 600;
  line-height: 1.2;
}
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.kpi {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-bg);
  border: 1px solid var(--app-border);
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.kpi-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi.clickable {
  cursor: pointer;
}
.kpi.clickable:hover {
  border-color: var(--app-primary);
}
.detail-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.hint {
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
</style>
