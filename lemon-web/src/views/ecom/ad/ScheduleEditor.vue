<template>
  <a-drawer :open="open" :title="title" width="min(980px, 100vw)" destroy-on-close @close="open = false">
    <a-spin :spinning="loading">
      <a-alert type="info" show-icon :message="$t('ecomAds.schedule.tip')" class="tip" />
      <a-form layout="inline" class="form-line">
        <a-form-item v-if="!fixedCampaign" :label="$t('ecomAds.schedule.campaign')">
          <a-select v-model:value="campaignId" show-search option-filter-prop="label" :options="campaignOptions" class="campaign" @change="loadSchedule" />
        </a-form-item>
        <a-form-item :label="$t('ecomAds.schedule.enabled')">
          <a-switch v-model:checked="enabled" />
        </a-form-item>
        <a-form-item :label="$t('ecomAds.schedule.minBid')">
          <a-input-number v-model:value="minBid" :min="0" :precision="2" />
        </a-form-item>
        <a-form-item :label="$t('ecomAds.schedule.maxBid')">
          <a-input-number v-model:value="maxBid" :min="0" :precision="2" />
        </a-form-item>
      </a-form>
      <div class="fill-bar">
        <span class="sub-text">{{ $t('ecomAds.schedule.selectHint') }}</span>
        <a-input-number v-model:value="fillValue" :min="0.1" :max="3" :step="0.1" :precision="2" size="small" :addon-before="$t('ecomAds.schedule.fillValue')" class="fill-input" />
        <a-button size="small" :disabled="!selection.size" @click="fill(false)">{{ $t('ecomAds.schedule.fillSelected') }}</a-button>
        <a-button size="small" @click="fill(true)">{{ $t('ecomAds.schedule.fillAll') }}</a-button>
        <a-dropdown>
          <a-button size="small">{{ $t('ecomAds.schedule.preset') }}</a-button>
          <template #overlay>
            <a-menu @click="({ key }) => preset(key)">
              <a-menu-item key="night">{{ $t('ecomAds.schedule.presetNight') }}</a-menu-item>
              <a-menu-item key="peak">{{ $t('ecomAds.schedule.presetPeak') }}</a-menu-item>
              <a-menu-item key="weekend">{{ $t('ecomAds.schedule.presetWeekend') }}</a-menu-item>
              <a-menu-item key="reset">{{ $t('ecomAds.schedule.reset') }}</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div class="grid-wrap" @mouseleave="dragging = false" @mouseup="dragging = false">
        <table class="grid">
          <thead>
            <tr>
              <th />
              <th v-for="h in 24" :key="h" class="hour">{{ h - 1 }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, d) in factors" :key="d">
              <th class="day">{{ $t(`ecomAds.schedule.weekday.d${d}`) }}</th>
              <td
                v-for="(f, h) in row"
                :key="h"
                :class="['cell', { selected: selection.has(`${d}-${h}`) }]"
                :style="{ background: color(f) }"
                :title="`${$t(`ecomAds.schedule.weekday.d${d}`)} ${$t('ecomAds.schedule.hour', { h })}: ${f}`"
                @mousedown.prevent="startSelect(d, h)"
                @mouseenter="extendSelect(d, h)"
              >
                {{ Number(f).toFixed(1) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </a-spin>
    <template #footer>
      <div class="footer">
        <a-button v-if="exists" v-perm="'ecom:ad:schedule'" danger :loading="saving" @click="remove">{{ $t('ecomAds.schedule.restore') }}</a-button>
        <span class="spacer" />
        <a-button @click="open = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button v-perm="'ecom:ad:schedule'" type="primary" :loading="saving" :disabled="!campaignId" @click="save">{{ $t('common.action.save') }}</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { deleteSchedule, getSchedule, pageCampaigns, saveSchedule } from '@/api/ecom/ads'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { t } from '@/locales'

/**
 * 分时策略编辑（抽屉）：星期 × 小时的竞价系数表（拖动选择后填充，预设夜间降价 / 晚高峰加价 / 周末加价），启用开关与最低 / 最高竞价。
 * open(活动 ID) 编辑指定活动；open(null, 店铺 ID) 先选择活动。保存后后端按当前时段立即应用一次，触发 success
 */
const emit = defineEmits(['success'])
const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const fixedCampaign = ref(false)
const campaignId = ref()
const campaignName = ref('')
const campaignOptions = ref([])
const factors = ref(ones())
const enabled = ref(true)
const minBid = ref()
const maxBid = ref()
const exists = ref(false)
const fillValue = ref(1.2)
const selection = ref(new Set())
const dragging = ref(false)
let anchor = null

const title = computed(() => (campaignName.value ? `${t('ecomAds.schedule.edit')} · ${campaignName.value}` : t('ecomAds.schedule.add')))

function ones() {
  return Array.from({ length: 7 }, () => Array.from({ length: 24 }, () => 1))
}

async function show(id, storeId) {
  open.value = true
  selection.value = new Set()
  fixedCampaign.value = !!id
  campaignId.value = id || undefined
  campaignName.value = ''
  factors.value = ones()
  exists.value = false
  if (!id) {
    const res = await pageCampaigns({ current: 1, size: 500, storeId })
    campaignOptions.value = (res?.records || []).map((c) => ({ value: c.id, label: `${c.name} · ${c.storeName}` }))
    return
  }
  await loadSchedule(id)
}

async function loadSchedule(id) {
  loading.value = true
  try {
    const s = await getSchedule(id)
    campaignName.value = s.campaignName || ''
    factors.value = (s.factors || ones()).map((r) => r.map(Number))
    enabled.value = s.id ? s.status === STATUS_ENABLED : true
    minBid.value = s.minBid == null ? undefined : Number(s.minBid)
    maxBid.value = s.maxBid == null ? undefined : Number(s.maxBid)
    exists.value = !!s.id
  } finally {
    loading.value = false
  }
}

/** 系数颜色：小于 1 偏蓝，大于 1 偏橙，越偏离越深 */
function color(f) {
  const v = Number(f)
  if (v === 1) return 'transparent'
  const a = Math.min(0.75, Math.abs(v - 1) * 0.6 + 0.1)
  return v < 1 ? `rgba(22, 119, 255, ${a})` : `rgba(250, 140, 22, ${a})`
}

function rect(a, b) {
  const s = new Set()
  for (let d = Math.min(a[0], b[0]); d <= Math.max(a[0], b[0]); d++) {
    for (let h = Math.min(a[1], b[1]); h <= Math.max(a[1], b[1]); h++) s.add(`${d}-${h}`)
  }
  return s
}
function startSelect(d, h) {
  dragging.value = true
  anchor = [d, h]
  selection.value = rect(anchor, anchor)
}
function extendSelect(d, h) {
  if (dragging.value && anchor) selection.value = rect(anchor, [d, h])
}

function fill(all) {
  const v = Number(fillValue.value)
  if (!v) return
  factors.value = factors.value.map((row, d) => row.map((f, h) => (all || selection.value.has(`${d}-${h}`) ? v : f)))
}

function preset(key) {
  if (key === 'reset') {
    factors.value = ones()
  } else if (key === 'night') {
    factors.value = factors.value.map((row) => row.map((f, h) => (h <= 6 ? 0.6 : f)))
  } else if (key === 'peak') {
    factors.value = factors.value.map((row) => row.map((f, h) => (h >= 19 && h <= 22 ? 1.3 : f)))
  } else if (key === 'weekend') {
    factors.value = factors.value.map((row, d) => (d >= 5 ? row.map((f) => Math.round(f * 1.2 * 100) / 100) : row))
  }
}

async function save() {
  saving.value = true
  try {
    const r = await saveSchedule(campaignId.value, {
      factors: factors.value,
      status: enabled.value ? STATUS_ENABLED : STATUS_DISABLED,
      minBid: minBid.value ?? null,
      maxBid: maxBid.value ?? null
    })
    message.success(t('ecomAds.schedule.applied', { msg: r.message }))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

function remove() {
  Modal.confirm({
    title: t('ecomAds.schedule.confirmDelete'),
    onOk: async () => {
      const r = await deleteSchedule(campaignId.value)
      message.success(t('ecomAds.schedule.applied', { msg: r.message }))
      open.value = false
      emit('success')
    }
  })
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.form-line {
  margin-bottom: 12px;
  row-gap: 8px;
}
.campaign {
  width: 280px;
}
.fill-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.fill-input {
  width: 150px;
}
.grid-wrap {
  overflow-x: auto;
  user-select: none;
}
.grid {
  border-collapse: collapse;
  font-size: 12px;
}
.grid th,
.grid td {
  border: 1px solid var(--app-border);
  text-align: center;
  padding: 0;
}
.hour {
  width: 34px;
  font-weight: normal;
  color: var(--app-text-3);
}
.day {
  width: 48px;
  padding: 0 6px !important;
  font-weight: normal;
  white-space: nowrap;
}
.cell {
  height: 30px;
  cursor: pointer;
}
.cell.selected {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
}
.footer {
  display: flex;
  gap: 8px;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
