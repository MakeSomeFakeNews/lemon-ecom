<template>
  <SubPage :open="open" :title="detail ? $t('ecomOper.promo.detailTitle', { name: detail.name }) : ''" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <DictTag type="ecom_promo_status" :value="detail.status" />
        <DictTag v-if="detail.phase" type="ecom_promo_phase" :value="detail.phase" />
        <DictTag type="ecom_promo_push_status" :value="detail.pushStatus" />
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOper.common.store')">{{ detail.storeName }} <DictTag type="ecom_platform" :value="detail.platform" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.promoType')"><DictTag type="ecom_promo_type" :value="detail.promoType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.period')" :span="2">{{ periodText(detail) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.discountPercent')">{{ detail.discountPercent == null ? '-' : `${detail.discountPercent}%` }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.threshold')">{{ amount(detail.thresholdAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.reduce')">{{ amount(detail.reduceAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.budget')">{{ amount(detail.budget) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.fee')">{{ amount(detail.feeAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.promo.feeBase')">{{ money(detail.feeBase) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOper.common.owner')">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.platformPromoId" :label="$t('ecomOper.promo.platformId')">{{ detail.platformPromoId }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.pushMessage" :label="$t('ecomOper.promo.pushMessage')" :span="2"><span class="danger">{{ detail.pushMessage }}</span></a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOper.promo.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 860 }" class="block"
                 :locale="{ emptyText: $t('ecomOper.promo.noItemsTip') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sku'">
              <div>{{ record.sellerSku }}</div>
              <div class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
            </template>
            <template v-else-if="column.key === 'originalPrice'">{{ money(record.originalPrice) }}</template>
            <template v-else-if="column.key === 'promoPrice'">{{ money(record.promoPrice) }}</template>
            <template v-else-if="column.key === 'discountPercent'">{{ record.discountPercent == null ? '-' : `${record.discountPercent}%` }}</template>
          </template>
        </a-table>

        <template v-if="effect">
          <div class="section-title">{{ $t('ecomOper.promo.effect') }}</div>
          <a-alert type="info" show-icon :message="$t('ecomOper.promo.effectTip', { days: effect.days })" class="block" />
          <a-row :gutter="[12, 12]" class="block">
            <a-col v-for="w in windows" :key="w.key" :xs="24" :md="8">
              <div class="window" :class="{ during: w.key === 'during' }">
                <div class="window-head">
                  <b>{{ $t(`ecomOper.promo.${w.key}`) }}</b>
                  <span class="sub-text">{{ String(w.begin).slice(5, 16) }} ~ {{ String(w.end).slice(5, 16) }}</span>
                  <a-tag v-if="w.partial" color="orange">{{ $t('ecomOper.promo.partial') }}</a-tag>
                </div>
                <div class="metrics">
                  <div><span class="label">{{ $t('ecomOper.common.orders') }}</span>{{ w.orders }}</div>
                  <div><span class="label">{{ $t('ecomOper.common.qty') }}</span>{{ qty(w.qty) }}</div>
                  <div><span class="label">{{ $t('ecomOper.common.sales') }}</span>{{ money(w.sales) }}</div>
                  <div><span class="label">{{ $t('ecomOper.promo.grossProfit') }}</span>{{ money(w.grossProfit) }}</div>
                  <div v-if="Number(w.fee)"><span class="label">{{ $t('ecomOper.promo.fee') }}</span><span class="danger">−{{ money(w.fee) }}</span></div>
                  <div><span class="label">{{ $t('ecomOper.common.profit') }}</span><b :class="{ danger: Number(w.profit) < 0 }">{{ money(w.profit) }}</b></div>
                  <div><span class="label">{{ $t('ecomOper.common.profitRate') }}</span>{{ pct(w.profitRate) }}</div>
                  <div><span class="label">{{ $t('ecomOper.promo.dailyQty') }}</span>{{ qty(w.dailyQty) }}</div>
                  <div><span class="label">{{ $t('ecomOper.promo.dailySales') }}</span>{{ money(w.dailySales) }}</div>
                </div>
              </div>
            </a-col>
          </a-row>
          <a-row :gutter="[12, 12]" class="block">
            <a-col v-for="c in lifts" :key="c.key" :xs="12" :md="6">
              <div class="card">
                <div class="label">{{ c.label }}<a-tooltip v-if="c.tip" :title="c.tip"><QuestionCircleOutlined class="tip" /></a-tooltip></div>
                <div class="value" :class="Number(c.value) > 0 ? 'up' : Number(c.value) < 0 ? 'danger' : ''">{{ signedPct(c.value) }}</div>
              </div>
            </a-col>
          </a-row>
          <div class="section-title">{{ $t('ecomOper.promo.dailyChart') }}</div>
          <EChart :option="dailyOption" height="280px" class="block" />
          <div class="section-title">{{ $t('ecomOper.promo.bySku') }}</div>
          <a-table row-key="sellerSku" size="small" :columns="skuColumns" :data-source="effect.skus" :pagination="false" :scroll="{ x: 900 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'sku'">
                <div>{{ record.sellerSku }}</div>
                <div class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
              </template>
              <template v-else-if="column.key.endsWith('Qty')">{{ qty(record[column.key]) }}</template>
              <template v-else-if="column.key.endsWith('Sales')">{{ money(record[column.key]) }}</template>
              <template v-else-if="column.key === 'qtyLift'">
                <span :class="Number(record.qtyLift) > 0 ? 'up' : Number(record.qtyLift) < 0 ? 'danger' : ''">{{ signedPct(record.qtyLift) }}</span>
              </template>
            </template>
          </a-table>
        </template>
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
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { CheckOutlined, CloseCircleOutlined, CloudUploadOutlined, DeleteOutlined, EditOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { cancelPromotion, confirmPromotion, deletePromotion, getPromotion, promotionEffect, pushPromotion } from '@/api/ecom/oper'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { canDelete, canEdit, canPush, isActive, isDraft, periodText } from './promo'

/** 促销详情子页：活动信息、参与 SKU、效果分析（前 / 中 / 后对比、增长、费用回报率、每日走势、按 SKU）。ref 调用 open(id)、reload() */
const emit = defineEmits(['changed', 'edit'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const dict = useDict('ecom_platform', 'ecom_promo_type', 'ecom_promo_status', 'ecom_promo_phase', 'ecom_promo_push_status')

const open = ref(false)
const loading = ref(false)
const detail = ref()
const effect = ref()
let currentId

const amount = (v) => (v == null ? '-' : `${money(v)} ${detail.value?.currency || ''}`)
const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
const signedPct = (v) => (v == null ? '-' : `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`)

const itemColumns = [
  { title: t('ecomOper.promo.sellerSku'), key: 'sku', width: 280 },
  { title: t('ecomOper.promo.originalPrice'), key: 'originalPrice', width: 100, align: 'right' },
  { title: t('ecomOper.promo.itemDiscount'), key: 'discountPercent', width: 90, align: 'right' },
  { title: t('ecomOper.promo.promoPrice'), key: 'promoPrice', width: 100, align: 'right' },
  { title: t('ecomOper.promo.promoQty'), dataIndex: 'promoQty', width: 100, align: 'right' },
  { title: t('ecomOper.promo.limitPerBuyer'), dataIndex: 'limitPerBuyer', width: 100, align: 'right' }
]
const skuColumns = [
  { title: t('ecomOper.promo.sellerSku'), key: 'sku', width: 240 },
  { title: `${t('ecomOper.promo.before')} ${t('ecomOper.common.qty')}`, key: 'beforeQty', width: 100, align: 'right' },
  { title: `${t('ecomOper.promo.during')} ${t('ecomOper.common.qty')}`, key: 'duringQty', width: 100, align: 'right' },
  { title: `${t('ecomOper.promo.after')} ${t('ecomOper.common.qty')}`, key: 'afterQty', width: 100, align: 'right' },
  { title: `${t('ecomOper.promo.before')} ${t('ecomOper.common.sales')}`, key: 'beforeSales', width: 120, align: 'right' },
  { title: `${t('ecomOper.promo.during')} ${t('ecomOper.common.sales')}`, key: 'duringSales', width: 120, align: 'right' },
  { title: `${t('ecomOper.promo.after')} ${t('ecomOper.common.sales')}`, key: 'afterSales', width: 120, align: 'right' },
  { title: t('ecomOper.promo.skuLift'), key: 'qtyLift', width: 100, align: 'right' }
]

const windows = computed(() => (effect.value ? [effect.value.before, effect.value.during, effect.value.after] : []))
const lifts = computed(() => {
  const e = effect.value
  if (!e) return []
  return [
    { key: 'qty', label: t('ecomOper.promo.qtyLift'), value: e.qtyLift },
    { key: 'sales', label: t('ecomOper.promo.salesLift'), value: e.salesLift },
    { key: 'profit', label: t('ecomOper.promo.profitLift'), value: e.profitLift },
    { key: 'roi', label: t('ecomOper.promo.roi'), value: e.roi, tip: t('ecomOper.promo.roiTip') }
  ]
})

const dailyOption = computed(() => {
  const e = effect.value
  const rows = e?.daily || []
  const start = dayjs(e?.during?.begin).format('MM-DD')
  const end = dayjs(e?.during?.end).format('MM-DD')
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => dayjs(r.day).format('MM-DD')) },
    yAxis: [{ type: 'value', minInterval: 1 }, { type: 'value', splitLine: { show: false } }],
    series: [
      {
        name: t('ecomOper.common.qty'), type: 'bar', barMaxWidth: 16, itemStyle: { color: '#91caff' }, data: rows.map((r) => Number(r.qty)),
        markLine: { symbol: 'none', lineStyle: { color: '#fa8c16', type: 'dashed' }, label: { formatter: (p) => p.name }, data: [
          { xAxis: start, name: t('ecomOper.promo.startTime') },
          { xAxis: end, name: t('ecomOper.promo.endTime') }
        ] }
      },
      { name: t('ecomOper.common.sales'), type: 'line', smooth: true, yAxisIndex: 1, itemStyle: { color: '#52c41a' }, data: rows.map((r) => Number(r.sales)) }
    ]
  }
})

async function load(id) {
  loading.value = true
  try {
    detail.value = await getPromotion(id)
    effect.value = isDraft(detail.value) ? null : await promotionEffect(id)
  } finally {
    loading.value = false
  }
}
function show(id) {
  currentId = id
  detail.value = undefined
  effect.value = undefined
  open.value = true
  return load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)
const after = async () => {
  await load(currentId)
  emit('changed')
}

const platformLabel = (p) => findDict(dict.ecom_platform, p)?.label || p
const confirm = useAction(confirmPromotion, { confirm: () => t('ecomOper.promo.confirmConfirm', { name: detail.value.name }), reload: after })
const push = useAction(pushPromotion, {
  confirm: () => t('ecomOper.promo.pushConfirm', { name: detail.value.name, platform: platformLabel(detail.value.platform) }),
  successKey: (r) => t('ecomOper.promo.pushed', { id: r?.platformPromoId || '' }),
  reload: after
})
const cancel = useAction(cancelPromotion, { confirm: () => ({ title: t('ecomOper.promo.cancelConfirm', { name: detail.value.name }), okType: 'danger' }), reload: after })
const remove = useAction(deletePromotion, {
  confirm: () => ({ title: t('ecomOper.promo.deleteConfirm', { name: detail.value.name }), okType: 'danger' }),
  successKey: 'common.msg.deleteSuccess',
  reload: () => {
    open.value = false
    emit('changed')
  }
})

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (canDelete(d) && can('ecom:promotion:remove')) list.push({ key: 'delete', label: t('common.action.delete'), icon: DeleteOutlined, danger: true, run: () => remove(d.id) })
  if ((isDraft(d) || isActive(d)) && can('ecom:promotion:confirm')) list.push({ key: 'cancel', label: t('ecomOper.promo.cancel'), icon: CloseCircleOutlined, danger: true, run: () => cancel(d.id) })
  if (canEdit(d) && can('ecom:promotion:edit')) list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  if (canPush(d) && can('ecom:promotion:push')) list.push({ key: 'push', label: t('ecomOper.promo.push'), icon: CloudUploadOutlined, run: () => push(d.id) })
  if (isDraft(d) && can('ecom:promotion:confirm')) list.push({ key: 'confirm', label: t('ecomOper.promo.confirm'), icon: CheckOutlined, primary: true, run: () => confirm(d.id) })
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
.window {
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  padding: 10px 12px;
  height: 100%;
}
.window.during {
  border-color: #fa8c16;
}
.window-head {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
}
.metrics .label,
.card .label {
  color: var(--app-text-3);
  font-size: 12px;
  margin-right: 6px;
}
.card {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-fill, #fafafa);
}
.card .value {
  font-size: 20px;
  font-weight: 600;
}
.tip {
  margin-left: 4px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.up {
  color: #52c41a;
}
.danger {
  color: #ff4d4f;
}
</style>
