<template>
  <a-row :gutter="[16, 16]" class="rank-row">
    <a-col :xs="24" :xl="10">
      <a-card :bordered="false" class="page-card" :title="$t('ecomFix.dash.topSku')">
        <template #extra>
          <a-radio-group v-model:value="skuBy" size="small" button-style="solid" @change="loadSku">
            <a-radio-button v-for="b in SKU_BY" :key="b" :value="b">{{ $t(`ecomFix.dash.by.${b}`) }}</a-radio-button>
          </a-radio-group>
        </template>
        <a-table row-key="key" size="small" :columns="skuColumns" :data-source="skuRows" :loading="loadingSku" :pagination="false" table-layout="fixed">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'rank'"><span class="rank" :class="`r${index + 1}`">{{ index + 1 }}</span></template>
            <template v-else-if="column.key === 'sku'">
              <div class="cell-ellipsis" :title="record.title">{{ record.sellerSku }}</div>
              <div class="sub-text cell-ellipsis">{{ record.storeName }} · {{ record.title }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'sales'">{{ shortMoney(record.sales) }}</template>
            <template v-else-if="column.key === 'profit'">
              <span :class="{ neg: nz(record.profit) < 0 }">{{ shortMoney(record.profit) }}</span>
              <div class="sub-text">{{ percent(record.margin) }}</div>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-col>
    <a-col :xs="24" :xl="8">
      <a-card :bordered="false" class="page-card" :title="$t('ecomFix.dash.storeRank')">
        <EChart v-if="storeRows.length" :option="storeOption" :height="`${Math.max(300, storeRows.slice(0, 10).length * 34 + 40)}px`" />
        <a-empty v-else :image="simpleImage" :description="$t('ecomDat.dash.noData')" class="empty" />
      </a-card>
    </a-col>
    <a-col :xs="24" :xl="6">
      <a-card :bordered="false" class="page-card" :title="$t('ecomFix.dash.platformShare')">
        <template v-if="platformRows.length">
          <EChart :option="platformOption" height="220px" />
          <div v-for="(p, i) in platformRows" :key="p.platform" class="platform-line">
            <span class="dot" :style="{ background: PALETTE[i % PALETTE.length] }" />
            <DictTag type="ecom_platform" :value="p.platform" />
            <span class="platform-share">{{ percent(p.percent) }}</span>
            <span class="sub-text platform-margin" :class="{ neg: nz(p.margin) < 0 }">{{ $t('ecomFix.dash.margin') }} {{ percent(p.margin) }}</span>
          </div>
        </template>
        <a-empty v-else :image="simpleImage" :description="$t('ecomDat.dash.noData')" class="empty" />
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import EChart from '@/components/charts/EChart.vue'
import DictTag from '@/components/DictTag.vue'
import { percent, shortMoney } from '@/components/charts/charts'
import { yuan } from '@/components/ecom/ecomReport'
import { dashboardPlatformShare, dashboardStoreRank, dashboardTopSku } from '@/api/ecom/dashboard'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { nz, qty } from '@/utils/number'
import { useAppStore } from '@/store/app'
import { t } from '@/locales'

/**
 * 排行：Top 10 SKU（按销量 / 销售额 / 利润，利润扣除广告花费）、店铺排行（销售额与利润率）、平台占比（销售额）。
 * params 为 { beginDate, endDate, storeId }（店铺排行与平台占比不按店铺筛选）
 */
const props = defineProps({ params: { type: Object, required: true } })
const dict = useDict('ecom_platform')
const app = useAppStore()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const SKU_BY = ['QTY', 'SALES', 'PROFIT']
const skuBy = ref('SALES')
const skuRows = ref([])
const storeRows = ref([])
const platformRows = ref([])
const loadingSku = ref(false)

const skuColumns = [
  { key: 'rank', width: 40, align: 'center' },
  { title: t('ecomFix.dash.sku'), key: 'sku' },
  { title: t('ecomFix.dash.units'), key: 'qty', width: 64, align: 'right' },
  { title: t('ecomFix.dash.sales'), key: 'sales', width: 92, align: 'right' },
  { title: t('ecomFix.dash.profit'), key: 'profit', width: 92, align: 'right' }
]

async function loadSku() {
  loadingSku.value = true
  try {
    const rows = (await dashboardTopSku({ ...props.params, by: skuBy.value, limit: 10 })) || []
    skuRows.value = rows.map((r) => ({ ...r, key: `${r.storeId}|${r.sellerSku}` }))
  } finally {
    loadingSku.value = false
  }
}

async function loadRanks() {
  const p = { beginDate: props.params.beginDate, endDate: props.params.endDate }
  const [stores, platforms] = await Promise.all([dashboardStoreRank(p), dashboardPlatformShare(p)])
  storeRows.value = stores || []
  platformRows.value = (platforms || []).filter((r) => nz(r.sales) > 0)
}

function load() {
  loadSku()
  loadRanks()
}
watch(() => props.params, load, { immediate: true, deep: true })
defineExpose({ load })

const storeOption = computed(() => {
  const top = storeRows.value.slice(0, 10).reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (items) => {
        const r = top[items[0].dataIndex]
        return `${r.storeName}<br/>${t('ecomFix.dash.sales')}: ${yuan(r.sales)}<br/>${t('ecomFix.dash.profit')}: ${yuan(r.profit)}<br/>${t('ecomFix.dash.margin')}: ${percent(r.margin)}`
      }
    },
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', splitNumber: 3, axisLabel: { formatter: (v) => shortMoney(v) } },
    yAxis: { type: 'category', data: top.map((r) => r.storeName), axisLabel: { width: 110, overflow: 'truncate' } },
    series: [
      {
        name: t('ecomFix.dash.sales'),
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { borderRadius: [0, 4, 4, 0], color: (p) => (nz(top[p.dataIndex].margin) < 0 ? '#ff7875' : '#4096ff') },
        label: { show: true, position: 'right', formatter: (p) => percent(top[p.dataIndex].margin), fontSize: 11, color: app.dark ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)', textBorderWidth: 0 },
        data: top.map((r) => nz(r.sales))
      }
    ]
  }
})

