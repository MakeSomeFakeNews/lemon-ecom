<template>
  <SubPage :open="open" :title="detail ? $t('ecomOwh.inbound.detailTitle', { no: detail.inboundNo }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_owh_inbound_status" :value="detail.status" />
      <a-tag v-if="detail.erpReceived === 1" color="green" :bordered="false">{{ $t('ecomOwh.inbound.erpReceived') }}</a-tag>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.errorMsg" type="warning" show-icon class="block" :message="$t('ecomOwh.common.errorMsg')" :description="detail.errorMsg" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOwh.inbound.source')">
            <DictTag type="ecom_owh_inbound_source" :value="detail.sourceType" /> {{ detail.sourceNo }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.account')">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.common.warehouse')">{{ detail.warehouseName }}（{{ detail.owhCode }}）</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.owhOrderNo')">{{ detail.owhOrderNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.owhStatus')">{{ detail.owhStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.qtyReceived')">{{ qty(detail.receivedQty) }} / {{ qty(detail.totalQty) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.pushTime')">{{ detail.pushTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.receiveTime')">{{ detail.receiveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOwh.inbound.syncTime')">{{ detail.syncTime || '-' }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOwh.inbound.firstLeg') }}</div>
        <a-row :gutter="16" class="block">
          <a-col :xs="24" :sm="12" :lg="6">
            <div class="label">{{ $t('ecomOwh.inbound.transportMode') }}</div>
            <a-select v-if="editing" v-model:value="form.transportMode" allow-clear class="full" :options="optionsWithCurrent(dict.ecom_transport_mode, form.transportMode)" />
            <DictTag v-else-if="detail.transportMode" type="ecom_transport_mode" :value="detail.transportMode" />
            <span v-else>-</span>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <div class="label">{{ $t('ecomOwh.inbound.carrier') }}</div>
            <a-input v-if="editing" v-model:value="form.carrier" :maxlength="64" />
            <span v-else>{{ detail.carrier || '-' }}</span>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <div class="label">{{ $t('ecomOwh.inbound.trackingNo') }}</div>
            <a-input v-if="editing" v-model:value="form.trackingNo" :maxlength="64" />
            <span v-else>{{ detail.trackingNo || '-' }}</span>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <div class="label">{{ $t('ecomOwh.inbound.etaDate') }}</div>
            <a-date-picker v-if="editing" v-model:value="form.etaDate" value-format="YYYY-MM-DD" class="full" />
            <span v-else>{{ detail.etaDate || '-' }}</span>
          </a-col>
          <a-col :span="24" class="remark-row">
            <div class="label">{{ $t('common.field.remark') }}</div>
            <a-input v-if="editing" v-model:value="form.remark" :maxlength="255" />
            <span v-else>{{ detail.remark || '-' }}</span>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">{{ $t('ecomOwh.inbound.boxes', { n: (editing ? form.boxes : detail.boxes).length }) }}</span>
          <a-button v-if="editing" size="small" class="add-row" @click="addBox"><template #icon><PlusOutlined /></template>{{ $t('ecomOwh.inbound.addBox') }}</a-button>
        </div>
        <a-table row-key="boxNo" size="small" :columns="boxColumns" :data-source="editing ? form.boxes : detail.boxes" :pagination="false" :scroll="{ x: 700 }" class="block">
          <template #bodyCell="{ column, record, index }">
            <template v-if="['length', 'width', 'height', 'weight'].includes(column.key)">
              <a-input-number v-if="editing" v-model:value="record[column.key]" :min="0" :precision="column.key === 'weight' ? 3 : 2" size="small" class="full" />
              <span v-else>{{ record[column.key] != null ? Number(record[column.key]) : '-' }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a v-if="editing && form.boxes.length > 1" class="danger" @click="removeBox(index)"><DeleteOutlined /></a>
            </template>
          </template>
        </a-table>

        <div class="items-head">
          <span class="section-title">{{ $t('ecomOwh.inbound.lines') }}</span>
          <span v-if="editing" class="sub-text">{{ $t('ecomOwh.inbound.linesTip') }}</span>
          <a-button v-if="editing" size="small" class="add-row" @click="addLine"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </div>
        <a-table :row-key="(r) => r.key || r.id" size="small" :columns="lineColumns" :data-source="editing ? form.lines : detail.items" :pagination="false" :scroll="{ x: 800 }" class="block">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'boxNo'">
              <a-select v-if="editing" v-model:value="record.boxNo" size="small" class="full" :options="form.boxes.map((b) => ({ value: b.boxNo, label: String(b.boxNo) }))" />
              <span v-else>{{ record.boxNo }}</span>
            </template>
            <template v-else-if="column.key === 'material'">
              <a-select v-if="editing" v-model:value="record.materialId" size="small" class="full" :options="materialOptions" />
              <template v-else>
                <div>{{ record.sku }}</div>
                <div class="sub-text">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
              </template>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-if="editing" v-model:value="record.qty" :min="0" :precision="0" size="small" class="full" />
              <span v-else>{{ qty(record.qty) }}</span>
            </template>
            <template v-else-if="column.key === 'receivedQty'">{{ editing ? '-' : qty(record.receivedQty) }}</template>
            <template v-else-if="column.key === 'action'">
              <a v-if="editing && form.lines.length > 1" class="danger" @click="form.lines.splice(index, 1)"><DeleteOutlined /></a>
            </template>
          </template>
        </a-table>
        <OwhDiffList :source-type="OwhDiffSource.INBOUND" :source-id="detail.id" :refresh-key="detail.updateTime" />
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" :loading="a.loading" @click="a.run">{{ a.label }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getOwhInbound, updateOwhInbound } from '@/api/ecom/owh'
import OwhDiffList from '@/components/ecom/owh/OwhDiffList.vue'
import { OwhDiffSource } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { optionsWithCurrent } from '@/utils/dict'
import { qty } from '@/utils/number'
import { canSyncInbound, isInboundDraft, isInboundPushedOnly, useOwhCaps } from '../owh-outbound/owh'

/**
 * 海外仓入库单详情：来源、服务商单号与收货进度、头程信息、箱子（箱唛、尺寸、毛重）与装箱明细（箱号 × SKU × 数量，同步收货数量）。
 * 草稿可修改头程信息与装箱（各物料合计须与来源单据一致）。open(id)、reload()；推送、同步、取消交给页面（事件带入库单）
 */
const emit = defineEmits(['push', 'sync', 'cancel', 'void', 'saved'])
const hasCap = useOwhCaps()
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_transport_mode')

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const detail = ref(null)
const editing = ref(false)
const form = ref({ boxes: [], lines: [] })
let currentId = null
let seq = 0

const boxColumns = computed(() => [
  { title: t('ecomOwh.inbound.boxNo'), dataIndex: 'boxNo', width: 70 },
  { title: t('ecomOwh.inbound.boxMark'), dataIndex: 'boxMark', width: 180 },
  { title: t('ecomOwh.inbound.length'), key: 'length', width: 100 },
  { title: t('ecomOwh.inbound.width'), key: 'width', width: 100 },
  { title: t('ecomOwh.inbound.height'), key: 'height', width: 100 },
  { title: t('ecomOwh.inbound.weight'), key: 'weight', width: 110 },
  ...(editing.value ? [{ title: '', key: 'action', width: 50 }] : [])
])
const lineColumns = computed(() => [
  { title: t('ecomOwh.inbound.boxNo'), key: 'boxNo', width: 90 },
  { title: t('ecomOwh.inbound.sku'), key: 'material', width: 260 },
  { title: t('ecomOwh.inbound.qty'), key: 'qty', width: 110, align: 'right' },
  { title: t('ecomOwh.inbound.receivedQty'), key: 'receivedQty', width: 110, align: 'right' },
  ...(editing.value ? [{ title: '', key: 'action', width: 50 }] : [])
])
const materialOptions = computed(() => {
  const seen = new Map()
  for (const it of detail.value?.items || []) if (!seen.has(it.materialId)) seen.set(it.materialId, `${it.sku} ${it.materialName || ''}`.trim())
  return [...seen].map(([value, label]) => ({ value, label }))
})

async function load(id) {
  loading.value = true
  try {
    detail.value = await getOwhInbound(id)
  } finally {
    loading.value = false
  }
}

function show(id) {
  currentId = id
  detail.value = null
  editing.value = false
  open.value = true
  load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)

function startEdit() {
  const d = detail.value
  form.value = {
    transportMode: d.transportMode,
    carrier: d.carrier,
    trackingNo: d.trackingNo,
    etaDate: d.etaDate,
    remark: d.remark,
    boxes: d.boxes.map((b) => ({ boxNo: b.boxNo, boxMark: b.boxMark, length: num(b.length), width: num(b.width), height: num(b.height), weight: num(b.weight) })),
    lines: d.items.map((i) => ({ key: ++seq, boxNo: i.boxNo, materialId: i.materialId, qty: Number(i.qty) }))
  }
  editing.value = true
}
const num = (v) => (v == null ? undefined : Number(v))
function addBox() {
  const next = Math.max(0, ...form.value.boxes.map((b) => b.boxNo)) + 1
  form.value.boxes.push({ boxNo: next, boxMark: '-', length: undefined, width: undefined, height: undefined, weight: undefined })
}
function removeBox(index) {
  const no = form.value.boxes[index].boxNo
  form.value.boxes.splice(index, 1)
  const first = form.value.boxes[0].boxNo
  form.value.lines.forEach((l) => (l.boxNo === no ? (l.boxNo = first) : null))
}
function addLine() {
  form.value.lines.push({ key: ++seq, boxNo: form.value.boxes[form.value.boxes.length - 1]?.boxNo, materialId: materialOptions.value[0]?.value, qty: undefined })
}

async function save() {
  const f = form.value
  if (f.lines.some((l) => !l.boxNo || !l.materialId || !l.qty)) {
    message.warning(t('ecomOwh.inbound.lineIncomplete'))
    return
  }
  saving.value = true
  try {
    await updateOwhInbound(currentId, {
      transportMode: f.transportMode || null,
      carrier: f.carrier || null,
      trackingNo: f.trackingNo || null,
      etaDate: f.etaDate || null,
      remark: f.remark || null,
      boxes: f.boxes.map((b) => ({ boxNo: b.boxNo, length: b.length ?? null, width: b.width ?? null, height: b.height ?? null, weight: b.weight ?? null })),
      lines: f.lines.map((l) => ({ boxNo: l.boxNo, materialId: l.materialId, qty: l.qty }))
    })
    message.success(t('common.msg.saveSuccess'))
    editing.value = false
    await load(currentId)
    emit('saved')
  } finally {
    saving.value = false
  }
}

const actions = computed(() => {
  const d = detail.value
  const out = []
  if (editing.value) {
    out.push({ key: 'cancelEdit', label: t('common.action.cancel'), run: () => (editing.value = false) })
    out.push({ key: 'save', label: t('common.action.save'), primary: true, loading: saving.value, run: save })
    return out
  }
  if (isInboundDraft(d) && userStore.hasPerm('ecom:owhInbound:add')) out.push({ key: 'edit', label: t('ecomOwh.inbound.editPacking'), run: startEdit })
  if (isInboundDraft(d) && userStore.hasPerm('ecom:owhInbound:cancel')) out.push({ key: 'cancel', label: t('ecomOwh.inbound.cancel'), danger: true, run: () => emit('cancel', d) })
  if (isInboundPushedOnly(d) && userStore.hasPerm('ecom:owhInbound:cancel')) {
    out.push(hasCap(d.provider, 'CANCEL_INBOUND')
      ? { key: 'cancel', label: t('ecomOwh.inbound.cancelPushed'), danger: true, run: () => emit('cancel', d) }
      : { key: 'void', label: t('ecomOwh.common.markVoided'), danger: true, run: () => emit('void', d) })
  }
  if (isInboundDraft(d) && userStore.hasPerm('ecom:owhInbound:push')) out.push({ key: 'push', label: t('ecomOwh.inbound.push'), primary: true, run: () => emit('push', d) })
  if (canSyncInbound(d) && userStore.hasPerm('ecom:owhInbound:sync')) out.push({ key: 'sync', label: t('ecomOwh.inbound.sync'), primary: true, run: () => emit('sync', d) })
  return out
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
}
.items-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 12px;
}
.add-row {
  margin-left: auto;
}
.label {
  margin-bottom: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.remark-row {
  margin-top: 12px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
