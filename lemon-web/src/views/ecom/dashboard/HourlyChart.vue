<template>
  <a-card :bordered="false" class="page-card" :title="$t('ecomFix.dash.hourly')">
    <template #extra>
      <a-space>
        <a-radio-group v-model:value="metric" size="small" button-style="solid">
          <a-radio-button value="sales">{{ $t('ecomFix.dash.sales') }}</a-radio-button>
          <a-radio-button value="orders">{{ $t('ecomFix.dash.orders') }}</a-radio-button>
        </a-radio-group>
        <a-radio-group v-model:value="mode" size="small">
          <a-radio-button value="hour">{{ $t('ecomFix.dash.byHour') }}</a-radio-button>
          <a-radio-button value="cum">{{ $t('ecomFix.dash.cumulative') }}</a-radio-button>
        </a-radio-group>
      </a-space>
    </template>
    <div class="summary sub-text">
      {{ $t('ecomFix.dash.hourlySummary', { hour: data.currentHour ?? '-', today: fmt(sum(data.today)), yesterday: fmt(sum(sameHours)) }) }}
    </div>
    <EChart v-if="hasData" :option="option" height="300px" />
    <a-empty v-else :image="simpleImage" :description="$t('ecomDat.dash.noData')" class="empty" />
  </a-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Empty } from 'ant-design-vue'
import EChart from '@/components/charts/EChart.vue'
import { shortMoney } from '@/components/charts/charts'
import { yuan } from '@/components/ecom/ecomReport'
import { nz, qty } from '@/utils/number'
import { t } from '@/locales'

/**
 * 实时销量：今日每小时（柱）与昨日（折线）对比，可切换按小时 / 累计、销售额 / 订单数；data 为 /ecom/dashboard/hourly 的结果
 */
const props = defineProps({ data: { type: Object, default: () => ({ today: [], yesterday: [] }) } })
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const hasData = computed(() => [...(props.data.today || []), ...(props.data.yesterday || [])].some((p) => nz(p.orders) || nz(p.sales)))
const metric = ref('sales')
const mode = ref('cum')

const values = (list) => (list || []).map((p) => nz(p[metric.value]))
const cum = (arr) => arr.reduce((acc, v, i) => [...acc, (i ? acc[i - 1] : 0) + v], [])
const sum = (list) => values(list).reduce((a, b) => a + b, 0)
const sameHours = computed(() => (props.data.yesterday || []).slice(0, (props.data.today || []).length))
const fmt = (v) => (metric.value === 'sales' ? yuan(v) : qty(v))

const option = computed(() => {
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)
  let today = values(props.data.today)
  let yesterday = values(props.data.yesterday)
  if (mode.value === 'cum') {
    today = cum(today)
    yesterday = cum(yesterday)
  }
  const tip = metric.value === 'sales' ? { valueFormatter: (v) => (v == null ? '-' : yuan(v)) } : {}
  return {
    tooltip: { trigger: 'axis', ...tip },
    legend: { top: 0 },
    grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value', axisLabel: metric.value === 'sales' ? { formatter: (v) => shortMoney(v) } : undefined, minInterval: metric.value === 'sales' ? 0 : 1 },
    series: [
      mode.value === 'cum'
        ? { name: t('ecomFix.dash.today'), type: 'line', smooth: true, symbol: 'circle', symbolSize: 6, areaStyle: { opacity: 0.12 }, itemStyle: { color: '#1677ff' }, data: today }
        : { name: t('ecomFix.dash.today'), type: 'bar', barMaxWidth: 16, itemStyle: { color: '#1677ff', borderRadius: [3, 3, 0, 0] }, data: today },
      { name: t('ecomFix.dash.yesterday'), type: 'line', smooth: true, showSymbol: false, lineStyle: { type: 'dashed' }, itemStyle: { color: '#faad14' }, data: yesterday }
    ]
  }
})
</script>

<style scoped>
.empty {
  padding: 96px 0;
}
.summary {
  margin-bottom: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