const platformLabel = (v) => findDict(dict.ecom_platform, v)?.label || v
/** 与 EChart 组件的默认配色一致，图例圆点与扇区颜色对应 */
const PALETTE = ['#1677ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#2f54eb', '#a0d911']
const platformOption = computed(() => {
  const total = platformRows.value.reduce((s, r) => s + nz(r.sales), 0)
  return {
    tooltip: { trigger: 'item', formatter: (p) => `${p.marker}${p.name}<br/>${yuan(p.value)} (${p.percent}%)` },
    title: {
      text: shortMoney(total),
      subtext: t('ecomFix.dash.sales'),
      left: 'center',
      top: '38%',
      itemGap: 4,
      textStyle: { fontSize: 16, fontWeight: 600, color: app.dark ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.85)' },
      subtextStyle: { fontSize: 12, color: app.dark ? 'rgba(255,255,255,0.45)' : 'rgba(0,0,0,0.45)' }
    },
    series: [
      {
        type: 'pie',
        radius: ['58%', '82%'],
        itemStyle: { borderRadius: 4, borderColor: 'transparent', borderWidth: 2 },
        label: { show: false },
        data: platformRows.value.map((r) => ({ name: platformLabel(r.platform), value: nz(r.sales) }))
      }
    ]
  }
})
</script>

<style scoped>
.rank-row > .ant-col > .ant-card {
  height: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank {
  display: inline-block;
  width: 20px;
  height: 20px;
  line-height: 20px;
  border-radius: 50%;
  font-size: 12px;
  background: var(--app-bg);
  color: var(--app-text-3);
}
.rank.r1 {
  background: #ff4d4f;
  color: #fff;
}
.rank.r2 {
  background: #fa8c16;
  color: #fff;
}
.rank.r3 {
  background: #fadb14;
  color: #fff;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.empty {
  padding: 80px 0;
}
.platform-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}
.platform-share {
  margin-left: auto;
  font-weight: 600;
}
.platform-margin {
  min-width: 84px;
  text-align: right;
  white-space: nowrap;
}
</style>
