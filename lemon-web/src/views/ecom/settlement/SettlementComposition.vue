<template>
  <div class="composition">
    <div class="chart-box">
      <div class="box-title">{{ $t('ecomDat.settlement.waterfall') }}</div>
      <EChart v-if="hasData" :option="option" height="280px" />
      <a-empty v-else :image="simpleImage" class="empty" />
    </div>
    <div class="list-box">
      <div class="box-title">
        {{ $t('ecomDat.settlement.breakdown') }}
        <span class="sub-text">{{ $t('ecomDat.settlement.ofSales') }}</span>
      </div>
      <div class="line income">
        <span class="line-label">{{ $t('ecomReport.settlement.salesAmount') }}</span>
        <span class="line-value">{{ money(settlement.salesAmount) }} <span class="sub-text">{{ settlement.currency }}</span></span>
      </div>
      <div v-for="f in fees" :key="f.key" class="line">
        <span class="line-label">{{ $t(`ecomReport.settlement.${f.key}`) }}</span>
        <span class="line-bar"><span :class="nz(f.value) < 0 ? 'minus' : 'plus'" :style="{ width: `${barWidth(f.value)}%` }" /></span>
        <span class="line-share sub-text">{{ f.share == null ? '-' : percent(Math.abs(f.share)) }}</span>
        <span class="line-value" :class="{ neg: nz(f.value) < 0 }">{{ money(f.value) }}</span>
      </div>
      <div class="line net">
        <span class="line-label">{{ $t('ecomReport.settlement.netAmount') }}</span>
        <span class="line-share sub-text">{{ netShare == null ? '' : percent(netShare) }}</span>
        <span class="line-value" :class="{ neg: nz(settlement.netAmount) < 0 }">{{ money(settlement.netAmount) }} <span class="sub-text">{{ settlement.currency }}</span></span>
      </div>
      <div class="line base">
        <span class="line-label">{{ $t('ecomReport.settlement.netBase') }}</span>
        <span class="sub-text">× {{ qty(settlement.exchangeRate, 6) }}</span>
        <span class="line-value">¥{{ money(settlement.netBase) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import EChart from '@/components/charts/EChart.vue'
import { percent, shortMoney } from '@/components/charts/charts'
import { useAppStore } from '@/store/app'
import { money, nz, qty } from '@/utils/number'
import { t } from '@/locales'

/**
 * 结算单费用构成：瀑布图（订单收入 → 各项费用 / 调整 → 结算净额）与费用明细（各项占订单收入的比例），金额为店铺币种。
 * 为 0 的费用项不在瀑布图中显示；明细的条形长度以最大一项费用为满格
 */
const props = defineProps({ settlement: { type: Object, required: true } })
const app = useAppStore()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const FEE_KEYS = ['refundAmount', 'commissionFee', 'fulfillmentFee', 'storageFee', 'adFee', 'otherFee', 'adjustment']

const sales = computed(() => nz(props.settlement.salesAmount))
const share = (v) => (sales.value ? (nz(v) / sales.value) * 100 : null)
const fees = computed(() => FEE_KEYS.map((key) => ({ key, value: props.settlement[key], share: share(props.settlement[key]) })))
const netShare = computed(() => share(props.settlement.netAmount))
/** 条形长度按最大的一项费用（绝对值）为满格，比例文字仍是占订单收入的百分比 */
const maxFee = computed(() => Math.max(0, ...FEE_KEYS.map((k) => Math.abs(nz(props.settlement[k])))))
const barWidth = (v) => (maxFee.value ? (Math.abs(nz(v)) / maxFee.value) * 100 : 0)
const hasData = computed(() => sales.value !== 0 || fees.value.some((f) => nz(f.value) !== 0))

const option = computed(() => {
  const s = props.settlement
  const steps = [
    { name: t('ecomReport.settlement.salesAmount'), value: sales.value, total: true },
    ...FEE_KEYS.filter((k) => nz(s[k]) !== 0).map((k) => ({ name: t(`ecomReport.settlement.${k}`), value: nz(s[k]) })),
    { name: t('ecomReport.settlement.netAmount'), value: nz(s.netAmount), total: true }
  ]
  // 透明的底座 + 可见的变动量叠加出瀑布效果
  let running = 0
  const base = []
  const delta = []
  steps.forEach((st) => {
    if (st.total) {
      base.push(Math.min(0, st.value))
      delta.push({ value: Math.abs(st.value), raw: st.value, itemStyle: { color: st.value < 0 ? '#ff4d4f' : '#1677ff' } })
      running = st.value
    } else {
      const end = running + st.value
      base.push(Math.min(running, end))
      delta.push({ value: Math.abs(st.value), raw: st.value, itemStyle: { color: st.value < 0 ? '#ff7875' : '#52c41a' } })
      running = end
    }
  })
  const text = app.dark ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)'
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (items) => {
        const d = items.find((i) => i.seriesIndex === 1)
        return `${d.name}<br/>${money(d.data.raw)} ${s.currency || ''}`
      }
    },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: steps.map((st) => st.name), axisLabel: { interval: 0, fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => shortMoney(v) } },
    series: [
      { type: 'bar', stack: 'w', silent: true, itemStyle: { color: 'transparent' }, emphasis: { disabled: true }, data: base },
      {
        type: 'bar',
        stack: 'w',
        barMaxWidth: 36,
        label: { show: true, position: 'top', fontSize: 11, color: text, textBorderWidth: 0, formatter: (p) => shortMoney(p.data.raw) },
        data: delta
      }
    ]
  }
})
</script>

<style scoped>
.composition {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(300px, 2fr);
  gap: 16px;
  margin-bottom: 20px;
}
@media (max-width: 1100px) {
  .composition {
    grid-template-columns: 1fr;
  }
}
.chart-box,
.list-box {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  min-width: 0;
}
.box-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: 400;
}
.line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
}
.line-label {
  width: 96px;
  flex: none;
  color: var(--app-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--app-hover);
  overflow: hidden;
}
.line-bar span {
  display: block;
  height: 100%;
  border-radius: 3px;
}
.line-bar .minus {
  background: #ff7875;
}
.line-bar .plus {
  background: #52c41a;
}
.line-share {
  width: 52px;
  text-align: right;
  flex: none;
}
.line-value {
  min-width: 110px;
  margin-left: auto;
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.line.income .line-value,
.line.net .line-value {
  font-weight: 600;
}
.line.net {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed var(--app-border);
}
.line.net .line-share {
  margin-left: auto;
}
.line.net .line-value {
  margin-left: 0;
  font-size: 16px;
  color: var(--app-primary);
}
.line.base .line-value {
  font-weight: 600;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.empty {
  padding: 80px 0;
}
</style>
