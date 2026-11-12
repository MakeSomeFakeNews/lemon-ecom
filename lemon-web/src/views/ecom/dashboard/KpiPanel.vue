<template>
  <div class="kpi-grid">
    <div v-for="p in periods" :key="p.key" class="page-card kpi" :class="p.key.toLowerCase()">
      <div class="kpi-head">
        <span class="kpi-title">{{ $t(`ecomFix.dash.period.${p.key}`) }}</span>
        <span class="sub-text">{{ rangeText(p) }}</span>
      </div>
      <div class="kpi-main">
        <div class="kpi-label">{{ $t('ecomFix.dash.sales') }}</div>
        <div class="kpi-value" :title="money(p.current.sales)"><span class="unit">¥</span>{{ shortMoney(p.current.sales) }}</div>
        <div class="compare">
          <span class="cmp">{{ $t(`ecomFix.dash.mom.${p.key}`) }} <Growth :value="p.mom.sales" /></span>
          <span class="cmp">{{ $t('ecomFix.dash.yoy') }} <Growth :value="p.yoy.sales" /></span>
        </div>
      </div>
      <div class="kpi-items">
        <div v-for="m in SUB" :key="m.key" class="item">
          <div class="item-label">{{ $t(`ecomFix.dash.${m.key}`) }}</div>
          <div class="item-value" :class="{ neg: m.money && nz(p.current[m.key]) < 0 }">
            <template v-if="m.money">{{ shortMoney(p.current[m.key]) }}</template>
            <template v-else>{{ qty(p.current[m.key]) }}</template>
          </div>
          <a-tooltip :title="`${$t(`ecomFix.dash.mom.${p.key}`)} ${fmt(p.previous[m.key], m.money)} · ${$t('ecomFix.dash.yoy')} ${fmt(p.lastYear[m.key], m.money)}`">
            <div class="item-growth"><Growth :value="p.mom[m.key]" small /></div>
          </a-tooltip>
        </div>
      </div>
    </div>
    <div v-if="!periods.length" class="page-card kpi empty"><a-skeleton active :paragraph="{ rows: 3 }" /></div>
  </div>
</template>

<script setup>
import { h } from 'vue'
import dayjs from 'dayjs'
import { shortMoney } from '@/components/charts/charts'
import { money, nz, qty } from '@/utils/number'

/**
 * 关键指标：今日 / 昨日 / 本月三张卡片，主指标为销售额（较昨日 / 较前一天 / 较上月同期，较去年同期），
 * 下方为订单数、件数、客单价、订单利润及其环比（悬停显示对比期的值）。periods 为 /ecom/dashboard/kpi 的结果
 */
defineProps({ periods: { type: Array, default: () => [] } })

const SUB = [
  { key: 'orders' },
  { key: 'units' },
  { key: 'avgOrder', money: true },
  { key: 'profit', money: true }
]

const fmt = (v, isMoney) => (isMoney ? money(v) : qty(v))
function rangeText(p) {
  const b = dayjs(p.begin)
  const e = dayjs(p.end)
  if (p.key === 'MONTH') return `${b.format('MM-DD')} ~ ${e.format('MM-DD HH:mm')}`
  if (p.key === 'TODAY') return `00:00 ~ ${e.format('HH:mm')}`
  return b.format('YYYY-MM-DD')
}

/** 增长率：正数绿色上箭头，负数红色下箭头，没有对比数据显示 - */
const Growth = (props) => {
  const v = props.value
  if (v === null || v === undefined) return h('span', { class: 'growth none' }, '-')
  const n = Number(v)
  return h('span', { class: ['growth', n >= 0 ? 'up' : 'down', props.small ? 'small' : ''] }, `${n >= 0 ? '↑' : '↓'} ${Math.abs(n).toFixed(1)}%`)
}
Growth.props = ['value', 'small']
</script>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}
.kpi {
  padding: 16px;
  margin-top: 0 !important;
  background: var(--app-card);
  position: relative;
  overflow: hidden;
}
.kpi::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: var(--app-primary);
}
.kpi.yesterday::before {
  background: #13c2c2;
}
.kpi.month::before {
  background: #722ed1;
}
.kpi-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.kpi-title {
  font-weight: 600;
  font-size: 15px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi-main {
  margin: 10px 0 12px;
}
.kpi-label,
.item-label {
  font-size: 12px;
  color: var(--app-text-3);
}
.kpi-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--app-text);
}
.unit {
  font-size: 14px;
  font-weight: 400;
  margin-right: 2px;
  color: var(--app-text-3);
}
.compare {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 12px;
  color: var(--app-text-3);
}
.kpi-items {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--app-border);
}
.item-value {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.neg {
  color: var(--status-critical, #cf1322);
}
:deep(.growth) {
  font-weight: 600;
}
:deep(.growth.small) {
  font-size: 12px;
}
:deep(.growth.up) {
  color: #389e0d;
}
:deep(.growth.down) {
  color: #cf1322;
}
:deep(.growth.none) {
  color: var(--app-text-3);
}
</style>
